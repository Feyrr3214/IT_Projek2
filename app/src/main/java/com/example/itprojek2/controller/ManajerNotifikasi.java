package com.example.itprojek2.controller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.itprojek2.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ManajerNotifikasi — Mengelola notifikasi sistem Android dan penyimpanan event ke Firebase.
 *
 * Notifikasi sistem Android:
 *   1. 🌵 TANAH KERING  → saat kelembaban < batas minimum
 *   2. 💧 TANAH TERLALU BASAH → saat kelembaban > batas maksimum
 *
 * Event Firebase (node: devices/{deviceId}/notifications/{pushId}):
 *   - Semua event di atas + pompa nyala/mati, perangkat online/offline
 *
 * Fitur anti-spam (cooldown):
 *   Notifikasi yang sama tidak akan dikirim lagi dalam 30 detik.
 */
public class ManajerNotifikasi {

    private static final String TAG = "ManajerNotifikasi";

    // Identitas channel notifikasi Android
    public static final String ID_SALURAN = "saluran_kelembaban_irigasi";
    private static final String NAMA_SALURAN = "Peringatan Kelembaban Tanah";
    private static final String DESC_SALURAN = "Notifikasi saat tanah terlalu kering atau terlalu basah";

    // ID unik setiap notifikasi sistem Android
    private static final int ID_NOTIF_KERING = 1001;
    private static final int ID_NOTIF_BASAH  = 1002;

    // Key SharedPreferences cooldown
    private static final String PREFS_NOTIF   = "NotifikasiKelembaban";
    private static final String KEY_KERING    = "waktu_notif_kering";
    private static final String KEY_BASAH     = "waktu_notif_basah";

    private static final long COOLDOWN_MS = 30 * 1000L;
    private static final int  BATAS_NOTIF = 100; // Maksimal notif disimpan di Firebase

    private final Context context;
    private DatabaseReference refNotifications; // nullable jika deviceId tidak disediakan

    // ─── Konstruktor ─────────────────────────────────────────────────────────

    /** Konstruktor tanpa Firebase (hanya notif sistem Android, backward compat) */
    public ManajerNotifikasi(Context context) {
        this.context = context.getApplicationContext();
        buatSaluranNotifikasi();
    }

    /** Konstruktor dengan Firebase — event disimpan ke Firebase juga */
    public ManajerNotifikasi(Context context, String deviceId) {
        this.context = context.getApplicationContext();
        buatSaluranNotifikasi();
        if (deviceId != null && !deviceId.isEmpty()) {
            refNotifications = FirebaseDatabase.getInstance()
                    .getReference("devices")
                    .child(deviceId)
                    .child("notifications");
        }
    }

    // ─── Setup Channel ────────────────────────────────────────────────────────

    public void buatSaluranNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ID_SALURAN, NAMA_SALURAN, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(DESC_SALURAN);
            channel.enableVibration(true);
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ─── Cek Kelembaban ───────────────────────────────────────────────────────

    /**
     * Periksa nilai kelembaban dan kirim notifikasi jika perlu.
     * Otomatis simpan ke Firebase jika deviceId sudah diset.
     */
    public void cekDanKirimNotifikasi(int kelembaban, int batasMin, int batasMax) {
        if (kelembaban <= 0) return;

        if (kelembaban < batasMin) {
            kirimNotifikasiKering(kelembaban, batasMin);
        } else if (kelembaban > batasMax) {
            kirimNotifikasiBasah(kelembaban, batasMax);
        }
    }

    private void kirimNotifikasiKering(int kelembaban, int batasMin) {
        if (!sudahCooldown(KEY_KERING)) return;

        String judul = "🍇 Tanah Kering!";
        String isi   = "Kelembaban " + kelembaban + "% — di bawah batas minimum " + batasMin
                     + "%. Segera lakukan penyiraman!";

        kirim(ID_NOTIF_KERING, judul, isi, R.drawable.ic_water_drop, new int[]{255, 230, 115, 0});
        simpanKeFirebase(judul, isi, "WARNING");
        simpanWaktuNotif(KEY_KERING);
        Log.d(TAG, "Notifikasi KERING dikirim: " + kelembaban + "%");
    }

