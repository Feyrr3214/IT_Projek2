package com.example.itprojek2.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.itprojek2.databinding.FragmentHistoryBinding;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;

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

        historyList = new ArrayList<>();
        historyList.add(new HistoryItem(1, "Jangan lupakan untuk siram tanaman ya!", "17/3/2026"));
        historyList.add(new HistoryItem(2, "Kelembaban tanah hari ini pada 30% mengaktifkan penyiraman otomatis", "17/3/2026"));
        historyList.add(new HistoryItem(3, "Pompa air mati, kelembaban kembali normal 65%", "16/3/2026"));

        adapter = new HistoryAdapter(historyList);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvHistory.setAdapter(adapter);

        // Load nama user dari sesi
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "User");
        String firstName = name.contains(" ") ? name.substring(0, name.indexOf(" ")) : name;
        binding.tvHeaderName.setText("Halo, " + firstName);

        binding.ivNotification.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigate(com.example.itprojek2.R.id.notificationFragment));

        // Tombol Pilih (tampilkan mode seleksi)
        binding.btnSelect.setOnClickListener(v -> {
            adapter.setSelectionMode(true);
            binding.btnSelect.setVisibility(View.GONE);
            binding.btnDeleteSelected.setVisibility(View.VISIBLE);
        });

        // Tombol Hapus
        binding.btnDeleteSelected.setOnClickListener(v -> {
            Iterator<HistoryItem> it = historyList.iterator();
            while (it.hasNext()) {
                if (it.next().isSelected()) it.remove();
            }
            adapter.setSelectionMode(false);
            adapter.notifyDataSetChanged();
            binding.btnSelect.setVisibility(View.VISIBLE);
            binding.btnDeleteSelected.setVisibility(View.GONE);
        });

        // Click item untuk seleksi
        adapter.setOnItemClickListener(position -> {
            if (adapter.isSelectionMode()) {
                HistoryItem item = historyList.get(position);
                item.setSelected(!item.isSelected());
                adapter.notifyItemChanged(position);
            }
        });

        // Set teks awal tombol
        binding.btnSelect.setText("Pilih");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
