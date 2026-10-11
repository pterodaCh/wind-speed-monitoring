package com.tetiana.wind_speed_monitoring.data.local

import androidx.room.TypeConverter
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementSource
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementStatus

//
//  SQLite не вміє зберігати enum - перетворюємо його на рядок і назад.
//  Зберігаємо name, а не ordinal: зміна порядку констант не зіпсує дані
//
class Converters {
    @TypeConverter fun statusToString(status: MeasurementStatus): String = status.name
    @TypeConverter fun stringToStatus(value: String): MeasurementStatus = MeasurementStatus.valueOf(value)

    @TypeConverter fun sourceToString(source: MeasurementSource): String = source.name
    @TypeConverter fun stringToSource(value: String): MeasurementSource = MeasurementSource.valueOf(value)
}