    private void kirimNotifikasiBasah(int kelembaban, int batasMax) {
        if (!sudahCooldown(KEY_BASAH)) return;

        String judul = "💧 Tanah Terlalu Basah!";
        String isi   = "Kelembaban " + kelembaban + "% — melebihi batas maksimum " + batasMax
                     + "%. Kurangi air atau periksa drainase.";

        kirim(ID_NOTIF_BASAH, judul, isi, R.drawable.ic_alert_triangle, new int[]{255, 0, 122, 255});
        simpanKeFirebase(judul, isi, "CRITICAL");
        simpanWaktuNotif(KEY_BASAH);
        Log.d(TAG, "Notifikasi BASAH dikirim: " + kelembaban + "%");
    }

    // ─── Event Manual (dipanggil dari luar) ──────────────────────────────────

    /** Simpan event pompa nyala ke Firebase */
    public void eventPompaMenyala(int kelembaban, String mode) {
        String judul = "💧 Penyiraman Dimulai";
        String isi;
        switch (mode) {
            case "auto":
                isi = "Penyiraman otomatis aktif — kelembaban saat ini " + kelembaban + "%.";
                break;
            case "schedule":
                isi = "Penyiraman terjadwal dimulai — kelembaban saat ini " + kelembaban + "%.";
                break;
            default:
                isi = "Penyiraman manual dimulai — kelembaban saat ini " + kelembaban + "%.";
        }
        simpanKeFirebase(judul, isi, "INFO");
    }

    /** Simpan event pompa mati ke Firebase */
    public void eventPompaMati(int durasiDetik, int kelembabanAkhir) {
        String judul = "✅ Penyiraman Selesai";
        String isi   = "Penyiraman selesai, durasi " + durasiDetik + " detik. "
                     + "Kelembaban akhir " + kelembabanAkhir + "%.";
        simpanKeFirebase(judul, isi, "SUCCESS");
    }

    /** Simpan event perangkat offline ke Firebase */
    public void eventPerangkatOffline() {
        String judul = "⚠️ Perangkat Tidak Merespons";
        String isi   = "ESP32 tidak mengirim data lebih dari 30 detik. Periksa koneksi WiFi.";
        simpanKeFirebase(judul, isi, "CRITICAL");
    }

    /** Simpan event perangkat kembali online ke Firebase */
    public void eventPerangkatOnline() {
        String judul = "🟢 Perangkat Terhubung";
        String isi   = "Perangkat ESP32 kembali online dan siap digunakan.";
        simpanKeFirebase(judul, isi, "SUCCESS");
    }

    /** Simpan event perubahan batas kelembaban ke Firebase */
    public void eventBatasKelembapanDiubah(int min, int max) {
        String judul = "⚙️ Batas Kelembaban Diubah";
        String isi   = "Batas kelembaban diperbarui: Min " + min + "% — Max " + max + "%.";
        simpanKeFirebase(judul, isi, "INFO");
    }

    // ─── Baca Notifikasi dari Firebase ────────────────────────────────────────

    public interface NotifListener {
        void onLoaded(List<NotifItem> items);
        void onError(String pesan);
    }

    public static class NotifItem {
        public String key;
        public String title;
        public String message;
        public String type;   // "WARNING" | "CRITICAL" | "SUCCESS" | "INFO"
        public long timestamp;
        public String date;

        public NotifItem() {}
    }

    private ValueEventListener listenerFirebase;
    private Query refListenTarget;

