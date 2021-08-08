package `in`.arbait

import `in`.arbait.http.*
import android.graphics.Color
import android.graphics.Color.red
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.createBalloon
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*

private const val TAG = "RegistrationFragment"

private const val DATE_FORMAT1 = "dd.MM.yyyy"
private const val DATE_FORMAT2 = "dd-MM-yyyy"
private const val DATE_FORMAT3 = "dd/MM/yyyy"
private const val WORKER_AGE_FROM = 18
private const val WORKER_AGE_UP_TO = 65

class RegistrationFragment : Fragment() {

  private val server = Server()

  private lateinit var tvRegistration: TextView
  private lateinit var etFirstName: EditText
  private lateinit var etLastName: EditText
  private lateinit var etBirthDate: EditText
  private lateinit var etPhone: EditText
  private lateinit var etPhoneWhatsapp: EditText
  private lateinit var btSamePhone: Button
  private lateinit var etPassword: EditText
  private lateinit var btDone: Button

  private lateinit var balloon: Balloon
  private lateinit var userBirthDate: Date
  private lateinit var birthDateFragmentDialog: BirthDateFragmentDialog

  private val setPhoneWaEqualsToPhone = { _: View ->
    etPhoneWhatsapp.text = etPhone.text
  }


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_registration, container, false)

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

    Log.i (TAG, "manufacturer is $MANUFACTURER")
    Log.i (TAG, "Android version is $VERSION")

    if (isSamsung() && versionIsNineOrGreater()) {
      Log.i (TAG, "Manufacturer is samsung and version >= 9")
      birthDateFragmentDialog = BirthDateFragmentDialog()

      etBirthDate.setOnClickListener {
        birthDateFragmentDialog.show(requireActivity().supportFragmentManager,
          "BirthDateFragmentDialog")
      }

      etBirthDate.setOnFocusChangeListener { view, hasFocus ->
        if (hasFocus) {
          birthDateFragmentDialog.show(requireActivity().supportFragmentManager,
            "BirthDateFragmentDialog")
        }
      }
    }


    etFirstName.setText("Дмитрий")
    etLastName.setText("Федоров")
    etBirthDate.setText("08.06.1987")
    etPhone.setText("89240078897")
    etPhoneWhatsapp.setText("89240078897")
    etPassword.setText("12345")

    return view
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
        }
      }
    }
  }

  private fun isInputValid(user: User): Boolean {
    if (user.birthDate == null) {
      Log.i (TAG, "Укажите дату рождения!")

      balloon = createBalloon(requireContext()) {
        setArrowSize(10)
        setWidth(BalloonSizeSpec.WRAP)
        setHeight(65)
        setArrowPosition(0.7f)
        setCornerRadius(4f)
        setAlpha(0.9f)
        setText("Укажите дату рождения! Пример правильной даты: 01.12.2000")
        setTextColorResource(R.color.white)
        setTextIsHtml(true)
        setBackgroundColor(Color.RED)
        setBalloonAnimation(BalloonAnimation.FADE)
        setLifecycleOwner(lifecycleOwner)
      }

      balloon.showAlignBottom(etBirthDate)
      return false
    }

    return if (isValidDate(user.birthDate)) {
      val currentTime = Calendar.getInstance().time
      val age = getDiffYears(userBirthDate, currentTime)

      Log.i (TAG, "Age is $age")

      if (age in WORKER_AGE_FROM..WORKER_AGE_UP_TO) {
        Log.i (TAG, "${user.birthDate} is valid date, $userBirthDate")
        true
      }
      else {
        Log.i (TAG, "У нас принимаются работники от $WORKER_AGE_FROM до" +
            "$WORKER_AGE_UP_TO лет, $userBirthDate")
        false
      }
    }
    else {
      Log.i (TAG, "${user.birthDate} is invalid date, $userBirthDate")
      false
    }

    return false
  }

  private fun isValidDate (date: String): Boolean {
    return  parseDate(date, DATE_FORMAT1) ||
            parseDate(date, DATE_FORMAT2) ||
            parseDate(date, DATE_FORMAT3)
  }

  private fun parseDate (date: String, dateFormat: String): Boolean {
    val sdf: DateFormat = SimpleDateFormat(dateFormat)
    sdf.isLenient = false
    try {
      val d = sdf.parse(date)
      d?.let {
        userBirthDate = d
      }
    }
    catch (e: ParseException) {
      return false
    }
    return true
  }

  private fun getDiffYears (first: Date?, last: Date?): Int {
    val a = getCalendar(first)
    val b = getCalendar(last)
    var diff = b[YEAR] - a[YEAR]
    if (a[MONTH] > b[MONTH] || a[MONTH] == b[MONTH] && a[DATE] > b[DATE]) {
      diff--
    }

    return diff
  }

  private fun getCalendar (date: Date?): Calendar {
    val cal = Calendar.getInstance()
    cal.time = date!!
    return cal
  }
}