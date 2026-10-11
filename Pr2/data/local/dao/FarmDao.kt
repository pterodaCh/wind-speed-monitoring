package com.tetiana.wind_speed_monitoring.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tetiana.wind_speed_monitoring.data.local.entity.WindFarmEntity
import com.tetiana.wind_speed_monitoring.data.local.relation.FarmWithTurbines
import kotlinx.coroutines.flow.Flow

//
//  DAO станцій.
//  Методи, що повертають Flow, - "живі" запити: Room сам повторює їх
//  після кожної зміни таблиць, а UI отримує нові дані автоматично
//
@Dao
interface FarmDao {

    @Insert
    suspend fun insert(farm: WindFarmEntity): Long

    @Query("SELECT * FROM wind_farms ORDER BY name")
    fun observeAll(): Flow<List<WindFarmEntity>>

    // @Transaction - вкладені @Relation-підзапити виконуються атомарно
    @Transaction
    @Query("SELECT * FROM wind_farms ORDER BY name")
    fun observeFarmsWithTurbines(): Flow<List<FarmWithTurbines>>

    @Query("SELECT COUNT(*) FROM wind_farms")
    suspend fun count(): Int
}