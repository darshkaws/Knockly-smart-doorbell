package com.example.knockly.network.repository;

import com.example.knockly.network.ApiClient;
import com.example.knockly.network.AuthInterceptor;
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
import com.example.knockly.network.service.AuthService;

import retrofit2.Call;

// Allows call to the API
// Initially created when app opens in KnocklyApp class
// To call in activity use "authRepo = AuthRepository.getInstance(LoginManager.getInstance())"
// Or if existing variable holding Login Manager then use that as argument
// Needs LoginManager to store & get access tokens
public class AuthRepository {

    private static AuthRepository instance;
    private final AuthService svc;

    private AuthRepository(AuthService svc) {
        this.svc = svc;
    }

    public static AuthRepository getInstance(AuthInterceptor.TokenStore tokenStore) {
        if (instance == null){
            instance =  new AuthRepository(ApiClient.get(tokenStore).create(AuthService.class));
        }
        return instance;
    }

    public static void resetInstance(AuthInterceptor.TokenStore tokenStore) {
        instance = new AuthRepository(
                ApiClient.get(tokenStore).create(AuthService.class));
    }

    public Call<Token> login(String user, String pass) {
        return svc.login(new LoginRequest(user, pass));
    }

    public Call<Token> register(String user, String email, String pass, String displayName) {
        return svc.register(new UserCreate(user, email, pass, displayName));
    }

    public Call<UserInfo> getCurrentUser() {
        return svc.getCurrentUser();
    }

    public Call<DoorbellOut[]> getUsersDoorbells(){
        return svc.getUsersDoorbells();
    }

    public Call<PermissionResponse[]> getUserPerms(String doorbellId){
        return svc.getUserPerms(doorbellId);
    }

    public Call<FaceResponse[]> getDoorbellFaces(String doorbellId){
        return svc.getDoorbellFaces(doorbellId);
    }

    public Call<LinkedUser[]> getDoorbellLinkedUsers(String doorbellId){
        return svc.getDoorbellLinkedUsers(doorbellId);
    }

    public Call<IsSubscribedResponse> checkIsSubscribed(String fcm_token, String doorbell_id){
        return svc.checkIsSubscribed(new NotifSubscriptionRequest(fcm_token, doorbell_id));
    }

    public Call<Void> subscribe(String fcm_token, String doorbell_id){
        return svc.subscribe(new NotifSubscriptionRequest(fcm_token, doorbell_id));
    }

    public Call<Void> unsubscribe(String fcm_token, String doorbell_id){
        return svc.unsubscribe(new NotifSubscriptionRequest(fcm_token, doorbell_id));
    }

    public Call<Void> updateFCMToken(String old_token, String new_token){
        return svc.updateFCMToken(new UpdateFCMTokenOut(old_token, new_token));
    }
}
