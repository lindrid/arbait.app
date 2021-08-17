package `in`.arbait.http

import retrofit2.Call
import retrofit2.http.*

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

  @POST("/api/verify_user")
  fun verifyUser (@HeaderMap headers: Map<String, String>,
                  @Query("user_ver_code") userVerCode: String): Call<String>

}