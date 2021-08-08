package `in`.arbait

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

class BirthDateFragmentDialog: DialogFragment(), View.OnClickListener {
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View?
  {
    dialog?.setTitle(R.string.reg_birth_date)
    val view = inflater.inflate(R.layout.fragment_dialog_birth_date, container, false)
    view.findViewById<Button>(R.id.bt_reg_dialog_ok).setOnClickListener(this)
    view.findViewById<Button>(R.id.bt_reg_dialog_cancel).setOnClickListener(this)
    return view
  }

  override fun onClick(v: View?) {
    TODO("Not yet implemented")
  }
}