package com.scripto.verification.Document;

import android.app.Activity;
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

public class DocumentSelectionActivity extends AppCompatActivity {
    String pathString;
    Activity mActivity;
    LinearLayout buttonPAN, buttonID, buttonPassport;

    ActivityResultLauncher<Intent> selectionActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            resultActivity -> {
                int resultCode = resultActivity.getResultCode();
                Intent data = resultActivity.getData();

                Intent result = new Intent();
                AppPref pref = new AppPref(mActivity);

                if (data != null) {
                    if (resultCode == RESULT_OK) {

                        pathString = data.getData().getPath();

                        result.putExtra("uri", pathString);
                        result.putExtra(getString(R.string.doc_sharpness), pref.getResponse(getString(R.string.doc_sharpness)));
                        result.putExtra(getString(R.string.doc_blur), pref.getResponse(getString(R.string.doc_blur)));
                        setResult(RESULT_OK, result);

                        Toast.makeText(mActivity, getString(R.string.msg_capture_saved) + pathString, Toast.LENGTH_LONG).show();

                    } else if (resultCode == RESULT_CANCELED) {

                        setResult(RESULT_CANCELED, result);
                        Toast.makeText(mActivity, getString(R.string.msg_error_capture_cancel), Toast.LENGTH_LONG).show();

                    } else {

                        setResult(RESULT_CANCELED, result);
                        Toast.makeText(mActivity, getString(R.string.msg_error_capture_failed), Toast.LENGTH_LONG).show();

                    }
                }

                pref.clearData();
                finish();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(getString(R.string.log_path), this, 0));

        setContentView(R.layout.activity_document_selection);

        Objects.requireNonNull(getSupportActionBar()).hide();

        mActivity = this;

        buttonPAN = findViewById(R.id.buttonPAN);
        buttonID = findViewById(R.id.buttonID);
        buttonPassport = findViewById(R.id.buttonPassport);

        buttonPAN.setOnClickListener(v -> proceedSelection(1.88/3));

        buttonID.setOnClickListener(v -> proceedSelection(4.8/3));

        buttonPassport.setOnClickListener(v -> proceedSelection(2.112/3));
    }

    private void proceedSelection(double aspect) {

        selectionActivityResultLauncher.launch(
                new Intent(mActivity, PreviewActivity.class)
                        .putExtra("aspect", aspect)
        );
    }
}