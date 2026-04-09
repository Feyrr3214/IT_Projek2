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
    private com.google.firebase.database.DatabaseReference dbRef;
    private EditText edtPhone;
    private EditText edtOtp;
    private Button btnSendOtp;
    private Button btnVerify;
    private String verificationId;
    
    // Data registrasi dari halaman sebelumnya
    private String regName, regPassword, regPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        auth = FirebaseAuth.getInstance();
        dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users");
        
        edtPhone = findViewById(R.id.edtPhone);
        edtOtp = findViewById(R.id.edtOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerify = findViewById(R.id.btnVerify);

        // Ambil data dari intent
        regName = getIntent().getStringExtra("name");
        regPassword = getIntent().getStringExtra("password");
        regPhone = getIntent().getStringExtra("phone");

        if (regPhone != null) {
            edtPhone.setText(regPhone);
            // Otomatis kirim OTP saat masuk jika nomor sudah ada
            sendOtp(regPhone);
        }

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
                        Toast.makeText(OtpActivity.this, "Kode OTP dikirim ke " + phone, Toast.LENGTH_SHORT).show();
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
                        com.google.firebase.auth.FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid());
                        }
                    } else {
                        Toast.makeText(this, "Verifikasi gagal!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid) {
        // Jika data pendaftaran lengkap, simpan ke Database
        if (regName != null && regPassword != null) {
            java.util.Map<String, Object> userData = new java.util.HashMap<>();
            userData.put("name", regName);
            userData.put("password", regPassword);
            userData.put("phone", regPhone != null ? regPhone : edtPhone.getText().toString().trim());
            userData.put("role", "user");
            userData.put("createdAt", com.google.firebase.database.ServerValue.TIMESTAMP);

            dbRef.child(uid).setValue(userData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                } else {
                    Toast.makeText(this, "Gagal simpan data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Jika hanya login OTP biasa tanpa data registrasi
            goToMainActivity();
        }
    }

    private void goToMainActivity() {
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
