package `in`.arbait.http

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Web {

  private val client = OkHttpClient.Builder().build()
  private val retrofit = Retrofit.Builder()
    .baseUrl(ARBAIT_BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .client(client)
    .build()
  private lateinit var webApi: WebApi

  fun registerUser (user: User, onResult: (String?) -> Unit) {
    webApi = retrofit.create(WebApi::class.java)
    webApi.register(user).enqueue(
      object : Callback<String> {
        override fun onFailure(call: Call<String>, t: Throwable) {
          onResult(null)
        }
        override fun onResponse(call: Call<String>, response: Response<String>) {
          onResult(response.body())
        }
      }
    )
  }

}