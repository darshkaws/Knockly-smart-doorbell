package com.example.knockly.network.dto;

public class UserCreate {
    public String username;
    public String email;
    public String password;
    public String display_name;

    public UserCreate(String givenUsername, String givenEmail, String givenPass, String givenName){
        username = givenUsername;
        email = givenEmail;
        password = givenPass;
        display_name = givenName;
    }
}
