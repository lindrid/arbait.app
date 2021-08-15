package `in`.arbait

import `in`.arbait.http.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText

private const val TAG = "PhoneConfirmation"

class PhoneConfirmationFragment: Fragment(), View.OnClickListener {

  private val server = Server()
  private val repository = UserRepository.get()
  private var callWasRequested = false

  private lateinit var rootView: View

  private lateinit var etCode: EditText
  private lateinit var btRequestCall: Button


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View?
  {
    val view = inflater.inflate(R.layout.fragment_phone_confirmation, container, false)

    etCode = view.findViewById(R.id.et_phone_conf_code)
    etCode.transformationMethod = NumericKeyBoardTransformationMethod()
    btRequestCall = view.findViewById(R.id.bt_phone_conf_request_call)

    btRequestCall.setOnClickListener(this)

    this.rootView = view
    return view
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.bt_phone_conf_request_call -> {
        onClickRequestCallButton()
      }
    }
  }


  private fun onClickRequestCallButton() {
    server.getIncomingCall { response ->
      when (response.code) {
        SERVER_OK     -> RequestCall().doOnServerOkResult()
        SYSTEM_ERROR  -> RequestCall().doOnSystemError(response)
        SERVER_ERROR  -> RequestCall().doOnServerError(response)
      }
    }
  }

  private inner class RequestCall {

    fun doOnServerOkResult() {
      callWasRequested = true
    }

    fun doOnServerError(response: Response) {
      if (response.isItValidationError) {
        Log.i(TAG, "Поле: ${response.errorValidationField}")
        doOnServerFieldValidationError(response)
      }

      val unknownServerError = getString(R.string.unknown_server_error, response.message)
      showErrorBalloon(requireContext(), rootView, unknownServerError)
    }

    private fun doOnServerFieldValidationError(response: Response) {
      val errorStr = getString(
        R.string.reg_server_validation_error,
        response.errorValidationField,
        response.message
      )
      when (response.errorValidationField) {
        "code" -> {
          showErrorBalloon(requireContext(), etCode, errorStr)
        }
      }
      return
    }

    fun doOnSystemError(response: Response) {
      if (!internetIsAvailable()) {
        showErrorBalloon(requireContext(), rootView, R.string.internet_is_not_available)
        return
      }

      val systemError = getString(R.string.system_error, response.message)
      showErrorBalloon(requireContext(), rootView, systemError)
    }

  }

}

private class NumericKeyBoardTransformationMethod: PasswordTransformationMethod() {
  override fun getTransformation(source: CharSequence, view: View?): CharSequence {
    return source
  }
}