package `in`.arbait

import `in`.arbait.http.User
import `in`.arbait.http.Web
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "RegistrationFragment"

class RegistrationFragment : Fragment() {

  private val web = Web()

  private lateinit var tvRegistration: TextView
  private lateinit var etFirstName: EditText
  private lateinit var etLastName: EditText
  private lateinit var etBirthdate: EditText
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

    tvRegistration = view.findViewById(R.id.tv_reg_registration)

    etFirstName = view.findViewById(R.id.et_reg_first_name)
    etLastName = view.findViewById(R.id.et_reg_last_name)
    etBirthdate = view.findViewById(R.id.et_reg_birthdate)
    etPhone = view.findViewById(R.id.et_reg_phone)
    etPhoneWhatsapp = view.findViewById(R.id.et_reg_phone_whatsapp)
    btSamePhone = view.findViewById(R.id.bt_reg_same_phone)
    etPassword = view.findViewById(R.id.etp_reg_password)
    btDone = view.findViewById(R.id.bt_reg_done)

    etPhone.addTextChangedListener(PhoneNumberFormattingTextWatcher())
    etPhoneWhatsapp.addTextChangedListener(PhoneNumberFormattingTextWatcher())

    btSamePhone.setOnClickListener(setPhoneWaEqualsToPhone)

    btDone.setOnClickListener {
      val birthdate = SimpleDateFormat("dd.MM.yyyy").parse(etBirthdate.text.toString())
      val user = User (
        id = null,
        firstName = etFirstName.text.toString(),
        lastName = etLastName.text.toString(),
        birthdate = birthdate,
        phone = etPhone.text.toString(),
        phoneWa = etPhoneWhatsapp.text.toString(),
        password = etPassword.text.toString()
      )

      Log.i (TAG, "User = ${user.toString()}")
      web.registerUser(user) {
        val toast: Toast = if (it != null) {
          Toast.makeText(requireContext(), "Все ок, сервер вернул: $it",
            Toast.LENGTH_SHORT)
        } else {
          Toast.makeText(requireContext(), "Регистрация не прошла", Toast.LENGTH_SHORT)
        }

        toast.show()
      }
    }

    return view
  }

}