package com.scripto.verifyyouruser;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.scripto.verifyyouruser.R;

import java.util.Objects;

public class ShowResults extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_results);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Extraction Result");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView textUID = findViewById(R.id.textUID);
        textUID.setText("User ID: " + getIntent().getStringExtra("UID"));

        TextView textResults = findViewById(R.id.textResults);
        textResults.setText(getIntent().getStringExtra("result"));

    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }
}