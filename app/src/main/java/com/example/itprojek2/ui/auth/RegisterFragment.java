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
            String password = etPassword.getText().toString().trim();
            String confirm  = etConfirm.getText().toString().trim();

            // Validasi input
            if (name.isEmpty()) {
                etName.setError("Nama tidak boleh kosong");
                return;
            }
            if (email.isEmpty()) {
                etEmail.setError("Email tidak boleh kosong");
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
                            // Gagal registrasi Auth
                            btnRegister.setEnabled(true);
                            Toast.makeText(getContext(), "Gagal daftar: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Link ke halaman Login
        view.findViewById(R.id.tvGoToLogin).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_register_to_login));
    }
}
