package com.example.itprojek2.controller;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * MoistureHistoryManager
 *
 * Menyimpan dan mengambil riwayat kelembaban tanah dari Firebase.
 * Struktur Firebase:
 *   devices/{deviceId}/moisture_history/{timestamp_ms} = int (0-100)
 *
 * Mendukung filter:
 *   - HARI_INI   : data per jam untuk hari ini
 *   - KEMARIN    : data per jam untuk hari kemarin
 *   - MINGGU_INI : data rata-rata per hari untuk 7 hari terakhir
 *   - BULAN_INI  : data rata-rata per hari untuk bulan ini
 *   - BULAN_KEMARIN : data rata-rata per hari untuk bulan kemarin
 */
public class MoistureHistoryManager {

    public enum FilterMode {
        HARI_INI, KEMARIN, MINGGU_INI, BULAN_INI, BULAN_KEMARIN
    }

    public interface OnHistoryLoadedListener {
        /** labels: label sumbu X, values: nilai kelembaban (0-100), dapat null jika tidak ada data */
        void onLoaded(List<String> labels, List<Float> values);
    }

    private final DatabaseReference refHistory;
    private ValueEventListener activeListener;
    private DatabaseReference activeRef;

    public MoistureHistoryManager(String deviceId) {
        refHistory = FirebaseDatabase.getInstance()
                .getReference("devices").child(deviceId).child("moisture_history");
    }

    // ─── Simpan nilai kelembaban baru ──────────────────────────────────────

    /**
     * Simpan satu titik data kelembaban ke Firebase dengan timestamp sekarang.
     * Panggil setiap kali dapat update moisture dari ESP32.
     *
     * @param value nilai kelembaban (0-100)
     */
    public void simpanData(int value) {
        long timestamp = System.currentTimeMillis();
        refHistory.child(String.valueOf(timestamp)).setValue(value);
    }

    // ─── Query data ────────────────────────────────────────────────────────

