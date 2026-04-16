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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        EditText etName = view.findViewById(R.id.etLoginName);
        EditText etPassword = view.findViewById(R.id.etLoginPassword);
        View btnLogin = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Nama tidak boleh kosong");
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

            // Buat email palsu dari nama (sama dengan saat registrasi)
            String fakeEmail = name.toLowerCase().replaceAll("\\s+", "") + "@smartwater.app";

            // Login via Firebase Authentication (gratis - Spark Plan)
            mAuth.signInWithEmailAndPassword(fakeEmail, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            // Ambil data user dari Realtime Database
                            dbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    btnLogin.setEnabled(true);

                                    String storedName = snapshot.child("name").getValue(String.class);
                                    String displayName = storedName != null ? storedName : name;

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
                                            "Gagal ambil data: " + error.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            btnLogin.setEnabled(true);
                            String errMsg = task.getException() != null
                                    ? task.getException().getMessage() : "Error tidak diketahui";

                            // Deteksi error spesifik
                            if (errMsg != null && (errMsg.contains("no user record") || errMsg.contains("user-not-found"))) {
                                etName.setError("Akun tidak ditemukan, silakan daftar dulu");
                                Toast.makeText(getContext(), "Akun tidak ditemukan.", Toast.LENGTH_LONG).show();
                            } else if (errMsg != null && (errMsg.contains("password is invalid") || errMsg.contains("wrong-password"))) {
                                etPassword.setError("Password salah");
                                Toast.makeText(getContext(), "Password yang kamu masukkan salah.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Login gagal: " + errMsg, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Link ke Register
        view.findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_register));
    }
}
