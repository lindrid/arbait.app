package `in`.arbait.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.*

@Entity(tableName = "EnrollingPermission")
data class EnrollingPermission (
  @PrimaryKey var userId: Int,
  var clickCountWithinOneMin: Int = 0,
  var enableClickTime: Long,
  var lastClickTime: Long,
): Serializable {}

