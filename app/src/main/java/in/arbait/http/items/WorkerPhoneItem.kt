package `in`.arbait.http.items

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class WorkerPhoneItem (
  val id: Int,
  @SerializedName("number") val number: String,
  @SerializedName("type") val type: String,
): Serializable {}