package com.example.knockly.indvDoorbellPage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.knockly.R;
import com.example.knockly.manageDoorbellPage.ManageDoorbellActivity;
import com.example.knockly.manageFacesPage.ManageFacesActivity;
import com.example.knockly.manageUsersPage.ManageUsersActivity;
import com.example.knockly.network.dto.PermissionResponse;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DoorbellOptionsTabFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DoorbellOptionsTabFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_USER_PERMS = "UserPerms";
    private static final String ARG_DOORBELL_ID = "DoorbellId";
    private static final String ARG_DOORBELL_NAME = "DoorbellName";

    private PermissionResponse[] mUserPerms;
    private String mDoorbellID;
    private String mDoorbellName;
    private boolean tabMinimised = false;
    private JSONObject permissions;

    public DoorbellOptionsTabFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param doorbellID Paramter 1.
     * @param userPerms Parameter 1.
     * @return A new instance of fragment test.
     */
    public static DoorbellOptionsTabFragment newInstance(String doorbellID, String doorbellName, Serializable userPerms) {
        DoorbellOptionsTabFragment fragment = new DoorbellOptionsTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOORBELL_ID, doorbellID);
        args.putString(ARG_DOORBELL_NAME, doorbellName);
        args.putSerializable(ARG_USER_PERMS, userPerms);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDoorbellID = getArguments().getString(ARG_DOORBELL_ID);
            mDoorbellName = getArguments().getString(ARG_DOORBELL_NAME);
            mUserPerms = (PermissionResponse[]) getArguments().getSerializable(ARG_USER_PERMS  );
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_doorbell_options_tab, container, false);

        // Add on click listeners to change activities to all the classes
        Button[] buttons = {
                view.findViewById(R.id.manageUsersButton),
                view.findViewById(R.id.manageFacesButton),
                view.findViewById(R.id.manageDoorbellButton),
        };

        Class[] destinationActivities = {
                ManageUsersActivity.class,
                ManageFacesActivity.class,
                ManageDoorbellActivity.class,
        };

        final String[] permissionNames = {
                "manage_user_profiles",
                "manage_face_profiles",
                "manage_doorbell",
        };

        for (int m = 0; m < buttons.length; m++){
            Button currentButton = buttons[m];

            if (getPermission(permissionNames[m])){
                Class currentActivity = destinationActivities[m];
                currentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), currentActivity);
                        intent.putExtra("Doorbell_ID", mDoorbellID);
                        intent.putExtra("Doorbell_Name", mDoorbellName);
                        startActivity(intent);
                    }
                });
            }
            else{
                currentButton.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.grey));
                currentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getContext(), "You don’t have access to this page. Please contact the admin if needed.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        // Make minimise/expand button function
        MaterialButton minimiseButton = view.findViewById(R.id.minimiseButton);
        ConstraintLayout buttonContainer = view.findViewById(R.id.MinimiseTabButtonContainer);

        minimiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tabMinimised) {
                    buttonContainer.setVisibility(View.VISIBLE);
                    minimiseButton.setText(R.string.buttonMinimiseOptionsTitle);
                    minimiseButton.setIconResource(R.drawable.down_arrow);
                }
                else {
                    buttonContainer.setVisibility(View.GONE);
                    minimiseButton.setText(R.string.buttonExpandOptionsTitle);
                    minimiseButton.setIconResource(R.drawable.up_arrow);
                }
                tabMinimised = !tabMinimised;
            }
        });

        return view;
    }

    private boolean getPermission(String permName){
        if (mUserPerms != null) {
            // Loop through array to find permission
            for (PermissionResponse perm : mUserPerms){
                if (Objects.equals(perm.permission, permName)){
                    return perm.granted;
                }
            }
        }
        return false;
    }
}