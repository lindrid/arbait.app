package `in`.arbait.http.response

import `in`.arbait.http.User
import com.google.gson.annotations.SerializedName

class UserResponse (var response: Response = Response()) {
  @SerializedName("user")
  lateinit var user: User
}