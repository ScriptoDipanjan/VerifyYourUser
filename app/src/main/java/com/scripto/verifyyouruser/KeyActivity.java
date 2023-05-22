package com.scripto.verifyyouruser;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.scripto.verifyyouruser.R;

import java.util.Objects;

public class KeyActivity extends AppCompatActivity {
    String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key);

        Objects.requireNonNull(getSupportActionBar()).hide();

        SharedPreferences pref = getSharedPreferences("Key", Context.MODE_PRIVATE);
        key = pref.getString("key", null);

        TextView textKey = findViewById(R.id.textKey);
        textKey.setText("Current X-API Key: " + key);

        Button buttonProceed = findViewById(R.id.buttonProceed);
        buttonProceed.setOnClickListener(v -> {
            if(key != null){
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Add an X-API Key to proceed", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonAddKey = findViewById(R.id.buttonAddKey);
        buttonAddKey.setOnClickListener(v -> {
            EditText editTextKey = findViewById(R.id.editTextKey);
            String keyData = editTextKey.getText().toString();
            if(!keyData.isEmpty()){
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("key", keyData);
                editor.apply();
                Toast.makeText(this, "X-API Key added successfully!", Toast.LENGTH_SHORT).show();
                key = keyData;
                textKey.setText("Current X-API Key: " + key);
            } else {
                Toast.makeText(this, "Enter an X-API Key to proceed", Toast.LENGTH_SHORT).show();
            }
        });

    }
}