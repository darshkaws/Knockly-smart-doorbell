package com.example.knockly.homePage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.media3.common.util.UnstableApi;

import com.example.knockly.R;
import com.example.knockly.addDoorbellPage.AddDoorbellActivity;
import com.example.knockly.indvDoorbellPage.IndvDoorbellActivity;
import com.example.knockly.network.dto.DoorbellOut;
import com.example.knockly.network.dto.LinkedUser;
import com.example.knockly.utils.ButtonUtils;
import com.example.knockly.utils.SSHUtils;
import com.example.knockly.utils.ViewUtils;
import com.google.android.material.button.MaterialButton;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A {@link Fragment} subclass that takes an array of json objects representing
 * all doorbells the user is currently attached to.
 * It dynamically loads a button for each doorbell that links to the page to show the individual doorbell
 * Finally it add a button to allow the user to add a new doorbell
 * All content it within a scroll box so that the user can have as many buttons as they want
 */
public class HomePageButtonsFragment extends Fragment {

    // Parameter Initialisation
    private static final String ARG_DOORBELLS = "doorbells";

    private DoorbellOut[] mDoorbells;
    private Context context;
    private HashMap<String, HashMap<String, String>> doorbells = new HashMap<>();
    private Session feedSession;
    public String commandDir = "cm2211-project-group-8/edge_device/Live Feed";
    private final String serverCmd = "\nchmod +x startServer.sh\n./startServer.sh";
    private int returnStatus;

    public HomePageButtonsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param doorbells JSON Array where each element is a json object containing all relevant
     *                  information about each individual doorbell
     * @return A new instance of fragment HomePageButtonsFragment.
     */
    public static HomePageButtonsFragment newInstance(DoorbellOut[] doorbells) {
        HomePageButtonsFragment fragment = new HomePageButtonsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DOORBELLS, doorbells);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDoorbells = (DoorbellOut[]) getArguments().getSerializable(ARG_DOORBELLS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        HashMap<String, String> main = new HashMap<>();
        main.put("username", "admin");
        main.put("password", "admin");
        main.put("piHost", "");
        HashMap<String, String> feed = new HashMap<>();
        feed.put("username", "pi");
        feed.put("password", "raspberry");
        feed.put("piHost", "");
        doorbells.put("main", main);
        doorbells.put("feed", feed);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home_page_buttons, container, false);

        // Get layout to add buttons to
        GridLayout layout = view.findViewById(R.id.HomePageScrollLayout);
        layout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        View loadingOverlay = view.findViewById(R.id.loading_overlay);
        ProgressBar loadingBar = view.findViewById(R.id.loading_spinner);
        TextView loadingText = view.findViewById(R.id.loading_text);

        context = getContext();

        final Thread networkThread = new Thread(() -> {
            // Populate piHost fields
            for (String key : doorbells.keySet()) {
                HashMap<String, String> creds = doorbells.get(key);
                String username = creds.get("username");
                String password = creds.get("password");
                String piHost = SSHUtils.getPiIP(context, username, password);
                creds.put("piHost", piHost);  // Store result back in map
                System.out.println("Found piHost for " + key + ": " + piHost);
            }


            // After data fetched, update UI
            requireActivity().runOnUiThread(() -> {
                // Hide loading overlay after buttons are set up
                loadingBar.setVisibility(View.GONE);
                loadingText.setVisibility(View.GONE);
                loadingOverlay.setVisibility(View.GONE);
                setupButtons(layout);
            });

            // Start session for feed
            String feedHost = doorbells.get("feed").get("piHost");
            String feedUser = doorbells.get("feed").get("username");
            String feedPass = doorbells.get("feed").get("password");

            startSession(feedUser, feedHost, feedPass);

            if (feedSession != null) {
                String command = "\ncd "+commandDir+"\n"+serverCmd;
                returnStatus = SSHUtils.execSSHCmd(command, feedSession);
            }
        });
        networkThread.start();

        return view;
    }
    private MaterialButton createAddDoorbellButton(Context context, int width, int height, int margins){
        MaterialButton button = ButtonUtils.createDynamicStyledButton(context, "Add Doorbell", width, height, margins);

        int padding = (int) (ViewUtils.dpToPx(12, context));

        button.setPadding(padding, padding, padding, padding);
        button.setIcon(ContextCompat.getDrawable(context, R.drawable.plus_icon));
        button.setIconSize((int) (ViewUtils.dpToPx(24, context)));
        button.setIconTint(null);
        return button;
    }

    private void setupButtons(GridLayout layout) {
        requireActivity().runOnUiThread(() -> {
            DoorbellOut currentObject;

            try {
                // Calculate button dimensions based on screen size
                int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
                int marginSize = (int) (ViewUtils.dpToPx(8, getContext()));
                int buttonWidth = (screenWidth / 2) - (4 * marginSize);
                int buttonHeight = (int) (ViewUtils.dpToPx(100, getContext()));
                for (int m = 0; m < mDoorbells.length; m++) {
                    currentObject = mDoorbells[m];
                    if (currentObject == null) {
                        continue;
                    }
                    String doorbellID = currentObject.doorbell_id;;
                    String doorbellName = currentObject.doorbell_name;
                    String ownerID = currentObject.owner_user_id;
                    LinkedUser[] linkedUsers = currentObject.linked_users;

                    // Create button for doorbell
                    MaterialButton doorbellButton = ButtonUtils.createDynamicStyledButton(getContext(), doorbellName, buttonWidth, buttonHeight, marginSize);

                    doorbellButton.setOnClickListener(new View.OnClickListener() {
                        @OptIn(markerClass = UnstableApi.class)
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(getActivity(), IndvDoorbellActivity.class);
                            intent.putExtra("doorbell_ID", doorbellID);
                            intent.putExtra("doorbell_name", doorbellName);
                            intent.putExtra("owner_id", ownerID);
                            intent.putExtra("linked_users", linkedUsers);
                            intent.putExtra("piHost_main", doorbells.get("main").get("piHost"));
                            intent.putExtra("piHost_feed", doorbells.get("feed").get("piHost"));
                            startActivity(intent);
                        }
                    });

                    // Add to screen
                    layout.addView(doorbellButton);
                }

                // Make and add 'Add Doorbell' button
                MaterialButton addBellButton = createAddDoorbellButton(getContext(), buttonWidth, buttonHeight, marginSize);

                addBellButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), AddDoorbellActivity.class);
                        startActivity(intent);
                    }
                });

                layout.addView(addBellButton);
            }
            catch (Exception e){
                System.out.println("Error creating buttons on home page");
                System.out.println(e);
            }
        });
    }

    private void startSession(String username, String piHost, String password) {
        if (!piHost.isBlank()) {
            feedSession = SSHUtils.startSSH(username, piHost, password);
        }
    }

    private void stopSession() {
        if (feedSession!=null) {
            SSHUtils.execSSHCmd("\n", feedSession);
        }
        SSHUtils.stopSSH(feedSession);
        feedSession = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopSession();
        // Start session for feed
        String feedHost = doorbells.get("feed").get("piHost");
        String feedUser = doorbells.get("feed").get("username");
        String feedPass = doorbells.get("feed").get("password");

        startSession(feedUser, feedHost, feedPass);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopSession();
    }
}