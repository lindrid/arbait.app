package `in`.arbait

import `in`.arbait.http.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText

private const val TAG = "PhoneConfirmation"

class PhoneConfirmationFragment: Fragment(), View.OnClickListener {

  private lateinit var server: Server
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

    server = Server(requireContext())
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
      val requestCall = RequestCall(response)
      when (response.code) {
        SERVER_OK     -> requestCall.doOnServerOkResult()
        SYSTEM_ERROR  -> requestCall.doOnSystemError()
        SERVER_ERROR  -> requestCall.doOnServerError()
      }
    }
  }

  private inner class RequestCall (response: Response):
    ReactionOnServerResponse (TAG, requireContext(), rootView, response)
  {
    override fun doOnServerOkResult() {
      callWasRequested = true
    }

    override fun doOnServerFieldValidationError(response: Response) {

    }
  }

}

private class NumericKeyBoardTransformationMethod: PasswordTransformationMethod() {
  override fun getTransformation(source: CharSequence, view: View?): CharSequence {
    return source
  }
}