package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CropPredictionDao {
    @Query("SELECT * FROM crop_predictions ORDER BY timestamp DESC")
    fun getAllPredictions(): Flow<List<CropPrediction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: CropPrediction)

    @Query("DELETE FROM crop_predictions WHERE id = :id")
    suspend fun deletePredictionById(id: Int)

    @Query("DELETE FROM crop_predictions")
    suspend fun clearAllPredictions()

    @Query("SELECT * FROM crop_predictions WHERE crop LIKE :query OR disease LIKE :query ORDER BY timestamp DESC")
    fun searchPredictions(query: String): Flow<List<CropPrediction>>

    @Query("SELECT * FROM crop_predictions WHERE crop = :crop ORDER BY timestamp DESC")
    fun filterPredictionsByCrop(crop: String): Flow<List<CropPrediction>>
}
