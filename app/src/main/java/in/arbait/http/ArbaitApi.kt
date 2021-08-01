package `in`.arbait

import retrofit2.Call
import retrofit2.http.POST

const val ARBAIT_BASE_URL = "http://arbait.in"

interface ArbaitApi {

  @POST("/")
  fun register(): Call<String>

}