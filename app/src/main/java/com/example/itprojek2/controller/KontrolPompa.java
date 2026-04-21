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
     * Mengatur mode secara eksklusif.
     * Jika satu menyala, matikan yang lainnya.
     * Mode: "autoWatering", "scheduleMode", "manualPump"
     */
    public void setModeEksklusif(String modeName, boolean isEnabled, @Nullable KallbackKontrol.PerintahListener listener) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        
        if (isEnabled) {
            updates.put("autoWatering", modeName.equals("autoWatering"));
            updates.put("scheduleMode", modeName.equals("scheduleMode"));
            updates.put("manualPump", modeName.equals("manualPump"));
        } else {
            updates.put(modeName, false);
        }

        refKontrol.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Mode " + modeName + " = " + isEnabled);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal set mode: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /** Nyalakan pompa secara manual */
    public void nyalakanPompa(@Nullable KallbackKontrol.PerintahListener listener) {
        setModeEksklusif("manualPump", true, listener);
    }

    /** Matikan pompa manual */
    public void matikanPompa(@Nullable KallbackKontrol.PerintahListener listener) {
        setModeEksklusif("manualPump", false, listener);
    }

    /** Aktifkan/nonaktifkan mode otomatis */
    public void aturPenyiramanOtomatis(boolean aktif, @Nullable KallbackKontrol.PerintahListener listener) {
        setModeEksklusif("autoWatering", aktif, listener);
    }
    
    /** Aktifkan/nonaktifkan mode terjadwal */
    public void aturPenyiramanTerjadwal(boolean aktif, @Nullable KallbackKontrol.PerintahListener listener) {
        setModeEksklusif("scheduleMode", aktif, listener);
    }
}
