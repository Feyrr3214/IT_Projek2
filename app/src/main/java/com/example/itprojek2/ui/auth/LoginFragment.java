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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {
    private DatabaseReference dbRef;

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

        EditText etEmail = view.findViewById(R.id.etLoginEmail);
        EditText etPassword = view.findViewById(R.id.etLoginPassword);
        View btnLogin = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email tidak boleh kosong");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password tidak boleh kosong");
                return;
            }

            // Simpan NavController sebelum masuk callback async
            androidx.navigation.NavController navController = Navigation.findNavController(v);

            btnLogin.setEnabled(false);
            Toast.makeText(getContext(), "Memeriksa akun...", Toast.LENGTH_SHORT).show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Login berhasil
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
                                        Toast.makeText(getContext(), "Gagal mengambil data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            // Login gagal
                            btnLogin.setEnabled(true);
                            Toast.makeText(getContext(), "Email atau password salah", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Link ke Register
        view.findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_register));
    }
}
