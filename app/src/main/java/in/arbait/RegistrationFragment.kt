package `in`.arbait

import `in`.arbait.http.*
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
private const val DATE_DELIMITER1 = '.'
private const val DATE_DELIMITER2 = '-'
private const val DATE_DELIMITER3 = '/'
private const val WORKER_AGE_FROM = 18
private const val WORKER_AGE_UP_TO = 65

private const val FIRST_NAME_MIN_LENGTH = 2
private const val FIRST_NAME_MAX_LENGTH = 20
private const val LAST_NAME_MIN_LENGTH = 2
private const val LAST_NAME_MAX_LENGTH = 20
private const val PASSWORD_MIN_LENGTH = 5
private const val PASSWORD_MAX_LENGTH = 25

private val DEFAULT_EDITTEXT_EMERALD_COLOR = Color.parseColor("#02dac5")


class RegistrationFragment : Fragment() {

  private val server = Server()
  private val repository = UserRepository.get()

  private var registrationFields = mutableListOf<EditText>()
  private var userBirthDate: Date? = null

  private lateinit var birthDateFragmentDialog: BirthDateFragmentDialog
  private lateinit var supportFragmentManager: FragmentManager
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

    registrationFields.add(etFirstName)
    registrationFields.add(etLastName)
    registrationFields.add(etBirthDate)
    registrationFields.add(etPhone)
    registrationFields.add(etPhoneWhatsapp)
    registrationFields.add(etPassword)

    etPhone.addTextChangedListener(PhoneNumberFormattingTextWatcher())
    etPhoneWhatsapp.addTextChangedListener(PhoneNumberFormattingTextWatcher())

    btSamePhone.setOnClickListener(setPhoneWaEqualsToPhone)

    btDone.setOnClickListener {
      val str = etPhoneWhatsapp.text.toString()
      val phoneWa = if (str.isEmpty()) { null } else { str }

      val user = User (
        id = null,
        firstName = etFirstName.text.toString(),
        lastName = etLastName.text.toString(),
        birthDate = etBirthDate.text.toString(),
        phone = etPhone.text.toString(),
        phoneWa = phoneWa,
        password = etPassword.text.toString()
      )
      Log.i (TAG, "User = ${user.toString()}")

      if (inputFieldsAreValid(user)) {
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

      etBirthDate.setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
          createBirthDateDialog()
          setUnderlineColor(etBirthDate, DEFAULT_EDITTEXT_EMERALD_COLOR)
        }
        else {
          setUnderlineColor(etBirthDate, Color.BLACK)
        }
      }

