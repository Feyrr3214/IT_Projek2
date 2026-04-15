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

    private android.os.Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private DeviceStatus latestStatus;
    
    private long serverTimeOffset = 0L;

    /**
     * Buat controller baru untuk device tertentu.
     *
     * @param deviceId ID perangkat ESP32 (contoh: "esp32_01")
     */
    public IrrigationController(String deviceId) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        controlRef = rootRef.child("devices").child(deviceId).child("control");
        statusRef = rootRef.child("devices").child(deviceId).child("status");

        // Selalu clear lcdMessage saat app start agar pesan lama/stale tidak terbawa
        // (Sebelumnya Android nulis pesan operasional ke lcdMessage, sekarang sudah tidak lagi)
        controlRef.child("lcdMessage").setValue("")
                .addOnSuccessListener(unused -> Log.d(TAG, "lcdMessage di-clear saat startup"))
                .addOnFailureListener(e -> Log.e(TAG, "Gagal clear lcdMessage: " + e.getMessage()));

        // Inisialisasi default values lainnya jika belum ada
        controlRef.child("manualPump").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    controlRef.child("manualPump").setValue(false);
                    controlRef.child("autoWatering").setValue(false);
                    Log.d(TAG, "Inisialisasi default control values");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Gagal cek data awal: " + error.getMessage());
            }
        });

        // Sinkronisasi waktu HP dengan Server Firebase biar nggak kena bug beda jam (clock drift)
        DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    serverTimeOffset = snapshot.getValue(Long.class);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ========================================
    // KONTROL POMPA
    // ========================================

    /**
     * Nyalakan pompa manual — notifikasi LCD ditangani langsung di ESP32
     */
    public void startManualPump(@Nullable OnCommandListener listener) {
        controlRef.child("manualPump").setValue(true)
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
     * Matikan pompa manual — notifikasi LCD ditangani langsung di ESP32
     */
    public void stopManualPump(@Nullable OnCommandListener listener) {
        controlRef.child("manualPump").setValue(false)
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
     * Atur mode penyiraman otomatis — notifikasi LCD ditangani langsung di ESP32
     */
    public void setAutoWatering(boolean enabled, @Nullable OnCommandListener listener) {
        controlRef.child("autoWatering").setValue(enabled)
                .addOnSuccessListener(unused -> {
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
     * Hapus pesan custom dari LCD — ESP32 akan kembali ke tampilan status normal.
     */
    public void clearLcdMessage() {
        controlRef.child("lcdMessage").setValue("")
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "LCD message dihapus"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Gagal hapus LCD message: " + e.getMessage()));
    }

    /**
     * Ganti kredensial WiFi perangkat (Remote WiFi Setup)
     */
    public void updateWifi(String ssid, String pass, @Nullable OnCommandListener listener) {
        java.util.Map<String, Object> wifiData = new java.util.HashMap<>();
        wifiData.put("ssid", ssid);
        wifiData.put("pass", pass);

        controlRef.child("newWifi").setValue(wifiData)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Permintaan ganti WiFi terkirim: " + ssid);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal kirim permintaan WiFi: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
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

        heartbeatHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        heartbeatRunnable = () -> {
            if (latestStatus != null) {
                latestStatus.online = false;
                listener.onStatusUpdate(latestStatus);
            }
        };

        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                DataSnapshot statusNode = snapshot.child("status");
                DataSnapshot controlNode = snapshot.child("control");

                DeviceStatus status = new DeviceStatus();

                // Status dari node 'status'
                if (statusNode.exists()) {
                    status.pumpRunning = getBooleanSafe(statusNode, "pumpRunning", false);
                    status.moisture = getIntSafe(statusNode, "moisture", 0);
                    status.lastWatered = getStringSafe(statusNode, "lastWatered", "-");
                    status.lastDuration = getIntSafe(statusNode, "lastDuration", 0);
                    
                    boolean isOnline = getBooleanSafe(statusNode, "online", false);
                    long lastPingRaw = getLongSafe(statusNode, "lastPing", 0L);

                    if (lastPingRaw > 0) {
                        // ESP32 bisa nulis lastPing dalam 2 format:
                        //  - Epoch DETIK   (dari NTP langsung, contoh: 1_700_000_000)
                        //  - Epoch MILIDETIK (dari Firebase.setTimestamp(), contoh: 1_700_000_000_000)
                        // Bedainnya: epoch milidetik >= 10_000_000_000 (> 10 milyar)
                        long lastPingMs;
                        if (lastPingRaw < 10_000_000_000L) {
                            lastPingMs = lastPingRaw * 1000L; // Konversi detik → ms
                        } else {
                            lastPingMs = lastPingRaw; // Sudah dalam ms
                        }

                        long currentMs = System.currentTimeMillis() + serverTimeOffset;
                        long diffSeconds = Math.abs(currentMs - lastPingMs) / 1000;
                        Log.d(TAG, "lastPing diff: " + diffSeconds + " detik");

                        // Jika selisih > 30 detik, anggap offline
                        if (diffSeconds > 30) {
                            isOnline = false;
                        } else {
                            isOnline = true;
                        }

                    } else if (lastPingRaw == -1) {
                        // NTP belum sync saat data dikirim, fallback ke field "online"
                        // isOnline sudah dibaca dari getBooleanSafe di atas → tetap pakai itu
                        Log.d(TAG, "lastPing = -1 (NTP belum sync), fallback ke field online: " + isOnline);

                    } else {
                        // lastPing = 0: data lama/hantu sebelum fix, anggap OFFLINE
                        isOnline = false;
                    }
                    status.online = isOnline;
                }

                // Status dari node 'control'
                if (controlNode.exists()) {
                    status.autoWatering = getBooleanSafe(controlNode, "autoWatering", true);
                }

                latestStatus = status;
                listener.onStatusUpdate(status);

                // Reset timeout heartbeat
                if (heartbeatHandler != null) {
                    heartbeatHandler.removeCallbacks(heartbeatRunnable);
                    if (status.online) {
                        // 20 detik tanpa data masuk dari ESP32 = OFFLINE
                        heartbeatHandler.postDelayed(heartbeatRunnable, 20000); 
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Status listener error: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        };

        // Listen ke level device untuk dapet control & status sekaligus
        statusRef.getParent().addValueEventListener(statusListener);
    }

    /**
     * Berhenti mendengarkan status.
     */
    public void stopListening() {
        if (statusListener != null) {
            statusRef.getParent().removeEventListener(statusListener);
            statusListener = null;
        }
        if (heartbeatHandler != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatHandler = null;
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

    private long getLongSafe(DataSnapshot snapshot, String key, long def) {
        DataSnapshot child = snapshot.child(key);
        if (child.exists() && child.getValue() != null) {
            try {
                if (child.getValue() instanceof Long) {
                    return (Long) child.getValue();
                } else if (child.getValue() instanceof Integer) {
                    return ((Integer) child.getValue()).longValue();
                } else if (child.getValue() instanceof Double) {
                    return ((Double) child.getValue()).longValue();
                } else {
                    return Long.parseLong(child.getValue().toString());
                }
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
        public boolean autoWatering = false; // Tambahan: status mode otomatis (Default: OFF)
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
