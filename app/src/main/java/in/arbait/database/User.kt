package `in`.arbait.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class User (
  @PrimaryKey var id: UUID = UUID.randomUUID(),
  var phone: String = "",
  var password: String = "",
  var isConfirmed: Boolean = false,
  var login: Boolean = false,
  var createdAt: Date = Date())
{

}

