package com.example.itprojek2;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.itprojek2.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();

        // Cek apakah sudah ada sesi login yang tersimpan
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedUid = prefs.getString("uid", null);

        if (savedUid != null && !savedUid.isEmpty()) {
            // UID ditemukan → langsung skip ke halaman utama tanpa login lagi
            navController.navigate(R.id.action_welcome_to_main_skip,
                    null,
                    new androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.welcomeFragment, true)
                            .build());
        }
        // Kalau tidak ada sesi → tetap di welcomeFragment (default startDestination)
    }
}
