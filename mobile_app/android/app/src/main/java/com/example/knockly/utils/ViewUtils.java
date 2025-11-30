package com.example.knockly.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

public class ViewUtils {
    public static float dpToPx(float dp, Context context){
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static int getScreenWidth(Context context){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public static int getScreenHeight(Context context){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }
}