    /**
     * Mulai listen notifikasi dari Firebase secara real-time.
     */
    public void mulaiListen(NotifListener listener) {
        if (refNotifications == null) {
            listener.onError("Device ID belum diset.");
            return;
        }
        stopListen();
        listenerFirebase = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NotifItem> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        NotifItem item = new NotifItem();
                        item.key       = child.getKey();
                        item.title     = ambilString(child, "title", "-");
                        item.message   = ambilString(child, "message", "-");
                        item.type      = ambilString(child, "type", "INFO");
                        item.timestamp = ambilLong(child, "timestamp", 0L);
                        item.date      = ambilString(child, "date", "-");
                        list.add(0, item); // Paling baru di atas
                    } catch (Exception e) {
                        Log.w(TAG, "Gagal parse notif: " + e.getMessage());
                    }
                }
                listener.onLoaded(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };
        refListenTarget = refNotifications.limitToLast(BATAS_NOTIF);
        refListenTarget.addValueEventListener(listenerFirebase);
    }

    /** Hentikan listener notifikasi */
    public void stopListen() {
        if (listenerFirebase != null && refListenTarget != null) {
            refListenTarget.removeEventListener(listenerFirebase);
            listenerFirebase = null;
        }
    }

    /** Hapus semua notifikasi dari Firebase */
    public void hapusSemua(Runnable onSelesai) {
        if (refNotifications == null) return;
        refNotifications.removeValue()
                .addOnSuccessListener(unused -> { if (onSelesai != null) onSelesai.run(); })
                .addOnFailureListener(e -> Log.e(TAG, "Gagal hapus semua notif: " + e.getMessage()));
    }

    /** Hapus satu notifikasi dari Firebase berdasarkan push key */
    public void hapus(String pushKey, Runnable onSelesai) {
        if (refNotifications == null || pushKey == null) return;
        refNotifications.child(pushKey).removeValue()
                .addOnSuccessListener(unused -> { if (onSelesai != null) onSelesai.run(); })
                .addOnFailureListener(e -> Log.e(TAG, "Gagal hapus notif: " + e.getMessage()));
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    /**
     * Simpan event notifikasi ke Firebase Realtime Database.
     */
    private void simpanKeFirebase(String judul, String isi, String tipe) {
        if (refNotifications == null) return;
        long now = System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(now));

        Map<String, Object> data = new HashMap<>();
        data.put("title", judul);
        data.put("message", isi);
        data.put("type", tipe);
        data.put("timestamp", now);
        data.put("date", dateStr);

        refNotifications.push().setValue(data)
                .addOnSuccessListener(unused -> Log.d(TAG, "Notif disimpan ke Firebase: " + judul))
                .addOnFailureListener(e -> Log.e(TAG, "Gagal simpan notif: " + e.getMessage()));
    }

    /**
     * Bangun dan tampilkan notifikasi sistem Android.
     */
    private void kirim(int notifId, String judul, String isi, int ikonRes, int[] warnArgb) {
        try {
            Intent intentBuka = context.getPackageManager()
                    .getLaunchIntentForPackage(context.getPackageName());
            if (intentBuka != null) {
                intentBuka.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, notifId, intentBuka != null ? intentBuka : new Intent(),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ID_SALURAN)
                    .setSmallIcon(ikonRes)
                    .setContentTitle(judul)
                    .setContentText(isi)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(isi))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVibrate(new long[]{0, 300, 200, 300});

            if (warnArgb != null && warnArgb.length == 4) {
                builder.setColor(android.graphics.Color.argb(
                        warnArgb[0], warnArgb[1], warnArgb[2], warnArgb[3]));
                builder.setColorized(true);
            }

            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context,
                        android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Izin POST_NOTIFICATIONS belum diberikan.");
                    return;
                }
            }

            managerCompat.notify(notifId, builder.build());

        } catch (Exception e) {
            Log.e(TAG, "Gagal tampilkan notifikasi: " + e.getMessage());
        }
    }

    private boolean sudahCooldown(String kunciPrefs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE);
        long waktuTerakhir = prefs.getLong(kunciPrefs, 0L);
        return (System.currentTimeMillis() - waktuTerakhir) >= COOLDOWN_MS;
    }

    private void simpanWaktuNotif(String kunciPrefs) {
        context.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                .edit().putLong(kunciPrefs, System.currentTimeMillis()).apply();
    }

    public void resetCooldown() {
        context.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                .edit().remove(KEY_KERING).remove(KEY_BASAH).apply();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
