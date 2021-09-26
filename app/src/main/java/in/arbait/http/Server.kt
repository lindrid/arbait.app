package `in`.arbait.http

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type

private const val TAG = "Server"

class Server (private val context: Context) {

  val applicationsResponse: MutableLiveData<ApplicationsResponse> = MutableLiveData()

  private val gson = GsonBuilder()
    .setLenient()
    .registerTypeAdapter(Boolean::class.java, IntBooleanDeserializer())
    .create()

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
  }

  private val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor(SendSavedCookiesInterceptor(context))
    .addInterceptor(SaveReceivedCookiesInterceptor(context))
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

  fun updateApplicationsResponse () {
    serverApi.getAppList(headers).enqueue (
      object : Callback<ApplicationsResponse> {

        override fun onFailure (call: Call<ApplicationsResponse>, t: Throwable) {
          Log.e (TAG, "getAppList FAILED!", t)
          applicationsResponse.value = ApplicationsResponse(Response(t))
        }

        override fun onResponse (call: Call<ApplicationsResponse>,
                                 response: Response<ApplicationsResponse>)
        {
          if (response.code() == 200) {
            Log.i(TAG, "Response received, response.body() = ${response.body()}")
            val appsResponse: ApplicationsResponse? = response.body()
            applicationsResponse.value = appsResponse ?: ApplicationsResponse()
          }
          else {
            Log.e (TAG, "Server error with code ${response.code()}")
            response.errorBody()?.let {
              applicationsResponse.value = ApplicationsResponse(Response(it, response.code()))
            }
          }
        }

      }
    )
  }


  fun registerUser (user: User, onResult: (`in`.arbait.http.Response) -> Unit) {
    serverApi.register(headers, user).enqueue (
      getCallbackObjectShort("registerUser", onResult)
    )
  }

  fun loginUser (phone: String, onResult: (`in`.arbait.http.Response) -> Unit) {
    serverApi.login(headers, phone).enqueue (
      getCallbackObjectShort("loginUser", onResult)
    )
  }

  fun getIncomingCall (onResult: (`in`.arbait.http.Response) -> Unit) {
    serverApi.sendVerRequest(headers).enqueue (
      getCallbackObjectShort("getIncomingCall", onResult)
    )
  }

  fun verifyUser (code: String, onResult: (`in`.arbait.http.Response) -> Unit) {
    serverApi.verifyUser(headers, code).enqueue(
      getCallbackObjectShort("verifyUser", onResult)
    )
  }

  fun verifyUserForLogin (code: String, onResult: (`in`.arbait.http.Response) -> Unit) {
    serverApi.verifyUserForLogin(headers, code).enqueue(
      getCallbackObjectShort("verifyUserForLogin", onResult)
    )
  }


  private fun getCallbackObjectShort (
    funcName: String,
    onResult: (`in`.arbait.http.Response) -> Unit): Callback<String>
  {
    return getCallbackObject(
      "$funcName.onFailure",
      "$funcName.response.isSuccessful",
      "NOT SUCCESSFUL $funcName.response, errorBody",
      onResult
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
            this.response = Response(it, response.code())
          }
        }
        onResult(this.response)
      }
    }
  }

}

// преобразуем, например, hourly_job = 1 в hourlyJob = true в нашем ApplicationItem
class IntBooleanDeserializer : JsonDeserializer<Boolean> {

  override fun deserialize (json: JsonElement, typeOfT: Type?,
                            context: JsonDeserializationContext?): Boolean
  {
    return json.asInt == 1
  }

}