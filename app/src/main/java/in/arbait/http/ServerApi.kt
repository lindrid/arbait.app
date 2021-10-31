package `in`.arbait.http

import `in`.arbait.APP_VERSION
import `in`.arbait.http.response.ApplicationUserResponse
import `in`.arbait.http.response.ServiceDataResponse
import `in`.arbait.http.response.UserResponse
import retrofit2.Call
import retrofit2.http.*

const val ARBAIT_BASE_URL = "https://arbait.in"

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

  @POST("/api/porter/enroll")
  fun enrollPorter (@HeaderMap headers: Map<String, String>,
                    @Query("app_id") appId: Int): Call<ApplicationUserResponse>

  @POST("/api/porter/refuse")
  fun refuseApp ( @HeaderMap headers: Map<String, String>,
                  @Query("app_id") appId: Int): Call<ApplicationUserResponse>

  @POST("/api/porter/change/app/debit_card")
  fun changeDebitCard ( @HeaderMap headers: Map<String, String>,
                  @Query("app_id") appId: Int,
                  @Query("debit_card_id") debitCardId: Int?,
                  @Query("debit_card") debitCard: String?): Call<ApplicationUserResponse>

  @GET("/api/service/data/{v}/{c}")
  fun getData (@HeaderMap headers: Map<String, String>,
               @Path("v") version: String,
               @Path("c") cache: Int): Call<ServiceDataResponse>
}