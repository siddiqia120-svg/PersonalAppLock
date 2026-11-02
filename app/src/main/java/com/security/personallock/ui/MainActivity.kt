package com.security.personallock.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.security.personallock.R
import com.security.personallock.data.AppLockPreferences
import com.security.personallock.data.IntruderRepository
import com.security.personallock.databinding.ActivityMainBinding
import com.security.personallock.service.AppLockAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppLockPreferences
    private lateinit var repository: IntruderRepository
    private lateinit var adapter: AppListAdapter

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        checkPermissions()
        if (isGranted) {
            Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppLockPreferences(this)
        repository = IntruderRepository(this)

        setupRecyclerView()
        setupListeners()

        // Check if master PIN is set
        if (!prefs.isPinSet()) {
            showSetPinDialog(isFirstTime = true)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        updateVaultCount()
        loadInstalledApps()
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(emptyList(), prefs) { app, isLocked ->
            // Feedback
            val status = if (isLocked) "Locked" else "Unlocked"
            Toast.makeText(this, "${app.name} is now $status", Toast.LENGTH_SHORT).show()
        }
        binding.rvAppsList.layoutManager = LinearLayoutManager(this)
        binding.rvAppsList.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnChangePin.setOnClickListener {
            showSetPinDialog(isFirstTime = false)
        }

        binding.btnIntruderVault.setOnClickListener {
            startActivity(Intent(this, IntruderVaultActivity::class.java))
        }

        binding.btnGrantPermissions.setOnClickListener {
            requestNextMissingPermission()
        }
    }

    private fun updateVaultCount() {
        val intruders = repository.getAllRecords()
        binding.btnIntruderVault.text = "Intruders (${intruders.size})"
    }

    private fun checkPermissions() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        val hasCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasAccessibility = isAccessibilityServiceEnabled(this, AppLockAccessibilityService::class.java)

        val allGranted = hasOverlay && hasCamera && hasAccessibility

        if (allGranted) {
            binding.cardPermissions.visibility = View.GONE
        } else {
            binding.cardPermissions.visibility = View.VISIBLE
            val missing = mutableListOf<String>()
            if (!hasOverlay) missing.add("Overlay")
            if (!hasCamera) missing.add("Camera")
            if (!hasAccessibility) missing.add("Accessibility")
            binding.tvPermissionStatus.text = "Missing: ${missing.joinToString(", ")}"
        }
    }

    private fun requestNextMissingPermission() {
        // 1. Overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        // 2. Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        // 3. Accessibility
        if (!isAccessibilityServiceEnabled(this, AppLockAccessibilityService::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please enable 'Personal AppLock' in Accessibility", Toast.LENGTH_LONG).show()
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any {
            it.equals(expectedComponentName, ignoreCase = true)
        }
    }

    private fun loadInstalledApps() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val lockedPackages = prefs.getLockedPackages()

            val appList = resolveInfos.mapNotNull { resolveInfo ->
                val pName = resolveInfo.activityInfo.packageName
                if (pName == packageName) return@mapNotNull null // Exclude ourselves

                val name = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val isLocked = lockedPackages.contains(pName)

                AppItem(name, pName, icon, isLocked)
            }.sortedBy { it.name.lowercase() }

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                adapter.updateData(appList)
            }
        }
    }

    private fun showSetPinDialog(isFirstTime: Boolean) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter 4-digit PIN"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle(if (isFirstTime) "Setup Master PIN" else "Change Master PIN")
            .setMessage("This PIN will unlock your secured apps and the Intruder Vault.")
            .setView(input)
            .setCancelable(!isFirstTime)
            .setPositiveButton("Save") { _, _ ->
                val pin = input.text.toString().trim()
                if (pin.length == 4) {
                    prefs.masterPin = pin
                    binding.btnChangePin.text = getString(R.string.btn_change_pin)
                    Toast.makeText(this, "PIN successfully saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN must be exactly 4 digits!", Toast.LENGTH_SHORT).show()
                    if (isFirstTime) showSetPinDialog(true)
                }
            }
            .show()
    }
}
