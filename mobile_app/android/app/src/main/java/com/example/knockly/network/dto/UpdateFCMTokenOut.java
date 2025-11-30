package com.example.knockly.network.dto;

public class UpdateFCMTokenOut {
    public String old_token;
    public String new_token;

    public UpdateFCMTokenOut(String givenOldToken, String givenNewToken){
        old_token = givenOldToken;
        new_token = givenNewToken;
    }
}
