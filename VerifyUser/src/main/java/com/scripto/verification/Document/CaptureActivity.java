package com.scripto.verification.Document;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

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

import me.pqpo.smartcropperlib.view.CropImageView;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.AutoFocusCallback {

    Activity mActivity;
    double aspect = 0.0;
    float startTime;
    Camera mCamera;
    Bitmap originalImage, processedImage, savingImage;
    File path;
    File imageFile;
    FileOutputStream outputStream = null;

    boolean statusFocused = false, isFlashOn = false;
    int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK, height, width, heightScreen, widthScreen, heightCamera, widthCamera;
    double frameWidth, frameHeight, frameLeft, frameTop;

    View main_preview, frame_preview, bound_documents, left_layer, top_layer, right_layer, bottom_layer, imagePreviewContainer;
    SurfaceView surfaceView;
    CropImageView imagePreview;
    Button btnCapture, btnFlash;
    TableRow rowAction;
    TextView textLightMsg, textRetry, textOk;
    ProgressBar progressLoading;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getString(R.string.log_path), this, 0));

        System.loadLibrary("opencv_java4");

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_capture);
        if(getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mActivity = this;
        path = mActivity.getExternalCacheDir();

        aspect = getIntent().getDoubleExtra("aspect", aspect);

        clearFolder();

        if(aspect > 0.0){
            main_preview = findViewById(R.id.main_preview);
            frame_preview = findViewById(R.id.frame_preview);
            bound_documents = findViewById(R.id.bound_documents);
            left_layer = findViewById(R.id.left_layer);
            top_layer = findViewById(R.id.top_layer);
            right_layer = findViewById(R.id.right_layer);
            bottom_layer = findViewById(R.id.bottom_layer);
            surfaceView = findViewById(R.id.surface_camera);
            imagePreview = findViewById(R.id.imagePreview);
            btnCapture = findViewById(R.id.btnCapture);
            btnFlash = findViewById(R.id.btnFlash);
            textLightMsg = findViewById(R.id.textLightMsg);
            imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
            rowAction = findViewById(R.id.rowAction);
            textRetry = findViewById(R.id.textRetry);
            textOk = findViewById(R.id.textOk);
            progressLoading = findViewById(R.id.progressLoading);

            btnFlash.setOnClickListener(v -> {
                Camera.Parameters parameters = mCamera.getParameters();
                if(!isFlashOn) {
                    btnFlash.setForeground(AppCompatResources.getDrawable(mActivity, R.mipmap.flash_on));
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(parameters);
                } else {
                    btnFlash.setForeground(AppCompatResources.getDrawable(mActivity, R.mipmap.flash_off));
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(parameters);
                    mCamera.stopPreview();
                }

                mCamera.startPreview();
                isFlashOn = !isFlashOn;
            });

            btnCapture.setOnClickListener(v -> {
                if(statusFocused){
                    mCamera.takePicture(null, null, (data, camera) -> {
                        imagePreviewContainer.setVisibility(View.VISIBLE);
                        rowAction.setVisibility(View.VISIBLE);
                        btnCapture.setVisibility(View.GONE);
                        btnFlash.setVisibility(View.GONE);
                        textLightMsg.setVisibility(View.GONE);

                        int angleToRotate = getRotationAngle();
                        angleToRotate = angleToRotate + 180;

                        originalImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                        processedImage = rotateBitmap(originalImage, angleToRotate);
                        imagePreview.setImageToCrop(processedImage);

                        setupCamera();
                    });
                } else {
                    Toast.makeText(mActivity, getString(R.string.msg_hold_steady), Toast.LENGTH_LONG).show();
                }
            });

            textOk.setOnClickListener(v -> {
                Toast.makeText(mActivity, getString(R.string.msg_wait), Toast.LENGTH_LONG).show();
                progressLoading.setVisibility(View.VISIBLE);

                new Handler().postDelayed(() -> {

                    savingImage = imagePreview.crop();
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
                }, 250);
            });

            textRetry.setOnClickListener(v -> recreate());

            main_preview.setPadding(0, 0, 0, getBottomPadding());

            setupCamera();
        } else {
            Toast.makeText(mActivity, getString(R.string.msg_error_wrong_aspect_ratio), Toast.LENGTH_SHORT).show();
            finish();
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

    private void setupCamera() {
        try {
            mCamera = Camera.open(cameraID);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(this);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(surfaceHolder);

            Camera.Parameters parameters = mCamera.getParameters();

            List<String> flashModes = parameters.getSupportedFlashModes();
            if (flashModes.contains(android.hardware.Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            }
            //mCamera.setParameters(parameters);

            btnFlash.setForeground(AppCompatResources.getDrawable(mActivity, R.mipmap.flash_off));
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            isFlashOn = false;

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

        heightCamera = (int) (width * (widthScreen/(1.0 * height)));
        widthCamera = widthScreen;

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

        frameWidth = widthCamera * 0.8;
        frameHeight = frameWidth * aspect;
        frameLeft = widthCamera * 0.1;
        frameTop = (heightCamera - frameHeight) * 0.5;

        FrameLayout.LayoutParams paramsLeft = (FrameLayout.LayoutParams) left_layer.getLayoutParams();
        paramsLeft.setMargins(0, (int) frameTop, 0, (int) frameTop);
        paramsLeft.width = (int) (widthCamera * 0.1);
        left_layer.setLayoutParams(paramsLeft);

        FrameLayout.LayoutParams paramsTop = (FrameLayout.LayoutParams) top_layer.getLayoutParams();
        paramsTop.height = (int) frameTop;
        top_layer.setLayoutParams(paramsTop);

        FrameLayout.LayoutParams paramsRight = (FrameLayout.LayoutParams) right_layer.getLayoutParams();
        paramsRight.setMargins(0, (int) frameTop, 0, (int) frameTop);
        paramsRight.width = (int) (widthCamera * 0.1);
        right_layer.setLayoutParams(paramsRight);

        FrameLayout.LayoutParams paramsBottom = (FrameLayout.LayoutParams) bottom_layer.getLayoutParams();
        paramsBottom.height = (int) frameTop;
        bottom_layer.setLayoutParams(paramsBottom);

        FrameLayout.LayoutParams paramsFrame = (FrameLayout.LayoutParams) bound_documents.getLayoutParams();
        paramsFrame.setMargins((int) frameLeft, (int) frameTop, (int) frameLeft, (int) frameTop);
        bound_documents.setLayoutParams(paramsFrame);

        ConstraintLayout.LayoutParams paramsCapture = (ConstraintLayout.LayoutParams) btnCapture.getLayoutParams();
        paramsCapture.width = (int) (widthScreen * 0.15);
        paramsCapture.height = (int) (widthScreen * 0.15);
        btnCapture.setLayoutParams(paramsCapture);

        ConstraintLayout.LayoutParams paramsFlash = (ConstraintLayout.LayoutParams) btnFlash.getLayoutParams();
        paramsFlash.width = (int) (widthScreen * 0.15);
        paramsFlash.height = (int) (widthScreen * 0.15);
        btnFlash.setLayoutParams(paramsFlash);

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
        data.putExtra("uri", text);
        //data.setData(Uri.parse(text));
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(90);
            new Handler().postDelayed(() -> {
                if(mActivity != null) mCamera.autoFocus((success, camera) -> statusFocused = true);
            }, 500);
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int MAX_DURATION = 200;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            handleFocus(event, mCamera.getParameters());
            startTime = System.currentTimeMillis();
        }

        else if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if(System.currentTimeMillis() - startTime <= MAX_DURATION) {
                mCamera.autoFocus((Camera.AutoFocusCallback) mActivity);
            }
        }
        return true;
    }

    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);

        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        Rect touchRect = new Rect(
                (int) (x - 100),
                (int) (y - 100),
                (int) (x + 100),
                (int) (y + 100) );
        final Rect targetFocusRect = new Rect(
                touchRect.left * 2000/surfaceView.getWidth() - 1000,
                touchRect.top * 2000/surfaceView.getHeight() - 1000,
                touchRect.right * 2000/surfaceView.getWidth() - 1000,
                touchRect.bottom * 2000/surfaceView.getHeight() - 1000);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            try {
                List<Camera.Area> focusList = new ArrayList<>();
                Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
                focusList.add(focusArea);


                params.setFocusAreas(focusList);
                params.setMeteringAreas(focusList);
                mCamera.setParameters(params);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        setResult(RESULT_CANCELED, data);
        new Handler().postDelayed(super::onBackPressed, statusFocused ? 0 : 500);
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        statusFocused = true;
    }
}