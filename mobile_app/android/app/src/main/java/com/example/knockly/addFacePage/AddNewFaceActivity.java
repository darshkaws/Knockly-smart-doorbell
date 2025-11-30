package com.example.knockly.addFacePage;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.knockly.R;
import com.example.knockly.shared.PageHeaderFragment;
import com.example.knockly.utils.PermissionUtils;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;


public class AddNewFaceActivity extends AppCompatActivity {

    private static final int VIDEO_REQUEST = 101; // just an ID to recognize the video intent later
    private boolean videoExists = false;
    private EditText etName;
    private File savedVideoFile;
    private Button btnRecord;
    private Button btnUpload;
    private Button reRecordBtn;
    private TextView successMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_new_face);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add header fragment
        PageHeaderFragment pageHeader = PageHeaderFragment.newInstance("Add Face", true, true);
        getSupportFragmentManager().beginTransaction().replace(R.id.AddFaceHeaderFragment, pageHeader).commit();

        // request camera and mic permissions (REALLY important or nothing works)
        if  (!PermissionUtils.hasCameraPerms(this)){
            PermissionUtils.getCameraPerms(AddNewFaceActivity.this);
        }

        etName = findViewById(R.id.etName);
        btnRecord = findViewById(R.id.btnRecord);
        btnUpload = findViewById(R.id.btnUpload);
        reRecordBtn = findViewById(R.id.ReRecordBtn);
        successMsg = findViewById(R.id.VidSavedSuccessMsg);

        // Make use first record button is visible (and other hidden)
        switchFromReRecordBtn();

        if (PermissionUtils.hasCameraPerms(this)){
            disableRecordVidBtn("Please enter a name before recording the video", () ->{});

        }
        else {
            disableRecordVidBtn("Error: Permissions to record needed. Please grant this in settings", () -> PermissionUtils.getCameraPerms(AddNewFaceActivity.this));
        }

        disableSubmitVidBtn("Please enter name and record the video before trying to upload");

        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (hasGivenName()){
                    enableRecordVidBtn();
                }
                else {
                    disableRecordVidBtn("Please enter a name before recording the video", () ->{});
                }
            }
        });
    }

    // This method launches the camera to record a video
    private void recordVideo() {
        if (!hasGivenName()){
            Toast.makeText(this, "Please enter a name before recording the video", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide soft keyboard if still on screen
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(AddNewFaceActivity.this.getCurrentFocus().getWindowToken(), 0);

        String name = etName.getText().toString().trim(); // grab name from the input field

        // create or get directory in app's private Movies folder for our videos
        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "FaceVideos");
        if (!dir.exists()) dir.mkdirs(); // make sure directory exists

        savedVideoFile = new File(dir, name + ".mp4"); // create a file for the new video

        Uri videoUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider", // must match the provider set in manifest
                savedVideoFile
        );

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE); // open default camera
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30); // limit video to 30 seconds
        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri); // save directly to our file
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // start camera intent
        startActivityForResult(intent, VIDEO_REQUEST);
    }

    // this uploads the video we just recorded to our Raspberry Pi via FTP
    private void uploadToFTP() {
        // check if we even have a video file to upload
        if (!hasRecordedVid()) {
            Toast.makeText(this, "Record a video first before adding the face", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasGivenName()){
            Toast.makeText(this, "A mame must be given before face can be added.", Toast.LENGTH_SHORT).show();
        }

        // use a separate thread for FTP because we don’t wanna block the UI
        new Thread(() -> {
            try {
                FTPClient ftp = new FTPClient();

                // FTP server info (this should match Pi’s settings)
                String ftpHost = "192.168.0.18"; // IP of Raspberry Pi
                int ftpPort = 2121;
                String ftpUser = "pi";
                String ftpPass = "raspberry";

                ftp.connect(ftpHost, ftpPort);
                boolean login = ftp.login(ftpUser, ftpPass); // try to log in

                if (!login) throw new Exception("FTP login failed.");

                ftp.enterLocalActiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                // upload the actual file
                FileInputStream fis = new FileInputStream(savedVideoFile);
                boolean success = ftp.storeFile("/" + savedVideoFile.getName(), fis); // uploads it
                fis.close();
                ftp.logout(); // done
                ftp.disconnect();

                // update UI based on upload result
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(AddNewFaceActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AddNewFaceActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                // show error message on the UI if something goes wrong
                runOnUiThread(() ->
                        Toast.makeText(AddNewFaceActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private boolean validateVideo(File videoFile) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFile.getAbsolutePath());

            // Duration check
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMs = Long.parseLong(durationStr);
            if (durationMs < 15000) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: Video must be at least 15 seconds long. Please record again", Toast.LENGTH_LONG).show()
                );
                retriever.release();
                return false;
            }

            // Brightness check on first frame
            Bitmap frame = retriever.getFrameAtTime(0);
            if (frame != null) {
                long totalBrightness = 0;
                int count = 0;

                for (int y = 0; y < frame.getHeight(); y += 10) {
                    for (int x = 0; x < frame.getWidth(); x += 10) {
                        int pixel = frame.getPixel(x, y);
                        int r = (pixel >> 16) & 0xff;
                        int g = (pixel >> 8) & 0xff;
                        int b = pixel & 0xff;
                        int brightness = (r + g + b) / 3;
                        totalBrightness += brightness;
                        count++;
                    }
                }

                long avgBrightness = totalBrightness / count;
                if (avgBrightness < 32) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Error: Video is too dark. Please record in better lighting.", Toast.LENGTH_LONG).show()
                    );
                    retriever.release();
                    return false;
                }
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: Could not extract frame for brightness check. Please record again", Toast.LENGTH_LONG).show()
                );
                retriever.release();
                return false;
            }

            retriever.release();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(this, "Error validating video: " + e.getMessage() + "Please record again", Toast.LENGTH_LONG).show()
            );
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtils.hasCameraPerms(this)){
            enableRecordVidBtn();
        }
        else{
            disableRecordVidBtn("Error: Permissions to record needed. Please grant this in settings", () -> PermissionUtils.getCameraPerms(AddNewFaceActivity.this));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VIDEO_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (validateVideo(savedVideoFile)) {
                    videoExists = true;
                    Toast.makeText(this, "Video saved successfully.", Toast.LENGTH_SHORT).show();
                    switchToReRecordBtn();
                    enableSubmitVidBtn();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Video recording cancelled.", Toast.LENGTH_SHORT).show();
                if (!videoExists) {
                    savedVideoFile = null;
                }
            } else {
                Toast.makeText(this, "Failed to record video.", Toast.LENGTH_SHORT).show();
                if (!videoExists){
                    savedVideoFile = null;
                }
            }
        }
    }

    private void enableRecordVidBtn(){
        btnRecord.setBackgroundColor(getColor(R.color.colorPrimary));
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordVideo();
            }
        });
    }

    private void disableRecordVidBtn(String toastMsg, Function func){
        btnRecord.setBackgroundColor(getColor(R.color.darkGrey));
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
                func.call();
            }
        });
    }

    private void enableSubmitVidBtn(){
        btnUpload.setBackgroundColor(getColor(R.color.colorPrimary));
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadToFTP();
            }
        });
    }

    private void disableSubmitVidBtn(String toastMsg){
        btnUpload.setBackgroundColor(getColor(R.color.darkGrey));
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchToReRecordBtn(){
        reRecordBtn.setVisibility(View.VISIBLE);
        reRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordVideo();
            }
        });

        successMsg.setVisibility(View.VISIBLE);
        btnRecord.setVisibility(View.GONE);
    }

    private void switchFromReRecordBtn(){
        btnRecord.setVisibility(View.VISIBLE);

        reRecordBtn.setVisibility(View.GONE);
        successMsg.setVisibility(View.GONE);
    }

    private boolean hasGivenName(){
        String name = etName.getText().toString().trim(); // grab name from the input field
        return !name.isEmpty();
    }

    private boolean hasRecordedVid(){
        return savedVideoFile != null && savedVideoFile.exists();
    }

    private interface Function {
        void call();
    }
}

