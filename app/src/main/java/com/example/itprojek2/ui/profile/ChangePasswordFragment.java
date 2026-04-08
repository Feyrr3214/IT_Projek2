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

        view.findViewById(R.id.btnSavePassword).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Password berhasil diperbarui!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigateUp();
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
}
