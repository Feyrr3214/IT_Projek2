package com.example.itprojek2.ui;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
    private static final float MIN_SCALE = 0.85f;
    private static final float MIN_ALPHA = 0.5f;

    @Override
    public void transformPage(@NonNull View view, float position) {
        int pageWidth = view.getWidth();
        int pageHeight = view.getHeight();

        // Paksa laman agar diam secara horizontal (mencegah geseran ke samping)
        view.setTranslationX(pageWidth * -position);

        if (position <= -1.0F || position >= 1.0F) {
            // Diluar jangkauan
            view.setAlpha(0f);
        } else if (position <= 0.0F) { 
            // Halaman lama yang mau ditutup:
            // Mundur/Menciut sedikit ke belakang TANPA jadi transparan (agar tak putih)
            view.setTranslationY(0f);
            float scale = 1f - (Math.abs(position) * 0.05f); // Mengecil maksimal 5%
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setAlpha(1f); // Tetap solid tidak tembus
            
            // Atur Elevation agar halaman ini berada di BAWAH halaman baru
            view.setTranslationZ(-1f);
        } else {
            // Halaman baru yang mau muncul:
            // Meluncur dari bawah ke atas menutupi hal. lama
            view.setTranslationY(pageHeight * position);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setAlpha(1f); // Sangat solid
            
            // Atur Elevation agar halaman baru menindih di ATAS (Overlap) halaman lama
            view.setTranslationZ(1f);
        }
    }
}
