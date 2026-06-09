package com.example.itprojek2.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbRef = FirebaseDatabase.getInstance().getReference("users");
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();

        EditText etName     = view.findViewById(R.id.etRegisterName);
        EditText etEmail    = view.findViewById(R.id.etRegisterEmail);
        EditText etPassword = view.findViewById(R.id.etRegisterPassword);
        EditText etConfirm  = view.findViewById(R.id.etRegisterConfirmPassword);
        View btnRegister    = view.findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm  = etConfirm.getText().toString();

            // ── Validasi Nama ──────────────────────────────────────────────────
            if (name.isEmpty()) {
                etName.setError("Nama tidak boleh kosong");
                etName.requestFocus();
                return;
            }
            if (name.length() < 3) {
                etName.setError("Nama minimal 3 karakter");
                etName.requestFocus();
                return;
            }
            if (!name.matches("[a-zA-Z ]+")) {
                etName.setError("Nama hanya boleh berisi huruf dan spasi (tanpa angka/simbol)");
                etName.requestFocus();
                return;
            }

            // ── Validasi Email ─────────────────────────────────────────────────
            if (email.isEmpty()) {
                etEmail.setError("Email tidak boleh kosong");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Format email tidak valid (contoh: nama@gmail.com)");
                etEmail.requestFocus();
                return;
            }

            // ── Validasi Password ──────────────────────────────────────────────
            String passwordError = validatePassword(password);
            if (passwordError != null) {
                etPassword.setError(passwordError);
                etPassword.requestFocus();
                return;
            }

            // ── Validasi Konfirmasi Password ───────────────────────────────────
            if (confirm.isEmpty()) {
                etConfirm.setError("Konfirmasi password tidak boleh kosong");
                etConfirm.requestFocus();
                return;
            }
            if (!password.equals(confirm)) {
                etConfirm.setError("Password tidak cocok");
                etConfirm.requestFocus();
                return;
            }

            btnRegister.setEnabled(false);
            Toast.makeText(getContext(), "Mendaftarkan akun...", Toast.LENGTH_SHORT).show();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Registrasi Auth berhasil
                            com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();

                                // Simpan data tambahan ke Realtime Database
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", name);
                                userData.put("email", email);
                                userData.put("role", "user");
                                userData.put("createdAt", com.google.firebase.database.ServerValue.TIMESTAMP);

                                dbRef.child(uid).setValue(userData).addOnCompleteListener(dbTask -> {
                                    btnRegister.setEnabled(true);
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(getContext(),
                                                "Pendaftaran berhasil! Silakan login dengan akun baru kamu.",
                                                Toast.LENGTH_SHORT).show();

                                        // Logout dulu supaya user harus login manual
                                        mAuth.signOut();

                                        Navigation.findNavController(v).navigate(R.id.action_register_to_login);
                                    } else {
                                        Toast.makeText(getContext(), "Gagal menyimpan data pengguna: " +
                                                dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } else {
                            // Gagal registrasi Auth — tampilkan pesan generik agar tidak bocorkan info
                            btnRegister.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "Pendaftaran gagal. Periksa kembali data kamu atau coba lagi nanti.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Link ke halaman Login
        view.findViewById(R.id.tvGoToLogin).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_register_to_login));
    }

    /**
     * Validasi rules password:
     * - Minimal 8 karakter
     * - Wajib ada huruf kapital (A-Z)
     * - Wajib ada huruf kecil (a-z)
     * - Wajib ada angka (0-9)
     * - TIDAK boleh mengandung simbol
     *
     * @return pesan error jika tidak valid, null jika valid
     */
    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "Password minimal 8 karakter";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password wajib mengandung minimal 1 huruf kapital (A-Z)";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password wajib mengandung minimal 1 huruf kecil (a-z)";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password wajib mengandung minimal 1 angka (0-9)";
        }
        if (!password.matches("[a-zA-Z0-9]+")) {
            return "Password tidak boleh mengandung simbol atau spasi";
        }
        return null; // Password valid
    }
}
