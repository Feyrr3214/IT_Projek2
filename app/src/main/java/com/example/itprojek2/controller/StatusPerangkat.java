package com.example.itprojek2.controller;

/**
 * StatusPerangkat — Model data yang mewakili kondisi perangkat ESP32.
 *
 * Diisi dari Firebase Realtime Database:
 *   devices/{id}/status/   → pumpRunning, moisture, lastWatered, lastDuration, online
 *   devices/{id}/control/  → autoWatering
 */
public class StatusPerangkat {

    /** Apakah pompa sedang aktif/menyiram */
    public boolean pompaMenyala = false;

    /** Apakah mode penyiraman otomatis aktif */
    public boolean penyiramanOtomatis = false;

    /** Kelembaban tanah saat ini (0-100%) */
    public int kelembaban = 0;

    /** Waktu penyiraman terakhir dilakukan */
    public String terakhirDisiram = "-";

    /** Durasi penyiraman terakhir (dalam detik) */
    public int durasiTerakhir = 0;

    /** Apakah ESP32 sedang online */
    public boolean online = false;

    //
    // Alias field (agar IrrigationController.DeviceStatus tetap kompatibel)
    // sehingga Fragment yang sudah dibuat tidak perlu diubah.
    //
    /** @see #pompaMenyala */
    public boolean pumpRunning     = false;
    /** @see #penyiramanOtomatis */
    public boolean autoWatering    = false;
    /** @see #kelembaban */
    public int     moisture        = 0;
    /** @see #terakhirDisiram */
    public String  lastWatered     = "-";
    /** @see #durasiTerakhir */
    public int     lastDuration    = 0;
}
