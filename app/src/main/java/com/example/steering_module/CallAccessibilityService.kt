package com.example.steering_module

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class CallAccessibilityService : AccessibilityService() {

    private val TAG = "CallService"

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter("com.example.steering_module.ACTION_PERFORM")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                broadcastReceiver,
                filter,
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                broadcastReceiver,
                filter
            )
        }

        Log.d(TAG, "Service connected and receiver registered")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // You can use this if you want to auto-trigger on state changes (optional)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val char = intent?.getCharExtra("CHAR", ' ')
            when (char) {
                'A' -> pickUpCall()
                'B' -> hangUpCall()
            }
        }
    }

    private fun pickUpCall() {
        Log.d(TAG, "Trying to pick up call")
        val rootNode = rootInActiveWindow ?: return
        val viewIdCandidates = listOf(
            "com.android.incallui:id/answer",           // AOSP
            "com.google.android.dialer:id/answer_button", // Google Dialer
            "com.samsung.android.incallui:id/answer"    // Samsung
        )

        for (viewId in viewIdCandidates) {
            val node = findNodeByViewId(rootNode, viewId)
            if (node != null) {
                Log.d(TAG, "Found button with viewId: $viewId")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
        performClickOnNodeByTextOrDescription("Answer") // English

    }

    private fun hangUpCall() {
        Log.d(TAG, "Trying to hang up call")
        val rootNode = rootInActiveWindow ?: return
        val viewIdCandidates = listOf(
            "com.android.incallui:id/end_call",                // AOSP
            "com.google.android.dialer:id/end_call_button",    // Google Dialer
            "com.samsung.android.incallui:id/end_call_button"  // Samsung Dialer
        )

        for (viewId in viewIdCandidates) {
            val node = findNodeByViewId(rootNode, viewId)
            if (node != null) {
                Log.d(TAG, "Found button with viewId: $viewId")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }

        Log.d(TAG, "Trying fallback by text or description for End")
        performClickOnNodeByTextOrDescription("End")

    }

    private fun performClickOnNodeByTextOrDescription(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val targetNode = findNode(rootNode, text)
        if (targetNode != null) {
            Log.d(TAG, "Found node with text/description: '$text', performing click.")
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            Log.d(TAG, "No node found with text/description: '$text'")
        }
    }
    private fun findNodeByViewId(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        val list = root.findAccessibilityNodeInfosByViewId(viewId)
        return list.firstOrNull()
    }


    private fun findNode(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.text?.toString()?.contains(text, true) == true ||
                node.contentDescription?.toString()?.contains(text, true) == true) {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }
}
