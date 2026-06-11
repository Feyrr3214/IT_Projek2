package com.example.itprojek2.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.itprojek2.controller.ManajerRiwayat;
import com.example.itprojek2.databinding.FragmentHistoryBinding;
import com.example.itprojek2.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String DEVICE_ID = "esp32_01";

    private FragmentHistoryBinding binding;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;
    private ManajerRiwayat manajerRiwayat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set initial visibility (dipindah dari XML ke runtime)
        binding.btnDeleteSelected.setVisibility(View.GONE);
        binding.layoutEmptyHistory.setVisibility(View.GONE);
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvHistory.setAdapter(adapter);

        // Inisialisasi controller riwayat Firebase
        manajerRiwayat = new ManajerRiwayat(DEVICE_ID);

        // Load nama user dari sesi
        android.content.SharedPreferences prefs =
                requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "User");
        String firstName = name.contains(" ") ? name.substring(0, name.indexOf(" ")) : name;
        binding.tvHeaderName.setText("Halo, " + firstName);

        // Navigasi ke notifikasi
        binding.ivNotification.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v)
                        .navigate(com.example.itprojek2.R.id.notificationFragment));

        // Tombol Pilih (mode seleksi)
        binding.btnSelect.setOnClickListener(v -> {
            adapter.setSelectionMode(true);
            binding.btnSelect.setVisibility(View.GONE);
            binding.btnDeleteAll.setVisibility(View.GONE);
            binding.btnDeleteSelected.setVisibility(View.VISIBLE);
        });

        // Tombol Hapus Semua
        binding.btnDeleteAll.setOnClickListener(v -> konfirmasiHapusSemua());

        // Tombol Hapus yang terpilih
        binding.btnDeleteSelected.setOnClickListener(v -> hapusItemTerpilih());

        // Click item untuk toggle seleksi
        adapter.setOnItemClickListener(position -> {
            if (adapter.isSelectionMode()) {
                HistoryItem item = historyList.get(position);
                item.setSelected(!item.isSelected());
                adapter.notifyItemChanged(position);
            }
        });

        binding.btnSelect.setText("Pilih");

        // Tampilkan empty state di awal
        tampilkanEmptyState(true);

        // Mulai load dari Firebase
        mulaiFetchRiwayat();
    }

    private void mulaiFetchRiwayat() {
        manajerRiwayat.mulaiListen(new ManajerRiwayat.RiwayatListener() {
            @Override
            public void onLoaded(List<ManajerRiwayat.RiwayatItem> items) {
                if (!isAdded() || binding == null) return;
                requireActivity().runOnUiThread(() -> {
                    historyList.clear();

                    if (items.isEmpty()) {
                        tampilkanEmptyState(true);
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    tampilkanEmptyState(false);

                    // Konversi RiwayatItem → HistoryItem
                    for (ManajerRiwayat.RiwayatItem raw : items) {
                        historyList.add(new HistoryItem(
                                raw.key,
                                raw.message,
                                raw.date,
                                raw.timestamp,
                                raw.type
                        ));
                    }

                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String pesan) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Gagal load riwayat: " + pesan, Toast.LENGTH_SHORT).show());
            }
        });
    }

    /** Hapus item-item yang sedang tercentang dari Firebase dan list lokal */
    private void hapusItemTerpilih() {
        // Kumpulkan key item yang dipilih
        List<String> keysToDelete = new ArrayList<>();
        for (HistoryItem item : historyList) {
            if (item.isSelected() && item.getKey() != null) {
                keysToDelete.add(item.getKey());
            }
        }

        if (keysToDelete.isEmpty()) {
            Toast.makeText(getContext(), "Tidak ada item yang dipilih.", Toast.LENGTH_SHORT).show();
            keluarModeSeleksi();
            return;
        }

        // Hapus dari Firebase
        manajerRiwayat.hapusBeberapa(keysToDelete, () -> {
            // Listener Firebase akan refresh list otomatis
        });

        // Langsung hapus dari list lokal untuk respons UI cepat
        Iterator<HistoryItem> it = historyList.iterator();
        while (it.hasNext()) {
            if (it.next().isSelected()) it.remove();
        }
        adapter.notifyDataSetChanged();
        keluarModeSeleksi();

        if (historyList.isEmpty()) {
            tampilkanEmptyState(true);
        }

        Toast.makeText(getContext(), keysToDelete.size() + " riwayat dihapus.", Toast.LENGTH_SHORT).show();
    }

    private void keluarModeSeleksi() {
        adapter.setSelectionMode(false);
        binding.btnSelect.setVisibility(View.VISIBLE);
        if (!historyList.isEmpty()) {
            binding.btnDeleteAll.setVisibility(View.VISIBLE);
        }
        binding.btnDeleteSelected.setVisibility(View.GONE);
    }

    private void tampilkanEmptyState(boolean tampil) {
        if (binding.layoutEmptyHistory != null) {
            binding.layoutEmptyHistory.setVisibility(tampil ? View.VISIBLE : View.GONE);
        }
        binding.rvHistory.setVisibility(tampil ? View.GONE : View.VISIBLE);
        if (binding.btnDeleteAll != null) {
            binding.btnDeleteAll.setVisibility(tampil ? View.GONE : View.VISIBLE);
        }
    }

    private void konfirmasiHapusSemua() {
        if (historyList.isEmpty()) {
            Toast.makeText(getContext(), "Riwayat sudah kosong.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null);
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        tvTitle.setText("Hapus Semua Riwayat?");
        tvMessage.setText("Apakah Anda yakin ingin menghapus semua riwayat aktivitas? Tindakan ini tidak dapat dibatalkan.");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            manajerRiwayat.hapusSemua(() -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    historyList.clear();
                    adapter.notifyDataSetChanged();
                    tampilkanEmptyState(true);
                    Toast.makeText(getContext(), "Semua riwayat berhasil dihapus.", Toast.LENGTH_SHORT).show();
                });
            });
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (manajerRiwayat != null) {
            manajerRiwayat.stopListen();
        }
        binding = null;
    }
}
