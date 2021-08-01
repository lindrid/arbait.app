package `in`.arbait.http

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

const val ARBAIT_BASE_URL = "http://arbait.in"

interface WebApi {

  @Headers("Content-Type: application/json")
  @POST("/api/auth/register")
  fun register(@Body userData: User): Call<String>

}