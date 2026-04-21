package com.example.itprojek2.controller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.itprojek2.R;

/**
 * ManajerNotifikasi — Mengelola notifikasi sistem Android untuk kondisi kelembaban.
 *
 * Notifikasi yang dikirim:
 *   1. 🌵 TANAH KERING  → saat kelembaban < batas minimum
 *   2. 💧 TANAH TERLALU BASAH → saat kelembaban > batas maksimum
 *
 * Fitur anti-spam (cooldown):
 *   Notifikasi yang sama tidak akan dikirim lagi dalam 10 menit
 *   untuk mencegah banjir notif saat sensor terbaca berulang.
 *
 * Syarat tampil: Pastikan izin POST_NOTIFICATIONS sudah diberikan
 *   (otomatis diminta di HomeFragment untuk Android 13+).
 */
public class ManajerNotifikasi {

    private static final String TAG = "ManajerNotifikasi";

    // Identitas channel notifikasi (daftarkan sekali saat app pertama buka)
    public static final String ID_SALURAN = "saluran_kelembaban_irigasi";
    private static final String NAMA_SALURAN = "Peringatan Kelembaban Tanah";
    private static final String DESC_SALURAN = "Notifikasi saat tanah terlalu kering atau terlalu basah";

    // ID unik setiap notifikasi
    private static final int ID_NOTIF_KERING = 1001;
    private static final int ID_NOTIF_BASAH  = 1002;

    // Key SharedPreferences untuk menyimpan waktu notif terakhir (anti-spam)
    private static final String PREFS_NOTIF   = "NotifikasiKelembaban";
    private static final String KEY_KERING    = "waktu_notif_kering";
    private static final String KEY_BASAH     = "waktu_notif_basah";

    // Cooldown minimum antar notifikasi yang sama (1 menit = 60.000 ms)
    private static final long COOLDOWN_MS = 1 * 60 * 1000L;

    private final Context context;

    public ManajerNotifikasi(Context context) {
        this.context = context.getApplicationContext();
        buatSaluranNotifikasi();
    }

