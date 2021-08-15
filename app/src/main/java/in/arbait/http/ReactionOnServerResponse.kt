package `in`.arbait.http

import `in`.arbait.App
import `in`.arbait.R
import `in`.arbait.internetIsAvailable
import `in`.arbait.showErrorBalloon
import android.content.Context
import android.util.Log
import android.view.View

abstract class ReactionOnServerResponse (
  private val TAG: String,
  private val context: Context,
  private val view: View,
  private val response: Response
) {

  abstract fun doOnServerOkResult()

  fun doOnServerError() {
    if (response.isItValidationError) {
      Log.i(TAG, "Поле: ${response.errorValidationField}")
      doOnServerFieldValidationError(response)
    }

    App.res?.let {
      val unknownServerError = it.getString(R.string.unknown_server_error, response.message)
      showErrorBalloon(context, view, unknownServerError)
    }
  }

  fun doOnSystemError() {
    if (!internetIsAvailable()) {
      showErrorBalloon(context, view, R.string.internet_is_not_available)
      return
    }

    App.res?.let {
      val systemError = it.getString(R.string.system_error, response.message)
      showErrorBalloon(context, view, systemError)
    }
  }

  abstract fun doOnServerFieldValidationError(response: Response)

  fun serverValidationError(): String {
    App.res?.let {
      return it.getString(
        R.string.server_validation_error,
        response.errorValidationField,
        response.message
      )
    }
    return "ServerValidationError"
  }
}