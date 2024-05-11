// CameraActivity
package com.example.taller_2;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;

public class CameraActivity extends AppCompatActivity {
    // Constants for camera permissions
    public static final int CAMERA_PERMISSION_ID = 6;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final String CAMERA_PERMISSION_NAME = Manifest.permission.CAMERA;

    // Views
    ImageView image;
    Button btnCamera, btnGallery;

    // Activity result launcher for gallery content
    ActivityResultLauncher<String> getGalleryContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            try {
                // Display the selected image from the gallery
                image.setImageBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(result)));
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Initialize views
        image = findViewById(R.id.img);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Button click listeners
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open gallery
                startGallery();
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open camera
                startCamera(v);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Handle permission request result
        if (requestCode == CAMERA_PERMISSION_ID) {
            afterPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Handle result of camera activity
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Display captured image
            image.setImageBitmap((Bitmap) data.getExtras().get("data"));
        }
    }

    // Method to request permission
    private void requestPermission(Activity context, String permission, String justification, int code) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                // Show rationale if needed
                Toast.makeText(context, justification, Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(context, new String[]{permission}, code);
        }
    }

    // Method called after permission is granted
    public void afterPermission() {
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION_NAME) == PackageManager.PERMISSION_GRANTED) {
            // Start camera activity
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE);
        }
    }

    // Method to start gallery
    public void startGallery() {
        // Launch gallery activity
        getGalleryContent.launch("image/*");
    }

    // Method to start camera
    public void startCamera(View v) {
        // Request camera permission
        requestPermission(this, CAMERA_PERMISSION_NAME, "Can we access your camera?", CAMERA_PERMISSION_ID);
        // After permission granted, start camera
        afterPermission();
    }
}
