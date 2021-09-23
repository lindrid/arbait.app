package `in`.arbait

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment

class LoginFragment: Fragment() {

  private lateinit var tvLogin: AppCompatTextView
  private lateinit var etPhone: AppCompatEditText
  private lateinit var btDone: AppCompatButton

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_login, container, false)

    setViews(view)

    return view
  }

  private fun setViews(view: View) {
    tvLogin = view.findViewById(R.id.tv_log_login)
    etPhone = view.findViewById(R.id.et_log_phone)
  }

}