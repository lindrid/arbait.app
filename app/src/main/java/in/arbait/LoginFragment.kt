package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.*
import `in`.arbait.http.response.Response
import `in`.arbait.http.response.SERVER_ERROR
import `in`.arbait.http.response.SERVER_OK
import `in`.arbait.http.response.SYSTEM_ERROR
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import java.util.*

private const val TAG = "LoginFragment"

const val VERIFY_FOR_LOGIN_ARG = "verifyForLogin"

class LoginFragment: Fragment()
{
  private lateinit var server: Server
  private lateinit var rootView: View

  private lateinit var tvLogin: AppCompatTextView
  private lateinit var etPhone: MonitoringEditText
  private lateinit var btDone: AppCompatButton

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_login, container, false)
    rootView = view
    server = Server(requireContext())

    setViews(view)

    setHasOptionsMenu(true)

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    actionBar?.title = appName
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.login_menu, menu)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.bt_login_reg) {
      val mainActivity = requireActivity() as MainActivity
      mainActivity.replaceOnFragment("Registration")
    }
    return super.onOptionsItemSelected(item)
  }


  private fun setViews(view: View) {
    tvLogin = view.findViewById(R.id.tv_log_login)
    etPhone = view.findViewById(R.id.et_log_phone)
    etPhone.addTextChangedListener(PhoneFormatWatcher(etPhone, viewLifecycleOwner))

    btDone = view.findViewById(R.id.bt_log_done)

    btDone.setOnClickListener {
      onBtDoneClick()
    }

    etPhone.setOnClickListener {
      setUnderlineColor(etPhone, DEFAULT_EDITTEXT_EMERALD_COLOR)
    }

    etPhone.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) {
        setUnderlineColor(etPhone, DEFAULT_EDITTEXT_EMERALD_COLOR)
      }
      else {
        setUnderlineColor(etPhone, Color.BLACK)
      }
    }
  }

  private fun onBtDoneClick() {
    val phone = etPhone.text.toString()

    if (phone.isNullOrEmpty()) {
      doWhenFieldEmptyOrWrong(etPhone, R.string.empty_phone, "Укажите номер телефона!")
      return
    }

    if (!phoneNumberIsValid(phone, "RU", TAG)) {
      doWhenFieldEmptyOrWrong(etPhone, R.string.wrong_phone, "Wrong phone $phone")
      return
    }

    server.loginUser(phone) {
      onResult(it)
    }
  }

  private fun onResult (response: Response) {
    when (response.type) {
      SERVER_OK     -> LoginReaction(response).doOnServerOkResult()
      SYSTEM_ERROR  -> LoginReaction(response).doOnSystemError()
      SERVER_ERROR  -> LoginReaction(response).doOnServerError()
    }
  }

  private fun doWhenFieldEmptyOrWrong(field: EditText, errorStrResource: Int,
                                      logStr: String): Boolean
  {
    return doWhenFieldEmptyOrWrong(field, getString(errorStrResource), logStr)
  }

  private fun doWhenFieldEmptyOrWrong(field: EditText, errorStr: String,
                                      logStr: String): Boolean
  {
    Log.i (TAG, logStr)
    showValidationError(requireContext(), field, errorStr)
    return false
  }

  private inner class LoginReaction (response: Response):
    ReactionOnResponse(TAG, requireContext(), rootView, response)
  {
    override fun doOnServerOkResult() {
      if (App.dbUser == null) {
        App.dbUser = User(0, "")
        App.dbUser?.let { user ->
          App.repository.addUser(user)
        }
      }
      App.dbUser?.let {
        // чтобы отличать от того случая, когда пользователь хочет не залогиниться,
        // а зарегистрироваться: it.login = true
        it.login = true
        it.isConfirmed = false
        App.repository.updateUser(it)
      }
      var args = Bundle().apply {
        putBoolean(VERIFY_FOR_LOGIN_ARG, true)
      }
      val mainActivity = context as MainActivity
      mainActivity.replaceOnFragment("PhoneConfirmation", args)
    }

    override fun doOnServerFieldValidationError(response: Response) {
      val errorStr = getString (
        R.string.server_validation_error,
        response.errorValidationField,
        response.message
      )
      when (response.errorValidationField) {
        "phone" -> {
          showErrorBalloon(requireContext(), etPhone, errorStr)
        }
      }
    }

    override fun doOnEndSessionError() {}
  }

}