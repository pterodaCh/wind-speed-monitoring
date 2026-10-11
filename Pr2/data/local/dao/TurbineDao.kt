package com.tetiana.wind_speed_monitoring.data.local.dao

import androidx.room.*
import com.tetiana.wind_speed_monitoring.data.local.entity.MeasurementEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.TurbineEntity
import com.tetiana.wind_speed_monitoring.data.local.relation.TurbineSummary
import kotlinx.coroutines.flow.Flow

//
//  DAO турбін. abstract class (а не interface) - щоб мати
//  власний метод з @Transaction, який викликає кілька інших методів
//
@Dao
abstract class TurbineDao {

    // ABORT: дубль назви в межах станції -> SQLiteConstraintException,
    // яку Repository перетворить на зрозуміле повідомлення
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(turbine: TurbineEntity): Long

    @Update
    abstract suspend fun update(turbine: TurbineEntity)

    // Вимірювання видаляться каскадно (ForeignKey.CASCADE)
    @Query("DELETE FROM turbines WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM turbines WHERE id = :id")
    abstract suspend fun getById(id: Long): TurbineEntity?

    @Query("SELECT * FROM turbines WHERE id = :id")
    abstract fun observeById(id: Long): Flow<TurbineEntity?>

    @Insert
    abstract suspend fun insertMeasurements(measurements: List<MeasurementEntity>)

    //
    //  Оптимізований агрегатний запит:
    //  - LEFT JOIN, щоб у список потрапили й турбіни без вимірювань;
    //  - обчислювані поля рахує SQLite (AVG/MAX/SUM/COUNT);
    //  - фільтрація (станція, пошук) і сортування - в самому SQL.
    //  Room не дозволяє параметр у ORDER BY напряму, тому використано
    //  ORDER BY CASE: спрацьовує лише гілка, що відповідає :sort
    //
    @Query(
        """
        SELECT t.id, t.farmId, t.name, f.name AS farmName,
               t.bladeRadius, t.efficiency, t.ratedPowerKw,
               COUNT(m.id) AS measurementCount,
               COALESCE(AVG(m.windSpeed), 0) AS avgWindSpeed,
               COALESCE(MAX(m.powerKw), 0) AS maxPowerKw,
               COALESCE(SUM(m.powerKw), 0) AS energyKwh,
               COALESCE(AVG(m.powerKw), 0) * 100.0 / t.ratedPowerKw AS capacityFactor,
               MAX(m.timestamp) AS lastMeasurementAt
        FROM turbines t
        INNER JOIN wind_farms f ON f.id = t.farmId
        LEFT JOIN measurements m ON m.turbineId = t.id
        WHERE (:farmId IS NULL OR t.farmId = :farmId)
          AND t.name LIKE '%' || :query || '%'
        GROUP BY t.id
        ORDER BY
            CASE WHEN :sort = 'NAME' THEN t.name END COLLATE NOCASE ASC,
            CASE WHEN :sort = 'ENERGY' THEN SUM(m.powerKw) END DESC,
            CASE WHEN :sort = 'WIND' THEN AVG(m.windSpeed) END DESC,
            CASE WHEN :sort = 'CAPACITY' THEN AVG(m.powerKw) * 100.0 / t.ratedPowerKw END DESC,
            t.name ASC
        """
    )
    abstract fun observeSummaries(farmId: Long?, query: String, sort: String): Flow<List<TurbineSummary>>

    //
    //  Транзакція: турбіна + її вимірювання записуються атомарно.
    //  Якщо вставка вимірювань впаде, турбіна теж не збережеться
    //
    @Transaction
    open suspend fun insertWithMeasurements(
        turbine: TurbineEntity,
        measurements: List<MeasurementEntity>
    ): Long {
        val turbineId = insert(turbine)
        insertMeasurements(measurements.map { it.copy(turbineId = turbineId) })
        return turbineId
    }
}