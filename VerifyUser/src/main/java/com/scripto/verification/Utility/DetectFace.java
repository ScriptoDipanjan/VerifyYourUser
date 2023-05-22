package com.scripto.verification.Utility;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.scripto.verification.R;

import java.util.List;
import java.util.Locale;

public class DetectFace {

    Context mContext;
    Bitmap mBitmap;
    ImageView imagePreview;
    TextView textResult;
    AppPref pref;
    String faceDetection = "", warnings = "";

    public DetectFace(Context mContext, Bitmap mBitmap, ImageView imagePreview, TextView textResult, AppPref pref) {
        this.mContext = mContext;
        this.mBitmap = mBitmap;
        this.imagePreview = imagePreview;
        this.textResult = textResult;
        this.pref = pref;
        pref.clearSelfieData(mContext);
    }

    public void detectFaces() {
        ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage(mContext.getString(R.string.msg_wait));
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        InputImage image = InputImage.fromBitmap(mBitmap, 0);

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        //.setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        Bitmap tempBitmap = Bitmap.createBitmap(mBitmap.getWidth(),mBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(tempBitmap);

        double coeff = Math.sqrt(canvas.getWidth() * canvas.getHeight()) / 250;

        Paint paintRect = new Paint();
        paintRect.setStrokeWidth((float) (1 * coeff));
        paintRect.setColor(Color.RED);
        paintRect.setStyle(Paint.Style.STROKE);

        Paint paintText = new Paint();

        paintText.setTextSize((float) (10 * coeff));
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(Color.RED);

        canvas.drawBitmap(mBitmap, 0, 0, paintRect);

        detector.process(image)
                .addOnSuccessListener(
                        faces -> {
                            Toast.makeText(mContext, mContext.getString(R.string.msg_success), Toast.LENGTH_LONG).show();

                            if(faces.size() > 1){
                                initWarning();
                                warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_multi_face), faces.size());
                            } else if(faces.size() == 0){
                                initWarning();
                                warnings += mContext.getString(R.string.msg_no_face);
                            }

                            pref.putResponse(mContext.getString(R.string.face_count), String.valueOf(faces.size()));

                            for (Face face : faces) {
                                String result = "";
                                int faceID = -1;
                                float openProbRightEye;
                                float openProbLeftEye;

                                Rect bounds = face.getBoundingBox();
                                canvas.drawRoundRect(new RectF(bounds.left, bounds.top, bounds.right, bounds.bottom), 2, 2, paintRect);

                                float rotX = face.getHeadEulerAngleX();  // Head is rotated to the right rotY degrees
                                float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                pref.putResponse(mContext.getString(R.string.face_angle_up_down), pref.getResponse(mContext.getString(R.string.face_angle_up_down)) != null ? pref.getResponse(mContext.getString(R.string.face_angle_up_down)) + ", " + rotX : String.valueOf(rotX));
                                pref.putResponse(mContext.getString(R.string.face_angle_left_right), pref.getResponse(mContext.getString(R.string.face_angle_left_right)) != null ? pref.getResponse(mContext.getString(R.string.face_angle_left_right)) + ", " + rotY : String.valueOf(rotY));
                                pref.putResponse(mContext.getString(R.string.face_angle_tilted), pref.getResponse(mContext.getString(R.string.face_angle_tilted)) != null ? pref.getResponse(mContext.getString(R.string.face_angle_tilted)) + ", " + rotZ : String.valueOf(rotZ));

                                if (face.getTrackingId() != null) {
                                    faceID = face.getTrackingId();

                                    int xPos = (bounds.centerX());
                                    int yPos = (int) (bounds.centerY() - ((paintText.descent() + paintText.ascent()) / 2)) ;

                                    canvas.drawText("ID: " + faceID, xPos, yPos, paintText);

                                    result += mContext.getString(R.string.text_face_id) + faceID;
                                    pref.putResponse(mContext.getString(R.string.face_id), pref.getResponse(mContext.getString(R.string.face_id)) != null ? pref.getResponse(mContext.getString(R.string.face_id)) + ", " + faceID : String.valueOf(faceID));
                                }


                                result += String.format(Locale.getDefault(), mContext.getString(R.string.msg_angle_up_down), rotX);
                                result += String.format(Locale.getDefault(), mContext.getString(R.string.msg_angle_left_right), rotY);
                                result += String.format(Locale.getDefault(), mContext.getString(R.string.msg_angle_clock_anti), rotZ);

                                if(rotX < -12.99){
                                    initWarning();
                                    warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_face_down), faceID);
                                } else if(rotX > 12.99){
                                    initWarning();
                                    warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_face_up), faceID);
                                } else if(rotY < -12.99){
                                    initWarning();
                                    warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_face_left), faceID);
                                } else if(rotY > 12.99){
                                    initWarning();
                                    warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_face_right), faceID);
                                } else if(rotZ < -12.99){
                                    initWarning();
                                    warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_face_clock), faceID);
                                } else if(rotZ > 12.99){
                                    initWarning();
                                    warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_face_anti), faceID);
                                }

                                // If landmark detection was enabled (mouth, ears, eyes, cheeks, and nose available):
                                FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                if (leftEar != null) {
                                    PointF leftEarPos = leftEar.getPosition();
                                }
                                FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
                                if (rightEar != null) {
                                    PointF rightEarPos = rightEar.getPosition();
                                }

                                // If classification was enabled:
                                if (face.getSmilingProbability() != null) {
                                    float smileProb = face.getSmilingProbability();
                                }

                                result += mContext.getString(R.string.msg_right_eye_prob);
                                if (face.getRightEyeOpenProbability() != null) {
                                    openProbRightEye = face.getRightEyeOpenProbability();

                                    if(openProbRightEye < 0.8) {
                                        initWarning();
                                        warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_right_eye_close), faceID); //for non-flipped image
                                    }
                                    result += openProbRightEye;
                                    pref.putResponse(mContext.getString(R.string.face_open_prob_right), pref.getResponse(mContext.getString(R.string.face_open_prob_right)) != null ? pref.getResponse(mContext.getString(R.string.face_open_prob_right)) + ", " + openProbRightEye : String.valueOf(openProbRightEye));
                                } else {
                                    result += mContext.getString(R.string.msg_n_a);
                                    pref.putResponse(mContext.getString(R.string.face_open_prob_right), pref.getResponse(mContext.getString(R.string.face_open_prob_right)) != null ? pref.getResponse(mContext.getString(R.string.face_open_prob_right)) + ", " + 0 : "0");
                                }

                                result += mContext.getString(R.string.msg_left_eye_prob);
                                if (face.getLeftEyeOpenProbability() != null) {
                                    openProbLeftEye = face.getLeftEyeOpenProbability();

                                    if(openProbLeftEye < 0.8) {
                                        initWarning();
                                        warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_left_eye_close), faceID); //for non-flipped image
                                    }

                                    result += openProbLeftEye;
                                    pref.putResponse(mContext.getString(R.string.face_open_prob_left), pref.getResponse(mContext.getString(R.string.face_open_prob_left)) != null ? pref.getResponse(mContext.getString(R.string.face_open_prob_left)) + ", " + openProbLeftEye : String.valueOf(openProbLeftEye));
                                } else {
                                    result += mContext.getString(R.string.msg_n_a);
                                    pref.putResponse(mContext.getString(R.string.face_open_prob_left), pref.getResponse(mContext.getString(R.string.face_open_prob_left)) != null ? pref.getResponse(mContext.getString(R.string.face_open_prob_left)) + ", " + 0 : "0");
                                }

                                faceDetection += result + "\n\n" + mContext.getString(R.string.msg_divider);

                                //Log.e("Face", String.valueOf(faceID));

                                float LeftX, LeftY, faceLeftStartX, faceLeftStartY, faceRightStartX,
                                        faceRightStartY, faceLeftEndX, faceRightEndX, LeftEBStartY, rightEBStartY;

                                float rightX, rightY, noseTopY, noseBottomY, LeftEBY, rightEBY;

                                float leftEyeStartX, leftEyeStartY, leftEyeEndX, leftEyeEndY;

                                float rightEyeStartX, rightEyeStartY, rightEyeEndX, rightEyeEndY;

                                if(face.getContour(FaceContour.LEFT_EYE) != null
                                        && face.getContour(FaceContour.RIGHT_EYE) != null
                                        && face.getContour(FaceContour.NOSE_BRIDGE) != null
                                        && face.getContour(FaceContour.FACE) != null ){
                                    initWarning();

                                    List<PointF> faceContour = face.getContour(FaceContour.FACE).getPoints();
                                    List<PointF> leftEyeContour = face.getContour(FaceContour.LEFT_EYE).getPoints();
                                    List<PointF> rightEyeContour = face.getContour(FaceContour.RIGHT_EYE).getPoints();
                                    List<PointF> noseBridgeContour = face.getContour(FaceContour.NOSE_BRIDGE).getPoints();
                                    List<PointF> noseBottomContour = face.getContour(FaceContour.NOSE_BOTTOM).getPoints();
                                    List<PointF> leftEyeBrowBottomContour = face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM).getPoints();
                                    List<PointF> rightEyeBrowBottomContour = face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM).getPoints();

                                    LeftX = leftEyeContour.get(8).x;
                                    LeftY = leftEyeContour.get(8).y;

                                    rightX = rightEyeContour.get(0).x;
                                    rightY = rightEyeContour.get(0).y;

                                    noseTopY = noseBridgeContour.get(0).y;
                                    noseBottomY = noseBridgeContour.get(1).y;

                                    LeftEBY = leftEyeBrowBottomContour.get(4).y;
                                    rightEBY = rightEyeBrowBottomContour.get(4).y;

                                    LeftEBStartY = leftEyeBrowBottomContour.get(4).y;
                                    rightEBStartY = rightEyeBrowBottomContour.get(3).y;

                                    faceLeftStartX = faceContour.get(35).x;
                                    faceLeftEndX = faceContour.get(34).x;
                                    faceRightStartX = faceContour.get(1).x;
                                    faceRightEndX = faceContour.get(0).x;

                                    faceLeftStartY = faceContour.get(35).y;
                                    faceRightStartY = faceContour.get(1).y;

                                    leftEyeStartX = leftEyeContour.get(3).x;
                                    leftEyeStartY = leftEyeContour.get(3).y;
                                    leftEyeEndX = leftEyeContour.get(11).x;
                                    leftEyeEndY = leftEyeContour.get(11).y;

                                    rightEyeStartX = rightEyeContour.get(3).x;
                                    rightEyeStartY = rightEyeContour.get(3).y;
                                    rightEyeEndX = rightEyeContour.get(11).x;
                                    rightEyeEndY = rightEyeContour.get(11).y;

                                    float topY = Math.max(LeftEBY + (LeftY - LeftEBY)/2, rightEBY + (rightY - rightEBY)/2);
                                    float bottomY = noseTopY + (noseBottomY - noseTopY)/2;
                                    int width = (int) (rightX - LeftX);
                                    int height = (int) (bottomY - topY);
                                    //canvas.drawRoundRect(new RectF(LeftX, topY, rightX, bottomY), 1, 1, paintRect);
                                    Bitmap noseBitmap = Bitmap.createBitmap(mBitmap, (int) (LeftX + (width * 0.3)), (int) topY, (int) (width * 0.4), height, null, false);
                                    Bitmap leftPupilBitmap = Bitmap.createBitmap(mBitmap, (int) leftEyeStartX, (int) leftEyeStartY, (int) (leftEyeEndX - leftEyeStartX), (int) (leftEyeEndY - leftEyeStartY), null, false);
                                    Bitmap rightPupilBitmap = Bitmap.createBitmap(mBitmap, (int) rightEyeStartX, (int) rightEyeStartY, (int) (rightEyeEndX - rightEyeStartX), (int) (rightEyeEndY - rightEyeStartY), null, false);

                                    /*noseBitmap = toGrayscale(noseBitmap);*/
                                    //imagePreview.setImageDrawable(new BitmapDrawable(mContext.getResources(), noseBitmap));


                                    if(getEmptyPlaceIndex(noseBitmap) || toGrayscale(leftPupilBitmap) || toGrayscale(rightPupilBitmap)){
                                        warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_glass_wearing), faceID);
                                    }

                                    height = (int) ((LeftEBStartY - faceLeftStartY) / 2);
                                    Bitmap leftBitmap = Bitmap.createBitmap(mBitmap, (int) (faceLeftStartX), (int) faceLeftStartY, (int) (faceLeftStartX - faceLeftEndX), height, null, false);

                                    height = (int) ((rightEBStartY - faceRightStartY) / 2);
                                    Bitmap rightBitmap = Bitmap.createBitmap(mBitmap, (int) (faceRightStartX), (int) faceRightStartY, (int) (faceRightStartX - faceRightEndX), height, null, false);

                                    if(getEmptyPlaceIndex(leftBitmap) || getEmptyPlaceIndex(rightBitmap)){
                                        warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_forehead_obstruction), faceID);
                                    }

                                    float face29x = faceContour.get(29).x;
                                    float widthX = leftEyeBrowBottomContour.get(0).x - faceContour.get(29).x;
                                    width = (int) (face29x - (widthX/2));
                                    height = (int) (faceContour.get(26).y - faceContour.get(29).y);

                                    if(width > 0 && widthX > 0 && height > 0){
                                        Bitmap earLeftBitmap = Bitmap.createBitmap(mBitmap, width, (int) faceContour.get(29).y, (int) (widthX/2), height, null, false);
                                        if((getEmptyPlaceIndex(earLeftBitmap) || getEmptyPlaceIndexInverse(earLeftBitmap))){
                                            warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_ear_obstruction_left), faceID);
                                        }
                                    } else {
                                        warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_ear_obstruction_left), faceID);
                                    }

                                    float face7x = faceContour.get(7).x;
                                    widthX = faceContour.get(7).x - rightEyeBrowBottomContour.get(0).x;
                                    width = (int) (0 + (widthX/2));
                                    height = (int) (faceContour.get(10).y - faceContour.get(7).y);

                                    if(width > 0 && height > 0){
                                        Bitmap earRightBitmap = Bitmap.createBitmap(mBitmap, (int) face7x, (int) faceContour.get(7).y, width, height, null, false);
                                        if(getEmptyPlaceIndex(earRightBitmap) || getEmptyPlaceIndexInverse(earRightBitmap)){
                                            warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_ear_obstruction_right), faceID);
                                        }
                                    } else {
                                        warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_ear_obstruction_right), faceID);
                                    }

                                    height = (int) (noseBottomContour.get(1).y - noseBridgeContour.get(1).y);
                                    width = (int) (noseBridgeContour.get(1).x - (height/2));

                                    if(width > 0 && height > 0){
                                        Bitmap noseBottomBitmap = Bitmap.createBitmap(mBitmap, width, (int) noseBridgeContour.get(1).y, height, height, null, false);
                                        if(getEmptyPlaceIndex(noseBottomBitmap) || getEmptyPlaceIndexInverse(noseBottomBitmap)){
                                            warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_nose_obstruction), faceID);
                                        }
                                    } else {
                                        warnings += String.format(Locale.getDefault(), mContext.getString(R.string.msg_nose_obstruction), faceID);
                                    }
                                }
                            }

                            imagePreview.setImageDrawable(new BitmapDrawable(mContext.getResources(), tempBitmap));

                            if(warnings.length() > 0)
                                faceDetection = warnings + mContext.getString(R.string.msg_divider) + faceDetection;

                            String resultData = textResult.getText().toString();

                            if(resultData.length() > 0 && !resultData.toLowerCase().contains("face")){
                                faceDetection += resultData;
                            }

                            textResult.setText(faceDetection);

                            progressDialog.dismiss();
                            //textResult.setVisibility(View.VISIBLE);

                        })
                .addOnFailureListener(
                        e -> {
                            e.printStackTrace();
                            faceDetection = mContext.getString(R.string.msg_failed) + e;

                            Toast.makeText(mContext, faceDetection, Toast.LENGTH_LONG).show();

                            String resultData = textResult.getText().toString();

                            if(resultData.length() > 0 && !resultData.toLowerCase().contains("face")){
                                faceDetection += resultData;
                            }

                            textResult.setText(faceDetection);

                            progressDialog.dismiss();
                        });
    }

    private void initWarning() {
        if(warnings.length() == 0) {
            warnings = mContext.getString(R.string.msg_warning);
        }
    }

    boolean getEmptyPlaceIndex(Bitmap bmpOriginal) {
        boolean result = false;
        bmpOriginal = Bitmap.createScaledBitmap(bmpOriginal, 16 , 16, false);

        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);

        int[] pixels = new int[width * height];
        int index = 0, compare = 0;
        for(int h=0; h<height; h++){
            for(int w=0; w<width; w++){
                pixels[index] = bmpGrayscale.getPixel(w, h);
                int comparisonResult = String.format("%6X", (0xFFFFFF & pixels[index])).compareTo("505050");
                if(comparisonResult <= -1)
                    compare++;
                index++;
            }
        }

        if(compare > 64)
            result = true;

        return result;
    }

    boolean getEmptyPlaceIndexInverse(Bitmap bmpOriginal) {
        boolean result = false;
        bmpOriginal = Bitmap.createScaledBitmap(bmpOriginal, 16 , 16, false);

        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);

        int[] pixels = new int[width * height];
        int index = 0, compare = 0;
        for(int h=0; h<height; h++){
            for(int w=0; w<width; w++){
                pixels[index] = bmpGrayscale.getPixel(w, h);
                int comparisonResult = String.format("%6X", (0xFFFFFF & pixels[index])).compareTo("505050");
                if(comparisonResult >= 16)
                    compare++;
                index++;
            }
        }

        if(compare > 64)
            result = true;

        return result;
    }

    boolean toGrayscale(Bitmap bmpOriginal) {
        boolean result = false;
        bmpOriginal = Bitmap.createScaledBitmap(bmpOriginal, 16 , 16, false);

        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);

        int[] pixels = new int[width * height];
        int index = 0, compare = 0;
        for(int h=0; h<height; h++){
            for(int w=0; w<width; w++){
                pixels[index] = bmpGrayscale.getPixel(w, h);
                int comparisonResult = String.format("%6X", (0xFFFFFF & pixels[index])).compareTo("000000");
                if(comparisonResult >= 16)
                    compare++;
                index++;
            }
        }

        if(compare > 128)
            result = true;

        //Log.e("comparisonResult gray", String.valueOf(compare));

        return result;
    }
}
