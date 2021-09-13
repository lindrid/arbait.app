package `in`.arbait.http

import `in`.arbait.ApplicationItem
import com.google.gson.annotations.SerializedName


class ApplicationsResponse (var response: Response = Response()) {
  @SerializedName("open_apps")
  lateinit var openApps: List<ApplicationItem>

  @SerializedName("closed_apps")
  lateinit var closedApps: List<ApplicationItem>
}