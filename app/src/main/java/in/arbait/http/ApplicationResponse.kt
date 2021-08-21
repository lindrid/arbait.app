package `in`.arbait.http

import `in`.arbait.ApplicationItem
import com.google.gson.annotations.SerializedName


class ApplicationResponse {
  @SerializedName("apps")
  lateinit var appItems: List<ApplicationItem>
}