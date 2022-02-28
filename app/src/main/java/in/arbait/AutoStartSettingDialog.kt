package `in`.arbait

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment

private const val TAG = "AutoStartSettingDialog"

class AutoStartSettingDialog: DialogFragment(), View.OnClickListener
{
  private lateinit var rootView: View
  private lateinit var tvAutoStInfo: AppCompatTextView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View?
  {
    val view = inflater.inflate(R.layout.fragment_dialog_autostart, container, false)
    rootView = view

    view.findViewById<Button>(R.id.bt_autost_ok).setOnClickListener(this)
    tvAutoStInfo = view.findViewById(R.id.tv_autost_info)

    var s1 = ""
    var s2 = ""

    if (manufacturerIsMeizu()) {
      s1 = getString(R.string.meizu)
      s2 = getString(R.string.meizu_setting)
    }

    if (manufacturerIsHuawei()) {
      s1 = getString(R.string.huawei)
      s2 = getString(R.string.huawei_setting)
    }

    if (manufacturerIsXiaomi()) {
      s1 = getString(R.string.xiaomi)
      s2 = getString(R.string.xiaomi_setting)
    }

    tvAutoStInfo.text = getString(R.string.autostart_info, s1, s2)

    return view
  }

  override fun onClick(v: View?) {
    if (v?.id == R.id.bt_autost_ok) {
      val bundle = Bundle().apply {
        putBoolean(OK_KEY, true)
      }
      requireActivity().supportFragmentManager.setFragmentResult(OK_KEY, bundle)
    }

    dismiss()
  }

  override fun getTheme(): Int {
    return R.style.DialogTheme
  }
}