package com.example.floatingiconoverlay;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusText = findViewById(R.id.tvStatus);
        Button btnEnable = findViewById(R.id.btnEnable);
        Button btnDisable = findViewById(R.id.btnDisable);

        updateStatus(statusText);

        btnEnable.setOnClickListener(v -> {
            if (canDrawOverlays()) {
                startOverlayService();
                updateStatus(statusText);
                Toast.makeText(this, "Floating icon started!", Toast.LENGTH_SHORT).show();
            } else {
                requestOverlayPermission();
            }
        });

        btnDisable.setOnClickListener(v -> {
            stopOverlayService();
            updateStatus(statusText);
            Toast.makeText(this, "Floating icon stopped.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView statusText = findViewById(R.id.tvStatus);
        updateStatus(statusText);
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
        );
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
        Toast.makeText(this, "Please grant 'Display over other apps' permission", Toast.LENGTH_LONG).show();
    }

    private void startOverlayService() {
        Intent intent = new Intent(this, FloatingOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopOverlayService() {
        Intent intent = new Intent(this, FloatingOverlayService.class);
        stopService(intent);
    }

    private void updateStatus(TextView statusText) {
        if (!canDrawOverlays()) {
            statusText.setText("⚠️ Permission required: 'Display over other apps'");
        } else if (FloatingOverlayService.isRunning) {
            statusText.setText("✅ Floating icon is ACTIVE");
        } else {
            statusText.setText("⭕ Floating icon is INACTIVE");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST && canDrawOverlays()) {
            startOverlayService();
        }
    }
}
