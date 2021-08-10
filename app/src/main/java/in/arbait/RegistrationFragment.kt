package `in`.arbait

import `in`.arbait.http.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.util.*

const val BIRTH_DATE_KEY = "birthDate"

private const val TAG = "RegistrationFragment"
private const val BIRTH_DATE_DIALOG_TAG = "BirthDateFragmentDialog"

private const val DEFAULT_BIRTH_DATE = "15.06.1995"
private const val DATE_FORMAT1 = "dd.MM.yyyy"
private const val DATE_FORMAT2 = "dd-MM-yyyy"
private const val DATE_FORMAT3 = "dd/MM/yyyy"
private const val WORKER_AGE_FROM = 18
private const val WORKER_AGE_UP_TO = 65

private const val PASSWORD_MIN_LENGTH = 5
private const val PASSWORD_MAX_LENGTH = 25

private val DEFAULT_EDITTEXT_EMERALD_COLOR = Color.parseColor("#02dac5")


class RegistrationFragment : Fragment() {

  private val server = Server()

  private lateinit var rootView: View
  private lateinit var tvRegistration: TextView
  private lateinit var etFirstName: EditText
  private lateinit var etLastName: EditText
  private lateinit var etBirthDate: EditText
  private lateinit var etPhone: EditText
  private lateinit var etPhoneWhatsapp: EditText
  private lateinit var btSamePhone: Button
  private lateinit var etPassword: EditText
  private lateinit var btDone: Button

  private var userBirthDate: Date? = null
  private lateinit var birthDateFragmentDialog: BirthDateFragmentDialog

  private lateinit var supportFragmentManager: FragmentManager
  private var defaultBackgroundTintList: ColorStateList? = null

