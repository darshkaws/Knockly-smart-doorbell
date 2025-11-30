package com.example.knockly.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Builds one Retrofit instance and keeps it for the whole app. */
public final class ApiClient {

    private static Retrofit retrofit;

    public static Retrofit get(AuthInterceptor.TokenStore tokenStore) {
        if (retrofit == null) {

            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient ok = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(tokenStore))
                    .addInterceptor(log)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(ok)
                    .build();
        }
        return retrofit;
    }

    private ApiClient() {}
}
