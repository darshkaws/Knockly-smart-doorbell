package com.example.knockly.indvDoorbellPage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.util.UnstableApi;

import com.example.knockly.R;
import com.example.knockly.network.dto.LinkedUser;
import com.example.knockly.network.dto.PermissionResponse;
import com.example.knockly.network.repository.AuthRepository;
import com.example.knockly.shared.PageHeaderFragment;
import com.example.knockly.utils.LoginManager;
import com.example.knockly.utils.SSHUtils;
import com.example.knockly.utils.ViewUtils;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
public class IndvDoorbellActivity extends AppCompatActivity implements LiveFeedFragment.FragmentCallback, DoorControlFragment.FragmentCallback {

    private AuthRepository repo = AuthRepository.getInstance(LoginManager.getInstance(this));

    private Insets systemBars;
    private  LiveFeedFragment liveFeedFragment;
    private Context context;
    private final String username = "admin";
    private final String password = "admin";
    public static String hostPiFeed = "";
    private static String hostPiMain = "";
    private Session sshSession;
    private final String commandDir = "Test_facial_rec/phone_commands";
    private String openCmd = "\nchmod +x open.sh\n./open.sh\n";
    private String closeCmd = "\nchmod +x close.sh\n./close.sh\n";

    private PermissionResponse[] userPerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_indv_doorbell);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        // Get data about current doorbell from intent that started the activity
        String doorbellID = intent.getStringExtra("doorbell_ID");
        String doorbellName = intent.getStringExtra("doorbell_name");
        String ownerID = intent.getStringExtra("owner_id");
        LinkedUser[] linkedUsers = (LinkedUser[]) intent.getSerializableExtra("linked_users");
        hostPiFeed = intent.getStringExtra("piHost_feed");
        hostPiMain = intent.getStringExtra("piHost_main");

        // Get information about users current permissions from API
        repo.getUserPerms(doorbellID).enqueue(new Callback<PermissionResponse[]>() {
            @Override
            public void onResponse(Call<PermissionResponse[]> call, Response<PermissionResponse[]> response) {
                // If API call is successful
                if (response.isSuccessful() && response.body()!=null){
                    userPerms = response.body();

                    // Add options tab at bottom of page
                    DoorbellOptionsTabFragment tabFragment = DoorbellOptionsTabFragment.newInstance(doorbellID, doorbellName, userPerms);
                    getSupportFragmentManager().beginTransaction().replace(R.id.IndvDoorbellOptionsFragment, tabFragment).commit();
                }
                // If API call is unsuccessful
                else{
                    Log.d("Indv Doorbell Activity", "Error fetching user perms" + response.code());
                    Toast.makeText(IndvDoorbellActivity.this, "Error fetching user permissions" + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PermissionResponse[]> call, Throwable t) {
                Log.d("Indv Doorbell Activity", "Error fetching user perms" + t.getMessage());
                Toast.makeText(IndvDoorbellActivity.this, "Error fetching user permissions" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Add header fragment
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance(doorbellName, true, true);
        getSupportFragmentManager().beginTransaction().replace(R.id.IndvDoorbellHeaderFragment, pageHeader).commit();

        // Add live feed fragment
        liveFeedFragment = LiveFeedFragment.newInstance(doorbellID);
        getSupportFragmentManager().beginTransaction().replace(R.id.LiveFeedContainer, liveFeedFragment).commit();

        // Add door control fragment
        DoorControlFragment doorControlFragment = DoorControlFragment.newInstance(doorbellID);
        getSupportFragmentManager().beginTransaction().replace(R.id.DoorControlContainer, doorControlFragment).commit();

        context = getApplicationContext();

        if (!hostPiMain.isBlank()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    startSession();
                }
            }).start();
        }
    }

    private void startSession() {
        if (!hostPiMain.isBlank()) {
            sshSession = SSHUtils.startSSH(username, hostPiMain, password);
        }
    }

    @Override
    public void expandLiveFeed(){
        View headerFragment = findViewById(R.id.IndvDoorbellHeaderFragment);
        View controlsFragment = findViewById(R.id.DoorControlContainer);
        View optionsFragment = findViewById(R.id.IndvDoorbellOptionsFragment);

        ConstraintLayout feedConstraint = findViewById((R.id.feedConstraint));

        controlsFragment.setVisibility(View.GONE);
        optionsFragment.setVisibility(View.GONE);
        headerFragment.setVisibility(View.GONE);

        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(feedConstraint);
        constraints.clear(R.id.feedConstraint, ConstraintSet.BOTTOM);
        constraints.connect(R.id.feedConstraint, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        constraints.constrainHeight(R.id.feedConstraint, ConstraintSet.MATCH_CONSTRAINT);
        constraints.applyTo(feedConstraint);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) feedConstraint.getLayoutParams();
        params.topMargin = 0;
        params.bottomMargin = 0;
        feedConstraint.setLayoutParams(params);

        View view = findViewById(R.id.main);
        view.setPadding(0,0,0,0);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.hide(WindowInsetsCompat.Type.navigationBars());
        controller.hide(WindowInsetsCompat.Type.statusBars());
    }

    @Override
    public void shrinkLiveFeed(){
        View headerFragment = findViewById(R.id.IndvDoorbellHeaderFragment);
        View controlsFragment = findViewById(R.id.DoorControlContainer);
        View optionsFragment = findViewById(R.id.IndvDoorbellOptionsFragment);

        ConstraintLayout feedConstraint = findViewById((R.id.feedConstraint));

        controlsFragment.setVisibility(View.VISIBLE);
        optionsFragment.setVisibility(View.VISIBLE);
        headerFragment.setVisibility(View.VISIBLE);

        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(feedConstraint);
        constraints.clear(R.id.feedConstraint, ConstraintSet.BOTTOM);
        constraints.connect(R.id.feedConstraint, ConstraintSet.BOTTOM, R.id.DoorControlContainer, ConstraintSet.BOTTOM);
        constraints.constrainHeight(R.id.feedConstraint, ConstraintSet.MATCH_CONSTRAINT);
        constraints.applyTo(feedConstraint);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) feedConstraint.getLayoutParams();
        params.topMargin = (int) ViewUtils.dpToPx(32, context);
        params.bottomMargin = 24;
        feedConstraint.setLayoutParams(params);

        View view = findViewById(R.id.main);
        view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
        controller.show(WindowInsetsCompat.Type.navigationBars());
        controller.show(WindowInsetsCompat.Type.statusBars());
    }

    private int returnStatus = -1;

    @Override
    public void openDoor() {
        if (sshSession != null) {
            new Thread(() -> {
                String command = "\ncd "+commandDir+"\n"+openCmd+"\n";
                returnStatus = SSHUtils.execSSHCmd(command, sshSession);

                runOnUiThread(() -> {
                    // If door opened successfully then change door status button to open
                    if (returnStatus == 200){
                        liveFeedFragment.setToggleButtonOn();
                        Toast.makeText(context, "Door opened successfully!", Toast.LENGTH_SHORT).show();
                    }
                    // If error occurred
                    else{
                        Toast.makeText(context, "Error opening door, please try again", Toast.LENGTH_SHORT).show();
                    }

                    returnStatus = -1;
                });
            }).start();
        }
    }

    @Override
    public void closeDoor() {
        if (sshSession != null) {
            new Thread(() -> {
                String command = "\ncd "+commandDir+"\n"+closeCmd+"\n";
                returnStatus = SSHUtils.execSSHCmd(command, sshSession);

                runOnUiThread(() -> {
                    // If door opened successfully then change door status button to open
                    if (returnStatus == 200){
                        liveFeedFragment.setToggleButtonOff();
                        Toast.makeText(context, "Door closed successfully!", Toast.LENGTH_SHORT).show();
                    }
                    // If error occurred
                    else{
                        Toast.makeText(context, "Error closing door, please try again", Toast.LENGTH_SHORT).show();
                    }

                    returnStatus = -1;
                });
            }).start();
        }
    }
}