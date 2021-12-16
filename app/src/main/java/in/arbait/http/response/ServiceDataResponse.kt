package `in`.arbait.http.response

import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.items.UserItem
import com.google.gson.annotations.SerializedName


class ServiceDataResponse (var response: Response = Response()) {
  @SerializedName("o")
  lateinit var openApps: List<ApplicationItem>

  @SerializedName("u")
  var user: UserItem? = null

  @SerializedName("t")
  var takenApps: List<ApplicationItem>? = null

  @SerializedName("n")
  var serverTime: String? = null

  @SerializedName("m")
  var maxPorterRating: Int? = null

  @SerializedName("w")
  var dispatcherWhatsapp: String? = null

  @SerializedName("c")
  var dispatcherPhoneCall: String? = null

  init {
    if (response.noResult)
      openApps = emptyList()
  }
}