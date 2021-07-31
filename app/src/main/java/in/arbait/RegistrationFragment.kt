package `in`.arbait

import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class RegistrationFragment : Fragment() {

  private lateinit var tvRegistration: TextView
  private lateinit var etPassword: EditText
  private lateinit var etPhone: EditText
  private lateinit var etPhoneWhatsapp: EditText
  private lateinit var btSamePhone: Button
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
    etPassword = view.findViewById(R.id.etp_reg_password)
    etPhone = view.findViewById(R.id.et_reg_phone)
    etPhoneWhatsapp = view.findViewById(R.id.et_reg_phone_whatsapp)
    btSamePhone = view.findViewById(R.id.bt_reg_same_phone)
    btDone = view.findViewById(R.id.bt_reg_done)

    etPhone.addTextChangedListener(PhoneNumberFormattingTextWatcher())
    etPhoneWhatsapp.addTextChangedListener(PhoneNumberFormattingTextWatcher())

    btSamePhone.setOnClickListener(setPhoneWaEqualsToPhone)

    return view
  }

}