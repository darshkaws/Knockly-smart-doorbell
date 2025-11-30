package com.example.knockly.loginPage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.knockly.R;
import com.example.knockly.homePage.HomeActivity;
import com.example.knockly.network.dto.Token;
import com.example.knockly.network.dto.UserInfo;
import com.example.knockly.network.repository.AuthRepository;
import com.example.knockly.shared.PageHeaderFragment;
import com.example.knockly.utils.LoginManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateAccountActivity extends AppCompatActivity {
    private LoginManager lm = LoginManager.getInstance(this);
    private AuthRepository repo = AuthRepository.getInstance(lm);

    private final int USERNAME_MIN_LENGTH = 5;
    private final int USERNAME_MAX_LENGTH = 12;

    private final int DISP_NAME_MIN_LENGTH = 2;
    private final int DISP_NAME_MAX_LENGTH = 12;

    private final int PASSWORD_MIN_LENGTH = 6;
    private final int PASSWORD_MAX_LENGTH = 16;

    private EditText createUsername;
    private EditText enterEmail;
    private EditText createDispName;
    private EditText createPass;

    private Button createAccBtn;

    private String username;
    private String email;
    private String dispName;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add header fragment
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance("Create Account", true, false);
        getSupportFragmentManager().beginTransaction().replace(R.id.CreateAccHeaderFragment, pageHeader).commit();

        createUsername = findViewById(R.id.createUsername);
        enterEmail = findViewById(R.id.enterEmail);
        createDispName = findViewById(R.id.createDispName);
        createPass = findViewById(R.id.createPassword);
        createAccBtn = findViewById(R.id.createAccountButton);

        createAccBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check that all input is valid
                if (!checkUsernameGiven()){
                    return;
                }
                if (!checkEmailGiven()){
                    return;
                }
                if (!checkNameGiven()){
                    return;
                }
                if (!checkPassGiven()){
                    return;
                }

                // If all input valid make call to API
                repo.register(username, email, password, dispName).enqueue(new Callback<Token>() {
                    @Override
                    public void onResponse(Call<Token> call, Response<Token> response) {
                        if (response.isSuccessful() && response.body() != null){
                            lm.saveJwt(response.body().access_token);

                            // Get user info like user ID from API
                            repo.getCurrentUser().enqueue(new Callback<UserInfo>() {
                                @Override
                                public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                                    // If userInfo API call successful
                                    if (response.isSuccessful() && response.body() != null){
                                        // Process response body
                                        UserInfo rBdy = response.body();

                                        // Save info to login manager
                                        lm.saveLogin(rBdy.user_id, rBdy.email, rBdy.username, rBdy.display_name);
                                        Log.d("Login Activity", lm.getCurrentUsername());

                                        // Move to home page
                                        startActivity(new Intent(CreateAccountActivity.this, HomeActivity.class));
                                        finish();
                                    }

                                    // If account created but login failed redirect to login page
                                    else{
                                        Toast.makeText(CreateAccountActivity.this, "Login failed, please try again ("+response.code()+")", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(CreateAccountActivity.this, LoginActivity.class));
                                        finish();
                                    }
                                }

                                // If account created but login failed redirect to login page
                                @Override
                                public void onFailure(Call<UserInfo> call, Throwable t) {
                                    Toast.makeText(CreateAccountActivity.this, "Login failed, network error: "+t.getMessage(), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(CreateAccountActivity.this, LoginActivity.class));
                                    finish();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<Token> call, Throwable t) {
                        Toast.makeText(CreateAccountActivity.this, "Account creation failure, network error: "+t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private boolean checkUsernameGiven(){
        username = createUsername.getText().toString().trim();

        if (username.isEmpty()){
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return false;
        }

        if ((username.length() < USERNAME_MIN_LENGTH) || (username.length() > USERNAME_MAX_LENGTH)){
            Toast.makeText(this, String.format("Username must be between %d and %d characters", USERNAME_MIN_LENGTH, USERNAME_MAX_LENGTH), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean checkEmailGiven(){
        email = enterEmail.getText().toString().trim();

        if (email.isEmpty()){
            Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!(email.contains("@")) || !(email.contains("."))){
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean checkNameGiven(){
        dispName = createDispName.getText().toString().trim();

        if (dispName.isEmpty()){
            Toast.makeText(this, "Please enter a display name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (dispName.length() < DISP_NAME_MIN_LENGTH || dispName.length() > DISP_NAME_MAX_LENGTH){
            Toast.makeText(this, String.format("Display name must be between %d and %d characters", DISP_NAME_MIN_LENGTH, DISP_NAME_MAX_LENGTH), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean checkPassGiven(){
        password = createPass.getText().toString().trim();

        if (password.isEmpty()){
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH){
            Toast.makeText(this, String.format("Password must be between %d and %d characters", PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}