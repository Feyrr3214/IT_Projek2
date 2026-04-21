package com.example.itprojek2.controller;

import android.util.Log;
import androidx.annotation.Nullable;
import com.google.firebase.database.DatabaseReference;

/**
 * KontrolPompa — Mengelola kontrol pompa air via Firebase Realtime Database.
 *
 * Firebase node yang dikontrol:
 *   devices/{id}/control/manualPump   → boolean (nyalakan/matikan manual)
 *   devices/{id}/control/autoWatering → boolean (mode otomatis berbasis kelembaban)
 */
public class KontrolPompa {

    private static final String TAG = "KontrolPompa";
    private final DatabaseReference refKontrol;

    public KontrolPompa(DatabaseReference refKontrol) {
        this.refKontrol = refKontrol;
    }

    /**
     * Nyalakan pompa secara manual.
     * ESP32 akan mendeteksi dan langsung menyalakan pompa.
     */
    public void nyalakanPompa(@Nullable KallbackKontrol.PerintahListener listener) {
        refKontrol.child("manualPump").setValue(true)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Pompa manual DINYALAKAN");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal nyalakan pompa: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Matikan pompa yang sedang menyala.
     */
    public void matikanPompa(@Nullable KallbackKontrol.PerintahListener listener) {
        refKontrol.child("manualPump").setValue(false)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Pompa manual DIMATIKAN");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal matikan pompa: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Aktifkan atau nonaktifkan mode penyiraman otomatis berbasis kelembaban.
     *
     * @param aktif true = aktifkan, false = matikan
     */
    public void aturPenyiramanOtomatis(boolean aktif, @Nullable KallbackKontrol.PerintahListener listener) {
        refKontrol.child("autoWatering").setValue(aktif)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Penyiraman otomatis: " + (aktif ? "AKTIF" : "NONAKTIF"));
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal atur penyiraman otomatis: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }
}
