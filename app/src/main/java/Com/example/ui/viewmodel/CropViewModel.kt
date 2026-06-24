package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.CropAnalysisResult
import com.example.data.api.GeminiClient
import com.example.data.api.PredictionItem
import com.example.data.database.AppDatabase
import com.example.data.database.CropPrediction
import com.example.data.repository.CropPredictionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DetectionUiState {
    object Idle : DetectionUiState
    object Loading : DetectionUiState
    data class Success(val result: CropAnalysisResult, val imageUri: String?) : DetectionUiState
    data class Error(val message: String) : DetectionUiState
}

class CropViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CropPredictionRepository
    
    // UI state for image detection
    private val _detectionUiState = MutableStateFlow<DetectionUiState>(DetectionUiState.Idle)
    val detectionUiState: StateFlow<DetectionUiState> = _detectionUiState.asStateFlow()

    // Search and filter states for history list
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilterCrop = MutableStateFlow("All")
    val selectedFilterCrop = _selectedFilterCrop.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CropPredictionRepository(database.cropPredictionDao())
    }

    // Historical predictions from DB
    val historyState: StateFlow<List<CropPrediction>> = combine(
        repository.allPredictions,
        _searchQuery,
        _selectedFilterCrop
    ) { predictions, query, filter ->
        var list = predictions
        
        if (filter != "All") {
            list = list.filter { it.crop.equals(filter, ignoreCase = true) }
        }
        
        if (query.isNotEmpty()) {
            list = list.filter {
                it.crop.contains(query, ignoreCase = true) || 
                it.disease.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dashboard Statistics calculated from history state
    val totalScans: StateFlow<Int> = historyState.combine(historyState) { history, _ -> history.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val healthyCount: StateFlow<Int> = historyState.combine(historyState) { history, _ ->
        history.count { it.disease.equals("Healthy", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val diseaseCount: StateFlow<Int> = historyState.combine(historyState) { history, _ ->
        history.count { !it.disease.equals("Healthy", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterCrop(crop: String) {
        _selectedFilterCrop.value = crop
    }

    // Analyze leaf using Gemini API and save to history
    fun analyzeCropImage(bitmap: Bitmap, imageUri: Uri?) {
        _detectionUiState.value = DetectionUiState.Loading
        viewModelScope.launch {
            try {
                val analysisResult = withContext(Dispatchers.IO) {
                    GeminiClient.analyzeCropLeaf(bitmap)
                }
                
                // Save to database
                val dbPrediction = CropPrediction(
                    crop = analysisResult.crop,
                    disease = analysisResult.disease,
                    confidence = analysisResult.confidence,
                    description = analysisResult.description,
                    symptomsJson = analysisResult.symptoms.joinToString("||"),
                    causesJson = analysisResult.causes.joinToString("||"),
                    treatmentJson = analysisResult.treatment.joinToString("||"),
                    preventionJson = analysisResult.prevention.joinToString("||"),
                    imageUri = imageUri?.toString()
                )
                
                withContext(Dispatchers.IO) {
                    repository.insert(dbPrediction)
                }

                _detectionUiState.value = DetectionUiState.Success(analysisResult, imageUri?.toString())
            } catch (e: Exception) {
                _detectionUiState.value = DetectionUiState.Error(e.message ?: "An unknown error occurred during analysis.")
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
        }
    }

    fun resetDetectionState() {
        _detectionUiState.value = DetectionUiState.Idle
    }

    // Convert CropPrediction entity back to CropAnalysisResult for detail viewing
    fun mapPredictionToResult(prediction: CropPrediction): CropAnalysisResult {
        return CropAnalysisResult(
            crop = prediction.crop,
            disease = prediction.disease,
            confidence = prediction.confidence,
            description = prediction.description,
            symptoms = prediction.symptomsJson.split("||").filter { it.isNotEmpty() },
            causes = prediction.causesJson.split("||").filter { it.isNotEmpty() },
            treatment = prediction.treatmentJson.split("||").filter { it.isNotEmpty() },
            prevention = prediction.preventionJson.split("||").filter { it.isNotEmpty() },
            topPredictions = listOf(
                PredictionItem(prediction.disease, prediction.confidence)
            )
        )
    }
}
