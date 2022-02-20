package `in`.arbait

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import java.util.*

private const val TAG = "RulesDialog"
const val CANCEL_KEY = "cancel_key"

class RulesDialog: DialogFragment(), View.OnClickListener
{
  private lateinit var rootView: View

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View?
  {
    val view = inflater.inflate(R.layout.fragment_rules, container, false)
    rootView = view

    view.findViewById<Button>(R.id.bt_rules_ok).setOnClickListener(this)
    view.findViewById<Button>(R.id.bt_rules_cancel).setOnClickListener(this)

    return view
  }

  override fun onClick(v: View?) {
    if (v?.id == R.id.bt_rules_ok) {
      App.dbUser?.let { user ->
        user.rulesShowDatetime = Date().time
        App.repository.updateUser(user)
      }
    }
    if (v?.id == R.id.bt_rules_cancel) {
      val bundle = Bundle().apply {
        putBoolean(CANCEL_KEY, true)
      }
      requireActivity().supportFragmentManager.setFragmentResult(CANCEL_KEY, bundle)
    }

    dismiss()
  }

  override fun getTheme(): Int {
    return R.style.DialogTheme
  }
}