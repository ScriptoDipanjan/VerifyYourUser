package com.scripto.verification.Utility;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;

public class DetectBlur {

    Context mContext;
    Bitmap mBitmap;
    Mat sourceMatImage;

    public static final int BLUR_THRESHOLD = 1000;

    public DetectBlur(Context mContext, Bitmap mBitmap, @NonNull Mat sourceMatImage) {
        this.mContext = mContext;
        this.mBitmap = mBitmap;
        this.sourceMatImage = sourceMatImage;
    }


    public Double getSharpnessScoreFromOpenCV() {
        Mat destination = new Mat();
        Mat matGray = new Mat();
        Utils.bitmapToMat(resizeBitmap(mBitmap), sourceMatImage);
        Imgproc.cvtColor(sourceMatImage, matGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(matGray, destination, 3);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(destination, median, std);
        return Double.valueOf(new DecimalFormat("0.00").format(Math.pow(std.get(0, 0)[0], 2.0)));
    }


    public int showScoreFromOpenCV(Double score) {
        int percent = 0;

        if(score < BLUR_THRESHOLD){
            percent = (int) (((BLUR_THRESHOLD - score)/ BLUR_THRESHOLD) * 100);
        }

        return percent;
    }

    private Bitmap resizeBitmap(Bitmap image)  {
        int width = image.getWidth();
        int height = image.getHeight();

        if(width > height) {
            float ratio = (width / (float) 500);
            width = 500;
            height = (int) (height / ratio);
        } else if (height > width){
            float ratio = (height / (float) 500);
            height = 500;
            width = (int) (width / ratio);
        } else {
            width = 500;
            height = 500;
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}
