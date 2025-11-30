package com.example.knockly.utils;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;

import com.example.knockly.R;
import com.google.android.material.button.MaterialButton;

public class ButtonUtils {
    // Helper method to style doorbell buttons dynamically
    public static MaterialButton createDynamicStyledButton(Context context, String text, int width, int height, int margins){
        MaterialButton button = new MaterialButton(context);

        // Set button text
        button.setText(text);
        button.setTextColor(ContextCompat.getColor(context, R.color.black));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        // Set button background
        button.setCornerRadius((int) (ViewUtils.dpToPx(5, context)));
        button.setStrokeWidth(3);
        button.setStrokeColorResource(R.color.black);
        button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.colorSecondary));

        // Set layout parameters
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = width;
        params.height = height;
        params.setMargins(margins, margins, margins, margins);

        // Make sure each button takes up same amount of space
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setGravity(Gravity.CENTER_VERTICAL);

        button.setLayoutParams(params);

        return button;
    }
}