    /**
     * Daftarkan channel notifikasi ke sistem Android.
     * Diperlukan untuk Android 8.0 (Oreo/API 26) ke atas.
     * Aman dipanggil berulang kali — sistem akan mengabaikan jika sudah ada.
     */
    public void buatSaluranNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ID_SALURAN,
                    NAMA_SALURAN,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(DESC_SALURAN);
            channel.enableVibration(true);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Saluran notifikasi didaftarkan.");
            }
        }
    }

    /**
     * Periksa nilai kelembaban dan kirim notifikasi jika perlu.
     *
     * @param kelembaban  Nilai kelembaban saat ini dari sensor (0–100%)
     * @param batasMin    Batas minimum — notifikasi kering jika kelembaban di bawah ini
     * @param batasMax    Batas maksimum — notifikasi basah jika kelembaban di atas ini
     */
    public void cekDanKirimNotifikasi(int kelembaban, int batasMin, int batasMax) {
        if (kelembaban <= 0) return; // Abaikan jika data 0% (kemungkinan sensor belum siap/tercabut)

        if (kelembaban < batasMin) {
            kirimNotifikasiKering(kelembaban, batasMin);
        } else if (kelembaban > batasMax) {
            kirimNotifikasiBasah(kelembaban, batasMax);
        }
        // Jika dalam rentang normal, tidak ada notifikasi
    }

    /**
     * Kirim notifikasi peringatan tanah kering (kelembaban terlalu rendah).
     */
    private void kirimNotifikasiKering(int kelembaban, int batasMin) {
        if (!sudahCooldown(KEY_KERING)) {
            Log.d(TAG, "Notifikasi kering diabaikan (cooldown aktif).");
            return;
        }

        String judul = "🍇 Tanah Kering!";
        String isi   = "Kelembaban " + kelembaban + "% — di bawah batas minimum " + batasMin
                     + "%. Segera lakukan penyiraman!";

        kirim(ID_NOTIF_KERING, judul, isi, R.drawable.ic_water_drop,
              new int[]{255, 230, 115, 0}); // Warna LED: Kuning-oranye
        simpanWaktuNotif(KEY_KERING);
        Log.d(TAG, "✅ Notifikasi KERING dikirim: " + kelembaban + "%");
    }

    /**
     * Kirim notifikasi peringatan tanah terlalu basah (kelembaban terlalu tinggi).
     */
    private void kirimNotifikasiBasah(int kelembaban, int batasMax) {
        if (!sudahCooldown(KEY_BASAH)) {
            Log.d(TAG, "Notifikasi basah diabaikan (cooldown aktif).");
            return;
        }

        String judul = "🍇 Tanah Terlalu Basah!";
        String isi   = "Kelembaban " + kelembaban + "% — melebihi batas maksimum " + batasMax
                     + "%. Kurangi air atau periksa drainase.";

        kirim(ID_NOTIF_BASAH, judul, isi, R.drawable.ic_alert_triangle,
              new int[]{255, 0, 122, 255}); // Warna LED: Biru
        simpanWaktuNotif(KEY_BASAH);
        Log.d(TAG, "✅ Notifikasi BASAH dikirim: " + kelembaban + "%");
    }

    /**
     * Bangun dan tampilkan notifikasi ke sistem Android.
     *
     * @param notifId  ID unik notifikasi
     * @param judul    Judul notifikasi
     * @param isi      Isi teks notifikasi
     * @param ikonRes  Resource drawable untuk ikon kecil notif
     * @param warnArgb Array [alpha, red, green, blue] untuk warna LED notifikasi
     */
    private void kirim(int notifId, String judul, String isi, int ikonRes, int[] warnArgb) {
        try {
            // Intent untuk membuka aplikasi saat notifikasi ditekan
            Intent intentBuka = context.getPackageManager()
                    .getLaunchIntentForPackage(context.getPackageName());
            if (intentBuka != null) {
                intentBuka.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, notifId, intentBuka != null ? intentBuka : new Intent(),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ID_SALURAN)
                    .setSmallIcon(ikonRes)
                    .setContentTitle(judul)
                    .setContentText(isi)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(isi))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVibrate(new long[]{0, 300, 200, 300});

            // Warna LED notifikasi (warnArgb: [alpha, r, g, b])
            if (warnArgb != null && warnArgb.length == 4) {
                builder.setColor(android.graphics.Color.argb(
                        warnArgb[0], warnArgb[1], warnArgb[2], warnArgb[3]));
                builder.setColorized(true);
            }

            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);

            // Cek izin (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context,
                        android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Izin POST_NOTIFICATIONS belum diberikan, notifikasi tidak dapat ditampilkan.");
                    return;
                }
            }

            managerCompat.notify(notifId, builder.build());

        } catch (Exception e) {
            Log.e(TAG, "Gagal menampilkan notifikasi: " + e.getMessage());
        }
    }

    /**
     * Cek apakah cooldown untuk notifikasi tertentu sudah lewat.
     *
     * @param kunciPrefs  Key SharedPreferences untuk menyimpan waktu terakhir notif
     * @return true jika boleh kirim notifikasi (cooldown sudah lewat), false jika belum
     */
    private boolean sudahCooldown(String kunciPrefs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE);
        long waktuTerakhir = prefs.getLong(kunciPrefs, 0L);
        long sekarang = System.currentTimeMillis();
        return (sekarang - waktuTerakhir) >= COOLDOWN_MS;
    }

    /**
     * Simpan waktu notifikasi terakhir dikirim ke SharedPreferences.
     */
    private void simpanWaktuNotif(String kunciPrefs) {
        context.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                .edit()
                .putLong(kunciPrefs, System.currentTimeMillis())
                .apply();
    }

    /**
     * Reset cooldown semua notifikasi (berguna saat debugging/testing).
     */
    public void resetCooldown() {
        context.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_KERING)
                .remove(KEY_BASAH)
                .apply();
        Log.d(TAG, "Cooldown notifikasi direset.");
    }
}
