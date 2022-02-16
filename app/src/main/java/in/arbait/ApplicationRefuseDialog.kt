package `in`.arbait

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

private const val TAG = "ApplicationRefuseDialog"
const val OK_KEY = "ok_key"

class ApplicationRefuseDialog: DialogFragment(), View.OnClickListener
{
  private lateinit var rootView: View

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View?
  {
    val view = inflater.inflate(R.layout.fragment_dialog_refuse, container, false)
    rootView = view

    view.findViewById<Button>(R.id.bt_dar_yes).setOnClickListener(this)
    view.findViewById<Button>(R.id.bt_dar_no).setOnClickListener(this)

    return view
  }

  override fun onClick(v: View?) {
    if (v?.id == R.id.bt_dar_yes) {
      val bundle = Bundle().apply {
        putBoolean(OK_KEY, true)
      }
      requireActivity().supportFragmentManager.setFragmentResult(OK_KEY, bundle)
    }

    dismiss()
  }
}