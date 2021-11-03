package `in`.arbait.http.response

import `in`.arbait.http.User
import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.items.UserItem
import com.google.gson.annotations.SerializedName

class ApplicationResponse (var response: Response = Response()) {
  @SerializedName("application")
  lateinit var app: ApplicationItem
}