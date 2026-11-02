package com.security.personallock.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.security.personallock.R
import com.security.personallock.data.AppLockPreferences
import com.security.personallock.databinding.ActivityLockScreenBinding
import com.security.personallock.util.CameraHelper

class LockScreenActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    private lateinit var binding: ActivityLockScreenBinding
    private lateinit var prefs: AppLockPreferences
    private var targetPackageName: String = ""
    private var targetAppName: String = "Application"
    private var currentPinInput: String = ""
    private var wrongAttemptsCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppLockPreferences(this)
        targetPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        setupAppDetails()
        setupKeypad()

        // Handle Back button to go to Home launcher (prevents bypassing lock)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })
    }

    private fun setupAppDetails() {
        if (targetPackageName.isNotEmpty()) {
            try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(targetPackageName, 0)
                targetAppName = pm.getApplicationLabel(appInfo).toString()
                binding.tvAppName.text = targetAppName
                binding.ivAppIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
            } catch (e: PackageManager.NameNotFoundException) {
                binding.tvAppName.text = "Locked App"
            }
        }
    }

    private fun setupKeypad() {
        val keys = listOf(
            binding.key0 to "0",
            binding.key1 to "1",
            binding.key2 to "2",
            binding.key3 to "3",
            binding.key4 to "4",
            binding.key5 to "5",
            binding.key6 to "6",
            binding.key7 to "7",
            binding.key8 to "8",
            binding.key9 to "9"
        )

        for ((button, digit) in keys) {
            button.setOnClickListener {
                onDigitClicked(digit)
            }
        }

        binding.keyBackspace.setOnClickListener {
            if (currentPinInput.isNotEmpty()) {
                currentPinInput = currentPinInput.dropLast(1)
                updatePinDots()
            }
        }
    }

    private fun onDigitClicked(digit: String) {
        if (currentPinInput.length < 4) {
            currentPinInput += digit
            updatePinDots()

            if (currentPinInput.length == 4) {
                verifyPin()
            }
        }
    }

    private fun updatePinDots() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        for (i in dots.indices) {
            if (i < currentPinInput.length) {
                dots[i].setBackgroundColor(ContextCompat.getColor(this, R.color.accent))
            } else {
                dots[i].setBackgroundResource(R.drawable.bg_pin_circle)
            }
        }
        binding.tvError.visibility = View.INVISIBLE
    }

    private fun verifyPin() {
        val savedPin = prefs.masterPin

        if (savedPin == null || currentPinInput == savedPin) {
            // Correct PIN!
            if (targetPackageName.isNotEmpty()) {
                prefs.unlockPackageSession(targetPackageName)
            }
            finish()
        } else {
            // Incorrect PIN! Intruder detected!
            wrongAttemptsCount++
            triggerIntruderAlert()
        }
    }

    private fun triggerIntruderAlert() {
        // Vibrate phone slightly
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }

        // Show error message
        binding.tvError.visibility = View.VISIBLE
        currentPinInput = ""
        updatePinDots()

        // SILENT 5-SECOND VIDEO RECORDING FROM FRONT CAMERA!
        CameraHelper.startSilent5SecRecording(
            activity = this,
            previewView = binding.stealthCameraPreview,
            targetAppName = targetAppName,
            targetPackageName = targetPackageName
        )
    }
}
