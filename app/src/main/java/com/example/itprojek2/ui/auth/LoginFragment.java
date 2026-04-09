package com.example.itprojek2.ui.auth;

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

        EditText etName = view.findViewById(R.id.etLoginName);
        EditText etPassword = view.findViewById(R.id.etLoginPassword);
        View btnLogin = view.findViewById(R.id.btnLogin);

        // Tombol Login
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

            // Nonaktifkan tombol sementara cek ke Firebase
            btnLogin.setEnabled(false);
            android.widget.Toast.makeText(getContext(), "Memeriksa akun...", android.widget.Toast.LENGTH_SHORT).show();

            // Query ke Firebase: cari user berdasarkan name
            dbRef.orderByChild("name").equalTo(name)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            btnLogin.setEnabled(true);

                            if (!snapshot.exists()) {
                                // Akun tidak ditemukan di database
                                etName.setError("Akun tidak ada atau belum terdaftar");
                                Toast.makeText(getContext(),
                                        "Akun tidak ditemukan. Silakan daftar terlebih dahulu.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            boolean loginSuccess = false;
                            String loggedInUid = null; // Tambahkan untuk simpan UID

                            // Loop untuk cek password
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                String dbPassword = userSnap.child("password").getValue(String.class);
                                if (dbPassword != null && dbPassword.equals(password)) {
                                    loginSuccess = true;
                                    loggedInUid = userSnap.getKey();
                                    break;
                                }
                            }

                            if (loginSuccess && loggedInUid != null) {
                                // Simpan sesi secara lokal ke SharedPreferences
                                android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
                                prefs.edit().putString("uid", loggedInUid).putString("name", name).apply();

                                Toast.makeText(getContext(), "Login berhasil! Selamat datang, " + name + "!", Toast.LENGTH_SHORT).show();
                                navController.navigate(R.id.action_login_to_main);
                            } else {
                                etPassword.setError("Password salah");
                                Toast.makeText(getContext(), "Password yang kamu masukkan salah.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            btnLogin.setEnabled(true);
                            Toast.makeText(getContext(), "Gagal terhubung ke database: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Link ke Register
        view.findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_register));
    }
}
