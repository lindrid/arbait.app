package `in`.arbait

import `in`.arbait.database.User
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
private const val CODE_LENGTH = 4

class PhoneConfirmationFragment (private val user: User): Fragment(), View.OnClickListener {

  private lateinit var server: Server
  private val repository = UserRepository.get()

  private lateinit var rootView: View

  private lateinit var etCode: EditText
  private lateinit var btRequestCall: Button
  private lateinit var btDone: Button


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
    btDone = view.findViewById(R.id.bt_phone_conf_done)

    btRequestCall.setOnClickListener(this)
    btDone.setOnClickListener(this)

    server = Server(requireContext())
    this.rootView = view
    return view
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.bt_phone_conf_request_call -> {
        onClickRequestCallButton()
      }
      R.id.bt_phone_conf_done -> {
        onClickDoneButton()
      }
    }
  }


  private fun onClickDoneButton() {
    if (!user.callReceived) {
      showErrorBalloon(requireContext(), rootView, R.string.phone_conf_not_requested_call)
      return
    }

    val code = etCode.text.toString()
    Log.i(TAG, "code: $code, code.isEmpty() = ${code.isEmpty()}")

    if (code.isEmpty()) {
      showValidationError(requireContext(), etCode, R.string.phone_conf_empty_code)
      return
    }

    if (code.length != CODE_LENGTH) {
      showValidationError(requireContext(), etCode, R.string.phone_conf_wrong_length)
      return
    }

    server.verifyUser(code) { response ->
      val verifyUser = VerifyUser(response)
      when (response.code) {
        SERVER_OK     -> verifyUser.doOnServerOkResult()
        SYSTEM_ERROR  -> verifyUser.doOnSystemError()
        SERVER_ERROR  -> verifyUser.doOnServerError()
      }
    }
  }

  private inner class VerifyUser (response: Response):
    ReactionOnResponse (TAG, requireContext(), rootView, response)
  {
    override fun doOnServerOkResult() {
      Log.i(TAG, "все ок, пользователь зарегистрирован")
      user.isConfirmed = true
      repository.updateUser(user)
      val mainActivity = requireActivity() as MainActivity
      mainActivity.replaceOnFragment("Applications")
    }

    override fun doOnServerFieldValidationError(response: Response) {
      val errorStr = getString (
        R.string.server_validation_error,
        response.errorValidationField,
        response.message
      )
      showErrorBalloon(requireContext(), etCode, errorStr)
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
    ReactionOnResponse (TAG, requireContext(), rootView, response)
  {
    override fun doOnServerOkResult() {
      user.callReceived = true
      repository.updateUser(user)
    }

    override fun doOnServerFieldValidationError(response: Response) {

    }
  }

}


// т.к. android:inputType="numberPassword", чтобы ввод цифр не превращался в *
// нужно сделать свою трансформацию, которая показывает цифры введенными как есть
// (а не заменяет их на звездочки, что делает стандартная трансформация для Password)

private class NumericKeyBoardTransformationMethod: PasswordTransformationMethod()
{
  override fun getTransformation(source: CharSequence, view: View?): CharSequence
  {
    return source
  }
}