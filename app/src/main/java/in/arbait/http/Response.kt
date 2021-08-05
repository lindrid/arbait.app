package `in`.arbait.http

import android.util.Log
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response

private const val TAG = "http.Response"

const val SYSTEM_ERROR = 0
const val SERVER_OK = 1
const val SERVER_ERROR = 2

class Response {

  var code: Int? = null
    private set

  var message: String? = null
    private set

  constructor (response: Response<String>) {
    code = SERVER_OK
    message = response.body()
  }

  constructor (error: String){
    code = SERVER_ERROR
    val obj = JSONObject(error)
    if (isItValidationError(obj)) {
      message = getErrorMsg(obj)
    }
  }

  constructor (t: Throwable) {
    code = SYSTEM_ERROR
    message = t.message
  }

  private fun isItValidationError (obj: JSONObject): Boolean {
    return obj.has("errors")
  }

  private fun getErrorMsg (obj: JSONObject): String? {
    val errors = obj.getJSONObject("errors")
    val keys = errors.names()

    if (keys.length() > 0) {
      val key = keys.getString(0)     // Here's your key
      val str = errors.getString(key)       // Here's your value
      return str.substring(1, str.length-2) // убираем скобки [] - в начале и в конце
    }

    return null
  }
}