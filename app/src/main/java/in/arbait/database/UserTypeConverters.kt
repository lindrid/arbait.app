package `in`.arbait.database

import androidx.room.TypeConverter
import java.util.*

class UserTypeConverters {

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

}