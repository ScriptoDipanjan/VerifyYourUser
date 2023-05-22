package com.scripto.verification.Utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExceptionHandler implements UncaughtExceptionHandler {

    private final UncaughtExceptionHandler defaultUEH;
    private final String localPath;
    int flag;
    Activity mActivity;
    static File dir = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS + "/CrashLogs");

    public ExceptionHandler(String localPath, Activity mActivity, int flag) {
        this.localPath = localPath;
        this.mActivity = mActivity;
        this.flag = flag;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

        deleteEmptyFiles();
    }

    private void deleteEmptyFiles() {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.length() == 0) {
                    if(file.delete()){
                        Log.e("File", "deleted");
                    }
                }
            }
        }
    }

    public void uncaughtException(@NonNull Thread t, Throwable e) {

        final Writer stringBuffSync = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringBuffSync);
        e.printStackTrace(printWriter);
        String stacktrace = stringBuffSync.toString();
        printWriter.close();

        if (localPath != null) {
            writeToFile(stacktrace);
        }

        defaultUEH.uncaughtException(t, e);
    }

    private void writeToFile(String currentStacktrace) {
        try {

            String filename = getFileName();

            File reportFile = new File(dir, filename);
            FileWriter fileWriter = new FileWriter(reportFile);
            fileWriter.append(currentStacktrace);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getFile(){

        String filename = getFileName();

        return new File(dir, filename);
    }

    private static String getFileName() {
        dir.mkdirs();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
        Date date = new Date();
        return "Error_Log_" + dateFormat.format(date) + ".txt";
    }

    public static void showError(Context context, String error){
        context.startActivity(new Intent(context, ErrorShow.class).putExtra("Error", error));
    }

}
