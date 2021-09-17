package `in`.arbait.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DebitCardItem (
  val id: Int,
  @SerializedName("number") val number: String
): Serializable {}
