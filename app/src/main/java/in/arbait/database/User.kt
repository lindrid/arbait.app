package `in`.arbait.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.*

@Entity
data class User (
  @PrimaryKey var phone: String,
  var password: String = "",
  var callReceived: Boolean = false,
  var isConfirmed: Boolean = false,
  var login: Boolean = false,
  var createdAt: Date = Date()
): Serializable {}

