package com.example.itprojek2.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        EditText etName     = view.findViewById(R.id.etRegisterName);
        EditText etPassword = view.findViewById(R.id.etRegisterPassword);
        EditText etConfirm  = view.findViewById(R.id.etRegisterConfirmPassword);
        View btnRegister    = view.findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirm  = etConfirm.getText().toString().trim();

            // Validasi input
            if (name.isEmpty()) {
                etName.setError("Nama tidak boleh kosong");
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Password minimal 6 karakter");
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

            btnRegister.setEnabled(false);
            Toast.makeText(getContext(), "Mendaftarkan akun...", Toast.LENGTH_SHORT).show();

            // Buat email palsu dari nama untuk Firebase Auth (gratis, Email/Password plan)
            // Contoh: "Evelyn" → "evelyn@smartwater.app"
            String fakeEmail = name.toLowerCase().replaceAll("\\s+", "") + "@smartwater.app";

            // Daftarkan ke Firebase Authentication (gratis - Spark Plan)
            mAuth.createUserWithEmailAndPassword(fakeEmail, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            // Simpan data user ke Realtime Database
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", name);
                            userData.put("role", "user");
                            userData.put("createdAt", com.google.firebase.database.ServerValue.TIMESTAMP);

                            dbRef.child(uid).setValue(userData).addOnCompleteListener(dbTask -> {
                                btnRegister.setEnabled(true);
                                if (dbTask.isSuccessful()) {
                                    // Simpan sesi lokal
                                    SharedPreferences prefs = requireActivity()
                                            .getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                                    prefs.edit()
                                            .putString("uid", uid)
                                            .putString("name", name)
                                            .apply();

                                    Toast.makeText(getContext(),
                                            "Pendaftaran berhasil! Selamat datang, " + name + "!",
                                            Toast.LENGTH_SHORT).show();

                                    Navigation.findNavController(v).navigate(R.id.action_register_to_main);
                                } else {
                                    Toast.makeText(getContext(), "Gagal simpan data: " +
                                            dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });

                        } else {
                            btnRegister.setEnabled(true);
                            String errMsg = task.getException() != null
                                    ? task.getException().getMessage() : "Error tidak diketahui";

                            // Cek error spesifik: nama sudah dipakai
                            if (errMsg != null && errMsg.contains("email address is already in use")) {
                                etName.setError("Nama sudah dipakai, coba nama lain");
                                Toast.makeText(getContext(), "Nama sudah terdaftar!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Gagal daftar: " + errMsg, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Link ke halaman Login
        view.findViewById(R.id.tvGoToLogin).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_register_to_login));
    }
}
