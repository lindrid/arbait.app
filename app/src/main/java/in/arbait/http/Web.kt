package `in`.arbait.http

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


private const val TAG = "Web"

class Web {

  private val gson = GsonBuilder()
    .setLenient()
    .create()
  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
  }
  private val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .build()
  private val retrofit = Retrofit.Builder()
    .baseUrl(ARBAIT_BASE_URL)
    .addConverterFactory(ScalarsConverterFactory.create())
    .addConverterFactory(GsonConverterFactory.create(gson))
    .client(client)
    .build()
  private lateinit var webApi: WebApi
  private val headers = HashMap<String, String>()


  init {
    headers["Accept"] = "application/json"
    headers["Content-Type"] = "application/json"
    headers["X-Authorization"] = "access_token"
  }

  fun registerUser (user: User, onResult: (String?, Boolean) -> Unit) {
    webApi = retrofit.create(WebApi::class.java)
    webApi.register(headers, user).enqueue(
      object : Callback<String> {
        override fun onFailure(call: Call<String>, t: Throwable) {
          Log.d (TAG, "register.onFailure: ${t.message}, $t")
          onResult(t.message, true)
        }
        override fun onResponse(call: Call<String>, response: Response<String>) {
          if (response.isSuccessful) {
            onResult(response.body(), false)
          }
          else {
            onResult(response.errorBody().toString(), true)
          }
        }
      }
    )
  }

}
