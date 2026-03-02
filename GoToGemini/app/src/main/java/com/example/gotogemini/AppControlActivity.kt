package com.example.gotogemini

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AppControlActivity : AppCompatActivity() {

    private var isFlashlightOn = false
    private var pendingBluetoothState = false

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_control)

        setupToggles()
        setupAppLaunchers()
        setupUtilities()

        findViewById<Button>(R.id.btnControlBack).setOnClickListener { finish() }
    }

    private fun setupToggles() {
        // Wi-Fi Toggle
        findViewById<Switch>(R.id.switchWifi).setOnCheckedChangeListener { _, isChecked ->
            try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = isChecked
                showToast("Wi-Fi ${if (isChecked) "ON" else "OFF"}")
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                showToast("Open Wi-Fi settings manually")
            }
        }

        // Bluetooth Toggle — with Android 12+ runtime permission handling
        findViewById<Switch>(R.id.switchBluetooth).setOnCheckedChangeListener { _, isChecked ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires BLUETOOTH_CONNECT runtime permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    pendingBluetoothState = isChecked
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        REQUEST_BLUETOOTH_CONNECT
                    )
                } else {
                    toggleBluetooth(isChecked)
                }
            } else {
                toggleBluetooth(isChecked)
            }
        }

        // Flashlight Toggle
        findViewById<Switch>(R.id.switchFlashlight).setOnCheckedChangeListener { _, isChecked ->
            toggleFlashlight(isChecked)
        }
    }

    @SuppressLint("MissingPermission")
    private fun toggleBluetooth(enable: Boolean) {
        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            if (btAdapter == null) {
                showToast("Bluetooth not available on this device")
                return
            }
            if (enable) {
                btAdapter.enable()
            } else {
                btAdapter.disable()
            }
            showToast("Bluetooth ${if (enable) "ON" else "OFF"}")
        } catch (e: SecurityException) {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            showToast("Open Bluetooth settings manually")
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            showToast("Bluetooth error: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted — proceed with the pending toggle
                toggleBluetooth(pendingBluetoothState)
            } else {
                showToast("Bluetooth permission denied. Opening settings.")
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                // Reset the switch
                findViewById<Switch>(R.id.switchBluetooth).isChecked = false
            }
        }
    }

    private fun setupAppLaunchers() {
        val appButtons = mapOf(
            R.id.btnWhatsApp to "com.whatsapp",
            R.id.btnChrome to "com.android.chrome",
            R.id.btnMaps to "com.google.android.apps.maps",
            R.id.btnGmail to "com.google.android.gm",
            R.id.btnCamera to "com.android.camera",
            R.id.btnCalendar to "com.google.android.calendar"
        )

        for ((btnId, packageName) in appButtons) {
            findViewById<Button>(btnId).setOnClickListener {
                launchApp(packageName)
            }
        }
    }

    private fun setupUtilities() {
        // Make Call
        findViewById<Button>(R.id.btnMakeCall).setOnClickListener {
            val number = findViewById<EditText>(R.id.phoneNumberInput).text.toString().trim()
            if (number.isNotEmpty()) {
                makeCall(number)
            } else {
                showToast("Enter a phone number")
            }
        }

        // Open Settings
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        // Open URL
        findViewById<Button>(R.id.btnOpenUrl).setOnClickListener {
            val url = findViewById<EditText>(R.id.urlInput).text.toString().trim()
            if (url.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                    if (url.startsWith("http")) url else "https://$url"
                ))
                startActivity(intent)
            }
        }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            showToast("App not installed: $packageName")
            // Open Play Store
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }
    }

    private fun makeCall(number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            startActivity(intent)
        } catch (e: SecurityException) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            startActivity(intent)
            showToast("Call permission needed, opening dialer")
        }
    }

    private fun toggleFlashlight(turnOn: Boolean) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, turnOn)
            isFlashlightOn = turnOn
            showToast("Flashlight ${if (turnOn) "ON" else "OFF"}")
        } catch (e: Exception) {
            showToast("Flashlight error: ${e.message}")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
