package `in`.arbait.http

import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response

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


  constructor (response: Response<String>) {
    code = SERVER_OK
    message = response.body()
  }

  constructor (errorBody: ResponseBody) {
    code = SERVER_ERROR
    val obj = JSONObject(errorBody.string())
    errorBody.close()
    isItValidationError = obj.has(ROOT_VALIDATION_ERROR_JSON_STR)
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
    }
  }

  constructor (t: Throwable) {
    code = SYSTEM_ERROR
    message = t.message
  }

  fun parseJsonArray (paramName: String) {
    if (code == SERVER_OK) {
      val json = JSONObject(message)
      val array = json.getJSONArray(paramName)
    }
  }


  private fun getErrorFieldAndMsg (obj: JSONObject): Pair<String, String>? {
    val errors = obj.getJSONObject("errors")
    val fields = errors.names()

    if (fields.length() > 0) {
      val field = fields.getString(0)
      var msg = errors.getString(field)
      msg = msg.substring(1, msg.length-2) // убираем скобки [] - в начале и в конце

      return Pair(field, msg)
    }

    return null
  }

}