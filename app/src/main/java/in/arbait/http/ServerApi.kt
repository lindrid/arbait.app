package `in`.arbait.http

import retrofit2.Call
import retrofit2.http.*

const val ARBAIT_BASE_URL = "http://arbait.in"

interface ServerApi {

  //@Headers("Content-Type: application/json")
  @POST("/api/auth/android/register")
  fun register (@HeaderMap headers: Map<String, String>, @Body userData: User): Call<String>

  @POST("/api/auth/android/login")
  fun login (@HeaderMap headers: Map<String, String>,  @Query("phone") phone: String): Call<String>

  @POST("/api/send_ver_request")
  fun sendVerRequest (@HeaderMap headers: Map<String, String>): Call<String>

  @POST("/api/verify/user")
  fun verifyUser (@HeaderMap headers: Map<String, String>,
                  @Query("user_ver_code") userVerCode: String): Call<UserResponse>

  @POST("/api/verify/login/user")
  fun verifyUserForLogin (@HeaderMap headers: Map<String, String>,
                  @Query("user_ver_code") userVerCode: String): Call<UserResponse>

  @GET("/api/service/data")
  fun getData (@HeaderMap headers: Map<String, String>): Call<ServiceDataResponse>
}