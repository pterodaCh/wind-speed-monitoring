package com.tetiana.wind_speed_monitoring.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tetiana.wind_speed_monitoring.data.local.dao.FarmDao
import com.tetiana.wind_speed_monitoring.data.local.dao.MeasurementDao
import com.tetiana.wind_speed_monitoring.data.local.dao.TurbineDao
import com.tetiana.wind_speed_monitoring.data.local.entity.MeasurementEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.TurbineEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.WindFarmEntity

//
//  Room Database - єдина точка доступу до SQLite.
//  Singleton: відкривати кілька з'єднань з однією БД дорого й небезпечно
//
@Database(
    entities = [WindFarmEntity::class, TurbineEntity::class, MeasurementEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WindDatabase : RoomDatabase() {

    abstract fun farmDao(): FarmDao
    abstract fun turbineDao(): TurbineDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: WindDatabase? = null

        // Double-checked locking - потокобезпечне створення одного екземпляра
        fun getInstance(context: Context): WindDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WindDatabase::class.java,
                    "wind_monitoring.db"
                )
                    // Для навчального проєкту: при зміні схеми БД перестворюється
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}