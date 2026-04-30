package com.vrikshaayush.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val imagePath: String,
    val cropType: String,
    val diseaseName: String,
    val confidence: Float,
    val severity: String,
    val timestamp: Long = System.currentTimeMillis()
)
