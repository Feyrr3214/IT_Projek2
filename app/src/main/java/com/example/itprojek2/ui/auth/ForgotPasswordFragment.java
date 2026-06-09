package com.example.itprojek2.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.itprojek2.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordFragment extends Fragment {

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.textfield.TextInputLayout tilEmail =
                view.findViewById(R.id.tilForgotEmail);
        com.google.android.material.textfield.TextInputEditText etEmail =
                view.findViewById(R.id.etForgotEmail);
        com.google.android.material.button.MaterialButton btnSend =
                view.findViewById(R.id.btnSendReset);
        View tvBackToLogin = view.findViewById(R.id.tvBackToLogin);

        // ── Tombol Kirim ─────────────────────────────────────────────────
        btnSend.setOnClickListener(v -> {
            String email = etEmail.getText() != null
                    ? etEmail.getText().toString().trim()
                    : "";

            // Validasi input
            if (email.isEmpty()) {
                tilEmail.setError("Email tidak boleh kosong");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Format email tidak valid");
                etEmail.requestFocus();
                return;
            }
            tilEmail.setError(null); // bersihkan error sebelumnya

            btnSend.setEnabled(false);
            btnSend.setText("Mengirim...");

            // Kirim email reset via Firebase Auth
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        btnSend.setEnabled(true);
                        btnSend.setText("Kirim Link Reset");

                        if (task.isSuccessful()) {
                            // Berhasil — beri info tanpa membocorkan apakah email terdaftar
                            Toast.makeText(
                                    getContext(),
                                    "Jika email terdaftar, link reset akan dikirim ke " + email
                                            + ". Cek kotak masuk atau folder spam kamu.",
                                    Toast.LENGTH_LONG
                            ).show();

                            // Kembali ke halaman login setelah berhasil
                            Navigation.findNavController(v)
                                    .navigate(R.id.action_forgotPassword_to_login);

                        } else {
                            // Tampilkan pesan error generik tanpa detail teknis
                            Toast.makeText(
                                    getContext(),
                                    "Gagal mengirim email. Periksa koneksi internet dan coba lagi.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        // ── Link Kembali ke Login ─────────────────────────────────────────
        tvBackToLogin.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_forgotPassword_to_login));
    }
}
