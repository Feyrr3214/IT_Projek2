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

public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etName = view.findViewById(R.id.etLoginName);
        EditText etPassword = view.findViewById(R.id.etLoginPassword);

        // Tombol Login
        view.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Nama tidak boleh kosong");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password tidak boleh kosong");
                return;
            }

            // TODO: Nanti ganti ke Firebase Auth
            Toast.makeText(getContext(), "Login berhasil!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigate(R.id.action_login_to_main);
        });

        // Link ke Register
        view.findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_register));
    }
}
