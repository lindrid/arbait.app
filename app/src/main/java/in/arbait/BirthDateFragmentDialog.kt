package `in`.arbait

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_DATE = "date"

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
    val datePicker = view.findViewById<DatePicker>(R.id.dp_reg_dialog_birth_date)

    val date = arguments?.getSerializable(ARG_DATE) as Date
    val calendar = getCalendar(date)
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    datePicker.updateDate(year, month, day)

    return view
  }

  override fun onClick(v: View?) {
    TODO("Not yet implemented")
  }

  companion object {
    fun newInstance (date: Date): BirthDateFragmentDialog {
      val args = Bundle().apply {
        putSerializable(ARG_DATE, date)
      }

      return BirthDateFragmentDialog().apply {
        arguments = args
      }
    }
  }
}