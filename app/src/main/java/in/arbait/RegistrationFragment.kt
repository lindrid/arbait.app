package `in`.arbait

import `in`.arbait.http.ReactionOnResponse
import `in`.arbait.http.Server
import `in`.arbait.http.User
import `in`.arbait.http.response.Response
import `in`.arbait.http.response.SERVER_ERROR
import `in`.arbait.http.response.SERVER_OK
import `in`.arbait.http.response.SYSTEM_ERROR
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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

class RegistrationFragment : Fragment() {

  private lateinit var server: Server
  private val repository = Repository.get()

  private var registrationFields = mutableListOf<EditText>()
  private var userBirthDate: Date? = null

  private lateinit var birthDateFragmentDialog: BirthDateFragmentDialog
  private lateinit var supportFragmentManager: FragmentManager
  private lateinit var rootView: View

  private lateinit var tvRegistration: TextView
  private lateinit var etFirstName: EditText
  private lateinit var etBirthDate: EditText
  private lateinit var etPhone: MonitoringEditText
  private lateinit var etPhoneWa: MonitoringEditText
  private lateinit var btSamePhone: AppCompatButton
  private lateinit var btDone: Button

  private val setPhoneWaEqualsToPhone = { _: View ->
    etPhoneWa.text = etPhone.text
  }


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_registration, container, false)
    supportFragmentManager = requireActivity().supportFragmentManager
    server = Server(requireContext())
    this.rootView = view

    tvRegistration = view.findViewById(R.id.tv_reg_registration)
    etFirstName = view.findViewById(R.id.et_reg_first_name)
    etBirthDate = view.findViewById(R.id.et_reg_birth_date)
    etPhone = view.findViewById(R.id.et_reg_phone) as MonitoringEditText
    etPhoneWa = view.findViewById(R.id.et_reg_phone_wa) as MonitoringEditText
    btSamePhone = view.findViewById(R.id.bt_reg_same_phone)
    btDone = view.findViewById(R.id.bt_reg_done)

    registrationFields.add(etFirstName)
    registrationFields.add(etBirthDate)
    registrationFields.add(etPhone)
    registrationFields.add(etPhoneWa)

    etPhone.addTextChangedListener(PhoneFormatWatcher(etPhone, viewLifecycleOwner))
    etPhoneWa.addTextChangedListener(PhoneFormatWatcher(etPhoneWa, viewLifecycleOwner))

    btSamePhone.setOnClickListener(setPhoneWaEqualsToPhone)

    btDone.setOnClickListener {
      val str = etPhoneWa.text.toString()
      val phoneWa = if (str.isEmpty()) { null } else { str }

      val user = User (
        id = null,
        firstName = etFirstName.text.toString(),
        birthDate = etBirthDate.text.toString(),
        phone = etPhone.text.toString(),
        phoneWa = phoneWa
      )
      Log.i (TAG, "User = ${user.toString()}")

      if (inputFieldsAreValid(user)) {
        server.registerUser(user) { response: Response ->
          onResult(response)
        }
      }
    }

    Log.i (TAG, "manufacturer is $MANUFACTURER")
    Log.i (TAG, "Android version is $VERSION")


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


    setHasOptionsMenu(true)

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    actionBar?.title = "$appName"
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.registration_menu, menu)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.bt_reg_login) {
      val mainActivity = requireActivity() as MainActivity
      mainActivity.replaceOnFragment("Login")
    }
    return super.onOptionsItemSelected(item)
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
    when (response.type) {
      SERVER_OK     -> RegisterReaction(response).doOnServerOkResult()
      SYSTEM_ERROR  -> RegisterReaction(response).doOnSystemError()
      SERVER_ERROR  -> RegisterReaction(response).doOnServerError()
    }
  }

  private inner class RegisterReaction (response: Response):
    ReactionOnResponse (TAG, requireContext(), rootView, response)
  {
    override fun doOnServerOkResult() {
      val now = Calendar.getInstance().time
      val user = `in`.arbait.database.User(
        0,
        etPhone.text.toString(),
        isConfirmed = false,
        login = false,
        isItRegistration = true,
        createdAt = now
      )
      val added = repository.addUser(user)
      Log.i (TAG, "added = $added")
      if (!added)
        repository.updateUser(user)

      val args = Bundle().apply {
        putBoolean(VERIFY_FOR_LOGIN_ARG, false)
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
        "first_name" -> {
          showErrorBalloon(requireContext(), etFirstName, errorStr)
        }
        "birth_date" -> {
          showErrorBalloon(requireContext(), etBirthDate, errorStr)
        }
        "phone" -> {
          showErrorBalloon(requireContext(), etPhone, errorStr)
        }
        "phone_wa" -> {
          showErrorBalloon(requireContext(), etPhoneWa, errorStr)
        }
      }
    }

    override fun doOnEndSessionError() {}
  }

  private fun inputFieldsAreValid(user: User): Boolean {
    if (user.firstName.isNullOrEmpty()) {
      return doWhenFieldEmptyOrWrong(etFirstName, R.string.reg_empty_first_name,
        "Укажите имя!")
    }

    if (user.phone.isNullOrEmpty()) {
      return doWhenFieldEmptyOrWrong(etPhone, R.string.empty_phone,
        "Укажите номер телефона!")
    }

    if (user.birthDate.isNullOrEmpty()) {
      return doWhenFieldEmptyOrWrong(etBirthDate, R.string.reg_empty_birth_date,
        "Укажите дату рождения!")
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
      return doWhenFieldEmptyOrWrong(etPhone, R.string.wrong_phone,
        "Wrong phone ${user.phone}")
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
}