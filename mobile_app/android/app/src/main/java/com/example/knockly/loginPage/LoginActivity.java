package com.example.knockly.loginPage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
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

import java.util.Objects;
import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class LoginActivity extends AppCompatActivity {
    private final LoginManager lm = LoginManager.getInstance(this);
    private final AuthRepository repo = AuthRepository.getInstance(lm);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check if biometrics can be used
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                break;
            default:
                removeBiometricsButton();
                break;
        }

        // Add header fragment
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance("Knockly Login", false, false);
        getSupportFragmentManager().beginTransaction().replace(R.id.LoginHeaderFragment, pageHeader).commit();

        // Get needed layout elements
        EditText editUsername = findViewById(R.id.editUsername);
        EditText editPassword = findViewById(R.id.editPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView linkRegister = findViewById(R.id.linkRegister);

        Button biometricBtn = findViewById(R.id.biometricsButton);
        TextView biometricLabel = findViewById(R.id.biometricButtonLabel);


        btnLogin.setOnClickListener(v -> {
            String user = editUsername.getText().toString().trim();
            String pass = editPassword.getText().toString().trim();

            // Call API to login
            repo.login(user, pass).enqueue(new Callback<Token>() {
                // On login API response
                @Override public void onResponse(Call<Token> c, Response<Token> rsp) {
                    // If login API call successful
                    if (rsp.isSuccessful() && rsp.body()!=null) {
                        // Save jwt token for future API calls
                        lm.saveJwt(rsp.body().access_token);

                        // Call API to get info about user logged in
                        repo.getCurrentUser().enqueue(new Callback<UserInfo>() {
                            View view = findViewById(R.id.main);
                            @Override
                            // On userInfo API response
                            public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                                // If userInfo API call successful
                                if (response.isSuccessful() && response.body() != null){
                                    // Process response body
                                    UserInfo rBdy = response.body();

                                    // Save info to login manager
                                    lm.saveLogin(rBdy.user_id, rBdy.email, rBdy.username, rBdy.display_name);
                                    Log.d("Login Activity", lm.getCurrentUsername());

                                    // Move to home page
                                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                    finish();
                                }

                                // If userInfo API response unsuccessful
                                else{
                                    Toast.makeText(LoginActivity.this, "Login failed ("+response.code()+")", Toast.LENGTH_SHORT).show();
                                }
                            }

                            // On userInfo API failure
                            @Override
                            public void onFailure(Call<UserInfo> call, Throwable t) {
                                Toast.makeText(LoginActivity.this, "Network error: "+t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    // If login API response unsuccessful
                    } else {
                        Toast.makeText(LoginActivity.this,"Login failed ("+rsp.code()+")", Toast.LENGTH_SHORT).show();
                    }
                }

                // On login API failure
                @Override public void onFailure(Call<Token> c, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Network error: "+t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Check if user can use biometric login
        // If not remove button
        if (lm.loginIsExpired()) {
            // Remove button & label
            removeBiometricsButton();
        } else {
            // Prefill username section if already logged in
            String username = lm.getCurrentUsername();
            editUsername.setText(username);

            // Check if JWT is still valid
            repo.getCurrentUser().enqueue(new Callback<UserInfo>() {
                @Override
                public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                    if (response.isSuccessful()){
                        // If token valid allow biometric login
                        biometricBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                biometricLogin();
                            }
                        });
                    }
                    // If response unsuccessful (unauthorised because of invalid token)
                    // ask to sign in to get fresh token
                    else{
                        removeBiometricsButton();
                    }
                }

                // If response unsuccessful (unauthorised because of invalid token)
                // ask to sign in to get fresh token
                @Override
                public void onFailure(Call<UserInfo> call, Throwable t) {
                    removeBiometricsButton();
                }
            });
        }

        // Link to create account page
        linkRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
                startActivity(intent);
            }
        });
    }
    private void removeBiometricsButton() {
        Button biometricBtn = findViewById(R.id.biometricsButton);
        TextView biometricLabel = findViewById(R.id.biometricButtonLabel);

        biometricBtn.setVisibility(View.GONE);
        biometricLabel.setVisibility(View.GONE);
    }

    private void biometricLogin(){
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.d("Login Activity", "Authetication error: " + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                View view = findViewById(android.R.id.content);
                Toast.makeText(getApplicationContext(),"Biometric login failed. Please try again", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Log in using biometrics")
                .setSubtitle("")
                .setNegativeButtonText("Use password instead")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

}