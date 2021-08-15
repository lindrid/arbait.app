package `in`.arbait.http

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

private const val TAG = "Server"

class Server {

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
  private val serverApi = retrofit.create(ServerApi::class.java)
  private val headers = HashMap<String, String>()


  init {
    headers["Accept"] = "application/json"
    headers["Content-Type"] = "application/json"
    headers["X-Authorization"] = "access_token"
  }

  fun registerUser (user: User, onResult: (`in`.arbait.http.Response) -> Unit) {
    serverApi.register(headers, user).enqueue (
      getCallbackObject (
        "register.onFailure",
        "register.response.isSuccessful",
        "NOT register.response.isSuccessful, errorBody",
        onResult
      )
    )
  }

  fun getIncomingCall (onResult: (`in`.arbait.http.Response) -> Unit) {
    serverApi.sendVerRequest(headers).enqueue (
      getCallbackObject (
        "getIncomingCall.onFailure",
        "getIncomingCall.response.isSuccessful",
        "NOT getIncomingCall.response.isSuccessful, errorBody",
        onResult
      )
    )
  }


  private fun getCallbackObject (
    msgOnFailure: String,
    msgOnSuccessful: String,
    msgOnServerError: String,
    onResult: (`in`.arbait.http.Response) -> Unit): Callback<String>
  {
    return object : Callback<String> {
      private lateinit var response: `in`.arbait.http.Response

      override fun onFailure(call: Call<String>, t: Throwable) {
        Log.d (TAG, "$msgOnFailure: ${t.message}, $t")
        response = Response(t)
        onResult(response)
      }
      override fun onResponse(call: Call<String>, response: Response<String>) {
        if (response.isSuccessful) {
          Log.d (TAG, "$msgOnSuccessful: $response")
          this.response = Response(response)
        }
        else {
          response.errorBody()?.let {
            Log.d (TAG, "$msgOnServerError: $it")
            this.response = Response(it)
          }
        }
        onResult(this.response)
      }
    }
  }

}