package com.example.itprojek2.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.itprojek2.OtpActivity;
import com.example.itprojek2.R;

public class RegisterFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etName     = view.findViewById(R.id.etRegisterName);
        EditText etPassword = view.findViewById(R.id.etRegisterPassword);
        EditText etConfirm  = view.findViewById(R.id.etRegisterConfirmPassword);
        EditText etPhone    = view.findViewById(R.id.etRegisterPhone);

        // Tombol Lanjut — kirim data ke halaman OTP
        view.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String name        = etName.getText().toString().trim();
            String password    = etPassword.getText().toString().trim();
            String confirm     = etConfirm.getText().toString().trim();
            String phoneRaw    = etPhone.getText().toString().trim();

            // Validasi input
            if (name.isEmpty()) {
                etName.setError("Nama tidak boleh kosong");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password tidak boleh kosong");
                return;
            }
            if (confirm.isEmpty()) {
                etConfirm.setError("Konfirmasi password tidak boleh kosong");
                return;
            }
            if (!password.equals(confirm)) {
                etConfirm.setError("Password tidak cocok");
                return;
            }
            if (phoneRaw.isEmpty()) {
                etPhone.setError("Nomor HP tidak boleh kosong");
                return;
            }

            // Normalisasi awalan: buang 0, 62, atau +62 jika user ngetik dobel
            // (karena di UI sudah ada prefix +62, user mungkin ngetik: 088..., 62881..., atau +62881...)
            String digits;
            if (phoneRaw.startsWith("+62")) {
                digits = phoneRaw.substring(3);
            } else if (phoneRaw.startsWith("62")) {
                digits = phoneRaw.substring(2);
            } else if (phoneRaw.startsWith("0")) {
                digits = phoneRaw.substring(1);
            } else {
                digits = phoneRaw;
            }

            String formattedPhone = "+62" + digits;

            if (digits.isEmpty() || formattedPhone.length() < 10) {
                etPhone.setError("Nomor HP terlalu pendek");
                return;
            }

            // Lanjut ke halaman verifikasi OTP
            Intent intent = new Intent(getActivity(), OtpActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("password", password);
            intent.putExtra("phone", formattedPhone);
            startActivity(intent);
        });

        // Link ke halaman Login
        view.findViewById(R.id.tvGoToLogin).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_register_to_login));
    }
}
