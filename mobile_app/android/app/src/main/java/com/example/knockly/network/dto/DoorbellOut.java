package com.example.knockly.network.dto;

import java.io.Serializable;

public class DoorbellOut implements Serializable {
    public String doorbell_id;
    public String doorbell_name;
    public String owner_user_id;
    public LinkedUser[] linked_users;
}

