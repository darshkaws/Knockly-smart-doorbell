package com.example.knockly;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.knockly.network.repository.AuthRepository;
import com.example.knockly.utils.LoginManager;

// Custom application file to set up app wide components
public class KnocklyApp extends Application {

    // Runs when app starts to initialise any singletons etc.
    @Override
    public void onCreate(){
        super.onCreate();
        LoginManager lm = LoginManager.getInstance(this);
        AuthRepository.getInstance(lm);

        // Create notification channel to send push notifications
        NotificationChannel channel = new NotificationChannel(
                "default_channel",
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        // Turn off dark mode while not tested
        // Delete line to allow dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
