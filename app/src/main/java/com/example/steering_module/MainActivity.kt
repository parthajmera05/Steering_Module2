package com.example.steering_module

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.steering_module.ui.theme.CarControlTheme

class MainActivity : ComponentActivity() {
    private lateinit var btManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isConnected = mutableStateOf(false)

        // Request permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
            }
        }

        btManager = BluetoothManager(
            context = this,
            onCharReceived = { char -> handleChar(char) },
            onConnected = { connected -> isConnected.value = connected }
        )

        requestBluetoothPermissions()

        setContent {
            CarControlTheme {
                val context = LocalContext.current

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = if (isConnected.value) "Connected to HC-05" else "Not Connected")
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { btManager.connectToHC05() }) {
                        Text("Connect to HC-05")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        startActivity(intent)
                    }) {
                        Text("Enable Accessibility Service")
                    }
                }
            }
        }
    }

    private fun handleChar(char: Char) {
        runOnUiThread {
            Toast.makeText(this, "Received: $char", Toast.LENGTH_SHORT).show()
        }

        when (char) {
            'A', 'B' -> {
                val intent = Intent("com.example.steering_module.ACTION_PERFORM")
                intent.putExtra("CHAR", char)
                sendBroadcast(intent)
            }
            'C' -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            'D' -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            'E' -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            'F' -> adjustVolume(AudioManager.ADJUST_RAISE)
            'G' -> adjustVolume(AudioManager.ADJUST_LOWER)
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val downTime = System.currentTimeMillis()

        val downEvent = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val upEvent = KeyEvent(downTime, downTime, KeyEvent.ACTION_UP, keyCode, 0)

        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    private fun adjustVolume(direction: Int) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1001
            )
        }
    }
}