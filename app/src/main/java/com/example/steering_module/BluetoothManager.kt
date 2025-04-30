package com.example.steering_module

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.InputStream
import java.util.*
import kotlin.concurrent.thread

class BluetoothManager(
    private val context: Context,
    private val onCharReceived: (Char) -> Unit,
    private val onConnected: (Boolean) -> Unit
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null

    fun connectToHC05() {
        if (!hasBluetoothPermissions()) {
            Log.e("BluetoothManager", "Missing Bluetooth permissions")
            onConnected(false)
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter?.bondedDevices?.find {
            it.name == "HC-05"
        }

        if (device == null) {
            Log.e("BluetoothManager", "HC-05 not paired")
            onConnected(false)
            return
        }

        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")


        thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter.cancelDiscovery()
                socket?.connect()

                inputStream = socket?.inputStream
                onConnected(true)
                listenForData()
            } catch (e: Exception) {
                Log.e("BluetoothManager", "Connection failed: ${e.message}")
                onConnected(false)
            }
        }
    }

    private fun listenForData() {
        try {
            val buffer = ByteArray(1024)
            while (true) {
                val bytes = inputStream?.read(buffer) ?: break
                if (bytes > 0) {
                    val receivedChar = buffer[0].toInt().toChar()
                    onCharReceived(receivedChar)
                }
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Error reading data: ${e.message}")
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun disconnect() {
        try {
            inputStream?.close()
            socket?.close()
            onConnected(false)
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Error closing connection: ${e.message}")
        }
    }
}
