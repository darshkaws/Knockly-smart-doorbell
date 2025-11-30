package com.example.knockly.network.dto;

public class LoginRequest {
    public String username_or_email;
    public String password;
    public String totp_code;

    public LoginRequest(String id, String pwd) {
        this.username_or_email = id;
        this.password = pwd;
    }
}