      val withoutBirthDate = true
      setRegistrationFieldsListeners(withoutBirthDate)
    }
    else {
      setRegistrationFieldsListeners()
    }

    this.rootView = view
    return view
  }


  private fun setRegistrationFieldsListeners (withoutBirthDate: Boolean = false) {
    registrationFields.forEach { field ->
      if (!withoutBirthDate || field != etBirthDate) {
        field.setOnClickListener {
          setUnderlineColor(field, DEFAULT_EDITTEXT_EMERALD_COLOR)
        }
        field.setOnFocusChangeListener { _, hasFocus ->
          if (hasFocus) {
            setUnderlineColor(field, DEFAULT_EDITTEXT_EMERALD_COLOR)
          }
          else {
            setUnderlineColor(field, Color.BLACK)
          }
        }
      }
    }
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
      SERVER_OK     -> doOnServerOkResult()
      SYSTEM_ERROR  -> doOnSystemError(response)
      SERVER_ERROR  -> doOnServerError(response)
    }
  }

  private fun doOnServerOkResult() {
    val now = Calendar.getInstance().time
    val user = `in`.arbait.database.User(
      etPhone.text.toString(),
      etPassword.text.toString(),
      isConfirmed = false,
      login = false,
      createdAt = now
    )
    repository.addUser(user)
    val mainActivity = context as MainActivity
    mainActivity.replaceOnFragment("PhoneConfirmationFragment")
  }

  private fun doOnServerError(response: Response) {
    if (response.isItValidationError) {
      Log.i (TAG, "Поле: ${response.errorValidationField}")
      doOnServerFieldValidationError(response)
    }

    val unknownServerError = getString(R.string.unknown_server_error, response.message)
    showErrorBalloon(requireContext(), this.rootView, unknownServerError)
  }

  private fun doOnServerFieldValidationError (response: Response) {
    val errorStr = getString (
      R.string.server_validation_error,
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

  private fun doOnSystemError(response: Response) {
    if (!internetIsAvailable()) {
      showErrorBalloon(requireContext(), this.rootView, R.string.internet_is_not_available)
      return
    }

    val systemError = getString(R.string.system_error, response.message)
    showErrorBalloon(requireContext(), this.rootView, systemError)
  }

  private fun inputFieldsAreValid(user: User): Boolean {
    if (user.firstName.isNullOrEmpty()) {
      return doWhenFieldEmptyOrWrong(etFirstName, R.string.reg_empty_first_name,
        "Укажите имя!")
    }

    if (user.lastName.isNullOrEmpty()) {
      return doWhenFieldEmptyOrWrong(etLastName, R.string.reg_empty_last_name,
        "Укажите фамилию!")
    }

    if (user.phone.isNullOrEmpty()) {
      return doWhenFieldEmptyOrWrong(etPhone, R.string.reg_empty_phone,
        "Укажите номер телефона!")
    }

    if (user.birthDate.isNullOrEmpty()) {
      return doWhenFieldEmptyOrWrong(etBirthDate, R.string.reg_empty_birth_date,
        "Укажите дату рождения!")
    }

    if (user.password.isNullOrEmpty()) {
      val emptyPassword = getString (
        R.string.reg_empty_password,
        PASSWORD_MIN_LENGTH,
        PASSWORD_MAX_LENGTH
      )
      return doWhenFieldEmptyOrWrong(etPassword, emptyPassword,
        "Придумайте пароль длиной от $PASSWORD_MIN_LENGTH до $PASSWORD_MAX_LENGTH символов!")
    }

    if (firstNameIsValid(user.firstName)) {
      if (user.firstName.length < FIRST_NAME_MIN_LENGTH ||
          user.firstName.length > FIRST_NAME_MAX_LENGTH)
      {
        val wrongLength = getString (
          R.string.reg_wrong_first_name_length,
          FIRST_NAME_MIN_LENGTH,
          FIRST_NAME_MAX_LENGTH
        )
        return doWhenFieldEmptyOrWrong(etFirstName, wrongLength,
          "Wrong first name length - ${user.firstName}")
      }
    }
    else {
      return doWhenFieldEmptyOrWrong(etFirstName, R.string.reg_wrong_first_name,
        "Wrong first name - ${user.firstName}")
    }

    if (lastNameIsValid(user.lastName)) {
      if (user.lastName.length < LAST_NAME_MIN_LENGTH ||
        user.lastName.length > LAST_NAME_MAX_LENGTH)
      {
        val wrongLength = getString (
          R.string.reg_wrong_last_name_length,
          LAST_NAME_MIN_LENGTH,
          LAST_NAME_MAX_LENGTH
        )
        return doWhenFieldEmptyOrWrong(etLastName, wrongLength,
          "Wrong last name length - ${user.lastName}")
      }
    }
    else {
      return doWhenFieldEmptyOrWrong(etLastName, R.string.reg_wrong_last_name,
        "Wrong last name - ${user.lastName}")
    }

    if (!dateIsValid(user.birthDate)) {
      return doWhenFieldEmptyOrWrong(etBirthDate, R.string.reg_wrong_birth_date,
        "${user.birthDate} is invalid date, $userBirthDate")
    }
    else {
      val delimiter = getDelimiter(user.birthDate)
      val currentTime = Calendar.getInstance().time
      val currentYear = Calendar.getInstance().get(Calendar.YEAR)

      if (delimiter != null) {
        val year = getFullYear(user.birthDate.substringAfterLast(delimiter))
        val wrongBirthDate = ( (year < currentYear - 100) || (year >= currentYear) )

        Log.i (TAG, "year = $year")

        if (wrongBirthDate) {
          return doWhenFieldEmptyOrWrong(etBirthDate, R.string.reg_wrong_birth_date,
            "Неправильная дата ${user.birthDate}. currentTime.year = $currentYear")
        }
        else {
          setUserBirthDateYear(year)
        }
      }

      val age = getDiffYears(userBirthDate, currentTime)
      Log.i (TAG, "Age is $age")

      if (age in WORKER_AGE_FROM..WORKER_AGE_UP_TO) {
        Log.i (TAG, "${user.birthDate} is valid date, $userBirthDate")
      }
      else {
        val wrongAge = getString (
          R.string.reg_wrong_age,
          WORKER_AGE_FROM,
          WORKER_AGE_UP_TO
        )
        return doWhenFieldEmptyOrWrong(etBirthDate, wrongAge,"У нас принимаются работники" +
            "от $WORKER_AGE_FROM до $WORKER_AGE_UP_TO лет, $userBirthDate")
      }
    }

    if (!phoneNumberIsValid(user.phone, "RU", TAG)) {
      return doWhenFieldEmptyOrWrong(etPhone, R.string.reg_wrong_phone,
        "Wrong phone ${user.phone}")
    }

    if (!passwordIsValid(user.password)) {
      val invalidPassword = getString (
        R.string.reg_invalid_password_length,
        PASSWORD_MIN_LENGTH,
        PASSWORD_MAX_LENGTH
      )
      return doWhenFieldEmptyOrWrong(etPassword, invalidPassword, invalidPassword)
    }

    return true
  }



  private fun getDelimiter(birthDate: String): Char? {
    return when {
      birthDate.indexOf(DATE_DELIMITER1) != -1 -> {
        DATE_DELIMITER1
      }
      birthDate.indexOf(DATE_DELIMITER2) != -1 -> {
        DATE_DELIMITER2
      }
      birthDate.indexOf(DATE_DELIMITER3) != -1 -> {
        DATE_DELIMITER3
      }
      else -> null
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

  private fun setUserBirthDateYear(year: Int) {
    userBirthDate?.let {
      val calendar = getCalendar(userBirthDate)
      calendar.set(year, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
      userBirthDate = calendar.time
    }
  }

  private fun firstNameIsValid (firstName: String): Boolean {
    return firstName.matches("^[а-яА-Я]*$".toRegex())
  }

  private fun lastNameIsValid(lastName: String): Boolean {
    return lastName.matches("^[а-яА-Я]*$".toRegex())
  }

  private fun dateIsValid (dateStr: String): Boolean {
    return  dateFormatIsValid(dateStr, DATE_FORMAT1) ||
            dateFormatIsValid(dateStr, DATE_FORMAT2) ||
            dateFormatIsValid(dateStr, DATE_FORMAT3)
  }

  private fun dateFormatIsValid (dateStr: String, format: String): Boolean {
    val date = strToDate(dateStr, format)
    if (date != null) {
      userBirthDate = date
      return true
    }
    return false
  }

  private fun passwordIsValid(password: String?): Boolean {
    if (password == null) {
      return false
    }

    return password.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH
  }
}