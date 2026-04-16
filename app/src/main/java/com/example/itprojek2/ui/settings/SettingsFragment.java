package com.example.itprojek2.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.itprojek2.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    private com.example.itprojek2.controller.IrrigationController controller;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        controller = new com.example.itprojek2.controller.IrrigationController("esp32_01");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load nama user dari sesi
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "User");
        String firstName = name.contains(" ") ? name.substring(0, name.indexOf(" ")) : name;
        binding.tvHeaderName.setText("Halo, " + firstName);

        binding.ivNotification.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigate(com.example.itprojek2.R.id.notificationFragment));

        binding.cardDeviceSettings.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigate(com.example.itprojek2.R.id.deviceSetupFragment));

        binding.cardMoistureLimit.setOnClickListener(v -> showMoistureLimitDialog());

        binding.cardChangeWifi.setOnClickListener(v -> showChangeWifiDialog());
    }

    private void showMoistureLimitDialog() {
        android.view.View dialogView = LayoutInflater.from(requireContext())
                .inflate(com.example.itprojek2.R.layout.dialog_moisture_limit, null);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        com.google.android.material.slider.Slider sliderMin = dialogView.findViewById(com.example.itprojek2.R.id.sliderMin);
        com.google.android.material.slider.Slider sliderMax = dialogView.findViewById(com.example.itprojek2.R.id.sliderMax);
        android.widget.TextView tvMin = dialogView.findViewById(com.example.itprojek2.R.id.tvMinValue);
        android.widget.TextView tvMax = dialogView.findViewById(com.example.itprojek2.R.id.tvMaxValue);
        android.view.View layoutWarning = dialogView.findViewById(com.example.itprojek2.R.id.layoutWarning);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(com.example.itprojek2.R.id.btnSaveLimit);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(com.example.itprojek2.R.id.btnCancelLimit);

        // Load nilai tersimpan dari Firebase dulu
        controller.loadMoistureThreshold((min, max) -> {
            sliderMin.setValue(min);
            sliderMax.setValue(max);
            tvMin.setText(min + "%");
            tvMax.setText(max + "%");
        });

        // Update preview realtime saat slider digeser
        sliderMin.addOnChangeListener((slider, value, fromUser) -> {
            int minVal = (int) value;
            int maxVal = (int) sliderMax.getValue();
            tvMin.setText(minVal + "%");
            layoutWarning.setVisibility(minVal >= maxVal ? android.view.View.VISIBLE : android.view.View.GONE);
            btnSave.setEnabled(minVal < maxVal);
        });

        sliderMax.addOnChangeListener((slider, value, fromUser) -> {
            int minVal = (int) sliderMin.getValue();
            int maxVal = (int) value;
            tvMax.setText(maxVal + "%");
            layoutWarning.setVisibility(minVal >= maxVal ? android.view.View.VISIBLE : android.view.View.GONE);
            btnSave.setEnabled(minVal < maxVal);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int minVal = (int) sliderMin.getValue();
            int maxVal = (int) sliderMax.getValue();

            btnSave.setEnabled(false);
            btnSave.setText("MENYIMPAN...");

            controller.saveMoistureThreshold(minVal, maxVal, new com.example.itprojek2.controller.IrrigationController.OnCommandListener() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        android.widget.Toast.makeText(getContext(),
                                "✓ Batas kelembaban berhasil disimpan!", android.widget.Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (isAdded()) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Simpan");
                        android.widget.Toast.makeText(getContext(),
                                "Gagal: " + errorMessage, android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        dialog.show();
    }

    private void showChangeWifiDialog() {
        android.view.View dialogView = LayoutInflater.from(requireContext()).inflate(com.example.itprojek2.R.layout.dialog_change_wifi, null);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.ImageView btnScanWifi = dialogView.findViewById(com.example.itprojek2.R.id.btnScanWifi);
        android.view.View layoutScanning = dialogView.findViewById(com.example.itprojek2.R.id.layoutScanning);
        android.view.View layoutNoWifi = dialogView.findViewById(com.example.itprojek2.R.id.layoutNoWifi);
        android.view.View layoutWifiOff = dialogView.findViewById(com.example.itprojek2.R.id.layoutWifiOff);
        android.view.View layoutLocationOff = dialogView.findViewById(com.example.itprojek2.R.id.layoutLocationOff);
        com.google.android.material.button.MaterialButton btnTurnOnWifi = dialogView.findViewById(com.example.itprojek2.R.id.btnTurnOnWifi);
        com.google.android.material.button.MaterialButton btnTurnOnLocation = dialogView.findViewById(com.example.itprojek2.R.id.btnTurnOnLocation);
        android.widget.ListView listViewWifi = dialogView.findViewById(com.example.itprojek2.R.id.listViewWifi);
        android.view.View dividerPassword = dialogView.findViewById(com.example.itprojek2.R.id.dividerPassword);
        android.view.View layoutSelectedWifi = dialogView.findViewById(com.example.itprojek2.R.id.layoutSelectedWifi);
        android.widget.TextView tvSelectedSsid = dialogView.findViewById(com.example.itprojek2.R.id.tvSelectedSsid);
        com.google.android.material.textfield.TextInputLayout tilPass = dialogView.findViewById(com.example.itprojek2.R.id.tilPass);
        com.google.android.material.textfield.TextInputEditText etPass = dialogView.findViewById(com.example.itprojek2.R.id.etPass);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(com.example.itprojek2.R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnUpdate = dialogView.findViewById(com.example.itprojek2.R.id.btnUpdate);

        final String[] selectedSsid = {""};

        btnTurnOnWifi.setOnClickListener(v -> {
            startActivity(new android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            dialog.dismiss();
        });

        btnTurnOnLocation.setOnClickListener(v -> {
            startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            dialog.dismiss();
        });

        Runnable scanWifiRunnable = () -> {
            layoutSelectedWifi.setVisibility(android.view.View.GONE);
            tilPass.setVisibility(android.view.View.GONE);
            dividerPassword.setVisibility(android.view.View.GONE);
            btnUpdate.setEnabled(false);

            layoutScanning.setVisibility(android.view.View.VISIBLE);
            listViewWifi.setVisibility(android.view.View.GONE);
            layoutNoWifi.setVisibility(android.view.View.GONE);
            
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                layoutScanning.setVisibility(android.view.View.GONE);
                Toast.makeText(getContext(), "Izin lokasi diperlukan untuk memindai WiFi", Toast.LENGTH_SHORT).show();
                return;
            }

            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) requireContext().getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
            if (wifiManager == null) {
                layoutScanning.setVisibility(android.view.View.GONE);
                layoutNoWifi.setVisibility(android.view.View.VISIBLE);
                return;
            }

            if (!wifiManager.isWifiEnabled()) {
                layoutScanning.setVisibility(android.view.View.GONE);
                layoutNoWifi.setVisibility(android.view.View.GONE);
                listViewWifi.setVisibility(android.view.View.GONE);
                layoutLocationOff.setVisibility(android.view.View.GONE);
                layoutWifiOff.setVisibility(android.view.View.VISIBLE);
                return;
            } else {
                layoutWifiOff.setVisibility(android.view.View.GONE);
            }

            android.location.LocationManager locationManager = (android.location.LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
            boolean isLocationEnabled = locationManager != null && (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER));
            if (!isLocationEnabled) {
                layoutScanning.setVisibility(android.view.View.GONE);
                layoutNoWifi.setVisibility(android.view.View.GONE);
                listViewWifi.setVisibility(android.view.View.GONE);
                layoutWifiOff.setVisibility(android.view.View.GONE);
                layoutLocationOff.setVisibility(android.view.View.VISIBLE);
                return;
            } else {
                layoutLocationOff.setVisibility(android.view.View.GONE);
            }

            try {
                wifiManager.startScan();
            } catch (Exception e) {
                // Abaikan jika device menolak startScan, kita ambil cache result aja
            }
            
            // Wait for scan results cache
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    java.util.List<android.net.wifi.ScanResult> results = wifiManager.getScanResults();
                    java.util.List<String> ssidList = new java.util.ArrayList<>();
                    
                    if (results != null) {
                        for (android.net.wifi.ScanResult result : results) {
                            if (result.SSID != null && !result.SSID.isEmpty() && !ssidList.contains(result.SSID)) {
                                ssidList.add(result.SSID);
                            }
                        }
                    }
                    
                    layoutScanning.setVisibility(android.view.View.GONE);
                    
                    if (ssidList.isEmpty()) {
                        layoutNoWifi.setVisibility(android.view.View.VISIBLE);
                    } else {
                        // Cek apakah fragment masih hidup sebelum pakai requireContext()
                        if (isAdded() && getContext() != null) {
                            listViewWifi.setVisibility(android.view.View.VISIBLE);
                            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, ssidList);
                            listViewWifi.setAdapter(adapter);
                        }
                    }
                } catch (Exception e) {
                    layoutScanning.setVisibility(android.view.View.GONE);
                    layoutNoWifi.setVisibility(android.view.View.VISIBLE);
                    Toast.makeText(getContext(), "Aktifkan GPS/Lokasi HP untuk memindai WiFi", Toast.LENGTH_SHORT).show();
                }
            }, 1000);
        };

        btnScanWifi.setOnClickListener(v -> scanWifiRunnable.run());
        
        listViewWifi.setOnItemClickListener((parent, view, position, id) -> {
            selectedSsid[0] = (String) parent.getItemAtPosition(position);
            
            layoutScanning.setVisibility(android.view.View.GONE);
            listViewWifi.setVisibility(android.view.View.GONE);
            layoutNoWifi.setVisibility(android.view.View.GONE);
            
            dividerPassword.setVisibility(android.view.View.VISIBLE);
            layoutSelectedWifi.setVisibility(android.view.View.VISIBLE);
            tilPass.setVisibility(android.view.View.VISIBLE);
            
            tvSelectedSsid.setText(selectedSsid[0]);
            btnUpdate.setEnabled(true);
            etPass.requestFocus();
        });

        // Initialize first scan
        scanWifiRunnable.run();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            String pass = etPass.getText().toString().trim();

            if (selectedSsid[0].isEmpty()) {
                Toast.makeText(getContext(), "Pilih jaringan WiFi terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }

            btnUpdate.setEnabled(false);
            btnUpdate.setText("MENGIRIM...");

            controller.updateWifi(selectedSsid[0], pass, new com.example.itprojek2.controller.IrrigationController.OnCommandListener() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "✓ Perintah Ganti WiFi Terkirim!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (isAdded()) {
                        btnUpdate.setEnabled(true);
                        btnUpdate.setText("Kirim ke Alat");
                        Toast.makeText(getContext(), "Gagal: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
