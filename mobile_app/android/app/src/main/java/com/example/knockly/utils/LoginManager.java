package com.example.knockly.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.example.knockly.network.AuthInterceptor;

// A singleton object instantiated in the custom app class (KnocklyApp.java)
// To use create object using "LoginManager loginManager = LoginManager.getInstance(context)"
// context can simply be "this" in an activity
// Then use "loginManager.function()" get information needed
public class LoginManager implements AuthInterceptor.TokenStore{
    // Constants used in the class
    // Some numbers to make calculations easier
    // Kept multiple so time between logins can be adjusted easily
    private static final long milliSecsInAMin = 60000;
    private static final long milliSecsInAnHour = milliSecsInAMin * 60;
    private static final long milliSecsInADay = milliSecsInAnHour * 24;

    // Number of milliseconds between re-authentication
    // Currently set to logging in every 3 days
    private static final long TIME_BETWEEN_LOGINS = milliSecsInADay * 3;

    // Names of pref keys
    private static final String PREFS_NAME = "securePrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_CURRENT_USER = "userID";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_LAST_LOGIN_TIME = "lastLogin";

    private static final String KEY_JWT = "jwtToken";

    // Variable to store preference info that is in xml
    private SharedPreferences prefs;

    // Singleton instance of class that can be used throughout app
    private static LoginManager instance;

    // Constructor
    // Private as only used if single instance hasn't been created yet
    private LoginManager(Context context) {
        try {
            String keyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            prefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    keyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // Check if previous login saved
            String userID = prefs.getString(KEY_CURRENT_USER, null);
            Long lastLogin = prefs.getLong(KEY_LAST_LOGIN_TIME, -1);
            // If data isn't valid then set to not logged in
            if (userID == null || userID.isEmpty() || lastLogin == -1) {
                prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply();
            } else {
                prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply();
            }
        } catch (Exception e) {
            System.out.println("Error creating login manager");
            System.out.println(e.getStackTrace());
        }
    }

    // Gets the instance and passes it to relevant activity
    // Ensures only 1 can be created
    public static synchronized LoginManager getInstance(Context context) {
        if (instance == null) {
            instance = new LoginManager(context);
        }
        return instance;
    }

    // Returns boolean for if user is currently logged into the app
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    // Once user is logged in it saves their details for future log ins
    public void saveLogin(String userID, String userEmail, String username, String displayName) {
        long currentTime = System.currentTimeMillis();
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_CURRENT_USER, userID)
                .putString(KEY_USERNAME, username)
                .putString(KEY_USER_EMAIL, userEmail)
                .putString(KEY_DISPLAY_NAME, displayName)
                .putLong(KEY_LAST_LOGIN_TIME, currentTime)
                .apply();
    }

    // Removes any data stored about user from xml file
    public void logout() {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_CURRENT_USER)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USERNAME)
                .remove(KEY_DISPLAY_NAME)
                .remove(KEY_LAST_LOGIN_TIME)
                .remove(KEY_JWT)
                .apply();
    }

    // Checks if it has been longer than defined time (TIME_BETWEEN_LOGINS)
    // Once certain amount of time has passed user has to log in with password again
    public boolean loginIsExpired() {
        if (isLoggedIn()) {
            long currentTime = System.currentTimeMillis();
            long loginTime = prefs.getLong(KEY_LAST_LOGIN_TIME, 0);
            long timeLoggedIn = currentTime - loginTime;

            if (timeLoggedIn < TIME_BETWEEN_LOGINS) {
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    // Returns UUID of current user
    // Returns empty string if not logged in / error
    public String getCurrentUserID() {
        if (isLoggedIn()) {
            return prefs.getString(KEY_CURRENT_USER, "");
        }
        return "";
    }

    // Returns email of current user
    // Returns empty string if not logged in / error
    public String getCurrentUserEmail() {
        if (isLoggedIn()) {
            return prefs.getString(KEY_USER_EMAIL, "");
        }
        return "";
    }

    // Returns username of current user
    // Returns empty string if not logged in / error
    public String getCurrentUsername() {
        if (isLoggedIn()) {
            return prefs.getString(KEY_USERNAME, "");
        }
        return "";
    }

    // Returns display name of current user
    // Returns empty string if not logged in / error
    public String getCurrentDisplayName() {
        if (isLoggedIn()) {
            return prefs.getString(KEY_DISPLAY_NAME, "");
        }
        return "";
    }

    // Returns time (in long form) that the user last logged in
    // Returns -1 if not logged in / error
    public long getLastLogin() {
        if (isLoggedIn()) {
            return prefs.getLong(KEY_LAST_LOGIN_TIME, -1);
        }
        return -1;
    }

    // Save JWT token
    public void saveJwt(String jwt) {
        prefs.edit()
                .putString(KEY_JWT, jwt)
                .apply();
    }

    // Retrieve JWT token (implements TokenStore interface)
    @Override // From TokenStore interface
    public String getJwt() {
        return prefs.getString(KEY_JWT, null);
    }
}
