package com.example.knockly;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.knockly.network.repository.AuthRepository;
import com.example.knockly.utils.LoginManager;
import com.example.knockly.utils.PermissionUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirebaseMessageReceiver extends FirebaseMessagingService {
    private AuthRepository repo = AuthRepository.getInstance(LoginManager.getInstance(this));

    // Override onNewToken to get new token
    @Override
    public void onNewToken(@NonNull String token)
    {
        Log.d("Firebase Message Reciever", "Refreshed token: " + token);
        SharedPreferences fcmPrefs = getSharedPreferences("fcmPrefs", MODE_PRIVATE);
        String old_token = fcmPrefs.getString("old_token", null);
        if (old_token == null){
            Log.d("Firebase Message Receiver", "Error updating token, old token could not be found");
            return;
        }

        repo.updateFCMToken(old_token, token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("Firebase Message Receiver", "Token update successful ");
                    fcmPrefs.edit().putString("old_token", token).apply();
                }
                else{
                    Log.d("Firebase Message Receiver", "Token update failed" + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.d("Firebase Message Receiver", "Token update failed" + t.getMessage());
            }
        });

    }

    // Override onMessageReceived() method to extract the
    // title and
    // body from the message passed in FCM
    @Override
    public void
    onMessageReceived(RemoteMessage remoteMessage) {

        if(remoteMessage.getNotification() != null){
            // Send notification even if app is open
           sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }

        if(remoteMessage.getData().size() > 0){
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }
    }

    @SuppressLint("MissingPermission")
    private void sendNotification(String title, String body){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "default_channel")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.notification_icon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (PermissionUtils.hasNotifPerms(getApplicationContext())) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }
}