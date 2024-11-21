package com.example.bachefinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button btnIa = findViewById(R.id.btn_ia);
        btnIa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentIa = new Intent(Home.this, IA.class);
                startActivity(intentIa);
            }
        });

        Button btnGps = findViewById(R.id.btn_gps);
        btnGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentGps = new Intent(Home.this, GPS.class);
                startActivity(intentGps);
            }
        });
    }
}