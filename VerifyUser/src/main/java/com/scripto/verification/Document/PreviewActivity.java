package com.scripto.verification.Document;

import static com.scripto.verification.Utility.ExceptionHandler.getFile;
import static com.scripto.verification.Utility.ExceptionHandler.showError;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.exifinterface.media.ExifInterface;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.scripto.verification.R;
import com.scripto.verification.Utility.AppPref;
import com.scripto.verification.Utility.DetectBlur;
import com.scripto.verification.Utility.ExceptionHandler;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PreviewActivity extends AppCompatActivity {

    Activity mActivity;
    Context mContext;
    AppPref pref;
    boolean allPermit = false, isCaptured = false;
    String path = "";
    String detectedResult;
    int width = 1024, height = 0, cWidth = 0, cHeight = 0;
    double aspect = 0.0, scaleFactor = 0.0;

    ConstraintLayout layoutPermission;
    ImageView imagePreview;
    TextView textPermission, textResult;
    LinearLayout buttonRetry, buttonSave;

    File f;
    Bitmap rawImage, previewImage;
    Matrix mat = new Matrix();
    Mat sourceMatImage;
    DetectBlur detectBlur;

    ActivityResultLauncher<Intent> previewActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            resultActivity -> {
                int resultCode = resultActivity.getResultCode();
                Intent data = resultActivity.getData();

                if(data != null){
                    if(resultCode == RESULT_CANCELED){
                        Intent intent = new Intent();
                        setResult(RESULT_CANCELED, intent);
                        finish();

                    } else if(resultCode == RESULT_OK) {
                        layoutPermission.setVisibility(View.GONE);

                        path = Uri.parse(data.getStringExtra("uri")).getPath();

                        try {
                            f = new File(path);
                            ExifInterface exif = new ExifInterface(f.getPath());
                            int orientation = exif.getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL);

                            int angle = 0;

                            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                                angle = 90;
                            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                                angle = 180;
                            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                                angle = 270;
                            }

                            mat.postRotate(angle);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 1;

                            rawImage = BitmapFactory.decodeStream(Files.newInputStream(f.toPath()), null, options);
                            previewImage = Bitmap.createBitmap(rawImage, 0, 0, rawImage.getWidth(), rawImage.getHeight(), mat, true);
                            imagePreview.setImageBitmap(previewImage);

                            detectBlur();

                            if(rawImage.getWidth() < 1024){
                                height = rawImage.getHeight();
                                width = rawImage.getWidth();
                            } else {
                                height = rawImage.getHeight() * width / rawImage.getWidth();
                            }

                        } catch (IOException | OutOfMemoryError e) {
                            e.printStackTrace();
                        }

                        buttonSave.setOnClickListener(v -> selectResolution());

                        buttonRetry.setOnClickListener(v -> {
                            isCaptured = false;
                            checkPermissions();
                        });
                    }
                }
            }
    );

    private final BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                new Handler().postDelayed(() -> sourceMatImage = new Mat(), 1000);
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        checkPermissions();
    }

    private void checkPermissions() {
        List<String> permission = new ArrayList<>();
        permission.add(Manifest.permission.CAMERA);
        if(android.os.Build.VERSION.SDK_INT < 29){
            permission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        Dexter.withContext(mContext)
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

    private void callCaptureActivity() {
        if(!isCaptured) {
            previewActivityResultLauncher.launch(
                    new Intent(mContext, CaptureActivity.class)
                            .putExtra("aspect", aspect)
            );

            isCaptured = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mOpenCVCallBack);
        } else {
            mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getString(R.string.log_path), this, 0));

        setContentView(R.layout.activity_preview);

        System.loadLibrary("opencv_java4");

        Objects.requireNonNull(getSupportActionBar()).hide();

        mContext = this;
        mActivity = this;
        pref = new AppPref(mContext);

        aspect = getIntent().getDoubleExtra("aspect", aspect);

        layoutPermission = findViewById(R.id.layoutPermission);
        textPermission = findViewById(R.id.textPermission);

        imagePreview = findViewById(R.id.imagePreview);
        textResult = findViewById(R.id.textResult);
        buttonRetry = findViewById(R.id.buttonRetry);
        buttonSave = findViewById(R.id.buttonSave);

        textResult.setMovementMethod(new ScrollingMovementMethod());

    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        setResult(RESULT_CANCELED, data);
        super.onBackPressed();
    }

    private void selectResolution() {

        String[] list_scale = getResources().getStringArray(R.array.list_scale);

        Dialog dialogResolution = new Dialog(mContext);
        dialogResolution.setContentView(R.layout.dialog_resolution);
        dialogResolution.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogResolution.setCanceledOnTouchOutside(false);
        dialogResolution.show();

        TextView textOrgSize = dialogResolution.findViewById(R.id.textOrgSize);
        TextView textOutSize = dialogResolution.findViewById(R.id.textOutSize);
        Spinner spinnerScale = dialogResolution.findViewById(R.id.spinnerScale);
        EditText editCustomHeight = dialogResolution.findViewById(R.id.editCustomHeight);
        EditText editCustomWidth = dialogResolution.findViewById(R.id.editCustomWidth);
        LinearLayout buttonSave = dialogResolution.findViewById(R.id.buttonSave);

        ArrayAdapter<CharSequence> scaleAdapter = ArrayAdapter.createFromResource(mContext, R.array.list_scale, android.R.layout.simple_list_item_1);
        scaleAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        spinnerScale.setAdapter(scaleAdapter);

        String orgSize = height + getString(R.string.text_x) + width;
        textOrgSize.setText(orgSize);

        spinnerScale.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(position > 0){
                    ((TextView)parent.getChildAt(0)).setTypeface(null, Typeface.BOLD);
                    scaleFactor = Double.parseDouble(list_scale[position].replace('x', ' '));
                    cHeight = (int) (height * scaleFactor);
                    cWidth = (int) (width * scaleFactor);
                } else {
                    ((TextView)parent.getChildAt(0)).setTypeface(null, Typeface.NORMAL);
                    scaleFactor = 0.0;
                    cHeight = cWidth = 0;
                }

                String outSize = cHeight + getString(R.string.text_x) + cWidth;
                textOutSize.setText(outSize);
                editCustomHeight.setText("");
                editCustomWidth.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        buttonSave.setOnClickListener(v -> {
            int cHeightTemp = 0, cWidthTemp = 0;

            try{
                cHeightTemp = Integer.parseInt(editCustomHeight.getText().toString());
                cWidthTemp = Integer.parseInt(editCustomWidth.getText().toString());
            } catch (Exception e){
                e.printStackTrace();
            }

            if(cHeightTemp > height || cWidthTemp > width){
                Toast.makeText(mContext, getString(R.string.msg_warning_dimension), Toast.LENGTH_SHORT).show();

            } else if (cHeightTemp > 0 || cWidthTemp > 0 || scaleFactor != 0){
                cHeight = cHeightTemp;
                cWidth = cWidthTemp;

                try {
                    dialogResolution.dismiss();
                    saveImageToExternal();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(mContext, getString(R.string.msg_select_size), Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void detectBlur() {
        if(sourceMatImage == null)
            sourceMatImage = new Mat();

        detectBlur = new DetectBlur(this, previewImage, sourceMatImage);

        double score = detectBlur.getSharpnessScoreFromOpenCV();
        int percent = detectBlur.showScoreFromOpenCV(score);

        String detectedBlur = "Image Sharpness: " + score + "/"
                + DetectBlur.BLUR_THRESHOLD + "\n\nProcess Result: ";

        if(percent == 0){
            detectedBlur += getString(R.string.text_image_ok);
        } else{
            detectedBlur += String.format(Locale.getDefault(), getString(R.string.text_image_blur), percent);
        }

        String resultData = textResult.getText().toString();

        if(resultData.length() > 0 && !resultData.toLowerCase().contains("blur")){
            detectedResult += detectedBlur;
        } else
            detectedResult = detectedBlur;

        textResult.setText(detectedResult);
        textResult.scrollBy(0, 0);

        pref.putResponse(getString(R.string.doc_sharpness), String.valueOf(score));
        pref.putResponse(getString(R.string.doc_blur), String.valueOf(percent));
    }

    public void saveImageToExternal() throws IOException {
        Bitmap rawImage = null;
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Saved_Images");
        path.mkdir();

        if(scaleFactor != 0){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) (1/scaleFactor);

            Bitmap initialImage = BitmapFactory.decodeStream(Files.newInputStream(f.toPath()),null, options);
            rawImage = Bitmap.createBitmap(initialImage, 0, 0, initialImage.getWidth(), initialImage.getHeight(), mat, true);
            rawImage = Bitmap.createScaledBitmap(rawImage, (int) (width * scaleFactor), (int) (height * scaleFactor), true);

        } else if(cHeight != 0){
            double ratio = (height * 1.0)/cHeight;
            int cWidthImage = (int) (width / ratio);
            int cHeightImage = height / ((int) ratio + 1);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) ratio + 1;

            Bitmap initialImage = BitmapFactory.decodeStream(Files.newInputStream(f.toPath()),null, options);

            rawImage = Bitmap.createBitmap(cWidth, cHeight, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(rawImage);
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.LIGHTEN);

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);

            int leftSpace = (cWidth - cWidthImage) / 2;
            int topSpace = (cHeight - cHeightImage) / 2;

            canvas.drawBitmap(initialImage, leftSpace + 10, topSpace, paint);
        }

        String timeStamp = new SimpleDateFormat(getString(R.string.text_time_format), Locale.getDefault()).format(new Date());

        File imageFile = new File(path, "IMG_" + timeStamp + ".png");
        FileOutputStream out = new FileOutputStream(imageFile);

        File reportFile = getFile();

        PrintStream ps = null;

        try {
            reportFile.createNewFile();
            try {
                ps = new PrintStream(reportFile);
            } catch (FileNotFoundException e) {
                showError(mContext, Log.getStackTraceString(e));
                e.printStackTrace();
            }
        } catch (IOException e) {
            showError(mContext, Log.getStackTraceString(e));
            e.printStackTrace();
        }

        try{
            if (rawImage != null) {
                rawImage.compress(Bitmap.CompressFormat.WEBP, 90, out);
            }
            out.flush();
            out.close();

            MediaScannerConnection
                    .scanFile(mContext,
                            new String[] { imageFile.getAbsolutePath() },
                            null,
                            (path1, uri) -> {
                                Intent result = new Intent();
                                result.setData(Uri.parse(path1));
                                setResult(RESULT_OK, result);
                                finish();
                            }
                    );

        } catch(Exception e) {
            if(ps != null) {
                e.printStackTrace(ps);
            } else {
                showError(mContext, Log.getStackTraceString(e));
            }
            throw new IOException();
        }

        if(ps != null)
            ps.close();
    }
}