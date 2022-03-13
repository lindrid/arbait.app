package `in`.arbait

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment

private const val TAG = "InfoDialog"

class InfoDialog(val step: Int): DialogFragment(), View.OnClickListener
{
  private lateinit var rootView: View
  private lateinit var tvInfoHeader: AppCompatTextView
  private lateinit var tvInfo: AppCompatTextView
  private lateinit var tvInfo2: AppCompatTextView
  private lateinit var ivNotifications: AppCompatImageView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View?
  {
    val view = inflater.inflate(R.layout.fragment_dialog_info, container, false)
    rootView = view

    view.findViewById<Button>(R.id.bt_autost_ok).setOnClickListener(this)
    tvInfoHeader = view.findViewById(R.id.tv_info_header)
    tvInfo = view.findViewById(R.id.tv_info)
    tvInfo2 = view.findViewById(R.id.tv_info_2)
    ivNotifications = view.findViewById(R.id.iv_info_notifications)

    if (step == 1) {
      tvInfoHeader.text = getString(R.string.dont_miss_header)
      tvInfo.text = getString(R.string.dont_miss_info)
      tvInfo2.text = getString(R.string.dont_miss_info_2)
      ivNotifications.visibility = View.VISIBLE
    }
    else if (step == 2) {
      tvInfoHeader.text = getString(R.string.correct_header)
      tvInfo.text = getString(R.string.correct_info)
      tvInfo2.visibility = View.INVISIBLE
      ivNotifications.visibility = View.INVISIBLE
    }

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