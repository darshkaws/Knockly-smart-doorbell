package com.example.knockly.utils;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonUtils {
    public static JSONObject safeParseStringToJsonObject(String string){
        try{
            return new JSONObject(string);
        }
        catch(Exception e){
            return new JSONObject();
        }
    }

    public static JSONArray safeParseStringToJsonArray(String string){
        try{
            return new JSONArray(string);
        }
        catch(Exception e){
            return new JSONArray();
        }
    }
}
