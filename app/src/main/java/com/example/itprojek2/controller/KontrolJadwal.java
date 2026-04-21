package com.example.itprojek2.controller;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * KontrolJadwal — Mengelola jadwal penyiraman otomatis harian (Multiple Schedules).
 *
 * Firebase node yang dikelola:
 *   devices/{id}/control/schedules/{idJadwal}/
 */
public class KontrolJadwal {

    private static final String TAG = "KontrolJadwal";
    private final DatabaseReference refSchedules;
    private ValueEventListener listenerSchedules;

    public KontrolJadwal(DatabaseReference refKontrol) {
        // Node baru "schedules" (pakai 's') untuk multiple schedules
        this.refSchedules = refKontrol.child("schedules");
    }

    /** Tambah jadwal baru dengan ID (push key) otomatis */
    public void tambahJadwal(int jam, int menit, int detik, boolean aktif,
                             @Nullable KallbackKontrol.PerintahListener listener) {
        String newId = refSchedules.push().getKey();
        if (newId == null) {
            if (listener != null) listener.onFailure("Gagal membuat ID jadwal");
            return;
        }
        
        ModelJadwal jadwal = new ModelJadwal(newId, jam, menit, detik, aktif);
        refSchedules.child(newId).setValue(jadwal)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Jadwal ditambahkan: " + newId);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal tambah jadwal: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /** Update jadwal yang sudah ada berdasarkan ID */
    public void updateJadwal(String id, int jam, int menit, int detik, boolean aktif,
                             @Nullable KallbackKontrol.PerintahListener listener) {
        if (id == null || id.isEmpty()) return;
        
        ModelJadwal jadwal = new ModelJadwal(id, jam, menit, detik, aktif);
        refSchedules.child(id).setValue(jadwal)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Jadwal diupdate: " + id);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal update jadwal: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /** Hapus jadwal berdasarkan ID */
    public void hapusJadwal(String id, @Nullable KallbackKontrol.PerintahListener listener) {
        if (id == null || id.isEmpty()) return;
        
        refSchedules.child(id).removeValue()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Jadwal dihapus: " + id);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal hapus jadwal: " + e.getMessage());
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Mendengarkan perubahan daftar jadwal secara realtime.
     * Panggil stopListen() ketika Fragment/Activity hancur (onDestroyView).
     */
    public void listenDaftarJadwal(KallbackKontrol.DaftarJadwalListener listener) {
        stopListen(); // Hentikan yang lama jika ada
        
        listenerSchedules = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.List<ModelJadwal> list = new java.util.ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelJadwal j = ds.getValue(ModelJadwal.class);
                    if (j != null) {
                        list.add(j);
                    }
                }
                
                // Urutkan jadwal berdasarkan waktu (jam lalu menit)
                java.util.Collections.sort(list, (a, b) -> {
                    if (a.getHour() != b.getHour()) {
                        return Integer.compare(a.getHour(), b.getHour());
                    }
                    return Integer.compare(a.getMinute(), b.getMinute());
                });
                
                Log.d(TAG, "Berhasil memuat " + list.size() + " jadwal");
                listener.onLoaded(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Gagal melisten daftar jadwal: " + error.getMessage());
                listener.onLoaded(new java.util.ArrayList<>());
            }
        };
        
        refSchedules.addValueEventListener(listenerSchedules);
    }
    
    /** Menghentikan realtime listener jadwal */
    public void stopListen() {
        if (listenerSchedules != null) {
            refSchedules.removeEventListener(listenerSchedules);
            listenerSchedules = null;
        }
    }
}
