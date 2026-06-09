package com.example.itprojek2;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.itprojek2.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();

        // Cek apakah sudah ada sesi login yang valid
        // KEAMANAN: harus validasi ke Firebase Auth, bukan hanya SharedPreferences lokal
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedUid = prefs.getString("uid", null);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        boolean sessionValid = savedUid != null && !savedUid.isEmpty() && firebaseUser != null;

        if (sessionValid) {
            // UID ada di lokal DAN Firebase Auth aktif → skip ke halaman utama
            navController.navigate(R.id.action_welcome_to_main_skip,
                    null,
                    new androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.welcomeFragment, true)
                            .build());
        } else if (savedUid != null && firebaseUser == null) {
            // SharedPreferences ada tapi Firebase Auth sudah expired → bersihkan sesi
            prefs.edit().clear().apply();
            // Tetap di welcomeFragment (default)
        }
        // Kalau tidak ada sesi sama sekali → tetap di welcomeFragment (default startDestination)
    }
}
