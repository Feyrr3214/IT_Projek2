package com.example.itprojek2.controller;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * KontrolKelembaban — Mengelola pengaturan batas kelembaban tanah.
 *
 * Firebase node yang dikelola:
 *   devices/{id}/control/threshold/
 *     minMoisture : int → pompa menyala jika kelembaban DI BAWAH nilai ini
 *     maxMoisture : int → pompa berhenti jika kelembaban MENCAPAI nilai ini
 *
 * Nilai default: min=30, max=70 (jika belum pernah diatur)
 */
public class KontrolKelembaban {

    private static final String TAG = "KontrolKelembaban";
    private final DatabaseReference refKontrol;

    public KontrolKelembaban(DatabaseReference refKontrol) {
        this.refKontrol = refKontrol;
    }

    /**
     * Simpan batas kelembaban ke Firebase.
     * ESP32 membaca nilai ini untuk mengontrol kapan pompa menyala/berhenti.
     *
     * @param nilaiMin Batas minimal (0–90): pompa nyala jika kelembaban di bawah nilai ini
     * @param nilaiMax Batas maksimal (10–100): pompa berhenti jika kelembaban mencapai nilai ini
     * @param listener Callback hasil operasi simpan
     */
    public void simpanBatas(int nilaiMin, int nilaiMax,
                            @Nullable KallbackKontrol.PerintahListener listener) {
        java.util.Map<String, Object> batas = new java.util.HashMap<>();
        batas.put("minMoisture", nilaiMin);
        batas.put("maxMoisture", nilaiMax);

        refKontrol.child("threshold").setValue(batas)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Batas kelembaban disimpan: min=" + nilaiMin + " max=" + nilaiMax);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal simpan batas kelembaban: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Baca batas kelembaban dari Firebase.
     * Jika belum pernah diatur, dikembalikan nilai default (30, 70).
     *
     * @param listener Callback yang menerima nilai min dan max
     */
    public void bacaBatas(KallbackKontrol.BatasKelembabanListener listener) {
        refKontrol.child("threshold").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int min = 30, max = 70;
                if (snapshot.exists()) {
                    min = ambilInt(snapshot, "minMoisture", 30);
                    max = ambilInt(snapshot, "maxMoisture", 70);
                }
                Log.d(TAG, "Batas kelembaban dibaca: min=" + min + " max=" + max);
                listener.onLoaded(min, max);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Gagal baca batas kelembaban: " + error.getMessage());
                listener.onLoaded(30, 70); // Fallback ke default
            }
        });
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    /**
     * Baca nilai integer dari DataSnapshot secara aman.
     * Firebase bisa return Integer atau Long tergantung versi SDK/device,
     * sehingga digunakan Number sebagai parent class.
     */
    private int ambilInt(DataSnapshot snapshot, String kunci, int def) {
        DataSnapshot child = snapshot.child(kunci);
        if (child.exists() && child.getValue() != null) {
            try {
                Object val = child.getValue();
                if (val instanceof Number) return ((Number) val).intValue();
            } catch (Exception e) {
                Log.w(TAG, "Gagal baca int untuk kunci '" + kunci + "': " + e.getMessage());
            }
        }
        return def;
    }
}
