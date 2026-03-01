package com.example.gotogemini

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AppControlActivity : AppCompatActivity() {

    private var isFlashlightOn = false

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

        // Bluetooth Toggle
        findViewById<Switch>(R.id.switchBluetooth).setOnCheckedChangeListener { _, isChecked ->
            try {
                val btAdapter = BluetoothAdapter.getDefaultAdapter()
                if (isChecked) btAdapter?.enable() else btAdapter?.disable()
                showToast("Bluetooth ${if (isChecked) "ON" else "OFF"}")
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                showToast("Open Bluetooth settings manually")
            }
        }

        // Flashlight Toggle
        findViewById<Switch>(R.id.switchFlashlight).setOnCheckedChangeListener { _, isChecked ->
            toggleFlashlight(isChecked)
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
