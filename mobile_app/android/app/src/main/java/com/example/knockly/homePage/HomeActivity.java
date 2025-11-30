package com.example.knockly.homePage;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.knockly.R;
import com.example.knockly.network.dto.DoorbellOut;
import com.example.knockly.network.repository.AuthRepository;
import com.example.knockly.shared.PageHeaderFragment;
import com.example.knockly.utils.LoginManager;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private DoorbellOut[] doorbells;
    private final LoginManager lm = LoginManager.getInstance(this);
    private final AuthRepository repo = AuthRepository.getInstance(lm);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get relevant doorbells from database
        Log.d("Home Activity", "Fetching doorbells");
        repo.getUsersDoorbells().enqueue(new Callback<DoorbellOut[]>() {
            @Override
            public void onResponse(Call<DoorbellOut[]> call, Response<DoorbellOut[]> response) {
                if (response.isSuccessful() && response.body()!=null){
                    doorbells = response.body();

                    // Add doorbell buttons to page
                    HomePageButtonsFragment buttonsFragment = HomePageButtonsFragment.newInstance(doorbells);
                    getSupportFragmentManager().beginTransaction().replace(R.id.HomeButtonsFragment, buttonsFragment).commit();
                }
                else{
                    Toast.makeText(HomeActivity.this, "Error fetching doorbells " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DoorbellOut[]> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Error fetching doorbells " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Add header fragment
        String displayName = lm.getCurrentDisplayName();
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance("Welcome " + displayName, false, true);
        getSupportFragmentManager().beginTransaction().replace(R.id.HomeHeaderFragment, pageHeader).commit();

    }
}