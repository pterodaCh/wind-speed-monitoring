package com.tetiana.wind_speed_monitoring


import android.app.Application
import com.tetiana.wind_speed_monitoring.di.AppContainer

//
//  Клас застосунку - точка створення залежностей.
//  AppContainer живе стільки ж, скільки процес, тому БД і Repository
//  існують в одному екземплярі й спільні для всіх ViewModel
//
class WindApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}