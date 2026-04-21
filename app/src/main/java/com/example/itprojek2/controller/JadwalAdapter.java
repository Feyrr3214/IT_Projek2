package com.example.itprojek2.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.itprojek2.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.List;

public class JadwalAdapter extends RecyclerView.Adapter<JadwalAdapter.JadwalViewHolder> {

    private List<ModelJadwal> listJadwal = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onToggle(ModelJadwal jadwal, boolean isChecked);
        void onDelete(ModelJadwal jadwal);
        void onEdit(ModelJadwal jadwal);
    }

    public JadwalAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<ModelJadwal> list) {
        this.listJadwal = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JadwalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_jadwal, parent, false);
        return new JadwalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JadwalViewHolder holder, int position) {
        ModelJadwal jadwal = listJadwal.get(position);

        holder.tvWaktu.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d",
                jadwal.getHour(), jadwal.getMinute()));
        holder.tvDurasi.setText("Durasi: " + jadwal.getDuration() + " detik");
        
        // Mencegah trigger listener saat bind data
        holder.switchAktif.setOnCheckedChangeListener(null);
        holder.switchAktif.setChecked(jadwal.isEnabled());
        holder.switchAktif.setOnCheckedChangeListener((btn, isChecked) -> {
            listener.onToggle(jadwal, isChecked);
        });

        holder.btnHapus.setOnClickListener(v -> listener.onDelete(jadwal));
        
        // Bisa klik card untuk edit
        holder.itemView.setOnClickListener(v -> listener.onEdit(jadwal));
    }

    @Override
    public int getItemCount() {
        return listJadwal.size();
    }

    static class JadwalViewHolder extends RecyclerView.ViewHolder {
        TextView tvWaktu, tvDurasi;
        SwitchMaterial switchAktif;
        ImageButton btnHapus;

        public JadwalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWaktu = itemView.findViewById(R.id.tvWaktuJadwal);
            tvDurasi = itemView.findViewById(R.id.tvDurasiJadwal);
            switchAktif = itemView.findViewById(R.id.switchJadwal);
            btnHapus = itemView.findViewById(R.id.btnHapusJadwal);
        }
    }
}
