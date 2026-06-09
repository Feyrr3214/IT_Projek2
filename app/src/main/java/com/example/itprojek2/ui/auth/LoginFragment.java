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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {
    private DatabaseReference dbRef;

    // ── Proteksi Brute Force ─────────────────────────────────────────────
    private static final int    MAX_ATTEMPTS       = 3;      // maks percobaan gagal
    private static final long   COOLDOWN_MS        = 30_000; // cooldown 30 detik
    private              int    loginAttempts      = 0;
    private              long   lastFailedAttemptTime = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbRef = FirebaseDatabase.getInstance().getReference("users");
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();

        EditText etEmail    = view.findViewById(R.id.etLoginEmail);
        EditText etPassword = view.findViewById(R.id.etLoginPassword);
        View btnLogin       = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {

            // ── Cek Cooldown Brute Force ────────────────────────────────
            long currentTime = System.currentTimeMillis();
            if (loginAttempts >= MAX_ATTEMPTS) {
                long elapsed = currentTime - lastFailedAttemptTime;
                if (elapsed < COOLDOWN_MS) {
                    long sisaDetik = (COOLDOWN_MS - elapsed) / 1000;
                    Toast.makeText(getContext(),
                            "Terlalu banyak percobaan gagal. Tunggu " + sisaDetik + " detik lagi.",
                            Toast.LENGTH_LONG).show();
                    return;
                } else {
                    // Reset setelah cooldown selesai
                    loginAttempts = 0;
                }
            }

            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            // ── Validasi Input Lokal ──────────────────────────────────────
            if (email.isEmpty()) {
                etEmail.setError("Email tidak boleh kosong");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Format email tidak valid");
                etEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password tidak boleh kosong");
                etPassword.requestFocus();
                return;
            }

            // Simpan NavController sebelum masuk callback async
            androidx.navigation.NavController navController = Navigation.findNavController(v);

            btnLogin.setEnabled(false);
            Toast.makeText(getContext(), "Memeriksa akun...", Toast.LENGTH_SHORT).show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Login berhasil — reset counter
                            loginAttempts = 0;

                            com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();

                                // Ambil nama user dari Realtime Database
                                dbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        btnLogin.setEnabled(true);
                                        String displayName = "Pengguna";
                                        if (snapshot.exists()) {
                                            String storedName = snapshot.child("name").getValue(String.class);
                                            if (storedName != null) {
                                                displayName = storedName;
                                            }
                                        }

                                        // Simpan sesi lokal
                                        SharedPreferences prefs = requireActivity()
                                                .getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                                        prefs.edit()
                                                .putString("uid", uid)
                                                .putString("name", displayName)
                                                .apply();

                                        Toast.makeText(getContext(),
                                                "Login berhasil! Selamat datang, " + displayName + "!",
                                                Toast.LENGTH_SHORT).show();
                                        navController.navigate(R.id.action_login_to_main);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        btnLogin.setEnabled(true);
                                        Toast.makeText(getContext(),
                                                "Gagal memuat data. Coba lagi nanti.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            // Login gagal — tambah counter & catat waktu
                            btnLogin.setEnabled(true);
                            loginAttempts++;
                            lastFailedAttemptTime = System.currentTimeMillis();

                            // Pesan generik: tidak membocorkan apakah email terdaftar atau tidak
                            int sisaPercobaan = MAX_ATTEMPTS - loginAttempts;
                            if (sisaPercobaan > 0) {
                                Toast.makeText(getContext(),
                                        "Email atau password salah. Sisa percobaan: " + sisaPercobaan,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(),
                                        "Terlalu banyak percobaan gagal. Silakan tunggu 30 detik.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Link ke Register
        view.findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_register));

        // Link ke Lupa Password
        view.findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_forgotPassword));
    }
}
