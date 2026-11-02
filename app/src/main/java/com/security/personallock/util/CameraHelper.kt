package com.security.personallock.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.security.personallock.data.IntruderRepository
import java.io.File

object CameraHelper {

    private const val TAG = "CameraHelper"
    private var activeRecording: Recording? = null
    private var isCurrentlyRecording = false

    fun startSilent5SecRecording(
        activity: AppCompatActivity,
        previewView: PreviewView,
        targetAppName: String,
        targetPackageName: String,
        onFinished: (() -> Unit)? = null
    ) {
        if (isCurrentlyRecording) {
            Log.d(TAG, "Recording already in progress, skipping duplicate trigger.")
            return
        }

        // Verify camera permission
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Camera permission not granted. Cannot capture intruder video.")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Prefer Front Camera to capture the intruder's face
                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD))
                    .build()

                val videoCapture = VideoCapture.withOutput(recorder)

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    activity,
                    cameraSelector,
                    preview,
                    videoCapture
                )

                // Destination file
                val repository = IntruderRepository(activity)
                val outputFile = File(
                    repository.videosDir,
                    "intruder_${System.currentTimeMillis()}.mp4"
                )

                val fileOutputOptions = FileOutputOptions.Builder(outputFile).build()

                isCurrentlyRecording = true

                activeRecording = videoCapture.output
                    .prepareRecording(activity, fileOutputOptions)
                    .start(ContextCompat.getMainExecutor(activity)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                Log.i(TAG, "Silent intruder video recording started.")
                            }
                            is VideoRecordEvent.Finalize -> {
                                isCurrentlyRecording = false
                                if (!event.hasError()) {
                                    Log.i(TAG, "Video successfully saved: ${outputFile.absolutePath}")
                                    repository.addRecord(outputFile, targetAppName, targetPackageName)
                                } else {
                                    Log.e(TAG, "Video recording error: ${event.error}")
                                }
                                onFinished?.invoke()
                            }
                        }
                    }

                // Automatically stop recording after 5 seconds (5000 ms)
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        activeRecording?.stop()
                        activeRecording = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isCurrentlyRecording = false
                    }
                }, 5000)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera recording", e)
                isCurrentlyRecording = false
            }
        }, ContextCompat.getMainExecutor(activity))
    }
}
