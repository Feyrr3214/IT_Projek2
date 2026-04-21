package com.example.itprojek2.controller;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * KontrolJadwal — Mengelola jadwal penyiraman otomatis harian.
 *
 * Firebase node yang dikelola:
 *   devices/{id}/control/schedule/
 *     aktif    : boolean → apakah jadwal sedang aktif
 *     jam      : int     → jam penyiraman (0–23, format 24 jam)
 *     menit    : int     → menit penyiraman (0–59)
 *     durasi   : int     → durasi pompa menyala dalam detik
 *     waktu    : String  → representasi "HH:mm" untuk Firebase Console
 *
 * CATATAN: Firmware ESP32 harus membaca node ini dan menyalakan pompa
 * saat jam sistem sesuai dengan nilai jam:menit yang tersimpan.
 */
public class KontrolJadwal {

    private static final String TAG = "KontrolJadwal";
    private final DatabaseReference refKontrol;

    public KontrolJadwal(DatabaseReference refKontrol) {
        this.refKontrol = refKontrol;
    }

    /**
     * Simpan konfigurasi jadwal penyiraman ke Firebase.
     *
     * @param jam       Jam penyiraman (0–23)
     * @param menit     Menit penyiraman (0–59)
     * @param detik     Durasi pompa menyala dalam detik (disarankan 5–120 detik)
     * @param aktif     true = jadwal aktif, false = jadwal dimatikan
     * @param listener  Callback hasil operasi simpan
     */
    public void simpanJadwal(int jam, int menit, int detik, boolean aktif,
                             @Nullable KallbackKontrol.PerintahListener listener) {
        java.util.Map<String, Object> jadwal = new java.util.HashMap<>();
        jadwal.put("enabled",  aktif);
        jadwal.put("hour",     jam);
        jadwal.put("minute",   menit);
        jadwal.put("duration", detik);
        jadwal.put("time",     String.format(java.util.Locale.getDefault(), "%02d:%02d", jam, menit));

        refKontrol.child("schedule").setValue(jadwal)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Jadwal disimpan: " + jam + ":" + menit
                            + " durasi=" + detik + "s aktif=" + aktif);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal simpan jadwal: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Baca konfigurasi jadwal dari Firebase.
     * Default jika belum diatur: jam 06:00, durasi 10 detik, tidak aktif.
     *
     * @param listener Callback yang menerima data jadwal
     */
    public void bacaJadwal(KallbackKontrol.JadwalListener listener) {
        refKontrol.child("schedule").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int jam = 6, menit = 0, durasi = 10;
                boolean aktif = false;
                if (snapshot.exists()) {
                    jam    = ambilInt(snapshot, "hour",     6);
                    menit  = ambilInt(snapshot, "minute",   0);
                    durasi = ambilInt(snapshot, "duration", 10);
                    aktif  = ambilBoolean(snapshot, "enabled", false);
                }
                Log.d(TAG, "Jadwal dibaca: " + jam + ":" + menit
                        + " durasi=" + durasi + "s aktif=" + aktif);
                listener.onLoaded(jam, menit, durasi, aktif);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Gagal baca jadwal: " + error.getMessage());
                listener.onLoaded(6, 0, 10, false);
            }
        });
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private int ambilInt(DataSnapshot snapshot, String kunci, int def) {
        DataSnapshot child = snapshot.child(kunci);
        if (child.exists() && child.getValue() != null) {
            try {
                Object val = child.getValue();
                if (val instanceof Number) return ((Number) val).intValue();
            } catch (Exception e) {
                Log.w(TAG, "Gagal baca int kunci '" + kunci + "': " + e.getMessage());
            }
        }
        return def;
    }

    private boolean ambilBoolean(DataSnapshot snapshot, String kunci, boolean def) {
        DataSnapshot child = snapshot.child(kunci);
        if (child.exists() && child.getValue() != null) {
            try { return (Boolean) child.getValue(); } catch (Exception e) { return def; }
        }
        return def;
    }
}
