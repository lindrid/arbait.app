package `in`.arbait.http.items

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DebitCardItem (
  val id: Int,
  @SerializedName("number") val number: String,
  @SerializedName("main") val main: Boolean
): Serializable {}
