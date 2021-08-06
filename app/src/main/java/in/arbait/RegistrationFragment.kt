package `in`.arbait

import `in`.arbait.http.*
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

private const val TAG = "RegistrationFragment"

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
      registerUser()
    }

    etFirstName.setText("Дмитрий")
    etLastName.setText("Федоров")
    etBirthDate.setText("08.06-1987")
    etPhone.setText("89240078897")
    etPhoneWhatsapp.setText("89240078897")
    etPassword.setText("12345")

    return view
  }


  private fun registerUser() {
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

    server.registerUser(user) { response: Response ->
      onResult(response)
    }
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

}