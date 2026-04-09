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

        // Load data profil dari Firebase
        loadUserProfile(view);
    }

    private void loadUserProfile(View view) {
        // Ambil UID dari SharedPreferences (jika user login manual pakai nama)
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String uid = prefs.getString("uid", null);

        // Fallback kalau gak ada di SharedPreferences (misal dari Auth resmi)
        if (uid == null) {
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                uid = currentUser.getUid();
            } else {
                return; // User belum login
            }
        }
        com.google.firebase.database.DatabaseReference userRef =
                com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("users").child(uid);

        userRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!isAdded() || snapshot == null) return;

                String name  = snapshot.child("name").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);
                String avatar = snapshot.child("avatar").getValue(String.class);

                android.widget.TextView tvUserName  = view.findViewById(R.id.tvUserName);
                android.widget.TextView tvAboutName = view.findViewById(R.id.tvAboutName);
                android.widget.TextView tvAboutPhone = view.findViewById(R.id.tvAboutPhone);
                android.widget.TextView tvHeaderName = view.findViewById(R.id.tvHeaderName);

                if (avatar != null && !avatar.isEmpty()) {
                    try {
                        byte[] decodedString = android.util.Base64.decode(avatar, android.util.Base64.DEFAULT);
                        android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        com.google.android.material.imageview.ShapeableImageView ivAvatar = view.findViewById(R.id.ivAvatar);
                        
                        ivAvatar.setImageBitmap(decodedByte);
                        
                        // Hapus tint warna ungu, pastikan scaleType-nya crop agar sesuai lingkaran
                        ivAvatar.setImageTintList(null);
                        ivAvatar.setPadding(0, 0, 0, 0);
                        ivAvatar.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }



                if (name != null) {
                    tvUserName.setText(name);
                    tvAboutName.setText(name);
                    
                    // Supaya tulisan Halo, User mengambil nama depan aja (kalau panjang)
                    String firstName = name.contains(" ") ? name.substring(0, name.indexOf(" ")) : name;
                    tvHeaderName.setText("Halo, " + firstName);
                }
                if (phone != null) {
                    String formattedPhone = phone;
                    // Format permintaan: +62 85849566409 (hanya spasi setelah +62)
                    if (phone.startsWith("+62") && phone.length() > 3) {
                        String rest = phone.substring(3).replaceAll("[^0-9]", "");
                        formattedPhone = "+62 " + rest;
                    }
                    tvAboutPhone.setText(formattedPhone);
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
            
            // Logout dari Firebase
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            
            // Hapus sesi lokal (SharedPreferences)
            requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE).edit().clear().apply();
            
            // Kembali ke halaman awal (Welcome) dengan cara restart Activity ke state awal
            android.content.Intent intent = new android.content.Intent(requireActivity(), com.example.itprojek2.MainActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
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
