package com.example.itprojek2.ui.profile;

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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class EditProfileFragment extends Fragment {

    private EditText etName, etPhone;
    private ImageView ivEditAvatar;
    private DatabaseReference dbRef;
    private String uId;
    
    private String base64Avatar = null;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream imageStream = requireContext().getContentResolver().openInputStream(imageUri);
                        Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        
                        // Resize agar tidak terlalu besar di Firebase (Maks 500x500)
                        int maxDim = 500;
                        int width = selectedImage.getWidth();
                        int height = selectedImage.getHeight();
                        if (width > maxDim || height > maxDim) {
                            float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
                            width = Math.round(ratio * width);
                            height = Math.round(ratio * height);
                            selectedImage = Bitmap.createScaledBitmap(selectedImage, width, height, true);
                        }

                        // Set ke ImageView
                        ivEditAvatar.setImageBitmap(selectedImage);

                        // Konversi ke Base64
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        selectedImage.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Kompresi 70%
                        byte[] imageBytes = baos.toByteArray();
                        base64Avatar = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                        
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.etName);
        etPhone = view.findViewById(R.id.etPhone);
        ivEditAvatar = view.findViewById(R.id.ivEditAvatar);

        // Ambil uId dari SharedPreferences (fallback ke FirebaseAuth)
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        uId = prefs.getString("uid", null);

        if (uId == null) {
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) uId = currentUser.getUid();
        }

        if (uId == null) {
            Toast.makeText(getContext(), "User tidak valid", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("users").child(uId);

        // Load data saat ini
        loadCurrentData();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Pilih foto
        view.findViewById(R.id.ivEditAvatar).getParent().requestLayout(); // sekedar refresh layout parent FrameLayout
        View btnCamera = ((View) view.findViewById(R.id.ivEditAvatar).getParent()); // FrameLayout yang membungkus gambar
        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // Simpan
        view.findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfileData(view));
    }

    private void loadCurrentData() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);
                String avatar = snapshot.child("avatar").getValue(String.class);

                if (name != null) etName.setText(name);
                if (phone != null) {
                    String cleanPhone = phone;
                    if (cleanPhone.startsWith("+62")) {
                        cleanPhone = cleanPhone.substring(3).trim();
                    } else if (cleanPhone.startsWith("0")) {
                        cleanPhone = cleanPhone.substring(1).trim();
                    }
                    etPhone.setText(cleanPhone);
                }

                if (avatar != null && !avatar.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivEditAvatar.setImageBitmap(decodedByte);
                        base64Avatar = avatar; // simpan referensi jika gak diganti
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(getContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileData(View view) {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(getContext(), "Nama dan Nomor Telepon tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Pakaikan kembali format +62 saat disimpan ke database
        if (phone.startsWith("0")) {
            phone = phone.substring(1);
        }
        phone = "+62 " + phone.replaceAll("[^0-9]", "");

        view.findViewById(R.id.btnSaveProfile).setEnabled(false);

        java.util.HashMap<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        if (base64Avatar != null) {
            updates.put("avatar", base64Avatar);
        }

        dbRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            view.findViewById(R.id.btnSaveProfile).setEnabled(true);

            if (task.isSuccessful()) {
                // Update SharedPreferences name biar header langsung berubah
                requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
                        .edit().putString("name", name).apply();

                Toast.makeText(getContext(), "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(view).navigateUp();
            } else {
                Toast.makeText(getContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
