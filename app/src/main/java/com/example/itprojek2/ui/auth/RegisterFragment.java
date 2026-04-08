package com.example.itprojek2.ui.auth;

import android.os.Bundle;
import com.example.itprojek2.OtpActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
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

        EditText etName = view.findViewById(R.id.etRegisterName);
        EditText etPassword = view.findViewById(R.id.etRegisterPassword);
        EditText etConfirm = view.findViewById(R.id.etRegisterConfirmPassword);

        // Tombol OTP
        view.findViewById(R.id.btnOtp).setOnClickListener(v -> {
            // Ambil nomor HP dari input nama (atau bisa tambahkan input khusus nomor HP)
            // Untuk contoh, gunakan nama sebagai nomor HP jika formatnya benar
            String phone = etName.getText().toString().trim();
            if (phone.isEmpty() || !phone.startsWith("+62")) {
                etName.setError("Masukkan nomor HP dengan format +62");
                return;
            }
            android.content.Intent intent = new android.content.Intent(getActivity(), com.example.itprojek2.OtpActivity.class);
            intent.putExtra("phone", phone);
            startActivity(intent);
        });

        // Tombol Selesai (Register)
        view.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirm = etConfirm.getText().toString().trim();

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

            // TODO: Nanti ganti ke Firebase Auth
            Toast.makeText(getContext(), "Pendaftaran berhasil!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigate(R.id.action_register_to_main);
        });

        // Link ke Login
        view.findViewById(R.id.tvGoToLogin).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_register_to_login));
    }
}
