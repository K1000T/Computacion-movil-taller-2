// MainActivity
package com.example.taller_2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {
    ImageButton camera, map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        camera = findViewById(R.id.ibtnCamera);
        map = findViewById(R.id.ibtnMap);

        // Set onClickListener for the camera button
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start CameraActivity when camera button is clicked
                startActivity(new Intent(v.getContext(), CameraActivity.class));
            }
        });

        // Set onClickListener for the map button
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start MapActivity when map button is clicked
                startActivity(new Intent(v.getContext(), MapActivity.class));
            }
        });

    }
}
