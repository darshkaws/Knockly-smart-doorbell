package com.example.knockly.network;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Automatically adds the JWT header if we have one. */
public class AuthInterceptor implements Interceptor {

    private final TokenStore tokenStore;   // tiny interface we define below

    public AuthInterceptor(TokenStore ts) {
        this.tokenStore = ts;
    }

    @Override public Response intercept(Chain chain) throws IOException {
        Request req = chain.request();
        String jwt = tokenStore.getJwt();
        Log.d("AuthInterceptor", "JWT: " + jwt); // Check if the JWT is non-null and valid
        if (jwt != null && !jwt.isEmpty()) {
            req = req.newBuilder()
                    .addHeader("Authorization", "Bearer " + jwt)
                    .build();
        }
        return chain.proceed(req);
    }

    /** Small abstraction so LoginManager or another class can supply the JWT */
    public interface TokenStore {
        String getJwt();
    }
}
