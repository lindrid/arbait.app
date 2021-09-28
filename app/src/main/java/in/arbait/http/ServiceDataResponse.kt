package `in`.arbait.http

import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.items.UserItem
import com.google.gson.annotations.SerializedName


class ServiceDataResponse (var response: Response = Response()) {
  @SerializedName("open_apps")
  lateinit var openApps: List<ApplicationItem>

  @SerializedName("user")
  var user: UserItem? = null

  init {
    if (response.noResult)
      openApps = emptyList()
  }
}