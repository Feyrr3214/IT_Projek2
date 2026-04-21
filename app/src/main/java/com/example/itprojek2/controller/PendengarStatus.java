package com.example.itprojek2.controller;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * PendengarStatus — Mendengarkan perubahan status perangkat ESP32 secara real-time via Firebase.
 *
 * Firebase node yang didengarkan:
 *   devices/{id}/status/   → pompaMenyala, kelembaban, terakhirDisiram, durasiTerakhir, online, lastPing
 *   devices/{id}/control/  → penyiramanOtomatis (autoWatering)
 *
 * Fitur heartbeat: jika 20 detik tanpa update → ESP32 dianggap OFFLINE.
 * Fitur sinkronasi waktu: offset waktu HP vs server Firebase dikoreksi otomatis.
 */
public class PendengarStatus {

    private static final String TAG = "PendengarStatus";

    private final DatabaseReference refPerangkat; // Node level devices/{id}
    private ValueEventListener listenerFirebase;

    private android.os.Handler handlerHeartbeat;
    private Runnable runnableHeartbeat;
    private StatusPerangkat statusTerakhir;

    private long offsetWaktuServer = 0L;

    public PendengarStatus(DatabaseReference refPerangkat) {
        this.refPerangkat = refPerangkat;

        // Sinkronisasi offset waktu HP vs Server Firebase
        DatabaseReference refOffset = com.google.firebase.database.FirebaseDatabase
                .getInstance().getReference(".info/serverTimeOffset");
        refOffset.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    offsetWaktuServer = snapshot.getValue(Long.class);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Mulai mendengarkan status ESP32 secara real-time.
     * Panggil {@link #berhenti()} saat Fragment/Activity di-destroy untuk mencegah memory leak.
     *
     * @param listener Callback yang menerima update status
     */
    public void mulai(KallbackKontrol.StatusListener listener) {
        berhenti(); // Bersihkan listener lama jika ada

        handlerHeartbeat = new android.os.Handler(android.os.Looper.getMainLooper());
        runnableHeartbeat = () -> {
            // Timeout: 20 detik tanpa data = ESP32 OFFLINE
            if (statusTerakhir != null) {
                statusTerakhir.online = statusTerakhir.pumpRunning = false;
                listener.onStatusUpdate(statusTerakhir);
            }
        };

        listenerFirebase = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                DataSnapshot nodeStatus  = snapshot.child("status");
                DataSnapshot nodeKontrol = snapshot.child("control");

                StatusPerangkat status = new StatusPerangkat();

                // Baca dari node 'status'
                if (nodeStatus.exists()) {
                    status.pumpRunning  = status.pompaMenyala  = ambilBoolean(nodeStatus, "pumpRunning", false);
                    status.moisture     = status.kelembaban     = ambilInt(nodeStatus, "moisture", 0);
                    status.lastWatered  = status.terakhirDisiram = ambilString(nodeStatus, "lastWatered", "-");
                    status.lastDuration = status.durasiTerakhir  = ambilInt(nodeStatus, "lastDuration", 0);

                    boolean sedangOnline = ambilBoolean(nodeStatus, "online", false);
                    long lastPingRaw     = ambilLong(nodeStatus, "lastPing", 0L);

                    if (lastPingRaw > 0) {
                        // ESP32 bisa nulis lastPing dalam epoch DETIK atau MILIDETIK
                        // Epoch ms >= 10_000_000_000 (lebih dari 10 miliar)
                        long lastPingMs = (lastPingRaw < 10_000_000_000L)
                                ? lastPingRaw * 1000L  // Konversi detik → ms
                                : lastPingRaw;         // Sudah dalam ms

                        long sekarangMs  = System.currentTimeMillis() + offsetWaktuServer;
                        long selisihDetik = Math.abs(sekarangMs - lastPingMs) / 1000;
                        Log.d(TAG, "Selisih lastPing: " + selisihDetik + " detik");

                        // > 30 detik → anggap offline
                        sedangOnline = selisihDetik <= 30;

                    } else if (lastPingRaw == -1) {
                        // NTP di ESP32 belum sinkron → gunakan field "online"
                        Log.d(TAG, "lastPing=-1 (NTP belum sync), pakai field online: " + sedangOnline);
                    } else {
                        // lastPing = 0 → data hantu/lama → OFFLINE
                        sedangOnline = false;
                    }
                    status.online = sedangOnline;
                }

                // Baca dari node 'control'
                if (nodeKontrol.exists()) {
                    status.autoWatering = status.penyiramanOtomatis
                            = ambilBoolean(nodeKontrol, "autoWatering", false);
                }

                statusTerakhir = status;
                listener.onStatusUpdate(status);

                // Reset timer heartbeat
                if (handlerHeartbeat != null) {
                    handlerHeartbeat.removeCallbacks(runnableHeartbeat);
                    if (status.online) {
                        handlerHeartbeat.postDelayed(runnableHeartbeat, 20_000L);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error listener status: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        };

        refPerangkat.addValueEventListener(listenerFirebase);
    }

    /**
     * Hentikan listener real-time.
     * WAJIB dipanggil di onDestroyView()/onDestroy() agar tidak terjadi memory leak.
     */
    public void berhenti() {
        if (listenerFirebase != null) {
            refPerangkat.removeEventListener(listenerFirebase);
            listenerFirebase = null;
        }
        if (handlerHeartbeat != null) {
            handlerHeartbeat.removeCallbacks(runnableHeartbeat);
            handlerHeartbeat = null;
        }
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private boolean ambilBoolean(DataSnapshot snap, String kunci, boolean def) {
        DataSnapshot child = snap.child(kunci);
        if (child.exists() && child.getValue() != null) {
            try { return (Boolean) child.getValue(); } catch (Exception e) { return def; }
        }
        return def;
    }

    private int ambilInt(DataSnapshot snap, String kunci, int def) {
        DataSnapshot child = snap.child(kunci);
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

    private long ambilLong(DataSnapshot snap, String kunci, long def) {
        DataSnapshot child = snap.child(kunci);
        if (child.exists() && child.getValue() != null) {
            try {
                Object val = child.getValue();
                if (val instanceof Long)    return (Long) val;
                if (val instanceof Integer) return ((Integer) val).longValue();
                if (val instanceof Double)  return ((Double) val).longValue();
                return Long.parseLong(val.toString());
            } catch (Exception e) { return def; }
        }
        return def;
    }

    private String ambilString(DataSnapshot snap, String kunci, String def) {
        DataSnapshot child = snap.child(kunci);
        if (child.exists() && child.getValue() != null) return child.getValue().toString();
        return def;
    }
}
