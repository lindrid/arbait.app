package `in`.arbait.http

import android.util.Log
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import java.lang.reflect.InvocationTargetException
import android.R.string
import java.net.URLDecoder


private const val TAG = "http.Response"
private const val ROOT_VALIDATION_ERROR_JSON_STR = "errors"
private const val ROOT_ERROR_WITH_CODE_MSG = "error_msg"

const val SYSTEM_ERROR = 0
const val SERVER_OK = 1
const val SERVER_ERROR = 2

class Response {

  var code: Int? = null
    private set

  var message: String? = null
    private set

  var isItValidationError = false
    private set

  var isItErrorWithCode = false
    private set

  var errorValidationField = ""
    private set


  constructor() {
    code = SERVER_OK
  }

  constructor (response: Response<String>) {
    code = SERVER_OK
    message = response.body()
  }

  constructor (errorBody: ResponseBody, errorCode: Int) {
    code = SERVER_ERROR
    // после первого обращения к errorBody.string(), далее
    // эта команда будет возвращать всегда пустую строку (или null)
    val errorBodyStr = errorBody.string()
    val errorMsg = "Server error code: $errorCode, message: $errorBodyStr"
    Log.i (TAG, errorMsg)
    try {
      // в случае 500 internal server error, следующая операция выбрасывает исключение
      val obj = JSONObject(errorBodyStr)
      isItValidationError = obj.has(ROOT_VALIDATION_ERROR_JSON_STR)
      Log.i (TAG, "isItValidationError = $isItValidationError")
      if (isItValidationError) {
        val pair = getErrorFieldAndMsg(obj)
        pair?.let {
          errorValidationField = pair.first
          message = pair.second
        }
        return
      }

      isItErrorWithCode = obj.has(ROOT_ERROR_WITH_CODE_MSG)
      if (isItErrorWithCode) {
        message = obj.getString(ROOT_ERROR_WITH_CODE_MSG)
        return
      }
    }
    catch (exception: Exception) {}

    message = errorMsg
  }

  constructor (t: Throwable) {
    code = SYSTEM_ERROR
    message = t.message
  }


  private fun getErrorFieldAndMsg (obj: JSONObject): Pair<String, String>? {
    val errors = obj.getJSONObject("errors")
    val fields = errors.names()

    fields?.let {
      if (fields.length() > 0) {
        val field = fields.getString(0)
        var msg = errors.getString(field)
        msg = msg.substring(1, msg.length-2) // убираем скобки [] - в начале и в конце

        return Pair(field, msg)
      }
    }

    return null
  }

}