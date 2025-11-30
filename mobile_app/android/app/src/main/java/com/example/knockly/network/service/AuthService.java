package com.example.knockly.network.service;

import com.example.knockly.network.dto.DoorbellOut;
import com.example.knockly.network.dto.FaceResponse;
import com.example.knockly.network.dto.IsSubscribedResponse;
import com.example.knockly.network.dto.LinkedUser;
import com.example.knockly.network.dto.LoginRequest;
import com.example.knockly.network.dto.NotifSubscriptionRequest;
import com.example.knockly.network.dto.PermissionResponse;
import com.example.knockly.network.dto.Token;
import com.example.knockly.network.dto.UpdateFCMTokenOut;
import com.example.knockly.network.dto.UserCreate;
import com.example.knockly.network.dto.UserInfo;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AuthService {
    @POST("auth/login")
    Call<Token> login(@Body LoginRequest body);

    @POST("auth/register")
    Call<Token> register(@Body UserCreate body);

    @GET("users/me")
    Call<UserInfo> getCurrentUser();

    @GET("doorbells")
    Call<DoorbellOut[]> getUsersDoorbells();

    @GET("doorbells/{doorbellId}/permissions")
    Call<PermissionResponse[]> getUserPerms(@Path("doorbellId") String doorbellId);

    @GET("doorbells/{doorbellId}/faces")
    Call<FaceResponse[]> getDoorbellFaces(@Path("doorbellId") String doorbellId);

    @GET("doorbells/{doorbellId}/getLinkedUsers")
    Call<LinkedUser[]> getDoorbellLinkedUsers(@Path("doorbellId") String doorbellId);

    @POST("notifications/isSubscribed")
    Call<IsSubscribedResponse> checkIsSubscribed(@Body NotifSubscriptionRequest body);

    @POST("notifications/subscribe")
    Call<Void> subscribe(@Body NotifSubscriptionRequest body);

    @POST("notifications/unsubscribe")
    Call<Void> unsubscribe(@Body NotifSubscriptionRequest body);

    @POST("notifications/updateFCMToken")
    Call<Void> updateFCMToken(@Body UpdateFCMTokenOut body);
}
