package com.example.floatingiconoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

public class FloatingOverlayService extends Service {

    public static boolean isRunning = false;

    private static final String CHANNEL_ID = "FloatingOverlayChannel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private View floatingView;
    private View expandedMenuView;
    private WindowManager.LayoutParams floatParams;

    // For drag tracking
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private long touchStartTime;
    private boolean isDragging = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        addFloatingView();
    }

    private void addFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_icon, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        floatParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        floatParams.gravity = Gravity.TOP | Gravity.START;
        floatParams.x = 100;
        floatParams.y = 300;

        windowManager.addView(floatingView, floatParams);

        // Setup drag + click on the icon
        ImageView icon = floatingView.findViewById(R.id.ivFloatingIcon);
        icon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = floatParams.x;
                        initialY = floatParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true;
                        }
                        if (isDragging) {
                            floatParams.x = initialX + (int) dx;
                            floatParams.y = initialY + (int) dy;
                            windowManager.updateViewLayout(floatingView, floatParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        long touchDuration = System.currentTimeMillis() - touchStartTime;
                        if (!isDragging && touchDuration < 300) {
                            // Tap — toggle expanded menu
                            toggleExpandedMenu();
                        }
                        return true;
                }
                return false;
            }
        });

        // Close button
        View closeBtn = floatingView.findViewById(R.id.btnClose);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> stopSelf());
        }
    }

    private void toggleExpandedMenu() {
        View menuPanel = floatingView.findViewById(R.id.menuPanel);
        if (menuPanel != null) {
            if (menuPanel.getVisibility() == View.VISIBLE) {
                menuPanel.setVisibility(View.GONE);
            } else {
                menuPanel.setVisibility(View.VISIBLE);
            }
        }

        // Menu item actions
        View btnHome = floatingView.findViewById(R.id.btnHome);
        View btnInfo = floatingView.findViewById(R.id.btnInfo);
        View btnSettings = floatingView.findViewById(R.id.btnSettings);

        if (btnHome != null) btnHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
        });

        if (btnInfo != null) btnInfo.setOnClickListener(v ->
            Toast.makeText(this, "FloatingIconOverlay v1.0\nDrag me anywhere!", Toast.LENGTH_SHORT).show()
        );

        if (btnSettings != null) btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private Notification buildNotification() {
        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Icon Active")
            .setContentText("Tap to open settings")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Floating Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the floating icon running");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
