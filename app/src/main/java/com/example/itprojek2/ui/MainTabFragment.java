package com.example.itprojek2.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.itprojek2.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainTabFragment extends Fragment {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.view_pager);
        bottomNav = view.findViewById(R.id.bottom_navigation);

        // Pasang Adapter
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Hapus limit preload jika ingin fragment di memori
        viewPager.setOffscreenPageLimit(3); 

        // Efek kustom Transformer dihapus sesuai permintaan


        // Matikan kemampuan usapan (swipe) geser layar antar tab layarnya (sesuai permintaan)
        viewPager.setUserInputEnabled(false);

        // Sinkronisasi BottomNav dengan ViewPager
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case MainPagerAdapter.PAGE_HOME:
                        bottomNav.setSelectedItemId(R.id.homeFragment);
                        break;
                    case MainPagerAdapter.PAGE_HISTORY:
                        bottomNav.setSelectedItemId(R.id.historyFragment);
                        break;
                    case MainPagerAdapter.PAGE_SETTINGS:
                        bottomNav.setSelectedItemId(R.id.settingsFragment);
                        break;
                    case MainPagerAdapter.PAGE_PROFILE:
                        bottomNav.setSelectedItemId(R.id.profileFragment);
                        break;
                }
            }
        });

        // Sinkronisasi ViewPager saat BottomNav diklik (false = tanpa animasi geser mulus)
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeFragment) {
                viewPager.setCurrentItem(MainPagerAdapter.PAGE_HOME, false);
                return true;
            } else if (itemId == R.id.historyFragment) {
                viewPager.setCurrentItem(MainPagerAdapter.PAGE_HISTORY, false);
                return true;
            } else if (itemId == R.id.settingsFragment) {
                viewPager.setCurrentItem(MainPagerAdapter.PAGE_SETTINGS, false);
                return true;
            } else if (itemId == R.id.profileFragment) {
                viewPager.setCurrentItem(MainPagerAdapter.PAGE_PROFILE, false);
                return true;
            }
            return false;
        });
    }
}
