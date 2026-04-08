package com.example.itprojek2.ui.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.itprojek2.R;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<NotificationItem> items;

    public NotificationAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isHeader() ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationItem item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText(item.getHeaderTitle());
            ((HeaderViewHolder) holder).tvHeader.setTextColor(0xFF888888);
            ((HeaderViewHolder) holder).tvHeader.setTextSize(12f);
            ((HeaderViewHolder) holder).tvHeader.setPadding(0, 16, 0, 8);
            ((HeaderViewHolder) holder).tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder h = (ItemViewHolder) holder;
            h.tvTitle.setText(item.getTitle());
            h.tvDesc.setText(item.getDescription());

            switch (item.getType()) {
                case CRITICAL:
                    h.ivIcon.setBackgroundResource(R.drawable.shape_icon_circle_red);
                    h.ivIcon.setImageResource(R.drawable.ic_warning);
                    h.ivIcon.setColorFilter(0xFFFF6969);
                    break;
                case SUCCESS:
                case INFO:
                    h.ivIcon.setBackgroundResource(R.drawable.shape_icon_circle_green);
                    h.ivIcon.setImageResource(R.drawable.ic_water_drop);
                    h.ivIcon.setColorFilter(0xFF5ED5A8);
                    break;
                case WARNING:
                    h.ivIcon.setBackgroundResource(R.drawable.shape_icon_circle_yellow);
                    h.ivIcon.setImageResource(R.drawable.ic_water_drop);
                    h.ivIcon.setColorFilter(0xFFF5A623);
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(View view) {
            super(view);
            tvHeader = view.findViewById(android.R.id.text1);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvDesc;
        ItemViewHolder(View view) {
            super(view);
            ivIcon = view.findViewById(R.id.ivNotifIcon);
            tvTitle = view.findViewById(R.id.tvNotifTitle);
            tvDesc = view.findViewById(R.id.tvNotifDesc);
        }
    }
}
