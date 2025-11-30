package com.example.knockly.manageFacesPage;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.knockly.R;
import com.example.knockly.network.dto.FaceResponse;
import com.example.knockly.utils.ViewUtils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LinkedFacesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LinkedFacesFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_FACES_ARRAY = "faces";

    private FaceResponse[] mFacesArr;

    public LinkedFacesFragment() {
        // Required empty public constructor
        mFacesArr = new FaceResponse[0];
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param facesArr Parameter 1.
     * @return A new instance of fragment LinkedFacesFragment.
     */
    public static LinkedFacesFragment newInstance(FaceResponse[] facesArr) {
        LinkedFacesFragment fragment = new LinkedFacesFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FACES_ARRAY, facesArr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFacesArr = (FaceResponse[]) getArguments().getSerializable(ARG_FACES_ARRAY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_linked_faces, container, false);

        // Get layout to add faces to
        LinearLayout layout = view.findViewById(R.id.LinkedFacesScrollLayout);

        // For each face add to screen
        for (FaceResponse currentFace : mFacesArr){
            CardView card = createFaceCard(getContext(), currentFace.face_profile_name);
            layout.addView(card);
        }

        return view;
    }

    private CardView createFaceCard(Context context, String faceName){
        int boxHeight = (int) (ViewUtils.dpToPx(100, context));
        int boxWidth = (int) ( ViewUtils.getScreenWidth(context) * 0.8);

        CardView box = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(boxWidth, boxHeight);
        cardParams.setMargins(0,30,0,0);
        box.setLayoutParams(cardParams);

        GradientDrawable boxBackground = new GradientDrawable();
        boxBackground.setColor(getResources().getColor(R.color.lightGrey));
        boxBackground.setCornerRadius(15);
        boxBackground.setStroke(2, getResources().getColor(R.color.black));

        box.setBackground(boxBackground);

        TextView text = new TextView(context);
        SpannableString styledText = new SpannableString("Name: " + faceName);
        styledText.setSpan(new StyleSpan(Typeface.BOLD), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setText(styledText);
        text.setPadding(30,30,30,30);
        text.setTextSize(20f);
        text.setGravity(Gravity.CENTER_VERTICAL);

        box.addView(text);

        return box;
    }
}