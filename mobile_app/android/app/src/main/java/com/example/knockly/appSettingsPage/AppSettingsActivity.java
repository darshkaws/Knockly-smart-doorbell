package com.example.knockly.appSettingsPage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.knockly.R;
import com.example.knockly.loginPage.LoginActivity;
import com.example.knockly.shared.PageHeaderFragment;
import com.example.knockly.utils.LoginManager;

public class AppSettingsActivity extends AppCompatActivity {
    private LoginManager lm = LoginManager.getInstance(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add header fragment
        String displayName = lm.getCurrentDisplayName();
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance(displayName + "'s Settings", true, false);
        getSupportFragmentManager().beginTransaction().replace(R.id.AppSettingsHeaderFragment, pageHeader).commit();

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lm.logout();
                Intent intent = new Intent(AppSettingsActivity.this, LoginActivity.class);
                startActivity(intent);
                ActivityCompat.finishAffinity(AppSettingsActivity.this);
            }
        });

    }
}