package `in`.arbait.http

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST

const val ARBAIT_BASE_URL = "http://arbait.in"

interface ServerApi {

  //@Headers("Content-Type: application/json")
  @POST("/api/auth/register")
  fun register (
    @HeaderMap headers: Map<String, String>,
    @Body userData: User
  ): Call<String>

  @POST("/api/send_ver_request")
  fun sendVerRequest (@HeaderMap headers: Map<String, String>): Call<String>

}