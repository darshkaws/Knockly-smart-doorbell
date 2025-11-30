package com.example.knockly.shared;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.knockly.R;
import com.example.knockly.appSettingsPage.AppSettingsActivity;

/**
 * A {@link Fragment} header to be added to each page for consistency.
 * To add to page use:
 *      PageHeaderFragment.newInstance(headerTitle, includeBackButton);
 *      getSupportFragmentManager().beginTransaction().replace(R.id.PageHeaderFragmentContainer, pageHeader).commit();
 * And add a FragmentContainerView to the page XML of at least 75dp to block out space on page
 *
 * Inputs:  headerTitle - a string that will be displayed at the top of the page, should tell the user where they are in the app
 *          includeBackButton - a boolean, if true a back button that mimics the action of the android back button will be added to the header
 */
public class PageHeaderFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TITLE = "headerTitle";
    private static final String ARG_INC_BACK_BUTTON = "includeBackButton";
    private static final String ARG_INC_SETTINGS_BUTTON = "includeSettingsButton";

    private String mHeaderTitle;
    private boolean mIncludeBackButton;
    private boolean mIncludeSettingsButton;

    public PageHeaderFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param headerTitle Title that will be displayed in the centre of the header on each page.
     * @param includeBackButton Boolean to determine whether back button at top left should be included
     * @return A new instance of fragment PageHeaderFragment.
     */
    public static PageHeaderFragment newInstance(String headerTitle, boolean includeBackButton, boolean includeSettingsButton) {
        PageHeaderFragment fragment = new PageHeaderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, headerTitle);
        args.putBoolean(ARG_INC_BACK_BUTTON, includeBackButton);
        args.putBoolean(ARG_INC_SETTINGS_BUTTON, includeSettingsButton);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mHeaderTitle = getArguments().getString(ARG_TITLE, getResources().getString(R.string.defaultHeaderTitle));
            mIncludeBackButton = getArguments().getBoolean(ARG_INC_BACK_BUTTON, false);
            mIncludeSettingsButton = getArguments().getBoolean(ARG_INC_SETTINGS_BUTTON, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Create base, inflated view
        View view = inflater.inflate(R.layout.fragment_page_header, container, false);

        // Find title of page header
        // If exists then set title to given one
        TextView titleTextView = view.findViewById((R.id.PageHeaderTitle));
        if (titleTextView != null){
            titleTextView.setText(mHeaderTitle);
        }

        // Check if back button needs to be shown or not
        // By default it isn't added
        ImageButton backButton = view.findViewById(R.id.headerBackButton);
        if (mIncludeBackButton){
            backButton.setVisibility(View.VISIBLE);
            backButton.setEnabled(true);
            backButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    //TODO: change to non depracated method
                    getActivity().onBackPressed();
                }
            });
        }
        else{
            backButton.setVisibility(View.INVISIBLE);
            backButton.setEnabled(false);
        }

        // Check if settings button needs to be shown or not
        // By default it isn't added
        ImageButton settingsButton = view.findViewById(R.id.headerSettingsButton);
        if (mIncludeSettingsButton){
            settingsButton.setVisibility(View.VISIBLE);
            settingsButton.setEnabled(true);
            settingsButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), AppSettingsActivity.class);
                    startActivity(intent);
                }
            });
        }
        else{
            settingsButton.setVisibility(View.INVISIBLE);
            settingsButton.setEnabled(false);
        }
        return view;
    }
}