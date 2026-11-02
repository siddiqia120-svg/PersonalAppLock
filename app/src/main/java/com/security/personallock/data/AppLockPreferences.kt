package com.security.personallock.data

import android.content.Context
import android.content.SharedPreferences

class AppLockPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("personal_app_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MASTER_PIN = "key_master_pin"
        private const val KEY_LOCKED_PACKAGES = "key_locked_packages"

        // In-memory set of temporarily unlocked packages during the current session
        val temporarilyUnlockedPackages = mutableSetOf<String>()
        var lastUnlockedTime = mutableMapOf<String, Long>()
    }

    var masterPin: String?
        get() = prefs.getString(KEY_MASTER_PIN, null)
        set(value) {
            prefs.edit().putString(KEY_MASTER_PIN, value).apply()
        }

    fun isPinSet(): Boolean = !masterPin.isNullOrEmpty()

    fun getLockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setPackageLocked(packageName: String, locked: Boolean) {
        val current = getLockedPackages().toMutableSet()
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    fun isPackageLocked(packageName: String): Boolean {
        return getLockedPackages().contains(packageName)
    }

    fun isPackageSessionUnlocked(packageName: String): Boolean {
        // Unlocked for 60 seconds of session or until app changes
        val unlockTime = lastUnlockedTime[packageName] ?: return false
        val now = System.currentTimeMillis()
        return (now - unlockTime) < 60_000
    }

    fun unlockPackageSession(packageName: String) {
        temporarilyUnlockedPackages.add(packageName)
        lastUnlockedTime[packageName] = System.currentTimeMillis()
    }

    fun lockPackageSession(packageName: String) {
        temporarilyUnlockedPackages.remove(packageName)
        lastUnlockedTime.remove(packageName)
    }
}
