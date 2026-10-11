package com.tetiana.wind_speed_monitoring.data.local.dao

import androidx.room.*
import com.tetiana.wind_speed_monitoring.data.local.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow

//
//  DAO вимірювань: CRUD + "живий" запит з фільтрацією та сортуванням
//
@Dao
interface MeasurementDao {

    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long

    @Update
    suspend fun update(measurement: MeasurementEntity)

    @Update
    suspend fun updateAll(measurements: List<MeasurementEntity>)

    @Delete
    suspend fun delete(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements WHERE turbineId = :turbineId")
    suspend fun getForTurbine(turbineId: Long): List<MeasurementEntity>

    // status - назва enum (TypeConverter зберігає саме її); null = усі статуси
    @Query(
        """
        SELECT * FROM measurements
        WHERE turbineId = :turbineId
          AND (:status IS NULL OR status = :status)
          AND windSpeed >= :minWindSpeed
        ORDER BY
            CASE WHEN :sort = 'DATE_DESC' THEN timestamp END DESC,
            CASE WHEN :sort = 'DATE_ASC' THEN timestamp END ASC,
            CASE WHEN :sort = 'POWER_DESC' THEN powerKw END DESC,
            CASE WHEN :sort = 'WIND_DESC' THEN windSpeed END DESC,
            timestamp DESC
        """
    )
    fun observeFiltered(
        turbineId: Long,
        status: String?,
        minWindSpeed: Double,
        sort: String
    ): Flow<List<MeasurementEntity>>
}