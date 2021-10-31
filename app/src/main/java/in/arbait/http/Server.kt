package `in`.arbait.http

import `in`.arbait.APP_VERSION
import `in`.arbait.http.response.ApplicationUserResponse
import `in`.arbait.http.response.ServiceDataResponse
import `in`.arbait.http.response.UserResponse
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

class Server (private val context: Context)
{
  val serviceDataResponse: MutableLiveData<ServiceDataResponse> = MutableLiveData()

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

  fun updateApplicationsResponse (cache: Boolean = true) {
    val intCache = if (cache) 1 else 0
    serverApi.getData(headers, APP_VERSION, intCache).enqueue (
      object : Callback<ServiceDataResponse> {

        override fun onFailure (call: Call<ServiceDataResponse>, t: Throwable) {
          Log.e (TAG, "getAppList FAILED!", t)
          serviceDataResponse.value = ServiceDataResponse(`in`.arbait.http.response.Response(t))
        }

        override fun onResponse (call: Call<ServiceDataResponse>,
                                 response: Response<ServiceDataResponse>)
        {
          if (response.code() == 200) {
            Log.i(TAG, "Response received, response.body() = ${response.body()}")
            val appsResponse: ServiceDataResponse? = response.body()
            serviceDataResponse.value = appsResponse ?: ServiceDataResponse()
          }
          else {
            Log.e (TAG, "Server error with code ${response.code()}")
            response.errorBody()?.let {
              serviceDataResponse.value = ServiceDataResponse(`in`.arbait.http.response.Response(it, response.code()))
            }
          }
        }

      }
    )
  }

  fun changeDebitCard ( appId: Int, debitCardId: Int?, debitCard: String?,
                        onResult: (ApplicationUserResponse) -> Unit)
  {
    appUserCallback.onResult = onResult
    serverApi.changeDebitCard(headers, appId, debitCardId, debitCard).enqueue(appUserCallback)
  }

  fun enrollPorter (appId: Int, onResult: (ApplicationUserResponse) -> Unit) {
    appUserCallback.onResult = onResult
    serverApi.enrollPorter(headers, appId).enqueue(appUserCallback)
  }

  fun refuseApp (appId: Int, onResult: (ApplicationUserResponse) -> Unit) {
    appUserCallback.onResult = onResult
    serverApi.refuseApp(headers, appId).enqueue (appUserCallback)
  }

  fun registerUser (user: User, onResult: (`in`.arbait.http.response.Response) -> Unit) {
    serverApi.register(headers, user).enqueue (
      getCallbackObjectShort("registerUser", onResult)
    )
  }

  fun loginUser (phone: String, onResult: (`in`.arbait.http.response.Response) -> Unit) {
    serverApi.login(headers, phone).enqueue (
      getCallbackObjectShort("loginUser", onResult)
    )
  }

  fun getIncomingCall (onResult: (`in`.arbait.http.response.Response) -> Unit) {
    serverApi.sendVerRequest(headers).enqueue (
      getCallbackObjectShort("getIncomingCall", onResult)
    )
  }

  fun verifyUser (code: String, onResult: (UserResponse) -> Unit) {
    userCallback.onResult = onResult
    serverApi.verifyUser(headers, code).enqueue(userCallback)
  }

  fun verifyUserForLogin (code: String, onResult: (UserResponse) -> Unit) {
    userCallback.onResult = onResult
    serverApi.verifyUserForLogin(headers, code).enqueue(userCallback)
  }

  private fun getCallbackObjectShort (
    funcName: String,
    onResult: (`in`.arbait.http.response.Response) -> Unit): Callback<String>
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
    onResult: (`in`.arbait.http.response.Response) -> Unit): Callback<String>
  {
    return object : Callback<String> {
      private lateinit var response: `in`.arbait.http.response.Response

      override fun onFailure(call: Call<String>, t: Throwable) {
        Log.d (TAG, "$msgOnFailure: ${t.message}, $t")
        response = `in`.arbait.http.response.Response(t)
        onResult(response)
      }
      override fun onResponse(call: Call<String>, response: Response<String>) {
        if (response.isSuccessful) {
          Log.d (TAG, "$msgOnSuccessful: $response")
          this.response = `in`.arbait.http.response.Response(response)
        }
        else {
          response.errorBody()?.let {
            Log.d (TAG, "$msgOnServerError: $it")
            this.response = `in`.arbait.http.response.Response(it, response.code())
          }
        }
        onResult(this.response)
      }
    }
  }

  private val userCallback = object : Callback<UserResponse>
  {
    lateinit var onResult: (UserResponse) -> Unit

    override fun onFailure (call: Call<UserResponse>, t: Throwable) {
      Log.e (TAG, "verify user FAILED!", t)
      onResult(UserResponse(`in`.arbait.http.response.Response(t)))
    }

    override fun onResponse (call: Call<UserResponse>,
                             response: Response<UserResponse>)
    {
      if (response.code() == 200) {
        Log.i(TAG, "UserResponse received, response.body() = ${response.body()}")
        val userResponse: UserResponse? = response.body()
        onResult(userResponse ?: UserResponse())
      }
      else {
        Log.e (TAG, "Server error with code ${response.code()}")
        response.errorBody()?.let {
          onResult(UserResponse(`in`.arbait.http.response.Response(it, response.code())))
        }
      }
    }
  }

  private val appUserCallback = object : Callback<ApplicationUserResponse>
  {
    lateinit var onResult: (ApplicationUserResponse) -> Unit

    override fun onFailure (call: Call<ApplicationUserResponse>, t: Throwable) {
      Log.e (TAG, "Enroll porter FAILED!", t)
      onResult(ApplicationUserResponse(`in`.arbait.http.response.Response(t)))
    }

    override fun onResponse (call: Call<ApplicationUserResponse>,
                             response: Response<ApplicationUserResponse>)
    {
      if (response.code() == 200) {
        Log.i(TAG, "ApplicationResponse received, response.body() = ${response.body()}")
        val appUserResponse: ApplicationUserResponse? = response.body()
        onResult(appUserResponse ?: ApplicationUserResponse())
      }
      else {
        Log.e (TAG, "Server error with code ${response.code()}")
        response.errorBody()?.let {
          onResult(ApplicationUserResponse(`in`.arbait.http.response.Response(it, response.code())))
        }
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