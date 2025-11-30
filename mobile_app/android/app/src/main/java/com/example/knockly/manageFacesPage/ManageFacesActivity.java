package com.example.knockly.manageFacesPage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.knockly.R;
import com.example.knockly.addFacePage.AddNewFaceActivity;
import com.example.knockly.network.dto.FaceResponse;
import com.example.knockly.network.repository.AuthRepository;
import com.example.knockly.shared.PageHeaderFragment;
import com.example.knockly.utils.LoginManager;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageFacesActivity extends AppCompatActivity {

    private AuthRepository repo = AuthRepository.getInstance(LoginManager.getInstance(this));

    private ArrayList<FaceResponse> uniqueFaces = new ArrayList<FaceResponse>();
    private ArrayList<String> names = new ArrayList<String>();
    private FaceResponse[] linkedFaces;
    private FaceResponse[] uniqueFacesArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_faces);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get info about doorbell from intent that started activity
        Intent intent = getIntent();
        String doorbellId = intent.getStringExtra("Doorbell_ID");
        String doorbellName = intent.getStringExtra("Doorbell_Name");

        // Add header fragment
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance("Manage Faces", true, true);
        getSupportFragmentManager().beginTransaction().replace(R.id.ManageFacesHeaderFragment, pageHeader).commit();

        // Button to link to add new face page
        Button button = findViewById(R.id.AddFacesButton);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(ManageFacesActivity.this, AddNewFaceActivity.class);
                intent.putExtra("Doorbell_ID", doorbellId);
                intent.putExtra("Doorbell_Name", doorbellName);
                startActivity(intent);
            }
        });

        // Get linked faces from API
        repo.getDoorbellFaces(doorbellId).enqueue(new Callback<FaceResponse[]>() {
            @Override
            public void onResponse(Call<FaceResponse[]> call, Response<FaceResponse[]> response) {
                if (response.isSuccessful() && response.body() != null){
                    linkedFaces = response.body();

                    // Filter faces so only unique ones are sent
                    for(FaceResponse face : linkedFaces){
                        if (!(names.contains(face.face_profile_name))){
                            uniqueFaces.add(face);
                            names.add(face.face_profile_name);
                        }
                    }

                    // Convert back to array
                    uniqueFacesArr = uniqueFaces.toArray(new FaceResponse[0]);
                    Log.d("Manage Faces Activity", Integer.toString(uniqueFacesArr.length));
                    // Pass faces to fragment & add fragment to page
                    LinkedFacesFragment facesFragment = LinkedFacesFragment.newInstance(uniqueFacesArr);
                    getSupportFragmentManager().beginTransaction().replace(R.id.LinkedFacesFragment, facesFragment).commit();
                }
                else{
                    Log.d("Manage Faces Activity", "Error fetching linked faces" + response.code());

                }
            }

            @Override
            public void onFailure(Call<FaceResponse[]> call, Throwable t) {
                Log.d("Manage Faces Activity", "Error fetching linked faces" + t.getMessage());
            }
        });
    }
}