package `in`.arbait.http.response.callback

import `in`.arbait.http.TAG
import `in`.arbait.http.response.ApplicationResponse
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

val appCallback = object : Callback<ApplicationResponse>
{
  lateinit var onResult: (ApplicationResponse) -> Unit

  override fun onFailure (call: Call<ApplicationResponse>, t: Throwable) {
    Log.e (TAG, "verify user FAILED!", t)
    onResult(ApplicationResponse(`in`.arbait.http.response.Response(t)))
  }

  override fun onResponse (call: Call<ApplicationResponse>,
                           response: Response<ApplicationResponse>)
  {
    if (response.code() == 200) {
      Log.i(TAG, "UserResponse received, response.body() = ${response.body()}")
      val appResponse: ApplicationResponse? = response.body()
      onResult(appResponse ?: ApplicationResponse())
    }
    else {
      Log.e (TAG, "Server error with code ${response.code()}")
      response.errorBody()?.let {
        onResult(ApplicationResponse(`in`.arbait.http.response.Response(it, response.code())))
      }
    }
  }
}