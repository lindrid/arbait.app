package `in`.arbait.http

import `in`.arbait.models.ApplicationItem
import com.google.gson.annotations.SerializedName

class UserResponse (var response: Response = Response()) {
  @SerializedName("user")
  lateinit var user: User
}