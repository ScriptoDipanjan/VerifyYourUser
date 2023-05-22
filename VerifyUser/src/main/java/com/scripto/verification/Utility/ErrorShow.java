package com.scripto.verification.Utility;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.scripto.verification.R;

import java.util.Objects;

public class ErrorShow extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_show);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Error Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String error = getIntent().getStringExtra("Error");

        TextView textError = findViewById(R.id.textError);
        Button buttonCopy = findViewById(R.id.buttonCopy);

        textError.setText(error);
        textError.setMovementMethod(new ScrollingMovementMethod());

        buttonCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error", error);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(ErrorShow.this, getString(R.string.msg_copy_success), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public boolean onNavigateUp() {
        return super.onNavigateUp();
    }
}