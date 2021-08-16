package `in`.arbait.http

import android.content.Context
import android.preference.PreferenceManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

private const val COOKIES_KEY = "appCookies"


class SendSavedCookiesInterceptor(private val context: Context) : Interceptor {

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val builder = chain.request().newBuilder()
    val preferences = PreferenceManager
      .getDefaultSharedPreferences(context)
      .getStringSet(COOKIES_KEY, HashSet()) as HashSet<String>

    preferences.forEach {
      builder.addHeader("Cookie", it)
    }

    return chain.proceed(builder.build())
  }
}

class SaveReceivedCookiesInterceptor(private val context: Context) : Interceptor {

  @JvmField
  val setCookieHeader = "Set-Cookie"

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalResponse = chain.proceed(chain.request())

    if (originalResponse.headers(setCookieHeader).isNotEmpty()) {
      val cookies = PreferenceManager
        .getDefaultSharedPreferences(context)
        .getStringSet(COOKIES_KEY, HashSet()) as HashSet<String>

      originalResponse.headers(setCookieHeader).forEach {
        cookies.add(it)
      }

      PreferenceManager
        .getDefaultSharedPreferences(context)
        .edit()
        .putStringSet(COOKIES_KEY, cookies)
        .apply()
    }

    return originalResponse
  }

}
