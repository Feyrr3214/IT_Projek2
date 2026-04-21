package com.example.itprojek2.controller;

/**
 * KallbackKontrol — Kumpulan semua antarmuka (interface) callback yang dipakai
 * oleh seluruh kelas kontroler irigasi.
 *
 * Cara pakai di Fragment:
 * controller.saveMoistureThreshold(30, 70, new
 * KallbackKontrol.PerintahListener() { ... });
 * // atau dengan lambda:
 * controller.loadSchedule((h, m, dur, aktif) -> { ... });
 */
public class KallbackKontrol {

    /**
     * Callback umum saat mengirim perintah ke Firebase.
     * Dipakai oleh: pompa, LCD, kelembaban, jadwal, WiFi.
     */
    public interface PerintahListener {
        void onSuccess();

        void onFailure(String pesanError);
    }

    /**
     * Callback saat update status realtime dari ESP32 diterima.
     */
    public interface StatusListener {
        void onStatusUpdate(StatusPerangkat status);

        void onError(String pesanError);
    }

    /**
     * Callback saat batas kelembaban selesai dibaca dari Firebase.
     */
    public interface BatasKelembabanListener {
        void onLoaded(int minKelembaban, int maxKelembaban);
    }

    /**
     * Callback saat jadwal penyiraman selesai dibaca dari Firebase.
     */
    public interface JadwalListener {
        void onLoaded(int jam, int menit, int durasiDetik, boolean aktif);
    }
}
