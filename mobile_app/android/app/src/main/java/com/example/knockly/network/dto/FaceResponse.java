package com.example.knockly.network.dto;

import java.io.Serializable;

public class FaceResponse implements Serializable {
    public String face_id;
    public String doorbell_id;
    public String face_profile_name;
    public boolean is_blocked;
}
