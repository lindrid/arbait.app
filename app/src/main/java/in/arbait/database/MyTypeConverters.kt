package `in`.arbait.database

import androidx.room.TypeConverter
import java.util.*

class MyTypeConverters {

  @TypeConverter
  fun fromConsiquences(cons: Consiquences): Int {
    return when (cons) {
      Consiquences.NOTHING -> 0
      Consiquences.DECREASE_RATING_AND_BANN -> 1
    }
  }

  @TypeConverter
  fun toConsiquences(consInt: Int): Consiquences {
    return when (consInt) {
      0 -> Consiquences.NOTHING
      1 -> Consiquences.DECREASE_RATING_AND_BANN
      else -> Consiquences.NOTHING
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