package com.security.personallock.ui

import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.security.personallock.R
import com.security.personallock.data.IntruderRecord
import com.security.personallock.data.IntruderRepository
import com.security.personallock.databinding.ActivityIntruderVaultBinding
import com.security.personallock.databinding.ItemIntruderRecordBinding
import java.io.File

class IntruderVaultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntruderVaultBinding
    private lateinit var repository: IntruderRepository
    private lateinit var adapter: IntruderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntruderVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = IntruderRepository(this)

        setSupportActionBar(binding.toolbarVault)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarVault.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadRecords()
    }

    private fun setupRecyclerView() {
        adapter = IntruderAdapter(
            records = emptyList(),
            onPlayClick = { record ->
                val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra(VideoPlayerActivity.EXTRA_VIDEO_PATH, record.videoFilePath)
                }
                startActivity(intent)
            },
            onDeleteClick = { record ->
                repository.deleteRecord(record)
                loadRecords()
            }
        )
        binding.rvIntruders.layoutManager = LinearLayoutManager(this)
        binding.rvIntruders.adapter = adapter
    }

    private fun loadRecords() {
        val records = repository.getAllRecords()
        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvIntruders.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvIntruders.visibility = View.VISIBLE
            adapter.updateData(records)
        }
    }
}

class IntruderAdapter(
    private var records: List<IntruderRecord>,
    private val onPlayClick: (IntruderRecord) -> Unit,
    private val onDeleteClick: (IntruderRecord) -> Unit
) : RecyclerView.Adapter<IntruderAdapter.IntruderViewHolder>() {

    class IntruderViewHolder(val binding: ItemIntruderRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntruderViewHolder {
        val binding = ItemIntruderRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IntruderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IntruderViewHolder, position: Int) {
        val record = records[position]
        holder.binding.tvIntruderApp.text = "Target: ${record.appName}"
        holder.binding.tvIntruderTime.text = record.formattedDate

        // Load video thumbnail
        val thumbnail = createVideoThumbnail(record.videoFilePath)
        if (thumbnail != null) {
            holder.binding.ivThumbnail.setImageBitmap(thumbnail)
        } else {
            holder.binding.ivThumbnail.setImageResource(R.drawable.ic_vault)
        }

        holder.itemView.setOnClickListener {
            onPlayClick(record)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(record)
        }
    }

    private fun createVideoThumbnail(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(file, Size(120, 120), null)
            } else {
                @Suppress("DEPRECATION")
                ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getItemCount(): Int = records.size

    fun updateData(newRecords: List<IntruderRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}
