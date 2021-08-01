package `in`.arbait.http

import com.google.gson.annotations.SerializedName
import java.util.*

data class User (
  @SerializedName("id") val id: Int?,
  @SerializedName("first_name") val firstName: String?,
  @SerializedName("last_name") val lastName: String?,
  @SerializedName("birthdate") val birthdate: Date?,
  @SerializedName("phone") val phone: String?,
  @SerializedName("phone_wa") val phoneWa: String?,
  @SerializedName("password") val password: String?
)