package com.example.knockly.indvDoorbellPage;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.example.knockly.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DoorControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DoorControlFragment extends Fragment {

    // TODO: Update arguments with data necessary for api call
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_DOORBELLID = "doorbell_ID";

    // TODO: Update arguments with data necessary for api call
    private String mDoorbellID;
    private FragmentCallback callback;

    // Interface to get functions from main activity
    public interface FragmentCallback{
        void openDoor();
        void closeDoor();
    }

    public DoorControlFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param doorbellID ID of doorbell to open for API
     * @return A new instance of fragment DoorControlFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DoorControlFragment newInstance(String doorbellID) {
        DoorControlFragment fragment = new DoorControlFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOORBELLID   , doorbellID);
        fragment.setArguments(args);
        return fragment;
    }

    // Send activity functions to interface
    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        try{
            callback = (FragmentCallback) context;
        }
        catch(ClassCastException e){
            // Error that will occur if activity does not implement the interface methods
            throw new ClassCastException(context.toString()
                    + " must implement FragmentCallback interface");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDoorbellID = getArguments().getString(ARG_DOORBELLID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_door_control, container, false);

        Button openButton = view.findViewById(R.id.openButton);
        Button closeButton = view.findViewById(R.id.closeButton);

        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.openDoor();
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.closeDoor();
            }
        });
        return view;
    }

    // Prevent memory leaks by removing callback when fragment is destroyed
    @Override
    public void onDetach(){
        super.onDetach();
        callback = null;
    }
}