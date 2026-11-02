package com.security.personallock.data

data class IntruderRecord(
    val id: String,
    val videoFilePath: String,
    val appName: String,
    val packageName: String,
    val timestampMillis: Long,
    val formattedDate: String
)
