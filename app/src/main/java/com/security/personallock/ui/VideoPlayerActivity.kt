package com.security.personallock.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.security.personallock.databinding.ActivityVideoPlayerBinding
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIDEO_PATH = "extra_video_path"
    }

    private lateinit var binding: ActivityVideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
        if (videoPath == null || !File(videoPath).exists()) {
            Toast.makeText(this, "Video file not found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnClosePlayer.setOnClickListener { finish() }

        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)

        binding.videoView.setVideoURI(Uri.fromFile(File(videoPath)))
        binding.videoView.setOnPreparedListener { mp ->
            binding.videoLoading.visibility = View.GONE
            mp.isLooping = true
            binding.videoView.start()
        }

        binding.videoView.setOnErrorListener { _, _, _ ->
            binding.videoLoading.visibility = View.GONE
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
            true
        }
    }
}
