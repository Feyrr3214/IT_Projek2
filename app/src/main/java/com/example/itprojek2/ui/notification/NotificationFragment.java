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

        // Tombol hapus semua
        if (binding.btnDeleteAll != null) {
            binding.btnDeleteAll.setOnClickListener(v -> konfirmasiHapusSemua());
        }

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

                    for (ManajerNotifikasi.NotifItem raw : items) {
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

    private void konfirmasiHapusSemua() {
        if (displayList.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada notifikasi untuk dihapus.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null);
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        tvTitle.setText("Hapus Semua Notifikasi?");
        tvMessage.setText("Apakah Anda yakin ingin menghapus semua notifikasi? Tindakan ini tidak dapat dibatalkan.");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            if (manajerNotifikasi != null) {
                manajerNotifikasi.hapusSemua(null);
                displayList.clear();
                adapter.notifyDataSetChanged();
                tampilkanEmptyState(true);
                Toast.makeText(requireContext(), "Semua notifikasi berhasil dihapus", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
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
        
        // Sembunyikan action bar (tombol hapus semua) jika kosong
        if (binding.layoutAction != null) {
            binding.layoutAction.setVisibility(tampil ? View.GONE : View.VISIBLE);
        }
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
