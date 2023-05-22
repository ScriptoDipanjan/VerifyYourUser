package com.scripto.verification.Selfie;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.scripto.verification.R;
import com.scripto.verification.Utility.ExceptionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback{
    Activity mActivity;
    Camera mCamera;
    Bitmap originalImage, processedImage, savingImage;
    File path;
    File imageFile;
    FileOutputStream outputStream = null;

    boolean allPermit = false;
    int cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT, height, width, heightScreen, widthScreen, heightCamera, widthCamera;

    View main_preview, frame_preview, view_flash;
    SurfaceView surfaceView;
    ImageView imagePreview;
    Button btnCapture;
    TableRow rowAction;
    TextView textRetry, textOk;

    @Override
    protected void onStart() {
        super.onStart();
        checkPermissions();
    }

    void checkPermissions() {
        List<String> permission = new ArrayList<>();
        permission.add(Manifest.permission.CAMERA);
        if(android.os.Build.VERSION.SDK_INT < 29){
            permission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        Dexter.withContext(mActivity)
                .withPermissions(permission)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            allPermit = true;
                            setupCamera();
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

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getString(R.string.log_path), this, 0));

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_capture_selfie);
        if(getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mActivity = this;
        path = mActivity.getExternalCacheDir();
        //cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;

        clearFolder();

        main_preview = findViewById(R.id.main_preview);
        frame_preview = findViewById(R.id.frame_preview);
        view_flash = findViewById(R.id.view_flash);
        surfaceView = findViewById(R.id.surface_camera);
        imagePreview = findViewById(R.id.imagePreview);
        btnCapture = findViewById(R.id.btnCapture);
        rowAction = findViewById(R.id.rowAction);
        textRetry = findViewById(R.id.textRetry);
        textOk = findViewById(R.id.textOk);

        btnCapture.setOnClickListener(v -> {
            view_flash.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> view_flash.setVisibility(View.GONE), 500);
            mCamera.takePicture(null, null, (data, camera) -> {
                rowAction.setVisibility(View.VISIBLE);
                btnCapture.setVisibility(View.GONE);

                int angleToRotate = getRotationAngle();
                angleToRotate = angleToRotate + 180;

                originalImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                processedImage = rotateBitmap(originalImage, angleToRotate);
                imagePreview.setImageBitmap(processedImage);

                setupCamera();
            });
        });

        textOk.setOnClickListener(v -> {
            imagePreview.buildDrawingCache();
            savingImage = imagePreview.getDrawingCache();
            imageFile = getFile();

            try {
                setResultOK(savingImage);

            } catch (Exception e) {
                e.printStackTrace();

                try {
                    setResultOK(processedImage);

                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        textRetry.setOnClickListener(v -> {
            imagePreview.setImageBitmap(null);
            rowAction.setVisibility(View.GONE);
            btnCapture.setVisibility(View.VISIBLE);
        });

        main_preview.setPadding(0, 0, 0, getBottomPadding());

        if(allPermit){
            setupCamera();
        } else {
            checkPermissions();
        }

    }

    private void clearFolder() {
        if (path.isDirectory()) {
            String[] children = path.list();
            for(int i = 0; i < Objects.requireNonNull(children).length; i++) {
                if(new File(path, children[i]).delete()){
                    Log.e("File", "deleted");
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        setResult(RESULT_CANCELED, data);
        super.onBackPressed();
    }

    private void setupCamera() {

        try {
            mCamera = Camera.open(cameraID);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(this);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size size = parameters.getPreferredPreviewSizeForVideo();
        height = size.height;
        width = size.width;

        Display display = ((WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        heightScreen = metrics.heightPixels;
        widthScreen = metrics.widthPixels;

        if(widthScreen > height){
            heightCamera = (int) (width * (widthScreen/(1.0 * height)));
            widthCamera = widthScreen;
        } else {
            heightCamera = width;
            widthCamera = height;
        }

        ConstraintSet set = new ConstraintSet();
        set.constrainHeight(frame_preview.getId(), heightCamera);
        set.constrainWidth(frame_preview.getId(), widthCamera);

        set.connect(frame_preview.getId(), ConstraintSet.LEFT,
                ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        set.connect(frame_preview.getId(), ConstraintSet.RIGHT,
                ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        set.connect(frame_preview.getId(), ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.TOP, (heightScreen - width)/2);
        set.connect(frame_preview.getId(), ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, (heightScreen - width)/2);

        set.applyTo((ConstraintLayout) main_preview);

        ConstraintLayout.LayoutParams paramsCapture = (ConstraintLayout.LayoutParams) btnCapture.getLayoutParams();
        paramsCapture.width = (int) (widthScreen * 0.15);
        paramsCapture.height = (int) (widthScreen * 0.15);
        btnCapture.setLayoutParams(paramsCapture);

    }

    @SuppressWarnings("InternalInsetResource")
    @SuppressLint("DiscouragedApi")
    int getBottomPadding(){
        Resources resources = mActivity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    int getRotationAngle() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraID, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = (info.orientation + degrees) % 360;
        result = (360 - result) % 360;

        return result;
    }

    Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mat = new Matrix();
        mat.postRotate(degree);
        mat.postScale(-1, 1, w/2f, h/2f);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mat, true);
    }

    @SuppressWarnings("all")
    @NonNull
    private File getFile() {
        path.mkdirs();

        String timeStamp = new SimpleDateFormat(getString(R.string.text_time_format), Locale.getDefault()).format(new Date());

        return new File(path, "IMG_" + timeStamp + ".png");
    }

    private void setResultOK(Bitmap bitmapImage) throws IOException {

        outputStream = new FileOutputStream(imageFile);

        bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

        outputStream.flush();
        outputStream.close();

        Intent data = new Intent();
        String text = imageFile.getCanonicalPath();
        data.setData(Uri.parse(text));
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(90);
        } else {
            Toast.makeText(mActivity, getString(R.string.msg_error_camera_not_found), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
    }
}