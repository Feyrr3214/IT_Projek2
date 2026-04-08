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
                "Kelembaban Tanah Kritis",
                "Kelembaban tanah turun hingga 25%. Pompa air otomatis telah diaktifkan.",
                NotificationItem.Type.CRITICAL));
        notifList.add(new NotificationItem(
                "Penyiraman Selesai",
                "Sistem berhasil menyiram selama 10 menit. Kelembaban saat ini 65%.",
                NotificationItem.Type.SUCCESS));

        // Header Kemarin
        notifList.add(new NotificationItem("KEMARIN"));
        notifList.add(new NotificationItem(
                "Penyiraman Otomatis Aktif",
                "Pompa air menyala karena kelembaban tanah turun hingga 28%.",
                NotificationItem.Type.WARNING));
        notifList.add(new NotificationItem(
                "Kelembaban Normal",
                "Kelembaban tanah telah kembali ke level normal (55%).",
                NotificationItem.Type.INFO));

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvNotifications.setAdapter(new NotificationAdapter(notifList));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
