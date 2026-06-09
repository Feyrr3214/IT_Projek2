package com.example.itprojek2.ui.profile;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.itprojek2.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordFragment extends Fragment {

    private boolean isOldPassVisible = false;
    private boolean isNewPassVisible = false;
    private boolean isConfirmPassVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> 
                Navigation.findNavController(v).navigateUp());

        EditText etOld = view.findViewById(R.id.etOldPassword);
        EditText etNew = view.findViewById(R.id.etNewPassword);
        EditText etConfirm = view.findViewById(R.id.etConfirmPassword);
        
        ImageView ivToggleOld = view.findViewById(R.id.ivToggleOldPass);
        ImageView ivToggleNew = view.findViewById(R.id.ivToggleNewPass);
        ImageView ivToggleConfirm = view.findViewById(R.id.ivToggleConfirmPass);

        ivToggleOld.setOnClickListener(v -> {
            isOldPassVisible = !isOldPassVisible;
            togglePasswordVisibility(etOld, ivToggleOld, isOldPassVisible);
        });

        ivToggleNew.setOnClickListener(v -> {
            isNewPassVisible = !isNewPassVisible;
            togglePasswordVisibility(etNew, ivToggleNew, isNewPassVisible);
        });

        ivToggleConfirm.setOnClickListener(v -> {
            isConfirmPassVisible = !isConfirmPassVisible;
            togglePasswordVisibility(etConfirm, ivToggleConfirm, isConfirmPassVisible);
        });

        View btnSave = view.findViewById(R.id.btnSavePassword);
        btnSave.setOnClickListener(v -> {
            String oldPass     = etOld.getText().toString();
            String newPass     = etNew.getText().toString();
            String confirmPass = etConfirm.getText().toString();

            // Validasi input
            if (oldPass.isEmpty()) {
                etOld.setError("Password lama tidak boleh kosong");
                etOld.requestFocus();
                return;
            }
            if (newPass.isEmpty()) {
                etNew.setError("Password baru tidak boleh kosong");
                etNew.requestFocus();
                return;
            }
            // Validasi rules password baru (konsisten dengan aturan saat registrasi)
            String passwordError = validatePassword(newPass);
            if (passwordError != null) {
                etNew.setError(passwordError);
                etNew.requestFocus();
                return;
            }
            if (oldPass.equals(newPass)) {
                etNew.setError("Password baru tidak boleh sama dengan password lama");
                etNew.requestFocus();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                etConfirm.setError("Konfirmasi password tidak cocok");
                etConfirm.requestFocus();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || user.getEmail() == null) {
                Toast.makeText(getContext(), "Sesi login tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show();
                return;
            }

            // Nonaktifkan tombol agar tidak double submit
            btnSave.setEnabled(false);
            Toast.makeText(getContext(), "Memverifikasi password lama...", Toast.LENGTH_SHORT).show();

            // Re-authenticate dengan password lama dulu
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
            user.reauthenticate(credential).addOnCompleteListener(reAuthTask -> {
                if (reAuthTask.isSuccessful()) {
                    // Re-auth berhasil, update ke password baru
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        btnSave.setEnabled(true);
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(getContext(),
                                    "Password berhasil diperbarui! Silakan login ulang.",
                                    Toast.LENGTH_LONG).show();
                            // Logout agar user login ulang pakai password baru
                            FirebaseAuth.getInstance().signOut();
                            Navigation.findNavController(v).navigate(R.id.action_changePassword_to_welcome);
                        } else {
                            Toast.makeText(getContext(),
                                    "Gagal memperbarui password, coba lagi.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Password lama salah
                    btnSave.setEnabled(true);
                    etOld.setError("Password lama tidak sesuai");
                    etOld.requestFocus();
                    Toast.makeText(getContext(), "Password lama salah, coba lagi.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageView icon, boolean isVisible) {
        if (isVisible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            icon.setImageResource(R.drawable.ic_visibility);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            icon.setImageResource(R.drawable.ic_visibility_off);
        }
        editText.setSelection(editText.getText().length());
    }

    /**
     * Validasi rules password (konsisten dengan RegisterFragment):
     * - Minimal 8 karakter
     * - Wajib ada huruf kapital (A-Z)
     * - Wajib ada huruf kecil (a-z)
     * - Wajib ada angka (0-9)
     * - TIDAK boleh mengandung simbol atau spasi
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
