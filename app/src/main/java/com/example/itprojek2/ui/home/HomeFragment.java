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
import com.example.itprojek2.controller.ManajerNotifikasi;
import com.example.itprojek2.databinding.FragmentHomeBinding;
import androidx.navigation.Navigation;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private IrrigationController controller;
    private ManajerNotifikasi manajerNotifikasi;

    // Mencegah infinite loop pada switch listener
    private boolean isAutoProgrammatic = false;
    private boolean isScheduleProgrammatic = false;
    private boolean isManualProgrammatic = false;

    // Batas kelembaban yang dimuat dari Firebase (untuk cek notifikasi)
    private int batasMin = 30;
    private int batasMax = 70;

    private static final String DEVICE_ID = "esp32_01";
    private static final int KODE_IZIN_NOTIFIKASI = 200;

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

        // Inisialisasi manajer notifikasi
        manajerNotifikasi = new ManajerNotifikasi(requireContext());

        // Minta izin notifikasi (Android 13+/API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        KODE_IZIN_NOTIFIKASI);
            }
        }

        // Load batas kelembaban dari Firebase untuk cek notifikasi
        controller.loadMoistureThreshold((min, max) -> {
            batasMin = min;
            batasMax = max;
        });

    

        // Load nama user dari sesi
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "User");
        String firstName = name.contains(" ") ? name.substring(0, name.indexOf(" ")) : name;
        binding.tvHeaderName.setText("Halo, " + firstName);

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
        // ========================================
        // 1. SWITCH SENSOR OTOMATIS
        // ========================================
        binding.switchAutoWatering.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isAutoProgrammatic) return;
            controller.setAutoWatering(isChecked, createModeListener("Otomatis", isChecked));
        });

        // ========================================
        // 2. SWITCH WAKTU TERJADWAL
        // ========================================
        binding.switchScheduleMode.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isScheduleProgrammatic) return;
            controller.setScheduleMode(isChecked, createModeListener("Terjadwal", isChecked));
        });

        // ========================================
        // 3. TOMBOL SIRAM MANUAL
        // ========================================
        binding.btnManualPump.setOnClickListener(v -> {
            boolean isCurrentlyOn = binding.btnManualPump.getText().toString().equalsIgnoreCase("STOP");
            if (!isCurrentlyOn) {
                controller.startManualPump(createModeListener("Manual", true));
            } else {
                controller.stopManualPump(createModeListener("Manual", false));
            }
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
        // TOMBOL HAPUS PESAN LCD
        // ========================================
        binding.btnClearLcd.setOnClickListener(v -> {
            controller.clearLcdMessage();
            Toast.makeText(getContext(), "Pesan LCD dihapus.", Toast.LENGTH_SHORT).show();
        });

        // ========================================
        // LISTENER STATUS REALTIME DARI ESP32
        // ========================================
        controller.listenToStatus(new IrrigationController.OnStatusUpdateListener() {
            @Override
            public void onStatusUpdate(IrrigationController.DeviceStatus status) {
                if (!isAdded() || binding == null) return;

                requireActivity().runOnUiThread(() -> {
                    // 1. Sync nilai switch
                    isAutoProgrammatic = true;
                    binding.switchAutoWatering.setChecked(status.autoWatering);
                    isAutoProgrammatic = false;

                    isScheduleProgrammatic = true;
                    binding.switchScheduleMode.setChecked(status.scheduleMode);
                    isScheduleProgrammatic = false;

                    // Update UI Tombol Manual
                    int colorPurple = ContextCompat.getColor(requireContext(), R.color.primary_purple);
                    int colorDisabled = ContextCompat.getColor(requireContext(), R.color.primary_purple_light); // Ungu pudar untuk abu-abu
                    int colorRed = ContextCompat.getColor(requireContext(), R.color.danger_red);

                    int[][] buttonStates = new int[][] {
                        new int[] {-android.R.attr.state_enabled}, // disabled
                        new int[] {android.R.attr.state_enabled}   // enabled
                    };

                    if (status.manualPump) {
                        binding.btnManualPump.setText("STOP");
                        binding.btnManualPump.setBackgroundTintList(new android.content.res.ColorStateList(
                                buttonStates, new int[] { colorDisabled, colorRed }
                        ));
                    } else {
                        binding.btnManualPump.setText("SIRAM");
                        binding.btnManualPump.setBackgroundTintList(new android.content.res.ColorStateList(
                                buttonStates, new int[] { colorDisabled, colorPurple }
                        ));
                    }

                    // 2. Terapkan Mutually Exclusive Visual (Abu-abu jika ada yang lain aktif)
                    boolean adaYangAktif = status.autoWatering || status.scheduleMode || status.manualPump;
                    
                    if (adaYangAktif) {
                        binding.switchAutoWatering.setEnabled(status.autoWatering);
                        binding.switchScheduleMode.setEnabled(status.scheduleMode);
                        binding.btnManualPump.setEnabled(status.manualPump);
                    } else {
                        // Jika offline semua, kembalikan ke aktif semua agar bisa ditekan
                        binding.switchAutoWatering.setEnabled(true);
                        binding.switchScheduleMode.setEnabled(true);
                        binding.btnManualPump.setEnabled(true);
                    }

                    // Update status online/offline
                    if (status.online) {
                        binding.tvDeviceOnlineStatus.setText("Perangkat: Online");
                        binding.tvDeviceOnlineStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.success_green));
                        binding.viewDeviceOnlineDot.setBackgroundResource(
                                R.drawable.shape_icon_circle_green);

                        // Update gauge kelembaban hanya jika online
                        binding.moistureGaugeView.setMoisturePercent(status.moisture);

                        // ════ CEK NOTIFIKASI KELEMBABAN ════
                        // Kirim notif ke HP jika kelembaban di luar rentang normal
                        if (manajerNotifikasi != null) {
                            manajerNotifikasi.cekDanKirimNotifikasi(
                                    status.moisture, batasMin, batasMax);
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
                    } else {
                        // ESP32 OFFLINE — reset semua ke kondisi awal
                        binding.tvDeviceOnlineStatus.setText("Perangkat: Offline");
                        binding.tvDeviceOnlineStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.danger_red));
                        binding.viewDeviceOnlineDot.setBackgroundResource(
                                R.drawable.shape_icon_circle_red);

                        // Reset gauge ke 0%
                        binding.moistureGaugeView.setMoisturePercent(0f);

                        // Reset status pompa
                        binding.tvPumpStatus.setText("Pompa: Mati");
                        binding.tvPumpStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.text_gray));

                        // Reset status watering
                        binding.tvWateringStatus.setText("Status: Perangkat Mati");
                        binding.tvWateringStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.danger_red));
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

    private IrrigationController.OnCommandListener createModeListener(String mode, boolean isChecked) {
        return new IrrigationController.OnCommandListener() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    String msg = isChecked ? ("Mode " + mode + " ON") : ("Mode " + mode + " OFF");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        };
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
