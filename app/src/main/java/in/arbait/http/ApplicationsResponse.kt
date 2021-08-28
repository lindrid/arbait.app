package `in`.arbait.http

import `in`.arbait.ApplicationItem
import com.google.gson.annotations.SerializedName


class ApplicationsResponse {
  @SerializedName("open_apps")
  lateinit var openApps: List<ApplicationItem>
}