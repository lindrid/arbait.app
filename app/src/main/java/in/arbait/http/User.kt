package `in`.arbait.http

import com.google.gson.annotations.SerializedName

data class User (
  val id: Int?,
  @SerializedName("first_name") val firstName: String?,
  @SerializedName("last_name") val lastName: String?,
  @SerializedName("birth_date") val birthDate: String?,
  val phone: String?,
  @SerializedName("phone_wa") val phoneWa: String?,
  val password: String?,
)