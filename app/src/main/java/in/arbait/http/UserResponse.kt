package `in`.arbait.http

import com.google.gson.annotations.SerializedName

class UserResponse (var response: Response = Response()) {
  @SerializedName("user")
  lateinit var user: User
}