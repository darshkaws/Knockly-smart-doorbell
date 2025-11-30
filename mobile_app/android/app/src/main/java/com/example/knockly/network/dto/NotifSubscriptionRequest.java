package com.example.knockly.network.dto;

public class NotifSubscriptionRequest {
    public String fcm_token;
    public String doorbell_id;

    public NotifSubscriptionRequest(String given_fcm_token, String given_doorbell_id){
        fcm_token = given_fcm_token;
        doorbell_id = given_doorbell_id;
    }
}
