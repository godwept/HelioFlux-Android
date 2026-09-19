package ca.stewark.helioflux.core.database

import androidx.room.TypeConverter
import ca.stewark.helioflux.core.model.DataSourceKey
import ca.stewark.helioflux.core.model.SolarImageType

class DatabaseConverters {
    @TypeConverter
    fun dataSourceKeyToString(value: DataSourceKey?): String? = value?.value

    @TypeConverter
    fun stringToDataSourceKey(value: String?): DataSourceKey? = value?.let(::DataSourceKey)

    @TypeConverter
    fun solarImageTypeToString(value: SolarImageType?): String? = value?.name

    @TypeConverter
    fun stringToSolarImageType(value: String?): SolarImageType? = value?.let(SolarImageType::valueOf)
}
