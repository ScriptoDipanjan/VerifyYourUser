package com.scripto.verification.Video;

import static com.scripto.verification.Utility.ExceptionHandler.getFile;
import static com.scripto.verification.Utility.ExceptionHandler.showError;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.scripto.verification.R;
import com.scripto.verification.Utility.ExceptionHandler;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import cdflynn.android.library.checkview.CheckView;
import de.hdodenhof.circleimageview.CircleImageView;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CascadeClassifier faceDetector, eyeDetector;
    public MediaRecorder mediaRecorder;
    File videoFile;
    boolean isDetected = false, isRecording = false, isRecognizing = false, isVerified = false;
    Mat mRGBA, mGray;
    int height, width, widthOval, heightOval, leftOval, topOval, rightOval, bottomOval;
    int successCounter = 0, statusRotation = 0,
            faceXStart = 0, faceXEnd = 0, faceWidthStart = 0,
            faceXStartMoving = 0, faceXEndMoving = 0, faceWidthStartMoving = 0;
    double time = 30;
    int brightness;

    Activity mActivity;
    ObjectAnimator animator;
    private CameraBridgeViewBase mOpenCvCameraView;
    ImageView imageOval;
    TextView textCounter, textInstruction;
    CircleImageView verify_touch;
    TableRow rowProgress;
    ProgressBar progressLeft, progressRight;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                try {
                    InputStream is_face = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    File mCascadeFace = new File(cascadeDir, getString(R.string.text_haarcascade_frontalface));
                    FileOutputStream os_face = new FileOutputStream(mCascadeFace);

                    byte[] buffer_face = new byte[4096];
                    int bytesRead_face;
                    while ((bytesRead_face = is_face.read(buffer_face)) != -1) {
                        os_face.write(buffer_face, 0, bytesRead_face);
                    }
                    is_face.close();
                    os_face.close();

                    faceDetector = new CascadeClassifier(mCascadeFace.getAbsolutePath());

                    InputStream is_eye = getResources().openRawResource(R.raw.haarcascade_eye);
                    File mCascadeEye = new File(cascadeDir, getString(R.string.text_haarcascade_eye));
                    FileOutputStream os_eye = new FileOutputStream(mCascadeEye);


                    byte[] buffer_eye = new byte[4096];
                    int bytesRead_eye;
                    while ((bytesRead_eye = is_eye.read(buffer_eye)) != -1) {
                        os_eye.write(buffer_eye, 0, bytesRead_eye);
                    }
                    is_eye.close();
                    os_eye.close();

                    eyeDetector = new CascadeClassifier(mCascadeEye.getAbsolutePath());

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(mActivity, getString(R.string.msg_error_loading_cascade), Toast.LENGTH_LONG).show();
                }
                mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getString(R.string.log_path), this, 0));

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        System.loadLibrary("opencv_java4");

        if(getSupportActionBar() != null)
            getSupportActionBar().hide();

        mActivity = this;

        mOpenCvCameraView = findViewById(R.id.CameraView);
        imageOval = findViewById(R.id.imageOval);
        textCounter = findViewById(R.id.textCounter);
        textInstruction = findViewById(R.id.textInstruction);
        verify_touch = findViewById(R.id.verify_touch);
        rowProgress = findViewById(R.id.rowProgress);
        progressLeft = findViewById(R.id.progressLeft);
        progressRight = findViewById(R.id.progressRight);

        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraPermissionGranted();

        brightness = Settings.System.getInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        Settings.System.putInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 1000);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        this.height = height;
        this.width = width;

        mRGBA = new Mat();
        mGray = new Mat();

        doBounceAnimation(textInstruction);

        runOnUiThread(() -> {
            AtomicInteger dynamicViews = new AtomicInteger();
            imageOval.getViewTreeObserver().addOnGlobalLayoutListener(
                    () -> {
                        if (dynamicViews.get() == 0) {
                            widthOval = imageOval.getWidth();
                            heightOval = imageOval.getHeight();
                            leftOval = imageOval.getLeft();
                            topOval = imageOval.getTop();
                            rightOval = imageOval.getRight();
                            bottomOval = imageOval.getBottom();
                        }
                    }
            );
        });
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mGray = inputFrame.gray();

        Core.flip(mRGBA, mRGBA, 1);

        int height = mRGBA.rows();
        int faceSize = Math.round(height * 0.5f);

        Mat temp = mRGBA.clone();
        Core.transpose(mRGBA, temp);
        Core.flip(temp, temp, 1);

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(
                temp,
                faceDetections,
                1.1,
                1,
                0,
                new Size(faceSize, faceSize), new Size()
        );

        for(Rect rect : faceDetections.toArray()){

            Rect eye_rect = new Rect(
                    (int)rect.tl().x,
                    (int)rect.tl().y,
                    (int)rect.br().x - (int)rect.tl().x,
                    (int)rect.br().y - (int)rect.tl().y
            );

            Mat cropped = new Mat(temp, eye_rect);
            MatOfRect eye_detect = new MatOfRect();
            eyeDetector.detectMultiScale(
                    cropped,
                    eye_detect,
                    1.1,
                    2,
                    0,
                    new Size(120, 120)
            );

            runOnUiThread(() -> {
                if(!isDetected && successCounter == 0){
                    if((rect.tl().x + (rect.width * 0.5) >= width * (1 - 0.025)
                            || rect.tl().x + (rect.width * 0.5) <= width * (1 + 0.025))
                            && (rect.height >= heightOval * 0.8 || rect.height <= heightOval * 0.85)
                            && eye_detect.toArray().length == 2){
                        startRecording();
                    }
                }

                else if(!isRecognizing && successCounter == 1){
                    initFaceMovement();
                }

                else if(successCounter == 2 && faceDetections.toArray().length == 1){

                    if((statusRotation == 0 || statusRotation == 2) && (faceXStart == 0 && faceXEnd == 0 && faceWidthStart == 0)){
                        faceXStart = (int) faceDetections.toArray()[0].tl().x;
                        faceXEnd = (int) faceDetections.toArray()[0].br().x;
                        faceWidthStart = (int)faceDetections.toArray()[0].br().x - (int)faceDetections.toArray()[0].tl().x;
                    }

                    faceXStartMoving = (int) faceDetections.toArray()[0].tl().x;
                    faceXEndMoving = (int) faceDetections.toArray()[0].br().x;
                    faceWidthStartMoving = (int)faceDetections.toArray()[0].br().x - (int)faceDetections.toArray()[0].tl().x;

                    if(statusRotation < 2){
                        int progress = (int) ((faceXStartMoving - faceXStart) * 100 / (faceWidthStart * 0.25));
                        progressRight.setProgress(progress);
                        progressLeft.setProgress(0);
                    }
                    else if(statusRotation < 4){
                        int progress = (int) ((faceXEnd - faceXEndMoving) * 100 / (faceWidthStart * 0.25));
                        progressLeft.setProgress(progress);
                        progressRight.setProgress(0);
                    }

                    if(statusRotation == 0 && progressRight.getProgress() > 95){

                        statusRotation = 1;
                        progressLeft.setProgress(0);
                        showInstruction(getString(R.string.msg_back_straight));

                        startVibration();

                    }
                    else if(statusRotation == 1 && progressRight.getProgress() < 5){

                        statusRotation = 2;
                        showSuccess();
                        progressLeft.setProgress(0);
                        progressRight.setProgress(0);
                        rowProgress.setVisibility(View.GONE);

                        new Handler().postDelayed(() -> {
                            faceXStart = faceXEnd = faceWidthStart = 0;
                            rowProgress.setVisibility(View.VISIBLE);
                            showInstruction(getString(R.string.msg_move_left_back_straight));
                        }, 1000);

                    }
                    else if(statusRotation == 2 && progressLeft.getProgress() > 95) {

                        statusRotation = 3;
                        progressRight.setProgress(0);
                        showInstruction(getString(R.string.msg_back_straight));

                        startVibration();

                    }
                    else if(statusRotation == 3 && progressLeft.getProgress() < 5){

                        statusRotation = 4;
                        successCounter = 3;
                        isVerified = true;
                        progressLeft.setProgress(0);
                        progressRight.setProgress(0);
                        rowProgress.setVisibility(View.GONE);
                        showSuccess();
                        if(animator.isRunning())
                            animator.end();

                        new Handler().postDelayed(this::stopRecording, 500);
                    }
                }
            });
        }

        return mRGBA;
    }

    private void showInstruction(String msg) {
        textInstruction.setText(msg);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync("opencv_java3", this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }

    @Override
    public void onBackPressed() {
        if(!isRecording){
            setCancel();
        }
    }

    private void doBounceAnimation(View targetView) {
        Interpolator interpolator = this::getPowOut;
        animator = ObjectAnimator.ofFloat(targetView, "translationY", 0, 25, 0);
        animator.setInterpolator(interpolator);
        animator.setStartDelay(250);
        animator.setDuration(500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }

    private float getPowOut(float elapsedTimeRate) {
        return (float) ((float) 1 - Math.pow(1 - elapsedTimeRate, 3));
    }

    private void showSuccess() {
        if (Looper.myLooper() == null)
            Looper.prepare();

        showInstruction(getString(R.string.msg_wait));
        Dialog check = new Dialog(mActivity);
        check.setContentView(R.layout.dialog_success);
        check.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        check.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        check.show();

        check.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        check.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        CheckView mCheckView = check.findViewById(R.id.check);
        mCheckView.check();

        startVibration();

        new Handler().postDelayed(check::dismiss, 1000);

        if(successCounter == 3)
            showInstruction(getString(R.string.msg_verification_success));
    }

    @SuppressLint("MissingPermission")
    private void startVibration() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void initFaceMovement() {

        new Handler().postDelayed(() -> {
            if(successCounter < 2)
                successCounter = 2;

            showInstruction(getString(R.string.msg_move_right_back_straight));
            rowProgress.setVisibility(View.VISIBLE);
        }, 1000);
    }

    protected void startRecording(){

        File reportFile = getFile();

        PrintStream ps = null;

        try {
            reportFile.createNewFile();
            try {
                ps = new PrintStream(reportFile);
            } catch (FileNotFoundException e) {
                showError(mActivity, Log.getStackTraceString(e));
                e.printStackTrace();
            }
        } catch (IOException e) {
            showError(mActivity, Log.getStackTraceString(e));
            e.printStackTrace();
        }

        new Handler().postDelayed(() -> successCounter = 1, 1000);

        showSuccess();
        isDetected = true;

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Saved_Videos");
        String timeStamp = new SimpleDateFormat(getString(R.string.text_time_format), Locale.getDefault()).format(new Date());

        path.mkdir();

        videoFile = new File(path, "VID_" + timeStamp + ".mp4");

        if (!isRecording) {
            try {
                if (mediaRecorder == null && !videoFile.exists()) {
                    videoFile.getParentFile().mkdirs();
                    videoFile.createNewFile();
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.reset();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
                    mediaRecorder.setOrientationHint(90);

                    mediaRecorder.setVideoSize(width, height);
                    mediaRecorder.setOutputFile(videoFile);
                    try {
                        mediaRecorder.prepare();
                        mOpenCvCameraView.setRecorder(mediaRecorder);
                        mediaRecorder.start();

                        isRecording = true;
                        updateTimer();
                    } catch (IOException e) {
                        e.printStackTrace(Objects.requireNonNull(ps));

                        setCancel();
                    }
                }
            } catch (Exception e) {
                if(ps != null) {
                    e.printStackTrace(ps);
                } else {
                    showError(mActivity, Log.getStackTraceString(e));
                }

                setCancel();
            }
        }

        if(ps != null)
            ps.close();
    }

    private void updateTimer() {
        String count;

        if((int) time < 10)
            count = getString(R.string.text_sec) + (int) time;
        else
            count = getString(R.string.text_secs) + (int) time;

        textCounter.setText(count);

        time--;

        if(time > -1 && isRecording && !isVerified) {
            new Handler().postDelayed(this::updateTimer, 1000);
        } else if(!(time > -1 && isRecording)) {
            stopRecording();
        }
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    protected void stopRecording() {

        File reportFile = getFile();

        PrintStream ps = null;

        try {
            reportFile.createNewFile();
            try {
                ps = new PrintStream(reportFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaRecorder != null && isRecording) {
            textInstruction.setVisibility(View.VISIBLE);
            rowProgress.setVisibility(View.GONE);
            isRecording = false;
            mOpenCvCameraView.disableView();
            textCounter.setText("");
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mOpenCvCameraView.setRecorder(null);
                String filePath = videoFile.getCanonicalPath();
                if(successCounter == 3) {
                    refreshGallery(videoFile);
                    new Handler().postDelayed(() -> {
                        Intent data = new Intent();
                        data.setData(Uri.parse(filePath));
                        setResult(RESULT_OK, data);
                        Settings.System.putInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, Math.min(brightness, 25));
                        finish();
                    }, 500);
                } else if(videoFile.delete()){
                    setCancel();
                }
            } catch(RuntimeException | IOException e) {
                if(ps != null) {
                    e.printStackTrace(ps);
                } else {
                    showError(mActivity, Log.getStackTraceString(e));
                }
                Toast.makeText(mActivity, getString(R.string.msg_video_save_failed) + " " + e.getMessage(), Toast.LENGTH_LONG).show();

                setCancel();
            }
        }

        if(ps != null)
            ps.close();
    }

    private void setCancel() {
        if(videoFile != null && videoFile.exists())
            videoFile.delete();

        Intent result = new Intent();
        setResult(RESULT_CANCELED, result);
        Settings.System.putInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, Math.min(brightness, 25));
        finish();
    }
}