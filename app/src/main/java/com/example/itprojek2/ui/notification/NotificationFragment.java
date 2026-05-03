package com.example.itprojek2.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.itprojek2.databinding.FragmentNotificationBinding;
import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;

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

        List<NotificationItem> notifList = new ArrayList<>();

        // Header Hari Ini
        notifList.add(new NotificationItem("HARI INI"));
        notifList.add(new NotificationItem(
                "🍇 Tanah Kering!",
                "Kelembaban 25% — di bawah batas minimum 30%. Segera lakukan penyiraman!",
                NotificationItem.Type.WARNING));
        notifList.add(new NotificationItem(
                "Penyiraman Jadwal Selesai",
                "Jadwal penyiraman pagi berhasil dieksekusi. Kelembaban saat ini 60%.",
                NotificationItem.Type.SUCCESS));

        // Header Kemarin
        notifList.add(new NotificationItem("KEMARIN"));
        notifList.add(new NotificationItem(
                "💧 Tanah Terlalu Basah!",
                "Kelembaban 85% — melebihi batas maksimum 80%. Kurangi air atau periksa drainase.",
                NotificationItem.Type.CRITICAL));
        notifList.add(new NotificationItem(
                "Sensor Offline",
                "Tidak dapat membaca data dari sensor kelembaban kebun.",
                NotificationItem.Type.CRITICAL));

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvNotifications.setAdapter(new NotificationAdapter(notifList));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
