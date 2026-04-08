package com.example.itprojek2.ui.profile;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.itprojek2.R;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        androidx.navigation.NavOptions options = new androidx.navigation.NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right).build();

        // Edit Profile
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.editProfileFragment, null, options)
        );

        // Change Password
        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.changePasswordFragment, null, options)
        );

        // Notification Icon
        view.findViewById(R.id.ivNotification).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.notificationFragment, null, options)
        );

        // Logout
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        // Delete Account
        view.findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnLogoutConfirm).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Sedang Keluar...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            // Implementasikan kelogic log out firebase (mAuth.signOut()) di sini nanti
        });

        dialog.show();
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btnDeleteConfirm).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Akun berhasil dihapus", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}
