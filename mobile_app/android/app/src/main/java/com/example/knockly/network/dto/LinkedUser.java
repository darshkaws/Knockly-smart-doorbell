package com.example.knockly.network.dto;

import java.io.Serializable;

public class LinkedUser implements Serializable {
    public String user_id;
    public String username;
    public String display_name; // Warning: May be null, optional
    public String role_id; // Warning: May be null, optional
    public String role_name; // Warning: May be null, optional
}

