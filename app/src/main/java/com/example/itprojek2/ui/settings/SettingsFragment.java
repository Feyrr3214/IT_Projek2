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

        binding.cardMoistureLimit.setOnClickListener(v ->
                Toast.makeText(getContext(), "Atur Batas Kelembaban", Toast.LENGTH_SHORT).show());

        binding.cardChangeWifi.setOnClickListener(v -> showChangeWifiDialog());
    }

    private void showChangeWifiDialog() {
        android.view.View dialogView = LayoutInflater.from(requireContext()).inflate(com.example.itprojek2.R.layout.dialog_change_wifi, null);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Make background transparent so the card corners show up
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        final com.google.android.material.textfield.TextInputEditText etSsid = dialogView.findViewById(com.example.itprojek2.R.id.etSsid);
        final com.google.android.material.textfield.TextInputEditText etPass = dialogView.findViewById(com.example.itprojek2.R.id.etPass);
        com.google.android.material.button.MaterialButton btnUpdate = dialogView.findViewById(com.example.itprojek2.R.id.btnUpdate);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(com.example.itprojek2.R.id.btnCancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            String ssid = etSsid.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (ssid.isEmpty()) {
                etSsid.setError("SSID tidak boleh kosong!");
                return;
            }

            btnUpdate.setEnabled(false);
            btnUpdate.setText("MENGIRIM...");

            controller.updateWifi(ssid, pass, new com.example.itprojek2.controller.IrrigationController.OnCommandListener() {
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
                        btnUpdate.setText("UPDATE");
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
