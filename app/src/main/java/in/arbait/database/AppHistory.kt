package `in`.arbait.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "AppHistory")
data class AppHistory (
  @PrimaryKey var appId: Int,
  var enrollTime: Long = 0,
  var refuseTime: Long = 0,
  var consequences: Consequences = Consequences.NOTHING
): Serializable {}

