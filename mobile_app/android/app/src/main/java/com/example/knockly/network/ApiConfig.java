package com.example.knockly.network;

/** Single source of truth for the base URL. */
public final class ApiConfig {
    // change this value at runtime if you store it in Settings - http://192.168.0.18:8000/
    public static String BASE_URL = "https://mutual-osprey-actually.ngrok-free.app";

    private ApiConfig() {}
}
