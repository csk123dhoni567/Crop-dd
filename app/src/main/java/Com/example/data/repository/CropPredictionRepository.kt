package com.example.data.repository

import com.example.data.database.CropPrediction
import com.example.data.database.CropPredictionDao
import kotlinx.coroutines.flow.Flow

class CropPredictionRepository(private val dao: CropPredictionDao) {
    val allPredictions: Flow<List<CropPrediction>> = dao.getAllPredictions()

    suspend fun insert(prediction: CropPrediction) {
        dao.insertPrediction(prediction)
    }

    suspend fun deleteById(id: Int) {
        dao.deletePredictionById(id)
    }

    suspend fun clearAll() {
        dao.clearAllPredictions()
    }

    fun search(query: String): Flow<List<CropPrediction>> {
        return dao.searchPredictions("%$query%")
    }

    fun filterByCrop(crop: String): Flow<List<CropPrediction>> {
        return dao.filterPredictionsByCrop(crop)
    }
}
