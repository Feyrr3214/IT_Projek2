package com.example.itprojek2.ui.history;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.itprojek2.R;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryItem> items;
    private boolean selectionMode = false;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public HistoryAdapter(List<HistoryItem> items) {
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        holder.tvMessage.setText(item.getMessage());
        holder.tvDate.setText(item.getDate());

        // Warna dot berdasarkan tipe
        setDotColor(holder, item.getType());

        // Highlight jika dipilih
        if (selectionMode && item.isSelected()) {
            holder.card.setStrokeColor(0xFF7B6ED6);
            holder.card.setStrokeWidth(4);
            holder.card.setCardBackgroundColor(0xFFF3F0FF);
        } else {
            holder.card.setStrokeWidth(0);
            holder.card.setCardBackgroundColor(Color.WHITE);
        }

        holder.card.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(holder.getAdapterPosition());
        });
    }

    /**
     * Set warna dot indikator berdasarkan tipe aktivitas.
     * pump=biru, auto=hijau, schedule=ungu, threshold=oranye, mode=abu, offline=merah
     */
    private void setDotColor(ViewHolder holder, String type) {
        int color;
        if (type == null) type = "pump";
        switch (type) {
            case "auto":      color = 0xFF5ED5A8; break; // Hijau (otomatis)
            case "schedule":  color = 0xFF7B6ED6; break; // Ungu (terjadwal)
            case "threshold": color = 0xFFF5A623; break; // Oranye (batas kelembaban)
            case "mode":      color = 0xFF888888; break; // Abu (mode)
            case "offline":   color = 0xFFFF6969; break; // Merah (offline)
            default:          color = 0xFF5B8CFF; break; // Biru (pompa manual)
        }
        if (holder.viewDot != null) {
            holder.viewDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(color));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvMessage, tvDate;
        View viewDot;

        ViewHolder(View view) {
            super(view);
            card      = (MaterialCardView) view;
            tvMessage = view.findViewById(R.id.tvMessage);
            tvDate    = view.findViewById(R.id.tvDate);
            viewDot   = view.findViewById(R.id.viewDot);
        }
    }
}
