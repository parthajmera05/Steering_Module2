package com.example.steering_module


import android.Manifest

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.steering_module.ui.theme.CarControlTheme

class MainActivity : ComponentActivity() {
    private lateinit var btManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isConnected = mutableStateOf(false)

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
        when (char) {
            'A', 'B' -> {
                // Broadcast to the Accessibility Service
                val intent = Intent("com.example.steering_module.ACTION_PERFORM")
                intent.putExtra("CHAR", char)
                sendBroadcast(intent)
            }
            'C' -> sendMediaCommand(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            'D' -> sendMediaCommand(KeyEvent.KEYCODE_MEDIA_NEXT)
            'E' -> sendMediaCommand(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            'F' -> adjustVolume(AudioManager.ADJUST_RAISE)
            'G' -> adjustVolume(AudioManager.ADJUST_LOWER)
        }
    }

    private fun sendMediaCommand(keyCode: Int) {
        val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val up = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        sendBroadcast(Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, down)
        })
        sendBroadcast(Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, up)
        })
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
