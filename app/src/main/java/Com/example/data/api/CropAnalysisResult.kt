package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CropAnalysisResult(
    val crop: String,
    val disease: String,
    val confidence: Double,
    val description: String,
    val symptoms: List<String>,
    val causes: List<String>,
    val treatment: List<String>,
    val prevention: List<String>,
    val topPredictions: List<PredictionItem>
)

@JsonClass(generateAdapter = true)
data class PredictionItem(
    val label: String,
    val confidence: Double
)
