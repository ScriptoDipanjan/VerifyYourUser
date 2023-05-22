package com.scripto.verifyyouruser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.scripto.verification.Document.DocumentSelectionActivity;
import com.scripto.verification.Selfie.SelfieVerification;
import com.scripto.verification.Video.VideoVerificationActivity;
import com.scripto.verifyyouruser.R;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    Context mContext;
    int requestCode = 0;
    String selfieURI, videoURI, document1URI, document2URI;
    boolean selfie = false, video = false, document1 = false, document2 = false;
    JSONObject document1Data = new JSONObject(), document2Data = new JSONObject();
    LinearLayout buttonSelfie, buttonVideo, buttonDocument1, buttonDocument2;
    TextView textVersion;

    ActivityResultLauncher<Intent> mainActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            resultActivity -> {
                int resultCode = resultActivity.getResultCode();
                Intent data = resultActivity.getData();

                if (data != null) {
                    if (requestCode == 1) {
                        if(resultCode == RESULT_OK) {
                            selfie = true;
                            selfieURI = data.getStringExtra("uri");
                        } else {
                            selfie = false;
                        }
                    } else if (requestCode == 2) {
                        if(resultCode == RESULT_OK) {
                            video = true;
                            videoURI = data.getData().getPath();
                        } else {
                            video = false;
                        }
                    } else if (requestCode == 3) {
                        if(resultCode == RESULT_OK) {
                            document1 = true;
                            document1URI = data.getStringExtra("uri");
                            try {
                                document1Data.put("Image Sharpness", data.getStringExtra(getString(com.scripto.verification.R.string.doc_sharpness)));
                                document1Data.put("Blur Percentage", data.getStringExtra(getString(com.scripto.verification.R.string.doc_blur)));
                                document1Data.put("filename", Uri.parse(document1URI).getLastPathSegment());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            document1 = false;
                        }
                    } else if (requestCode == 4) {
                        if(resultCode == RESULT_OK) {
                            document2 = true;
                            document2URI = data.getStringExtra("uri");
                            try {
                                document2Data.put("Image Sharpness", data.getStringExtra(getString(com.scripto.verification.R.string.doc_sharpness)));
                                document2Data.put("Blur Percentage", data.getStringExtra(getString(com.scripto.verification.R.string.doc_blur)));
                                document2Data.put("filename", Uri.parse(document2URI).getLastPathSegment());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            document2 = false;
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        if(getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mContext = this;

        textVersion = findViewById(R.id.textVersion);
        String version = "Version: " + BuildConfig.VERSION_NAME;
        textVersion.setText(version);

        buttonSelfie = findViewById(R.id.buttonSelfie);
        buttonVideo = findViewById(R.id.buttonVideo);
        buttonDocument1 = findViewById(R.id.buttonDocument1);
        buttonDocument2 = findViewById(R.id.buttonDocument2);

        buttonSelfie.setOnClickListener(v -> {
            requestCode = 1;
            mainActivityResultLauncher.launch(new Intent(mContext, SelfieVerification.class));
        });
        buttonVideo.setOnClickListener(v -> {
            requestCode = 2;
            mainActivityResultLauncher.launch(new Intent(mContext, VideoVerificationActivity.class));
        });
        buttonDocument1.setOnClickListener(v -> {
            requestCode = 3;
            mainActivityResultLauncher.launch(new Intent(mContext, DocumentSelectionActivity.class));
        });
        buttonDocument2.setOnClickListener(v -> {
            requestCode = 4;
            mainActivityResultLauncher.launch(new Intent(mContext, DocumentSelectionActivity.class));
        });
    }
}

//image 50-100kb
//video 10mb