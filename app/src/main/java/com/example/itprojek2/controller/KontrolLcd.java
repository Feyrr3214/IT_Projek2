package com.example.itprojek2.controller;

import android.util.Log;
import com.google.firebase.database.DatabaseReference;

/**
 * KontrolLcd — Mengelola pesan teks yang ditampilkan di layar LCD 16x2 pada ESP32.
 *
 * Firebase node yang dikontrol:
 *   devices/{id}/control/lcdMessage → String (format: "baris1|baris2")
 *
 * Contoh: "Menyiram...|Mohon Tunggu"
 * Setiap baris dibatasi 16 karakter.
 */
public class KontrolLcd {

    private static final String TAG = "KontrolLcd";
    private static final int MAKS_KARAKTER_LCD = 16;

    private final DatabaseReference refKontrol;

    public KontrolLcd(DatabaseReference refKontrol) {
        this.refKontrol = refKontrol;
    }

    /**
     * Kirim pesan 2 baris ke LCD ESP32.
     * Setiap baris otomatis dipotong jika melebihi 16 karakter.
     *
     * @param baris1 Teks baris pertama
     * @param baris2 Teks baris kedua
     */
    public void kirimPesan(String baris1, String baris2) {
        String aman1 = potong(baris1);
        String aman2 = potong(baris2);
        String gabungan = aman1 + "|" + aman2;

        refKontrol.child("lcdMessage").setValue(gabungan)
                .addOnSuccessListener(unused -> Log.d(TAG, "Pesan LCD terkirim: " + gabungan))
                .addOnFailureListener(e -> Log.e(TAG, "Gagal kirim pesan LCD: " + e.getMessage()));
    }

    /**
     * Kirim pesan 1 baris ke LCD (baris kedua dikosongkan).
     *
     * @param baris1 Teks baris pertama
     */
    public void kirimPesan(String baris1) {
        kirimPesan(baris1, "");
    }

    /**
     * Hapus pesan custom dari LCD.
     * ESP32 akan kembali menampilkan status normal (kelembaban, dll).
     */
    public void hapusPesan() {
        refKontrol.child("lcdMessage").setValue("")
                .addOnSuccessListener(unused -> Log.d(TAG, "Pesan LCD dihapus"))
                .addOnFailureListener(e -> Log.e(TAG, "Gagal hapus pesan LCD: " + e.getMessage()));
    }

    /** Potong teks agar tidak melebihi batas karakter LCD */
    private String potong(String teks) {
        if (teks == null) return "";
        return teks.length() > MAKS_KARAKTER_LCD ? teks.substring(0, MAKS_KARAKTER_LCD) : teks;
    }
}
