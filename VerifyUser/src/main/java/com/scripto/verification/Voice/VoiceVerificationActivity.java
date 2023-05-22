package com.scripto.verification.Voice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.scripto.verification.R;
import com.scripto.verification.Utility.ExceptionHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cdflynn.android.library.checkview.CheckView;

public class VoiceVerificationActivity extends AppCompatActivity {
    Activity mActivity;
    boolean allPermit = false, isRecord = false;
    String path = "";
    CheckView mCheckView;
    View success_layout, failed_layout;
    TextView textPermission;
    LinearLayout buttonSaveExit, buttonNoSaveExit, buttonRetrySuccess, buttonRetry, buttonExit;

    @Override
    protected void onStart() {
        super.onStart();

        if(!Settings.System.canWrite(this)){
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + mActivity.getPackageName()));
            startActivity(intent);
        } else if(!allPermit){
            checkPermissions();
        }
    }

    private void checkPermissions() {
        textPermission.setText(mActivity.getString(R.string.msg_allow_permissions));
        List<String> permission = new ArrayList<>();
        permission.add(Manifest.permission.RECORD_AUDIO);
        permission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permission.add(Manifest.permission.CAMERA);

        Dexter.withContext(mActivity)
                .withPermissions(permission)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            allPermit = true;
                            callCaptureActivity();
                        } else {
                            checkPermissions();
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(
                            List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_verification);

        Objects.requireNonNull(getSupportActionBar()).hide();

        mActivity = this;

        success_layout = findViewById(R.id.success_layout);
        failed_layout = findViewById(R.id.failed_layout);
        textPermission = findViewById(R.id.textPermission);
        mCheckView = findViewById(R.id.check);
        buttonSaveExit = findViewById(R.id.buttonSaveExit);
        buttonNoSaveExit = findViewById(R.id.buttonNoSaveExit);
        buttonRetrySuccess = findViewById(R.id.buttonRetrySuccess);
        buttonRetry = findViewById(R.id.buttonRetry);
        buttonExit = findViewById(R.id.buttonExit);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getString(R.string.log_path), this, 0));

        if(!allPermit)
            checkPermissions();
        else
            callCaptureActivity();
    }

    void callCaptureActivity() {
        textPermission.setText("");
        if(!isRecord){
            mActivity.startActivityForResult(
                    new Intent(mActivity, CameraActivity.class),
                    1);
            isRecord = true;
        }
    }

    @Override
    protected void onDestroy() {
        isRecord = false;
        mActivity = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            if(resultCode == RESULT_CANCELED){

                failed_layout.setVisibility(View.VISIBLE);

                buttonRetry.setOnClickListener(v -> {
                    isRecord = false;
                    success_layout.setVisibility(View.GONE);
                    failed_layout.setVisibility(View.GONE);
                    checkPermissions();
                });

                buttonExit.setOnClickListener(v -> {
                    Intent result = new Intent();
                    setResult(RESULT_CANCELED, result);
                    Toast.makeText(mActivity, getString(R.string.msg_error_video_failed), Toast.LENGTH_LONG).show();
                    finish();
                });

            } else if(resultCode == RESULT_OK && data != null) {
                path = data.getData().getPath();

                success_layout.setVisibility(View.VISIBLE);
                mCheckView.check();

                buttonSaveExit.setOnClickListener(v -> {
                    Intent result = new Intent();
                    result.setData(Uri.parse(path));
                    setResult(RESULT_OK, result);
                    Toast.makeText(mActivity, getString(R.string.msg_video_saved) + path, Toast.LENGTH_LONG).show();
                    finish();
                });

                buttonNoSaveExit.setOnClickListener(v -> {
                    if(new File(path).delete()){
                        Intent result = new Intent();
                        setResult(RESULT_CANCELED, result);
                        Toast.makeText(mActivity, getString(R.string.msg_error_video_failed), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

                buttonRetrySuccess.setOnClickListener(v -> {
                    isRecord = false;
                    success_layout.setVisibility(View.GONE);
                    failed_layout.setVisibility(View.GONE);
                    checkPermissions();
                });
            }
        }
    }
}