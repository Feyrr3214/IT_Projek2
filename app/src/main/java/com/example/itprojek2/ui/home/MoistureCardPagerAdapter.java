package com.example.itprojek2.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itprojek2.R;
import com.example.itprojek2.controller.MoistureHistoryManager;

import java.util.List;

/**
 * MoistureCardPagerAdapter
 *
 * Adapter ViewPager2 untuk card kelembaban dengan 2 halaman:
 *   Halaman 0: Gauge kelembaban (lingkaran arc)
 *   Halaman 1: Grafik riwayat kelembaban dengan chip filter
 */
public class MoistureCardPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int PAGE_GAUGE = 0;
    private static final int PAGE_CHART = 1;
    private static final int PAGE_COUNT = 2;

    // Referensi ke view halaman yang sudah diinflate (untuk update data dari luar)
    private MoistureGaugeView gaugeView;
    private MoistureChartView chartView;

    // Callback saat chip filter diklik (diteruskan ke HomeFragment)
    public interface OnFilterSelectedListener {
        void onFilterSelected(MoistureHistoryManager.FilterMode mode);
    }

    private OnFilterSelectedListener filterListener;

    public MoistureCardPagerAdapter(OnFilterSelectedListener listener) {
        this.filterListener = listener;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────

    static class GaugeViewHolder extends RecyclerView.ViewHolder {
        MoistureGaugeView gaugeView;
        GaugeViewHolder(View v) {
            super(v);
            gaugeView = v.findViewById(R.id.moistureGaugeView);
        }
    }

    static class ChartViewHolder extends RecyclerView.ViewHolder {
        MoistureChartView chartView;
        TextView chipHariIni, chipKemarin, chipMingguIni, chipBulanIni, chipBulanKemarin;
        ChartViewHolder(View v) {
            super(v);
            chartView       = v.findViewById(R.id.moistureChartView);
            chipHariIni     = v.findViewById(R.id.chipHariIni);
            chipKemarin     = v.findViewById(R.id.chipKemarin);
            chipMingguIni   = v.findViewById(R.id.chipMingguIni);
            chipBulanIni    = v.findViewById(R.id.chipBulanIni);
            chipBulanKemarin= v.findViewById(R.id.chipBulanKemarin);
        }
    }

    // ── Adapter overrides ─────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) { return position; }

    @Override
    public int getItemCount() { return PAGE_COUNT; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == PAGE_GAUGE) {
            View v = inflater.inflate(R.layout.page_moisture_gauge, parent, false);
            return new GaugeViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.page_moisture_chart, parent, false);
            return new ChartViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof GaugeViewHolder) {
            gaugeView = ((GaugeViewHolder) holder).gaugeView;

        } else if (holder instanceof ChartViewHolder) {
            ChartViewHolder cvh = (ChartViewHolder) holder;
            chartView = cvh.chartView;

            // Setup chip listener
            setupChips(cvh);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Update nilai kelembaban pada gauge */
    public void setMoisturePercent(float percent) {
        if (gaugeView != null) gaugeView.setMoisturePercent(percent);
    }

    /** Ambil referensi MoistureGaugeView */
    public MoistureGaugeView getGaugeView() { return gaugeView; }

    /** Ambil referensi MoistureChartView */
    public MoistureChartView getChartView() { return chartView; }

    /** Update data grafik */
    public void setChartData(List<String> labels, List<Float> values) {
        if (chartView != null) chartView.setData(labels, values);
    }

    // ── Helper: setup chip filter ─────────────────────────────────────────

    private TextView activeChip = null;

    private void setupChips(ChartViewHolder cvh) {
        // Default aktif: Hari ini
        activeChip = cvh.chipHariIni;
        applyChipActive(cvh.chipHariIni);

        cvh.chipHariIni.setOnClickListener(v -> {
            setActiveChip(cvh, cvh.chipHariIni);
            if (filterListener != null)
                filterListener.onFilterSelected(MoistureHistoryManager.FilterMode.HARI_INI);
        });
        cvh.chipKemarin.setOnClickListener(v -> {
            setActiveChip(cvh, cvh.chipKemarin);
            if (filterListener != null)
                filterListener.onFilterSelected(MoistureHistoryManager.FilterMode.KEMARIN);
        });
        cvh.chipMingguIni.setOnClickListener(v -> {
            setActiveChip(cvh, cvh.chipMingguIni);
            if (filterListener != null)
                filterListener.onFilterSelected(MoistureHistoryManager.FilterMode.MINGGU_INI);
        });
        cvh.chipBulanIni.setOnClickListener(v -> {
            setActiveChip(cvh, cvh.chipBulanIni);
            if (filterListener != null)
                filterListener.onFilterSelected(MoistureHistoryManager.FilterMode.BULAN_INI);
        });
        cvh.chipBulanKemarin.setOnClickListener(v -> {
            setActiveChip(cvh, cvh.chipBulanKemarin);
            if (filterListener != null)
                filterListener.onFilterSelected(MoistureHistoryManager.FilterMode.BULAN_KEMARIN);
        });
    }

    private void setActiveChip(ChartViewHolder cvh, TextView selected) {
        // Reset semua chip
        TextView[] all = {
            cvh.chipHariIni, cvh.chipKemarin, cvh.chipMingguIni,
            cvh.chipBulanIni, cvh.chipBulanKemarin
        };
        for (TextView chip : all) applyChipInactive(chip);

        // Aktifkan yang dipilih
        applyChipActive(selected);
        activeChip = selected;
    }

    private void applyChipActive(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_chip_selected);
        chip.setTextColor(android.graphics.Color.parseColor("#796DCF")); // ungu
        chip.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void applyChipInactive(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_chip_unselected);
        chip.setTextColor(android.graphics.Color.parseColor("#CCE8E6FF")); // putih bening
        chip.setTypeface(null, android.graphics.Typeface.NORMAL);
    }
}
