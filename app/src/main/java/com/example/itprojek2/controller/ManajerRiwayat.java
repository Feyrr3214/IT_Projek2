package com.example.itprojek2.controller;

import com.example.itprojek2.controller.AppLogger;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ManajerRiwayat — Mengelola log riwayat aktivitas penyiraman di Firebase.
 *
 * Node Firebase: devices/{deviceId}/history/{pushId}
 *   - message   : String  (deskripsi aktivitas)
 *   - timestamp : long    (epoch ms)
 *   - date      : String  (tanggal format dd/MM/yyyy HH:mm)
 *   - type      : String  (pump / auto / schedule / threshold / mode / offline)
 *
 * Maksimal 100 item terakhir yang ditampilkan.
 */
public class ManajerRiwayat {

    private static final String TAG          = "ManajerRiwayat";
    private static final int    BATAS_TAMPIL = 100;
    private static final long   MAKS_UMUR_MS = 7L * 24 * 60 * 60 * 1000; // 7 hari dalam ms

    private final DatabaseReference refHistory;
    private ValueEventListener listenerHistory;

    public interface RiwayatListener {
        void onLoaded(List<RiwayatItem> items);
        void onError(String pesan);
    }

    /**
     * Model data item riwayat dari Firebase.
     */
    public static class RiwayatItem {
        public String key;
        public String message;
        public long timestamp;
        public String date;
        public String type;

        public RiwayatItem() {} // Butuh untuk Firebase deserialization
    }

    public ManajerRiwayat(String deviceId) {
        refHistory = FirebaseDatabase.getInstance()
                .getReference("devices")
                .child(deviceId)
                .child("history");
    }

    /**
     * Simpan satu aktivitas ke Firebase history.
     *
     * @param message Deskripsi aktivitas
     * @param type    Tipe: "pump" | "auto" | "schedule" | "threshold" | "mode" | "offline"
     */
    public void simpan(String message, String type) {
        long now = System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(now));

        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("timestamp", now);
        data.put("date", dateStr);
        data.put("type", type);

        refHistory.push().setValue(data)
                .addOnSuccessListener(unused -> {
                    AppLogger.d(TAG, "Riwayat disimpan: " + message);
                    trimDataLama(); // Hapus otomatis data yang sudah terlalu lama
                })
                .addOnFailureListener(e -> AppLogger.e(TAG, "Gagal simpan riwayat: " + e.getMessage()));
    }

    /**
     * Hapus otomatis riwayat yang lebih dari 30 hari.
     * Dipanggil setiap kali ada data baru disimpan.
     */
    private void trimDataLama() {
        long batasWaktu = System.currentTimeMillis() - MAKS_UMUR_MS;
        refHistory.orderByChild("timestamp").endAt(batasWaktu)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        long jumlah = snapshot.getChildrenCount();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().removeValue();
                        }
                        if (jumlah > 0) {
                            AppLogger.d(TAG, "Auto-trim: " + jumlah + " riwayat lama (>30 hari) dihapus.");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        AppLogger.w(TAG, "Trim gagal: " + error.getMessage());
                    }
                });
    }

    /**
     * Mulai listen riwayat dari Firebase secara real-time.
     * Panggil stopListen() di onDestroyView().
     */
    public void mulaiListen(RiwayatListener listener) {
        stopListen();

        listenerHistory = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<RiwayatItem> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        RiwayatItem item = new RiwayatItem();
                        item.key = child.getKey();
                        item.message = ambilString(child, "message", "-");
                        item.timestamp = ambilLong(child, "timestamp", 0L);
                        item.date = ambilString(child, "date", "-");
                        item.type = ambilString(child, "type", "pump");
                        list.add(0, item); // Paling baru di atas
                    } catch (Exception e) {
                        AppLogger.w(TAG, "Gagal parse item: " + e.getMessage());
                    }
                }
                listener.onLoaded(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AppLogger.e(TAG, "Error listen history: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        };

        refHistory.limitToLast(BATAS_TAMPIL).addValueEventListener(listenerHistory);
    }

    /**
     * Hapus satu item riwayat dari Firebase berdasarkan push key.
     */
    public void hapus(String pushKey, Runnable onSelesai) {
        refHistory.child(pushKey).removeValue()
                .addOnSuccessListener(unused -> {
                    AppLogger.d(TAG, "Riwayat dihapus: " + pushKey);
                    if (onSelesai != null) onSelesai.run();
                })
                .addOnFailureListener(e -> AppLogger.e(TAG, "Gagal hapus: " + e.getMessage()));
    }

    /**
     * Hapus semua riwayat dari Firebase.
     */
    public void hapusSemua(Runnable onSelesai) {
        refHistory.removeValue()
                .addOnSuccessListener(unused -> {
                    AppLogger.d(TAG, "Semua riwayat dihapus.");
                    if (onSelesai != null) onSelesai.run();
                })
                .addOnFailureListener(e -> AppLogger.e(TAG, "Gagal hapus semua: " + e.getMessage()));
    }

    /**
     * Hapus item terpilih (List key) dari Firebase sekaligus.
     */
    public void hapusBeberapa(List<String> keys, Runnable onSelesai) {
        if (keys.isEmpty()) {
            if (onSelesai != null) onSelesai.run();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        for (String key : keys) {
            updates.put(key, null); // null = hapus
        }
        refHistory.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    AppLogger.d(TAG, keys.size() + " riwayat dihapus.");
                    if (onSelesai != null) onSelesai.run();
                })
                .addOnFailureListener(e -> AppLogger.e(TAG, "Gagal hapus beberapa: " + e.getMessage()));
    }

    /** Hentikan listener — panggil di onDestroyView() */
    public void stopListen() {
        if (listenerHistory != null) {
            refHistory.removeEventListener(listenerHistory);
            listenerHistory = null;
        }
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private String ambilString(DataSnapshot snap, String key, String def) {
        DataSnapshot c = snap.child(key);
        if (c.exists() && c.getValue() != null) return c.getValue().toString();
        return def;
    }

    private long ambilLong(DataSnapshot snap, String key, long def) {
        DataSnapshot c = snap.child(key);
        if (c.exists() && c.getValue() != null) {
            try {
                Object val = c.getValue();
                if (val instanceof Long) return (Long) val;
                if (val instanceof Integer) return ((Integer) val).longValue();
                if (val instanceof Double) return ((Double) val).longValue();
                return Long.parseLong(val.toString());
            } catch (Exception e) { return def; }
        }
        return def;
    }
}