    /**
     * Muat data riwayat sesuai filter yang dipilih, lalu panggil listener sekali.
     *
     * @param mode   filter periode
     * @param listener callback hasil query
     */
    public void muatData(FilterMode mode, OnHistoryLoadedListener listener) {
        // Hentikan listener lama jika ada
        stopListener();

        long[] range = hitungRange(mode);
        long startMs = range[0];
        long endMs   = range[1];

        activeRef = refHistory;
        activeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Kumpulkan semua data dalam rentang waktu
                Map<Long, List<Integer>> grouped = new LinkedHashMap<>();
                inisialisasiGroup(grouped, mode, startMs, endMs);

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        long ts = Long.parseLong(child.getKey());
                        if (ts < startMs || ts > endMs) continue;

                        Integer val = child.getValue(Integer.class);
                        if (val == null) continue;

                        long groupKey = hitungGroupKey(ts, mode);
                        if (grouped.containsKey(groupKey)) {
                            grouped.get(groupKey).add(val);
                        }
                    } catch (NumberFormatException ignored) {}
                }

                // Konversi ke label & nilai rata-rata
                List<String> labels = new ArrayList<>();
                List<Float>  values = new ArrayList<>();

                for (Map.Entry<Long, List<Integer>> entry : grouped.entrySet()) {
                    labels.add(buatLabel(entry.getKey(), mode));
                    List<Integer> dataList = entry.getValue();
                    if (dataList.isEmpty()) {
                        values.add(0f); // 0 berarti tidak ada data
                    } else {
                        float sum = 0;
                        for (int v : dataList) sum += v;
                        values.add(sum / dataList.size());
                    }
                }

                if (listener != null) listener.onLoaded(labels, values);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) listener.onLoaded(new ArrayList<>(), new ArrayList<>());
            }
        };

        activeRef.addListenerForSingleValueEvent(activeListener);
    }

    /** Hentikan listener aktif */
    public void stopListener() {
        if (activeRef != null && activeListener != null) {
            activeRef.removeEventListener(activeListener);
            activeListener = null;
            activeRef = null;
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    /** Hitung [startMs, endMs] berdasarkan mode filter */
    private long[] hitungRange(FilterMode mode) {
        Calendar cal = Calendar.getInstance();

        switch (mode) {
            case HARI_INI: {
                // Mulai tengah malam hari ini
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                return new long[]{start, System.currentTimeMillis()};
            }
            case KEMARIN: {
                cal.add(Calendar.DAY_OF_YEAR, -1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                long end   = start + 24 * 60 * 60 * 1000L - 1;
                return new long[]{start, end};
            }
            case MINGGU_INI: {
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long end = System.currentTimeMillis();
                cal.add(Calendar.DAY_OF_YEAR, -6);
                long start = cal.getTimeInMillis();
                return new long[]{start, end};
            }
            case BULAN_INI: {
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                return new long[]{start, System.currentTimeMillis()};
            }
            case BULAN_KEMARIN:
            default: {
                cal.add(Calendar.MONTH, -1);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                // Akhir bulan kemarin
                cal.add(Calendar.MONTH, 1);
                cal.add(Calendar.MILLISECOND, -1);
                long end = cal.getTimeInMillis();
                return new long[]{start, end};
            }
        }
    }

    /**
     * Inisialisasi Map grup dengan semua slot kosong (supaya grafik tidak loncat)
     * Key: "epoch per jam" untuk mode jam, atau "epoch per hari" untuk mode hari
     */
    private void inisialisasiGroup(Map<Long, List<Integer>> grouped, FilterMode mode,
                                   long startMs, long endMs) {
        Calendar cal = Calendar.getInstance();

        switch (mode) {
            case HARI_INI:
            case KEMARIN: {
                // 24 slot per jam
                cal.setTimeInMillis(startMs);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                for (int i = 0; i < 24; i++) {
                    grouped.put(cal.getTimeInMillis(), new ArrayList<>());
                    cal.add(Calendar.HOUR_OF_DAY, 1);
                }
                break;
            }
            case MINGGU_INI: {
                // 7 slot per hari
                cal.setTimeInMillis(startMs);
                zeroCal(cal);
                for (int i = 0; i < 7; i++) {
                    grouped.put(cal.getTimeInMillis(), new ArrayList<>());
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
            }
            case BULAN_INI: {
                // Per hari dari tanggal 1 sampai hari ini
                cal.setTimeInMillis(startMs);
                zeroCal(cal);
                Calendar calNow = Calendar.getInstance();
                zeroCal(calNow);
                while (!cal.after(calNow)) {
                    grouped.put(cal.getTimeInMillis(), new ArrayList<>());
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                break;
            }
            case BULAN_KEMARIN:
            default: {
                // Per hari dari tanggal 1 sampai akhir bulan kemarin
                cal.setTimeInMillis(startMs);
                zeroCal(cal);
                Calendar calEnd = Calendar.getInstance();
                calEnd.setTimeInMillis(endMs);
                zeroCal(calEnd);
                while (!cal.after(calEnd)) {
                    grouped.put(cal.getTimeInMillis(), new ArrayList<>());
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                break;
            }
        }
    }

    /** Hitung group key (epoch awal jam atau awal hari) dari sebuah timestamp */
    private long hitungGroupKey(long timestampMs, FilterMode mode) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMs);
        switch (mode) {
            case HARI_INI:
            case KEMARIN:
                // Bulatkan ke awal jam
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            default:
                // Bulatkan ke awal hari
                zeroCal(cal);
                break;
        }
        return cal.getTimeInMillis();
    }

    /** Buat label teks dari group key sesuai mode */
    private String buatLabel(long groupKey, FilterMode mode) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(groupKey);
        switch (mode) {
            case HARI_INI:
            case KEMARIN:
                return String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
            case MINGGU_INI: {
                String[] hari = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
                return hari[cal.get(Calendar.DAY_OF_WEEK) - 1];
            }
            default:
                return String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        }
    }

    /** Set jam/menit/detik/ms ke 0 pada Calendar */
    private void zeroCal(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
