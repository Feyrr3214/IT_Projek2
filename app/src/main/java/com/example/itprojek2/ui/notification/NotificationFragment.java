package com.example.itprojek2.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.itprojek2.R;
import com.example.itprojek2.controller.ManajerNotifikasi;
import com.example.itprojek2.databinding.FragmentNotificationBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationFragment extends Fragment {

    private static final String DEVICE_ID = "esp32_01";

    private FragmentNotificationBinding binding;
    private NotificationAdapter adapter;
    private List<NotificationItem> displayList;
    private ManajerNotifikasi manajerNotifikasi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi
        displayList = new ArrayList<>();
        adapter = new NotificationAdapter(displayList);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvNotifications.setAdapter(adapter);

        // Inisialisasi ManajerNotifikasi dengan deviceId
        manajerNotifikasi = new ManajerNotifikasi(requireContext(), DEVICE_ID);

        // Load nama user
        android.content.SharedPreferences prefs =
                requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "User");
        String firstName = name.contains(" ") ? name.substring(0, name.indexOf(" ")) : name;
        if (binding.tvHeaderName != null) {
            binding.tvHeaderName.setText("Halo, " + firstName);
        }

        // Hapus: Tombol hapus semua telah dihilangkan dari XML

        // Long press item untuk opsi tambahan jika perlu (tetap dipertahankan untuk opsi tap)
        adapter.setOnItemLongClickListener((position, item) -> {
            if (item.getFirebaseKey() != null) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Hapus Notifikasi")
                        .setMessage("Hapus notifikasi ini?")
                        .setPositiveButton("Hapus", (d, w) -> {
                            manajerNotifikasi.hapus(item.getFirebaseKey(), null);
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            }
        });

        // Swipe (geser) untuk menghapus dengan animasi modern
        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback simpleCallback = 
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            
            // Icon dan background untuk animasi swipe
            private android.graphics.drawable.ColorDrawable background = new android.graphics.drawable.ColorDrawable(0xFFFF4444); // Merah
            private android.graphics.drawable.Drawable deleteIcon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);

            @Override
            public boolean onMove(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder) {
                // Jangan izinkan swipe pada Header (seperti "HARI INI")
                if (viewHolder instanceof NotificationAdapter.HeaderViewHolder) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                
                // Jika sedang di-swipe ke kiri
                if (dX < 0) {
                    // Gambar background merah
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(c);

                    // Gambar icon tempat sampah jika digeser cukup jauh
                    if (deleteIcon != null) {
                        int itemHeight = itemView.getBottom() - itemView.getTop();
                        int iconMargin = (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;

                        deleteIcon.setTint(0xFFFFFFFF); // Icon warna putih
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        
                        // Buat icon muncul perlahan berdasarkan seberapa jauh digeser
                        int alpha = (int) (Math.abs(dX) / ((float) itemView.getWidth() / 3) * 255);
                        deleteIcon.setAlpha(Math.min(alpha, 255));
                        deleteIcon.draw(c);
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSwiped(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                NotificationItem item = displayList.get(position);
                if (item != null && item.getFirebaseKey() != null) {
                    manajerNotifikasi.hapus(item.getFirebaseKey(), null);
                    Toast.makeText(getContext(), "Notifikasi dihapus", Toast.LENGTH_SHORT).show();
                } else {
                    adapter.notifyItemChanged(position);
                }
            }
        };
        new androidx.recyclerview.widget.ItemTouchHelper(simpleCallback).attachToRecyclerView(binding.rvNotifications);

        // Tampilkan empty state di awal
        tampilkanEmptyState(true);

        // Mulai listen dari Firebase
        mulaiFetchNotifikasi();
    }

    private void mulaiFetchNotifikasi() {
        manajerNotifikasi.mulaiListen(new ManajerNotifikasi.NotifListener() {
            @Override
            public void onLoaded(List<ManajerNotifikasi.NotifItem> items) {
                if (!isAdded() || binding == null) return;
                requireActivity().runOnUiThread(() -> {
                    displayList.clear();

                    if (items.isEmpty()) {
                        tampilkanEmptyState(true);
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    tampilkanEmptyState(false);

                    // Group by tanggal
                    String headerTerakhir = "";
                    for (ManajerNotifikasi.NotifItem raw : items) {
                        String headerTanggal = getHeaderTanggal(raw.timestamp);
                        if (!headerTanggal.equals(headerTerakhir)) {
                            displayList.add(new NotificationItem(headerTanggal));
                            headerTerakhir = headerTanggal;
                        }
                        displayList.add(new NotificationItem(
                                raw.title,
                                raw.message,
                                parseType(raw.type),
                                raw.timestamp,
                                raw.key
                        ));
                    }

                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String pesan) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Gagal load notifikasi: " + pesan, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Method konfirmasiHapusSemua() dihapus sesuai request
    /** Konversi timestamp jadi label header tanggal */
    private String getHeaderTanggal(long timestamp) {
        if (timestamp <= 0) return "Sebelumnya";

        Calendar calItem = Calendar.getInstance();
        calItem.setTimeInMillis(timestamp);

        Calendar calHariIni = Calendar.getInstance();

        // Cek apakah hari ini
        if (calItem.get(Calendar.YEAR) == calHariIni.get(Calendar.YEAR) &&
                calItem.get(Calendar.DAY_OF_YEAR) == calHariIni.get(Calendar.DAY_OF_YEAR)) {
            return "HARI INI";
        }

        // Cek apakah kemarin
        calHariIni.add(Calendar.DAY_OF_YEAR, -1);
        if (calItem.get(Calendar.YEAR) == calHariIni.get(Calendar.YEAR) &&
                calItem.get(Calendar.DAY_OF_YEAR) == calHariIni.get(Calendar.DAY_OF_YEAR)) {
            return "KEMARIN";
        }

        // Tanggal lainnya
        return new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"))
                .format(new Date(timestamp)).toUpperCase();
    }

    /** Konversi string tipe dari Firebase ke enum */
    private NotificationItem.Type parseType(String type) {
        if (type == null) return NotificationItem.Type.INFO;
        switch (type.toUpperCase()) {
            case "CRITICAL": return NotificationItem.Type.CRITICAL;
            case "SUCCESS":  return NotificationItem.Type.SUCCESS;
            case "WARNING":  return NotificationItem.Type.WARNING;
            default:         return NotificationItem.Type.INFO;
        }
    }

    private void tampilkanEmptyState(boolean tampil) {
        if (binding.layoutEmptyNotif != null) {
            binding.layoutEmptyNotif.setVisibility(tampil ? View.VISIBLE : View.GONE);
        }
        binding.rvNotifications.setVisibility(tampil ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (manajerNotifikasi != null) {
            manajerNotifikasi.stopListen();
        }
        binding = null;
    }
}
