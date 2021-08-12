package `in`.arbait

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText


class PhoneConfirmationFragment: Fragment() {

  private lateinit var etCode: EditText

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View?
  {
    val view = inflater.inflate(R.layout.fragment_phone_confirmation, container, false)

    etCode = view.findViewById(R.id.et_phone_conf_code)
    etCode.transformationMethod = NumericKeyBoardTransformationMethod()

    return view
  }
}

private class NumericKeyBoardTransformationMethod: PasswordTransformationMethod() {
  override fun getTransformation(source: CharSequence, view: View?): CharSequence {
    return source
  }
}