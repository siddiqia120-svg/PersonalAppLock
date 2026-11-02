package com.security.personallock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.security.personallock.data.AppLockPreferences
import com.security.personallock.ui.LockScreenActivity

class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var prefs: AppLockPreferences
    private var lastForegroundPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        prefs = AppLockPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Skip our own application package
        if (packageName == this.packageName) {
            return
        }

        // Avoid repeated triggers for the exact same event
        if (packageName == lastForegroundPackage) {
            return
        }
        lastForegroundPackage = packageName

        // Check if user locked this app
        if (prefs.isPackageLocked(packageName)) {
            // Check if app is already unlocked in the current session
            if (!prefs.isPackageSessionUnlocked(packageName)) {
                Log.d("AppLockService", "Locked app detected: $packageName. Launching lock screen overlay.")
                val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra(LockScreenActivity.EXTRA_PACKAGE_NAME, packageName)
                }
                startActivity(lockIntent)
            }
        }
    }

    override fun onInterrupt() {
        Log.w("AppLockService", "Accessibility service interrupted.")
    }
}
