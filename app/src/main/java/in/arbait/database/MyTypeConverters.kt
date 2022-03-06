package `in`.arbait.database

import androidx.room.TypeConverter
import java.util.*

class MyTypeConverters {

  @TypeConverter
  fun fromConsiquences(cons: Consequences): Int {
    return when (cons) {
      Consequences.NOTHING -> 0
      Consequences.DECREASE_RATING_AND_BANN -> 1
    }
  }

  @TypeConverter
  fun toConsiquences(consInt: Int): Consequences {
    return when (consInt) {
      0 -> Consequences.NOTHING
      1 -> Consequences.DECREASE_RATING_AND_BANN
      else -> Consequences.NOTHING
    }
  }

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