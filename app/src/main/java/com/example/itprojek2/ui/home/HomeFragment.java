package com.example.itprojek2.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.itprojek2.R;
import com.example.itprojek2.controller.IrrigationController;
import com.example.itprojek2.databinding.FragmentHomeBinding;
import androidx.navigation.Navigation;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private IrrigationController controller;

    // Apakah pompa sedang dalam mode manual aktif (dari sisi Android)
    private boolean isManualPumpOn = false;

    // Mencegah infinite loop pada switch listener
    private boolean isAutoSwitchProgrammatic = false;

    private static final String DEVICE_ID = "esp32_01";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi controller Firebase
        controller = new IrrigationController(DEVICE_ID);

        // Gauge kelembaban — default sampai data Firebase masuk
        binding.moistureGaugeView.setMoisturePercent(0f);

        // ========================================
        // TOMBOL NOTIFIKASI
        // ========================================
        binding.ivNotification.setOnClickListener(v -> {
            androidx.navigation.NavOptions options = new androidx.navigation.NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right).build();
            Navigation.findNavController(view).navigate(R.id.notificationFragment, null, options);
        });

        // ========================================
        // TOMBOL SIRAM MANUAL (TOGGLE ON/OFF)
        // ========================================
        binding.btnManualWater.setOnClickListener(v -> {
            binding.btnManualWater.setEnabled(false);

            if (!isManualPumpOn) {
                // Nyalakan pompa
                controller.startManualPump(new IrrigationController.OnCommandListener() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isManualPumpOn = true;
                            binding.btnManualWater.setText("HENTIKAN POMPA");
                            binding.btnManualWater.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "✓ Pompa manual aktif! Pesan terkirim ke LCD.", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            binding.btnManualWater.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                // Matikan pompa
                controller.stopManualPump(new IrrigationController.OnCommandListener() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isManualPumpOn = false;
                            binding.btnManualWater.setText("SIRAM MANUAL SEKARANG");
                            binding.btnManualWater.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "Pompa manual dimatikan.", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            binding.btnManualWater.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        // ========================================
        // SWITCH PENYIRAMAN OTOMATIS
        // ========================================
        binding.switchAutoWatering.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isAutoSwitchProgrammatic) return; // Abaikan jika diubah dari kode

            controller.setAutoWatering(isChecked, new IrrigationController.OnCommandListener() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        String msg = isChecked
                                ? "✓ Penyiraman otomatis diaktifkan"
                                : "Penyiraman otomatis dinonaktifkan";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        // Rollback switch
                        isAutoSwitchProgrammatic = true;
                        binding.switchAutoWatering.setChecked(!isChecked);
                        isAutoSwitchProgrammatic = false;
                        Toast.makeText(getContext(),
                                "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // ========================================
        // TOMBOL KIRIM PESAN LCD
        // ========================================
        binding.btnSendLcd.setOnClickListener(v -> {
            String line1 = binding.etLcdLine1.getText().toString().trim();
            String line2 = binding.etLcdLine2.getText().toString().trim();

            if (line1.isEmpty() && line2.isEmpty()) {
                Toast.makeText(getContext(), "Tulis pesan untuk LCD!", Toast.LENGTH_SHORT).show();
                return;
            }

            controller.setLcdMessage(line1, line2);
            Toast.makeText(getContext(), "✓ Pesan terkirim ke LCD!", Toast.LENGTH_SHORT).show();

            // Bersihkan input
            binding.etLcdLine1.setText("");
            binding.etLcdLine2.setText("");
        });

        // ========================================
        // LISTENER STATUS REALTIME DARI ESP32
        // ========================================
        controller.listenToStatus(new IrrigationController.OnStatusUpdateListener() {
            @Override
            public void onStatusUpdate(IrrigationController.DeviceStatus status) {
                if (!isAdded() || binding == null) return;

                requireActivity().runOnUiThread(() -> {
                    // Update gauge kelembaban
                    binding.moistureGaugeView.setMoisturePercent(status.moisture);

                    // Update status online/offline
                    if (status.online) {
                        binding.tvDeviceOnlineStatus.setText("ESP32: Online");
                        binding.tvDeviceOnlineStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.success_green));
                        binding.viewDeviceOnlineDot.setBackgroundResource(
                                R.drawable.shape_icon_circle_green);
                    } else {
                        binding.tvDeviceOnlineStatus.setText("ESP32: Offline");
                        binding.tvDeviceOnlineStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.danger_red));
                        binding.viewDeviceOnlineDot.setBackgroundResource(
                                R.drawable.shape_icon_circle_red);
                    }

                    // Update status pompa
                    if (status.pumpRunning) {
                        binding.tvPumpStatus.setText("Pompa: Aktif 💧");
                        binding.tvPumpStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.info_blue));
                    } else {
                        binding.tvPumpStatus.setText("Pompa: Mati");
                        binding.tvPumpStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.text_gray));
                    }

                    // Update status penyiraman terakhir
                    binding.tvLastWatered.setText(status.lastWatered);
                    binding.tvLastDuration.setText(status.lastDuration + " Detik");

                    // Update status watering
                    if (status.pumpRunning) {
                        binding.tvWateringStatus.setText("Status: Sedang Menyiram...");
                        binding.tvWateringStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.info_blue));
                    } else {
                        binding.tvWateringStatus.setText("Status: Selesai");
                        binding.tvWateringStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.text_dark));
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Error data: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hentikan listener Firebase agar tidak memory leak
        if (controller != null) {
            controller.stopListening();
        }
        binding = null;
    }
}
