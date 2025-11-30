package com.example.knockly.manageDoorbellPage;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.knockly.FirebaseMessageReceiver;
import com.example.knockly.R;
import com.example.knockly.homePage.HomeActivity;
import com.example.knockly.network.dto.IsSubscribedResponse;
import com.example.knockly.network.repository.AuthRepository;
import com.example.knockly.shared.PageHeaderFragment;
import com.example.knockly.utils.LoginManager;
import com.example.knockly.utils.PermissionUtils;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageDoorbellActivity extends AppCompatActivity {
    private AuthRepository repo = AuthRepository.getInstance(LoginManager.getInstance(this));
    private String fcmToken;
    ToggleButton getNotifsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_doorbell);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get info about doorbell from intent that started activity
        Intent intent = getIntent();
        String doorbellId = intent.getStringExtra("Doorbell_ID");
        String doorbellName = intent.getStringExtra("Doorbell_Name");

        // Add header fragment
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance(doorbellName + " Settings", true, true);
        getSupportFragmentManager().beginTransaction().replace(R.id.ManageDoorbellHeaderFragment, pageHeader).commit();

        getNotifsButton = findViewById(R.id.ReceiveNotifsButton);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            // Once fcm token received
            fcmToken = task.getResult();
            repo.checkIsSubscribed(fcmToken, doorbellId).enqueue(new Callback<IsSubscribedResponse>() {
                @Override
                public void onResponse(Call<IsSubscribedResponse> call, Response<IsSubscribedResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        getNotifsButton.setChecked(response.body().is_subscribed);
                    } else {
                        Toast.makeText(ManageDoorbellActivity.this, "Error fetching notifications " + response.code(), Toast.LENGTH_SHORT).show();
                        Log.d("Manage Doorbell Activity", "Error checking if user is subscribed");
                    }
                    attachToggleListener(getNotifsButton, doorbellId);
                }

                @Override
                public void onFailure(Call<IsSubscribedResponse> call, Throwable t) {
                    Toast.makeText(ManageDoorbellActivity.this, "Error fetching notifications " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("Manage Doorbell Activity", "Error checking if user is subscribed");
                    attachToggleListener(getNotifsButton, doorbellId);
                }
            });
        });
    }

    private void attachToggleListener(ToggleButton getNotifsButton, String doorbellId){
        getNotifsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    if (!PermissionUtils.hasNotifPerms(ManageDoorbellActivity.this)){
                        Log.d("ManageDoorbellActivity", "Getting notification permissions");
                        PermissionUtils.getNotifPerms(ManageDoorbellActivity.this);
                    }
                    repo.subscribe(fcmToken, doorbellId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (!response.isSuccessful()) {
                                Toast.makeText(ManageDoorbellActivity.this, "Error subscribing to notifications " + response.code(), Toast.LENGTH_SHORT).show();
                                Log.d("Manage Doorbell Activity", "Error subscribing user");
                                getNotifsButton.setChecked(false);

                                // Update/set token in shared prefs for future updates
                                SharedPreferences fcmPrefs = getSharedPreferences("fcmPrefs", MODE_PRIVATE);
                                fcmPrefs.edit().putString("old_token", fcmToken).apply();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(ManageDoorbellActivity.this, "Error subscribing to notifications " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.d("Manage Doorbell Activity", "Error  subscribing user");
                            getNotifsButton.setChecked(false);
                        }
                    });
                }
                else{
                    repo.unsubscribe(fcmToken, doorbellId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (!response.isSuccessful()){
                                Toast.makeText(ManageDoorbellActivity.this, "Error unsubscribing from notifications " + response.code(), Toast.LENGTH_SHORT).show();
                                Log.d("Manage Doorbell Activity", "Error unsubscribing user");
                                getNotifsButton.setChecked(true);
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(ManageDoorbellActivity.this, "Error unsubscribing from notifications " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.d("Manage Doorbell Activity", "Error unsubscribing user");
                            getNotifsButton.setChecked(true);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100){
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(ManageDoorbellActivity.this, "Notification permissions not granted. Please enable in settings to turn this feature on", Toast.LENGTH_SHORT).show();
                getNotifsButton.setChecked(false);
            }
        }
    }
}

