package com.tetiana.wind_speed_monitoring.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.tetiana.wind_speed_monitoring.data.cache.MemoryCache
import com.tetiana.wind_speed_monitoring.data.local.WindDatabase
import com.tetiana.wind_speed_monitoring.data.local.entity.MeasurementEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.TurbineEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.WindFarmEntity
import com.tetiana.wind_speed_monitoring.data.local.relation.TurbineSummary
import com.tetiana.wind_speed_monitoring.data.remote.SensorDataSource
import com.tetiana.wind_speed_monitoring.domain.logic.EnergyAggregator
import com.tetiana.wind_speed_monitoring.domain.logic.PowerCalculator
import com.tetiana.wind_speed_monitoring.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.random.Random

// Помилка шару даних з повідомленням, зрозумілим користувачу
class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Результат запиту до сенсора: fromCache = true, якщо дані взято з кешу
data class SensorFetchResult(val measurement: MeasurementEntity, val fromCache: Boolean)

//
//  Repository - єдине джерело правди (Single Source of Truth) для ViewModel.
//  Архітектурні рішення:
//   1. Агрегує кілька джерел: Room (локальна БД), SensorDataSource (віддалений
//      анемометр) і MemoryCache (кеш показань сенсора).
//   2. Дані з сенсора спершу записуються в Room, а UI читає лише з Room -
//      тому екран завжди узгоджений з базою.
//   3. Читання - через Flow (реактивно), запис - через suspend-функції,
//      які повертають Result<T>: ViewModel не перехоплює винятки сама.
//   4. Бізнес-логіка (розрахунок потужності, агрегація звітів) виконується тут,
//      у фонових потоках (Dispatchers.IO / Default), а не в UI
//
class WindDataRepository(
    private val database: WindDatabase,
    private val sensorDataSource: SensorDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val farmDao = database.farmDao()
    private val turbineDao = database.turbineDao()
    private val measurementDao = database.measurementDao()

    private val sensorCache = MemoryCache<Long, MeasurementEntity>(SENSOR_CACHE_TTL_MS)

    // ======================= Читання (реактивні потоки) =======================

    fun observeFarms(): Flow<List<WindFarmEntity>> = farmDao.observeAll()

    fun observeTurbineSummaries(filter: TurbineFilter): Flow<List<TurbineSummary>> =
        turbineDao.observeSummaries(filter.farmId, filter.query.trim(), filter.sort.name)

    // Звіти по станціях: вкладений зв'язок Room + агрегація в Kotlin (у фоні)
    fun observeFarmReports(): Flow<List<FarmReport>> =
        farmDao.observeFarmsWithTurbines()
            .map { farms -> farms.map(EnergyAggregator::buildFarmReport) }
            .flowOn(defaultDispatcher)

    fun observeTurbine(turbineId: Long): Flow<TurbineEntity?> = turbineDao.observeById(turbineId)

    fun observeMeasurements(turbineId: Long, filter: MeasurementFilter): Flow<List<MeasurementEntity>> =
        measurementDao.observeFiltered(turbineId, filter.status?.name, filter.minWindSpeed, filter.sort.name)

    // ======================= Турбіни (CRUD) =======================

    suspend fun addTurbine(
        farmId: Long, name: String, bladeRadius: Double, efficiency: Double, ratedPowerKw: Double
    ): Result<Long> = safeCall {
        turbineDao.insert(
            TurbineEntity(
                farmId = farmId, name = name.trim(), bladeRadius = bladeRadius,
                efficiency = efficiency, ratedPowerKw = ratedPowerKw
            )
        )
    }

    //
    //  Транзакція: при зміні параметрів турбіни потужність усіх її вимірювань
    //  перераховується. Оновлення турбіни і всіх вимірювань - одна атомарна операція.
    //  Повертає кількість перерахованих вимірювань
    //
    suspend fun updateTurbine(
        id: Long, farmId: Long, name: String, bladeRadius: Double, efficiency: Double, ratedPowerKw: Double
    ): Result<Int> = safeCall {
        database.withTransaction {
            val current = turbineDao.getById(id) ?: throw NoSuchElementException("Турбіну не знайдено")
            val updated = current.copy(
                farmId = farmId, name = name.trim(), bladeRadius = bladeRadius,
                efficiency = efficiency, ratedPowerKw = ratedPowerKw
            )
            turbineDao.update(updated)
            val recalculated = measurementDao.getForTurbine(id).map { recalculate(it, updated) }
            measurementDao.updateAll(recalculated)
            recalculated.size
        }
    }

    suspend fun deleteTurbine(id: Long): Result<Unit> = safeCall {
        turbineDao.deleteById(id)
        sensorCache.invalidate(id)
    }

    // ======================= Вимірювання (CRUD) =======================

    suspend fun addMeasurement(
        turbineId: Long,
        windSpeed: Double,
        source: MeasurementSource = MeasurementSource.MANUAL,
        timestamp: Long = System.currentTimeMillis()
    ): Result<MeasurementEntity> = safeCall {
        insertMeasurement(turbineId, windSpeed, source, timestamp)
    }

    suspend fun updateMeasurement(measurement: MeasurementEntity, newWindSpeed: Double): Result<MeasurementEntity> =
        safeCall {
            val turbine = turbineDao.getById(measurement.turbineId)
                ?: throw NoSuchElementException("Турбіну не знайдено")
            val updated = recalculate(measurement.copy(windSpeed = newWindSpeed), turbine)
            measurementDao.update(updated)
            updated
        }

    suspend fun deleteMeasurement(measurement: MeasurementEntity): Result<Unit> = safeCall {
        measurementDao.delete(measurement)
    }

    //
    //  Отримання даних з сенсора з кешуванням:
    //   - якщо показання свіже (молодше за TTL) і не просили примусово оновити, повертаємо кеш;
    //   - інакше звертаємось до сенсора, зберігаємо в Room і оновлюємо кеш
    //
    suspend fun fetchFromSensor(turbineId: Long, forceRefresh: Boolean = false): Result<SensorFetchResult> =
        safeCall {
            if (!forceRefresh) {
                sensorCache.get(turbineId)?.let { return@safeCall SensorFetchResult(it, fromCache = true) }
            }
            val reading = sensorDataSource.fetchReading(turbineId)
            val saved = insertMeasurement(turbineId, reading.windSpeed, MeasurementSource.SENSOR, reading.timestamp)
            sensorCache.put(turbineId, saved)
            SensorFetchResult(saved, fromCache = false)
        }

    // ======================= Початкові дані =======================

    //
    //  Заповнення демо-даними при першому запуску.
    //  withTransaction - уся ініціалізація або виконується повністю, або не виконується зовсім
    //
    suspend fun seedIfEmpty(): Result<Unit> = safeCall {
        if (farmDao.count() > 0) return@safeCall
        database.withTransaction {
            val botiieve = farmDao.insert(WindFarmEntity(name = "Ботієвська ВЕС", region = "Запорізька обл."))
            val tyligul = farmDao.insert(WindFarmEntity(name = "Тилігульська ВЕС", region = "Миколаївська обл."))

            val demoTurbines = listOf(
                TurbineEntity(farmId = botiieve, name = "ВТ-01", bladeRadius = 45.0, efficiency = 38.0, ratedPowerKw = 3000.0),
                TurbineEntity(farmId = botiieve, name = "ВТ-02", bladeRadius = 40.0, efficiency = 35.0, ratedPowerKw = 2500.0),
                TurbineEntity(farmId = botiieve, name = "ВТ-03", bladeRadius = 50.0, efficiency = 40.0, ratedPowerKw = 3600.0),
                TurbineEntity(farmId = tyligul, name = "ТЛ-01", bladeRadius = 52.0, efficiency = 41.0, ratedPowerKw = 4200.0),
                TurbineEntity(farmId = tyligul, name = "ТЛ-02", bladeRadius = 48.0, efficiency = 39.0, ratedPowerKw = 3600.0)
            )
            val now = System.currentTimeMillis()
            demoTurbines.forEachIndexed { index, turbine ->
                val random = Random(seed = 42 + index)
                val measurements = (0 until DEMO_HOURS).map { hour ->
                    // Кожне 11-те вимірювання - шторм, щоб у фільтрах були всі статуси
                    val wind = if ((hour + index) % 11 == 0) 26.5 else 1.5 + random.nextDouble() * 20
                    buildMeasurement(
                        turbine, (wind * 10).toInt() / 10.0, MeasurementSource.SENSOR,
                        now - (DEMO_HOURS - hour) * HOUR_MS
                    )
                }
                turbineDao.insertWithMeasurements(turbine, measurements) // вкладена транзакція DAO
            }
        }
    }

    // ======================= Допоміжне =======================

    private suspend fun insertMeasurement(
        turbineId: Long, windSpeed: Double, source: MeasurementSource, timestamp: Long
    ): MeasurementEntity {
        val turbine = turbineDao.getById(turbineId) ?: throw NoSuchElementException("Турбіну не знайдено")
        val entity = buildMeasurement(turbine, windSpeed, source, timestamp)
        return entity.copy(id = measurementDao.insert(entity))
    }

    // Обчислювані поля (powerKw, status) завжди рахує бізнес-логіка, а не UI
    private fun buildMeasurement(
        turbine: TurbineEntity, windSpeed: Double, source: MeasurementSource, timestamp: Long
    ): MeasurementEntity {
        val result = PowerCalculator.calculate(windSpeed, turbine.bladeRadius, turbine.efficiency, turbine.ratedPowerKw)
        return MeasurementEntity(
            turbineId = turbine.id, windSpeed = windSpeed, powerKw = result.powerKw,
            status = result.status, source = source, timestamp = timestamp
        )
    }

    private fun recalculate(measurement: MeasurementEntity, turbine: TurbineEntity): MeasurementEntity {
        val result = PowerCalculator.calculate(
            measurement.windSpeed, turbine.bladeRadius, turbine.efficiency, turbine.ratedPowerKw
        )
        return measurement.copy(powerKw = result.powerKw, status = result.status)
    }

    //
    //  Централізована обробка помилок:
    //   - запис/читання переносяться в IO-потік;
    //   - технічні винятки перетворюються на зрозумілі повідомлення;
    //   - CancellationException пробрасується далі, щоб не зламати скасування корутин
    //
    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
        Result.success(withContext(ioDispatcher) { block() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: SQLiteConstraintException) {
        Result.failure(RepositoryException("Запис з такою назвою вже існує на цій станції", e))
    } catch (e: IOException) {
        Result.failure(RepositoryException("Сенсор недоступний: ${e.message}", e))
    } catch (e: NoSuchElementException) {
        Result.failure(RepositoryException(e.message ?: "Запис не знайдено", e))
    } catch (e: Exception) {
        Result.failure(RepositoryException("Помилка бази даних: ${e.message}", e))
    }

    companion object {
        const val SENSOR_CACHE_TTL_MS = 30_000L
        private const val DEMO_HOURS = 24
        private const val HOUR_MS = 3_600_000L
    }
}