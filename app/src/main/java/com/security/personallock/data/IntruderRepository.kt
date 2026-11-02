package com.security.personallock.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntruderRepository(private val context: Context) {

    private val gson = Gson()
    private val recordsFile: File
        get() = File(context.filesDir, "intruder_records.json")

    val videosDir: File
        get() {
            val dir = File(context.filesDir, "intruder_videos")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getAllRecords(): List<IntruderRecord> {
        if (!recordsFile.exists()) return emptyList()
        return try {
            val json = recordsFile.readText()
            val type = object : TypeToken<List<IntruderRecord>>() {}.type
            gson.fromJson<List<IntruderRecord>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @Synchronized
    fun addRecord(videoFile: File, appName: String, packageName: String): IntruderRecord {
        val records = getAllRecords().toMutableList()
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(now))

        val newRecord = IntruderRecord(
            id = "intruder_${now}",
            videoFilePath = videoFile.absolutePath,
            appName = appName,
            packageName = packageName,
            timestampMillis = now,
            formattedDate = formattedDate
        )

        records.add(0, newRecord) // add newest first
        saveRecords(records)
        return newRecord
    }

    @Synchronized
    fun deleteRecord(record: IntruderRecord) {
        val records = getAllRecords().toMutableList()
        records.removeAll { it.id == record.id }
        saveRecords(records)

        // Delete actual video file
        try {
            val file = File(record.videoFilePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveRecords(records: List<IntruderRecord>) {
        try {
            val json = gson.toJson(records)
            recordsFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
