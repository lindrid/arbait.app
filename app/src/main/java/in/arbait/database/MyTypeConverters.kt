package `in`.arbait.database

import androidx.room.TypeConverter
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.util.*

class MyTypeConverters {

  @TypeConverter
  fun fromDate(date: Date?): Long? {
    return date?.time
  }

  @TypeConverter
  fun toDate(millisSinceEpoch: Long?): Date? {
    return millisSinceEpoch?.let {
      Date(it)
    }
  }

  @TypeConverter
  fun toState(value: Int) = enumValues<AppState>()[value]

  @TypeConverter
  fun fromState(value: AppState) = value.ordinal

}