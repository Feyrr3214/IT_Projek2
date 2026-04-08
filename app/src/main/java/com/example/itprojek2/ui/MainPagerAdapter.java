package com.example.itprojek2.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.itprojek2.ui.history.HistoryFragment;
import com.example.itprojek2.ui.home.HomeFragment;
import com.example.itprojek2.ui.profile.ProfileFragment;
import com.example.itprojek2.ui.settings.SettingsFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    // Daftar posisi fragment
    public static final int PAGE_HOME = 0;
    public static final int PAGE_HISTORY = 1;
    public static final int PAGE_SETTINGS = 2;
    public static final int PAGE_PROFILE = 3;

    public MainPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case PAGE_HISTORY:
                return new HistoryFragment();
            case PAGE_SETTINGS:
                return new SettingsFragment();
            case PAGE_PROFILE:
                return new ProfileFragment();
            case PAGE_HOME:
            default:
                return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
