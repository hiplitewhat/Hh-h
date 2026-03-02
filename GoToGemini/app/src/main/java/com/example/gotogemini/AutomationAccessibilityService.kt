package com.example.gotogemini

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutomationAccessibilityService? = null
        var isConnected = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Listen for UI changes to detect content loaded
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    // Content changed — good time for screenshot
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // New window/activity opened
                }
            }
        }
    }

    override fun onInterrupt() {
        isConnected = false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isConnected = false
    }

    // ---------------------------
    // Automation helpers
    // ---------------------------

    fun clickOnText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            // Try parent
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                parent = parent.parent
            }
        }
        return false
    }

    fun typeText(nodeInfo: AccessibilityNodeInfo, text: String) {
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun findEditTextAndType(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editTexts = findNodesByClassName(rootNode, "android.widget.EditText")
        if (editTexts.isNotEmpty()) {
            typeText(editTexts[0], text)
            return true
        }
        return false
    }

    fun tapAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun swipeUp() {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.7f
        val endY = displayMetrics.heightPixels * 0.3f

        val path = Path()
        path.moveTo(centerX, startY)
        path.lineTo(centerX, endY)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun openNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    private fun findNodesByClassName(
        root: AccessibilityNodeInfo,
        className: String
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        if (root.className?.toString() == className) {
            result.add(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            result.addAll(findNodesByClassName(child, className))
        }
        return result
    }
}
