package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crop_predictions")
data class CropPrediction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val crop: String,
    val disease: String,
    val confidence: Double,
    val description: String,
    val symptomsJson: String,  // Stored as simple serialized/delimited string
    val causesJson: String,
    val treatmentJson: String,
    val preventionJson: String,
    val imageUri: String?,     // URI or local file path
    val timestamp: Long = System.currentTimeMillis()
)
