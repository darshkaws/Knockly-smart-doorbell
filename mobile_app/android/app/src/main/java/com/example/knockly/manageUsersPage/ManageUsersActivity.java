package com.example.knockly.manageUsersPage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.knockly.R;
import com.example.knockly.network.dto.LinkedUser;
import com.example.knockly.network.repository.AuthRepository;
import com.example.knockly.shared.PageHeaderFragment;
import com.example.knockly.utils.LoginManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUsersActivity extends AppCompatActivity {
    private AuthRepository repo = AuthRepository.getInstance(LoginManager.getInstance(this));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_users);
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
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance("Manage Users", true, true);
        getSupportFragmentManager().beginTransaction().replace(R.id.ManageUsersHeaderFragment, pageHeader).commit();

        // Get linked users from API
        repo.getDoorbellLinkedUsers(doorbellId).enqueue(new Callback<LinkedUser[]>() {
            @Override
            public void onResponse(Call<LinkedUser[]> call, Response<LinkedUser[]> response) {
                if (response.isSuccessful() && response.body() != null){
                    // Pass users to fragment and load fragment
                    LinkedUsersFragment usersFragment = LinkedUsersFragment.newInstance(response.body());
                    getSupportFragmentManager().beginTransaction().replace(R.id.LinkedUsersFragment, usersFragment).commit();
                }
                else{
                    Log.d("Manage Users Activity", "Error fetching linked users" + response.code());
                }
            }

            @Override
            public void onFailure(Call<LinkedUser[]> call, Throwable t) {
                Log.d("Manage Users Activity", "Error fetching linked users" + t.getMessage());
            }
        });
    }
}