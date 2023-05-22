package com.scripto.verification.Selfie;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.scripto.verification.R;
import com.scripto.verification.Utility.AppPref;
import com.scripto.verification.Utility.ExceptionHandler;

import java.util.Objects;

public class SelfieVerification extends AppCompatActivity {

    Context mContext;

    int requestCode = 0;

    ActivityResultLauncher<Intent> previewActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            resultActivity -> {
                int resultCode = resultActivity.getResultCode();
                Intent data = resultActivity.getData();

                Intent result = new Intent();
                AppPref pref = new AppPref(mContext);

                if(data != null){
                    if (requestCode == 1) {
                        if (resultCode == RESULT_OK) {

                            String pathString = data.getData().getPath();

                            result.putExtra("uri", pathString);
                            result.putExtra(getString(R.string.selfie_sharpness), pref.getResponse(getString(R.string.selfie_sharpness)));
                            result.putExtra(getString(R.string.selfie_blur), pref.getResponse(getString(R.string.selfie_blur)));
                            result.putExtra(getString(R.string.face_count), pref.getResponse(getString(R.string.face_count)));
                            result.putExtra(getString(R.string.face_angle_up_down), pref.getResponse(getString(R.string.face_angle_up_down)));
                            result.putExtra(getString(R.string.face_angle_left_right), pref.getResponse(getString(R.string.face_angle_left_right)));
                            result.putExtra(getString(R.string.face_angle_tilted), pref.getResponse(getString(R.string.face_angle_tilted)));
                            result.putExtra(getString(R.string.face_id), pref.getResponse(getString(R.string.face_id)));
                            result.putExtra(getString(R.string.face_open_prob_right), pref.getResponse(getString(R.string.face_open_prob_right)));
                            result.putExtra(getString(R.string.face_open_prob_left), pref.getResponse(getString(R.string.face_open_prob_left)));
                            setResult(RESULT_OK, result);

                            Toast.makeText(mContext, getString(R.string.msg_capture_saved) + pathString, Toast.LENGTH_LONG).show();

                        } else if (resultCode == RESULT_CANCELED) {

                            setResult(RESULT_CANCELED, result);
                            Toast.makeText(mContext, getString(R.string.msg_error_capture_cancel), Toast.LENGTH_LONG).show();

                        } else {

                            setResult(RESULT_CANCELED, result);
                            Toast.makeText(mContext, getString(R.string.msg_error_capture_failed), Toast.LENGTH_LONG).show();

                        }
                    } else if(requestCode == 2){
                        if (resultCode == RESULT_OK) {

                            String pathString = data.getData().getPath();

                            result.putExtra("uri", pathString);
                            result.putExtra(getString(R.string.selfie_sharpness), pref.getResponse(getString(R.string.selfie_sharpness)));
                            result.putExtra(getString(R.string.selfie_blur), pref.getResponse(getString(R.string.selfie_blur)));
                            result.putExtra(getString(R.string.face_count), pref.getResponse(getString(R.string.face_count)));
                            result.putExtra(getString(R.string.face_angle_up_down), pref.getResponse(getString(R.string.face_angle_up_down)));
                            result.putExtra(getString(R.string.face_angle_left_right), pref.getResponse(getString(R.string.face_angle_left_right)));
                            result.putExtra(getString(R.string.face_angle_tilted), pref.getResponse(getString(R.string.face_angle_tilted)));
                            result.putExtra(getString(R.string.face_id), pref.getResponse(getString(R.string.face_id)));
                            result.putExtra(getString(R.string.face_open_prob_right), pref.getResponse(getString(R.string.face_open_prob_right)));
                            result.putExtra(getString(R.string.face_open_prob_left), pref.getResponse(getString(R.string.face_open_prob_left)));
                            setResult(RESULT_OK, result);

                            Toast.makeText(mContext, getString(R.string.msg_gallery_saved) + pathString, Toast.LENGTH_LONG).show();

                        } else if (resultCode == RESULT_CANCELED) {

                            setResult(RESULT_CANCELED, result);
                            Toast.makeText(mContext, getString(R.string.msg_error_pick_cancel), Toast.LENGTH_LONG).show();

                        } else {

                            setResult(RESULT_CANCELED, result);
                            Toast.makeText(mContext, getString(R.string.msg_error_pick_failed), Toast.LENGTH_LONG).show();

                        }
                    }

                    pref.clearData();
                    finish();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getString(R.string.log_path), this, 0));

        setContentView(R.layout.activity_selfie_verification);

        Objects.requireNonNull(getSupportActionBar()).hide();

        mContext = this;

        LinearLayout buttonCamera = findViewById(R.id.buttonCamera);
        LinearLayout buttonGallery = findViewById(R.id.buttonGallery);

        buttonCamera.setOnClickListener(v -> proceedPreview("camera", 1));
        buttonGallery.setOnClickListener(v -> proceedPreview("gallery", 2));
    }

    private void proceedPreview(String type, int requestCode) {
        this.requestCode = requestCode;

        previewActivityResultLauncher.launch(
                new Intent(mContext, SelfiePreviewActivity.class)
                        .putExtra("type", type)
        );
    }
}