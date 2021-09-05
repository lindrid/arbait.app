package `in`.arbait.http

import com.google.gson.annotations.SerializedName

data class User (
  val id: Int?,
  @SerializedName("first_name") val firstName: String?,
  @SerializedName("birth_date") val birthDate: String?,
  @SerializedName("phone") val phone: String?,
  @SerializedName("phone_wa") val phoneWa: String?,
  @SerializedName("debit_card") val debitCard: String?,
)