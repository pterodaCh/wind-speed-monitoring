package com.tetiana.wind_speed_monitoring.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.tetiana.wind_speed_monitoring.WindApplication
import com.tetiana.wind_speed_monitoring.data.local.WindDatabase
import com.tetiana.wind_speed_monitoring.data.remote.SensorDataSource
import com.tetiana.wind_speed_monitoring.data.repository.WindDataRepository

//
//  Ручне впровадження залежностей (Dependency Injection без бібліотек).
//  Архітектурне рішення: ViewModel не створює Repository сама, а отримує
//  його ззовні. Тому її легко тестувати (можна підставити фейковий Repository),
//  а UI взагалі не знає про існування Room
//
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // lazy - БД відкривається лише при першому зверненні, а не при старті застосунку
    private val database: WindDatabase by lazy { WindDatabase.getInstance(appContext) }
    private val sensorDataSource: SensorDataSource by lazy { SensorDataSource() }

    val windRepository: WindDataRepository by lazy {
        WindDataRepository(database = database, sensorDataSource = sensorDataSource)
    }
}

// Доступ до контейнера з фабрики ViewModel
fun CreationExtras.windApplication(): WindApplication = this[APPLICATION_KEY] as WindApplication