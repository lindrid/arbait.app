package `in`.arbait.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PhoneItem (
  val id: Int,
  @SerializedName("number") val number: String,
  @SerializedName("type") val type: Int,
  @SerializedName("used_in_registration") val usedInRegistration: Boolean,
): Serializable {}