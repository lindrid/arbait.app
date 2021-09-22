package `in`.arbait.http

import `in`.arbait.models.ApplicationItem
import com.google.gson.annotations.SerializedName


class ApplicationsResponse (var response: Response = Response()) {
  @SerializedName("open_apps")
  lateinit var openApps: List<ApplicationItem>

  init {
    if (response.noResult)
      openApps = emptyList()
  }
}