  private val setPhoneWaEqualsToPhone = { _: View ->
    etPhoneWhatsapp.text = etPhone.text
  }


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_registration, container, false)
    supportFragmentManager = requireActivity().supportFragmentManager

    tvRegistration = view.findViewById(R.id.tv_reg_registration)

    etFirstName = view.findViewById(R.id.et_reg_first_name)
    etLastName = view.findViewById(R.id.et_reg_last_name)
    etBirthDate = view.findViewById(R.id.et_reg_birth_date)
    etPhone = view.findViewById(R.id.et_reg_phone)
    etPhoneWhatsapp = view.findViewById(R.id.et_reg_phone_whatsapp)
    btSamePhone = view.findViewById(R.id.bt_reg_same_phone)
    etPassword = view.findViewById(R.id.et_reg_password)
    btDone = view.findViewById(R.id.bt_reg_done)

    etPhone.addTextChangedListener(PhoneNumberFormattingTextWatcher())
    etPhoneWhatsapp.addTextChangedListener(PhoneNumberFormattingTextWatcher())

    btSamePhone.setOnClickListener(setPhoneWaEqualsToPhone)

    btDone.setOnClickListener {
      val user = User (
        id = null,
        firstName = etFirstName.text.toString(),
        lastName = etLastName.text.toString(),
        birthDate = etBirthDate.text.toString(),
        phone = etPhone.text.toString(),
        phoneWa = etPhoneWhatsapp.text.toString(),
        password = etPassword.text.toString()
      )
      Log.i (TAG, "User = ${user.toString()}")

      if (isInputValid(user)) {
        server.registerUser(user) { response: Response ->
          onResult(response)
        }
      }
    }

    etFirstName.setText("Дмитрий")
    etLastName.setText("Федоров")
    etBirthDate.setText("08.06.1987")
    etPhone.setText("89240078897")
    etPhoneWhatsapp.setText("89240078897")
    etPassword.setText("12")

    Log.i (TAG, "manufacturer is $MANUFACTURER")
    Log.i (TAG, "Android version is $VERSION")

    if (isSamsung() && versionIsNineOrGreater()) {
      Log.i(TAG, "Manufacturer is samsung and version >= 9")

      setBirthDateEditTextWhenDialogResult()

      etBirthDate.setOnClickListener {
        createBirthDateDialog()
        setUnderlineColor(etBirthDate, DEFAULT_EDITTEXT_EMERALD_COLOR)
      }

      etBirthDate.setOnFocusChangeListener { view, hasFocus ->
        if (hasFocus) {
          createBirthDateDialog()
          setUnderlineColor(etBirthDate, DEFAULT_EDITTEXT_EMERALD_COLOR)
        }
        else {
          setUnderlineColor(etBirthDate, Color.BLACK)
        }
      }
    }
    else {
      etBirthDate.setOnClickListener {
        setUnderlineColor(etBirthDate, DEFAULT_EDITTEXT_EMERALD_COLOR)
      }

      etBirthDate.setOnFocusChangeListener { view, hasFocus ->
        if (hasFocus) {
          setUnderlineColor(etBirthDate, DEFAULT_EDITTEXT_EMERALD_COLOR)
        }
        else {
          setUnderlineColor(etBirthDate, Color.BLACK)
        }
      }
    }

    this.rootView = view
    return view
  }


  private fun setBirthDateEditTextWhenDialogResult() {
    supportFragmentManager.setFragmentResultListener(BIRTH_DATE_KEY, viewLifecycleOwner)
    { _, bundle ->
      etBirthDate.setText(bundle.getString(BIRTH_DATE_KEY))
    }
  }

  private fun createBirthDateDialog() {
    val date = getValidBirthDateForSamsung9()
    birthDateFragmentDialog = BirthDateFragmentDialog.newInstance(date)
    birthDateFragmentDialog.show(supportFragmentManager, BIRTH_DATE_DIALOG_TAG)
  }

  private fun getValidBirthDateForSamsung9(): Date {
    val dateStr = etBirthDate.text.toString()
    val date = strToDate(dateStr, DATE_FORMAT1)
    if (date != null) {
      return date
    }

    return strToDate(DEFAULT_BIRTH_DATE, DATE_FORMAT1)!!
  }

  private fun onResult (response: Response) {
    when (response.code) {
      SERVER_OK -> {
        Log.i (TAG,"Все ок, сервер вернул: ${response.message}")
      }
      else -> {
        Log.i (TAG,"Регистрация не прошла, error is ${response.message}")
        if (response.isItValidationError) {
          Log.i (TAG, "Поле: ${response.errorValidationField}")
          val errorStr = getString (
            R.string.reg_server_validation_error,
            response.errorValidationField,
            response.message
          )
          when (response.errorValidationField) {
            "first_name" -> {
              showErrorBalloon(requireContext(), etFirstName, errorStr)
            }
            "last_name" -> {
              showErrorBalloon(requireContext(), etLastName, errorStr)
            }
            "birth_date" -> {
              showErrorBalloon(requireContext(), etBirthDate, errorStr)
            }
            "phone" -> {
              showErrorBalloon(requireContext(), etPhone, errorStr)
            }
            "phone_wa" -> {
              showErrorBalloon(requireContext(), etPhoneWhatsapp, errorStr)
            }
            "password" -> {
              showErrorBalloon(requireContext(), etPassword, errorStr)
            }
          }
          return
        }

        if (!isInternetAvailable()) {
          showErrorBalloon(requireContext(), this.rootView, R.string.internet_is_not_available)
          return
        }

        val systemError = getString(R.string.system_error, response.message)
        showErrorBalloon(requireContext(), this.rootView, systemError)
      }
    }
  }

  private fun isInputValid(user: User): Boolean {
    if (user.birthDate.isNullOrEmpty()) {
      Log.i (TAG, "Укажите дату рождения!")
      defaultBackgroundTintList = etBirthDate.backgroundTintList
      showValidationError(requireContext(), etBirthDate, R.string.reg_empty_birth_date)
      return false
    }

    if (isValidDate(user.birthDate)) {
      val currentTime = Calendar.getInstance().time
      val age = getDiffYears(userBirthDate, currentTime)

      Log.i (TAG, "Age is $age")

      if (age in WORKER_AGE_FROM..WORKER_AGE_UP_TO) {
        Log.i (TAG, "${user.birthDate} is valid date, $userBirthDate")
      }
      else {
        Log.i (TAG, "У нас принимаются работники от $WORKER_AGE_FROM до" +
            "$WORKER_AGE_UP_TO лет, $userBirthDate")
        val error = getString(R.string.reg_wrong_age, WORKER_AGE_FROM, WORKER_AGE_UP_TO)
        showValidationError(requireContext(), etBirthDate, error)
        return false
      }
    }
    else {
      Log.i (TAG, "${user.birthDate} is invalid date, $userBirthDate")
      showValidationError(requireContext(), etBirthDate, R.string.reg_wrong_birth_date)
      return false
    }

    if (!isValidPassword(user.password)) {
      val invalidPassword = getString (
        R.string.reg_invalid_password_length,
        PASSWORD_MIN_LENGTH,
        PASSWORD_MAX_LENGTH
      )
      showValidationError(requireContext(), etPassword, invalidPassword)
      return false
    }

    return true
  }


  private fun isValidPassword(password: String?): Boolean {
    if (password == null) {
      return false
    }

    return password.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH
  }

  private fun isValidDate (dateStr: String): Boolean {
    return  isValidFormatDate(dateStr, DATE_FORMAT1) ||
            isValidFormatDate(dateStr, DATE_FORMAT2) ||
            isValidFormatDate(dateStr, DATE_FORMAT3)
  }

  private fun isValidFormatDate (dateStr: String, format: String): Boolean {
    val date = strToDate(dateStr, format)
    if (date != null) {
      userBirthDate = date
      return true
    }
    return false
  }
}