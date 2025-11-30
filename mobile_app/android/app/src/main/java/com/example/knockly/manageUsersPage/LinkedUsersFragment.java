package com.example.knockly.manageUsersPage;

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
import com.example.knockly.network.dto.LinkedUser;
import com.example.knockly.utils.ViewUtils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LinkedUsersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LinkedUsersFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_LINKED_USERS = "linkedUsers";

    // TODO: Rename and change types of parameters
    private LinkedUser[] mLinkedUsersArr;

    public LinkedUsersFragment() {
        // Required empty public constructor
        mLinkedUsersArr = new LinkedUser[0];
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param linkedUsers Parameter 1.
     * @return A new instance of fragment LinkedUsersFragment.
     */
    public static LinkedUsersFragment newInstance(LinkedUser[] linkedUsers) {
        LinkedUsersFragment fragment = new LinkedUsersFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LINKED_USERS, linkedUsers);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mLinkedUsersArr = (LinkedUser[]) getArguments().getSerializable(ARG_LINKED_USERS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_linked_users, container, false);

        LinearLayout layout = view.findViewById(R.id.LinkedUsersScrollLayout);

        for (LinkedUser linkedUser : mLinkedUsersArr){
            CardView card = createUserCard(getContext(), linkedUser.username, linkedUser.display_name, linkedUser.role_name);
            layout.addView(card);
        }

        return view;
    }

    private CardView createUserCard(Context context, String username, String displayName, String roleName){
        int boxHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
        int boxWidth = (int) ( ViewUtils.getScreenWidth(context) * 0.8);

        CardView box = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(boxWidth, boxHeight);
        cardParams.setMargins(0,30,0,0);
        box.setLayoutParams(cardParams);
        box.setContentPadding(0,10,0,10);

        GradientDrawable boxBackground = new GradientDrawable();
        boxBackground.setColor(getResources().getColor(R.color.lightGrey));
        boxBackground.setCornerRadius(15);
        boxBackground.setStroke(2, getResources().getColor(R.color.black));

        box.setBackground(boxBackground);

        // Inner layout to list text views
        LinearLayout innerLayout = new LinearLayout(context);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(20,20,20,20);
        box.addView(innerLayout);

        // Check, display name may be null (optional)
        if (displayName != null) {
            TextView nameText = createTextSection(context, "Name: ", displayName);
            innerLayout.addView(nameText);
        }

        TextView usernameText = createTextSection(context, "Username: ", username);
        innerLayout.addView(usernameText);

        // Check, role name may be null (optional)
        if (roleName != null) {
            TextView roleText = createTextSection(context, "Role: ", roleName);
            innerLayout.addView(roleText);
        }

        return box;
    }

    private TextView createTextSection(Context context, String label, String value){
        TextView text = new TextView(context);
        SpannableString styledText = new SpannableString(label + value);
        styledText.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setText(styledText);
        text.setPadding(30,10,30,10);
        text.setTextSize(20f);
        text.setGravity(Gravity.CENTER_VERTICAL);

        return text;
    }
}