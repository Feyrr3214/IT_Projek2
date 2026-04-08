package com.example.itprojek2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.concurrent.TimeUnit;

public class OtpActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText edtPhone;
    private EditText edtOtp;
    private Button btnSendOtp;
    private Button btnVerify;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        auth = FirebaseAuth.getInstance();
        edtPhone = findViewById(R.id.edtPhone);
        edtOtp = findViewById(R.id.edtOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerify = findViewById(R.id.btnVerify);

        btnSendOtp.setOnClickListener(v -> {
            String phone = edtPhone.getText().toString().trim();
            if (!phone.isEmpty()) {
                sendOtp(phone);
            } else {
                Toast.makeText(this, "Masukkan nomor HP", Toast.LENGTH_SHORT).show();
            }
        });

        btnVerify.setOnClickListener(v -> {
            String otp = edtOtp.getText().toString().trim();
            if (!otp.isEmpty() && verificationId != null) {
                verifyOtp(otp);
            } else {
                Toast.makeText(this, "Masukkan kode OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendOtp(String phone) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Toast.makeText(OtpActivity.this, "Gagal mengirim OTP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String vid, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = vid;
                        Toast.makeText(OtpActivity.this, "Kode OTP dikirim", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtp(String otp) {
        if (verificationId != null) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
            signInWithCredential(credential);
        }
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verifikasi berhasil!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Verifikasi gagal!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
