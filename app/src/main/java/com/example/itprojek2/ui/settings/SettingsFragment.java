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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.ivNotification.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigate(com.example.itprojek2.R.id.notificationFragment));

        binding.cardDeviceSettings.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigate(com.example.itprojek2.R.id.deviceSetupFragment));

        binding.cardMoistureLimit.setOnClickListener(v ->
                Toast.makeText(getContext(), "Atur Batas Kelembaban", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
