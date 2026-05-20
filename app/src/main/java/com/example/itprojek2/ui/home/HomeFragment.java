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
import com.example.itprojek2.controller.ManajerRiwayat;
import com.example.itprojek2.databinding.FragmentHomeBinding;
import androidx.navigation.Navigation;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private IrrigationController controller;
    private ManajerNotifikasi manajerNotifikasi;
    private ManajerRiwayat manajerRiwayat;

    // Mencegah infinite loop pada switch listener
    private boolean isAutoProgrammatic = false;
    private boolean isScheduleProgrammatic = false;
    private boolean isManualProgrammatic = false;

    // Batas kelembaban yang dimuat dari Firebase (untuk cek notifikasi)
    private int batasMin = 30;
    private int batasMax = 70;

    // Tracking state pompa & online sebelumnya untuk deteksi perubahan
    private boolean pumpWasRunning = false;
    private boolean wasOnline      = false;
    private boolean firstStatus    = true; // Abaikan event online pertama kali (inisialisasi)

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

        // Inisialisasi manajer notifikasi (dengan deviceId agar event disimpan ke Firebase)
        manajerNotifikasi = new ManajerNotifikasi(requireContext(), DEVICE_ID);

        // Inisialisasi manajer riwayat
        manajerRiwayat = new ManajerRiwayat(DEVICE_ID);

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

        // Listen batas kelembaban secara real-time dari Firebase agar notifikasi akurat
        controller.listenMoistureThreshold((min, max) -> {
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
        // 1. SWITCH SENSOR OTOMATIS
        // ========================================
        binding.switchAutoWatering.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isAutoProgrammatic) return;
            // Optimistic: langsung disable interaksi sementara
            binding.switchAutoWatering.setEnabled(false);
            controller.setAutoWatering(isChecked, new IrrigationController.OnCommandListener() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        binding.switchAutoWatering.setEnabled(true);
                        String msg = isChecked ? "Mode Otomatis ON" : "Mode Otomatis OFF";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    });
                }
                @Override
                public void onFailure(String errorMessage) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        // Rollback switch jika gagal
                        isAutoProgrammatic = true;
                        binding.switchAutoWatering.setChecked(!isChecked);
                        isAutoProgrammatic = false;
                        binding.switchAutoWatering.setEnabled(true);
                        Toast.makeText(getContext(), "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // ========================================
        // 2. SWITCH WAKTU TERJADWAL
        // ========================================
        binding.switchScheduleMode.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isScheduleProgrammatic) return;
            // Optimistic: langsung disable interaksi sementara
            binding.switchScheduleMode.setEnabled(false);
            controller.setScheduleMode(isChecked, new IrrigationController.OnCommandListener() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        binding.switchScheduleMode.setEnabled(true);
                        String msg = isChecked ? "Mode Terjadwal ON" : "Mode Terjadwal OFF";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    });
                }
                @Override
                public void onFailure(String errorMessage) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        // Rollback switch jika gagal
                        isScheduleProgrammatic = true;
                        binding.switchScheduleMode.setChecked(!isChecked);
                        isScheduleProgrammatic = false;
                        binding.switchScheduleMode.setEnabled(true);
                        Toast.makeText(getContext(), "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // ========================================
        // 3. TOMBOL SIRAM MANUAL
        // ========================================
        binding.btnManualPump.setOnClickListener(v -> {
            boolean isCurrentlyOn = binding.btnManualPump.getText().toString().equalsIgnoreCase("STOP");
            // Optimistic: langsung disable tombol sementara
            binding.btnManualPump.setEnabled(false);
            if (!isCurrentlyOn) {
                controller.startManualPump(new IrrigationController.OnCommandListener() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> binding.btnManualPump.setEnabled(true));
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            binding.btnManualPump.setEnabled(true);
                            Toast.makeText(getContext(), "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                controller.stopManualPump(new IrrigationController.OnCommandListener() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> binding.btnManualPump.setEnabled(true));
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            binding.btnManualPump.setEnabled(true);
                            Toast.makeText(getContext(), "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
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
                        if (manajerNotifikasi != null) {
                            manajerNotifikasi.cekDanKirimNotifikasi(
                                    status.moisture, batasMin, batasMax);
                        }

                        // ════ EVENT POMPA: Deteksi pompa nyala/mati ════
                        if (!firstStatus) {
                            if (status.pumpRunning && !pumpWasRunning) {
                                // Pompa baru nyala
                                String mode = status.autoWatering ? "auto"
                                            : status.scheduleMode ? "schedule" : "manual";
                                if (manajerNotifikasi != null) {
                                    manajerNotifikasi.eventPompaMenyala(status.moisture, mode);
                                }
                                String pesanRiwayat;
                                switch (mode) {
                                    case "auto":     pesanRiwayat = "Penyiraman otomatis dimulai — kelembaban " + status.moisture + "%." ; break;
                                    case "schedule": pesanRiwayat = "Penyiraman terjadwal dimulai — kelembaban " + status.moisture + "%." ; break;
                                    default:         pesanRiwayat = "Penyiraman manual dimulai — kelembaban " + status.moisture + "%." ;
                                }
                                if (manajerRiwayat != null) {
                                    manajerRiwayat.simpan(pesanRiwayat, mode.equals("manual") ? "pump" : mode);
                                }
                            } else if (!status.pumpRunning && pumpWasRunning) {
                                // Pompa baru mati
                                if (manajerNotifikasi != null) {
                                    manajerNotifikasi.eventPompaMati(status.lastDuration, status.moisture);
                                }
                                String pesanRiwayat = "Penyiraman selesai, durasi " + status.lastDuration
                                        + " detik. Kelembaban akhir " + status.moisture + "%.";
                                if (manajerRiwayat != null) {
                                    manajerRiwayat.simpan(pesanRiwayat, "pump");
                                }
                            }
                        }
                        pumpWasRunning = status.pumpRunning;

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
                        // ════ EVENT OFFLINE ════
                        if (!firstStatus && wasOnline) {
                            if (manajerNotifikasi != null) manajerNotifikasi.eventPerangkatOffline();
                        }

                        // ESP32 OFFLINE
                        binding.tvDeviceOnlineStatus.setText("Perangkat: Offline");
                        binding.tvDeviceOnlineStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.danger_red));
                        binding.viewDeviceOnlineDot.setBackgroundResource(
                                R.drawable.shape_icon_circle_red);

                        // Reset gauge ke 0%
                        binding.moistureGaugeView.setMoisturePercent(0f);

                        // Pompa status tidak diketahui
                        binding.tvPumpStatus.setText("Pompa: Tidak Diketahui");
                        binding.tvPumpStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.text_gray));

                        // Disable semua kontrol saat offline
                        binding.btnManualPump.setEnabled(false);
                        binding.btnManualPump.setText("SIRAM");
                        binding.btnManualPump.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(
                                        ContextCompat.getColor(requireContext(), R.color.text_gray)));
                        binding.switchAutoWatering.setEnabled(false);
                        binding.switchScheduleMode.setEnabled(false);

                        // Status penyiraman
                        binding.tvWateringStatus.setText("Status: Perangkat Offline");
                        binding.tvWateringStatus.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.danger_red));
                    }

                    // ════ EVENT ONLINE KEMBALI ════
                    if (!firstStatus && !wasOnline && status.online) {
                        if (manajerNotifikasi != null) manajerNotifikasi.eventPerangkatOnline();
                    }
                    wasOnline   = status.online;
                    firstStatus = false;
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
        // Hentikan semua listener Firebase agar tidak memory leak
        if (controller != null) {
            controller.stopListening();
        }
        if (manajerRiwayat != null) {
            manajerRiwayat.stopListen();
        }
        binding = null;
    }
}
