package com.example.itprojek2.ui.auth;

import android.view.animation.AnimationUtils;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.itprojek2.R;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Animasi untuk logo
        ImageView ivLogo = view.findViewById(R.id.ivLogo);
        ivLogo.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in));

        // Animasi untuk teks
        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        TextView tvSubtitle = view.findViewById(R.id.tvSubtitle);
        
        tvWelcome.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_with_alpha));
        tvSubtitle.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_with_alpha));

        // Animasi untuk tombol
        View btnNext = view.findViewById(R.id.btnNext);
        btnNext.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.scale_bounce));

        btnNext.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_welcome_to_login));
    }
}
