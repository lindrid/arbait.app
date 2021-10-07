package `in`.arbait.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.*

@Entity
data class User (
  var id: Int,

  @PrimaryKey var phone: String,
  var callReceived: Boolean = false,
  var isConfirmed: Boolean = false,
  var login: Boolean = false,
  var isItRegistration: Boolean = false,
  var headerWasPressed: Boolean = false,

  var notificationsOff: Boolean = false,
  var soundOff: Boolean = false,

  var createdAt: Date = Date(),
  var updatedAt: Date = Date(),
): Serializable {}

