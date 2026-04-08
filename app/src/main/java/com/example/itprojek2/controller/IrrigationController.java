package com.example.itprojek2.controller;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * IrrigationController — Controller untuk mengirim perintah ke ESP32
 * melalui Firebase Realtime Database.
 *
 * Struktur Firebase:
 * devices/
 *   esp32_01/
 *     control/
 *       manualPump: boolean
 *       autoWatering: boolean
 *       lcdMessage: String (maks 16 karakter per baris, format "baris1|baris2")
 *     status/
 *       pumpRunning: boolean
 *       moisture: int (0-100)
 *       lastWatered: String
 *       lastDuration: int (detik)
 *       online: boolean
 */
public class IrrigationController {

    private static final String TAG = "IrrigationCtrl";
    private static final int LCD_MAX_CHARS = 16; // LCD 16x2

    private final DatabaseReference controlRef;
    private final DatabaseReference statusRef;
    private ValueEventListener statusListener;

    /**
     * Buat controller baru untuk device tertentu.
     *
     * @param deviceId ID perangkat ESP32 (contoh: "esp32_01")
     */
    public IrrigationController(String deviceId) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        controlRef = rootRef.child("devices").child(deviceId).child("control");
        statusRef = rootRef.child("devices").child(deviceId).child("status");

        // Inisialisasi default values jika belum ada
        controlRef.child("manualPump").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    controlRef.child("manualPump").setValue(false);
                    controlRef.child("autoWatering").setValue(true);
                    controlRef.child("lcdMessage").setValue("");
                    Log.d(TAG, "Inisialisasi default control values");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Gagal cek data awal: " + error.getMessage());
            }
        });
    }

    // ========================================
    // KONTROL POMPA
    // ========================================

    /**
     * Nyalakan pompa manual + kirim pesan ke LCD
     */
    public void startManualPump(@Nullable OnCommandListener listener) {
        controlRef.child("manualPump").setValue(true)
                .addOnSuccessListener(unused -> {
                    setLcdMessage("Pompa Manual", "Sedang Aktif...");
                    Log.d(TAG, "Pompa manual DINYALAKAN");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal nyalakan pompa: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Matikan pompa manual + kirim pesan ke LCD
     */
    public void stopManualPump(@Nullable OnCommandListener listener) {
        controlRef.child("manualPump").setValue(false)
                .addOnSuccessListener(unused -> {
                    setLcdMessage("Pompa Manual", "Dimatikan");
                    Log.d(TAG, "Pompa manual DIMATIKAN");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal matikan pompa: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Atur mode penyiraman otomatis
     */
    public void setAutoWatering(boolean enabled, @Nullable OnCommandListener listener) {
        controlRef.child("autoWatering").setValue(enabled)
                .addOnSuccessListener(unused -> {
                    if (enabled) {
                        setLcdMessage("Mode Otomatis", "Diaktifkan");
                    } else {
                        setLcdMessage("Mode Otomatis", "Dinonaktifkan");
                    }
                    Log.d(TAG, "Auto watering: " + (enabled ? "AKTIF" : "NONAKTIF"));
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal atur auto watering: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    // ========================================
    // KONTROL LCD
    // ========================================

    /**
     * Kirim pesan 2 baris ke LCD 16x2.
     * Setiap baris dibatasi 16 karakter. Format disimpan sebagai "baris1|baris2".
     *
     * @param line1 Teks baris pertama (maks 16 karakter)
     * @param line2 Teks baris kedua (maks 16 karakter)
     */
    public void setLcdMessage(String line1, String line2) {
        String safeL1 = truncateLcd(line1);
        String safeL2 = truncateLcd(line2);
        String combined = safeL1 + "|" + safeL2;

        controlRef.child("lcdMessage").setValue(combined)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "LCD message terkirim: " + combined))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Gagal kirim LCD message: " + e.getMessage()));
    }

    /**
     * Kirim pesan 1 baris ke LCD (baris kedua kosong).
     */
    public void setLcdMessage(String line1) {
        setLcdMessage(line1, "");
    }

    /**
     * Potong teks agar tidak melebihi 16 karakter LCD
     */
    private String truncateLcd(String text) {
        if (text == null) return "";
        return text.length() > LCD_MAX_CHARS ? text.substring(0, LCD_MAX_CHARS) : text;
    }

    // ========================================
    // LISTENER STATUS DARI ESP32
    // ========================================

    /**
     * Mulai mendengarkan perubahan status dari ESP32 secara realtime.
     */
    public void listenToStatus(OnStatusUpdateListener listener) {
        stopListening(); // Hapus listener lama kalau ada

        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                DeviceStatus status = new DeviceStatus();
                status.pumpRunning = getBooleanSafe(snapshot, "pumpRunning", false);
                status.moisture = getIntSafe(snapshot, "moisture", 0);
                status.lastWatered = getStringSafe(snapshot, "lastWatered", "-");
                status.lastDuration = getIntSafe(snapshot, "lastDuration", 0);
                status.online = getBooleanSafe(snapshot, "online", false);

                listener.onStatusUpdate(status);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Status listener error: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        };

        statusRef.addValueEventListener(statusListener);
    }

    /**
     * Berhenti mendengarkan status.
     */
    public void stopListening() {
        if (statusListener != null) {
            statusRef.removeEventListener(statusListener);
            statusListener = null;
        }
    }

    // ========================================
    // HELPER
    // ========================================

    private boolean getBooleanSafe(DataSnapshot snapshot, String key, boolean def) {
        DataSnapshot child = snapshot.child(key);
        if (child.exists() && child.getValue() != null) {
            try {
                return (Boolean) child.getValue();
            } catch (Exception e) {
                return def;
            }
        }
        return def;
    }

    private int getIntSafe(DataSnapshot snapshot, String key, int def) {
        DataSnapshot child = snapshot.child(key);
        if (child.exists() && child.getValue() != null) {
            try {
                return ((Long) child.getValue()).intValue();
            } catch (Exception e) {
                return def;
            }
        }
        return def;
    }

    private String getStringSafe(DataSnapshot snapshot, String key, String def) {
        DataSnapshot child = snapshot.child(key);
        if (child.exists() && child.getValue() != null) {
            return child.getValue().toString();
        }
        return def;
    }

    // ========================================
    // DATA CLASSES & INTERFACES
    // ========================================

    /**
     * Data status perangkat ESP32
     */
    public static class DeviceStatus {
        public boolean pumpRunning = false;
        public int moisture = 0;
        public String lastWatered = "-";
        public int lastDuration = 0;
        public boolean online = false;
    }

    /**
     * Callback saat mengirim perintah ke Firebase
     */
    public interface OnCommandListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * Callback saat menerima update status dari ESP32
     */
    public interface OnStatusUpdateListener {
        void onStatusUpdate(DeviceStatus status);
        void onError(String errorMessage);
    }
}
