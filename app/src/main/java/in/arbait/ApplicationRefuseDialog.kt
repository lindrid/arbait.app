package `in`.arbait

import `in`.arbait.database.Consequences
import `in`.arbait.http.poll_service.DAY
import `in`.arbait.http.poll_service.HOUR
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.util.*

private const val TAG = "ApplicationRefuseDialog"
const val OK_KEY = "ok_key"
const val NO_CONSEQUENCES_KEY = "no_cons_key"

class ApplicationRefuseDialog(private val consequences: Consequences,
                              private val decreaseRatingPercent: Int,
                              private val bannDaysCount: Int,
                              private val bannHoursCount: Int
                              ): DialogFragment(), View.OnClickListener
{
  private lateinit var rootView: View
  private lateinit var tvAreYouSure: TextView

  @SuppressLint("SetTextI18n")
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View?
  {
    val view = inflater.inflate(R.layout.fragment_dialog_refuse, container, false)
    rootView = view

    tvAreYouSure = view.findViewById(R.id.tv_dar_are_you_sure)
    when (consequences) {
      Consequences.DECREASE_RATING_AND_BANN -> {
        tvAreYouSure.text = "${tvAreYouSure.text} " + getDaysHoursStr() + " " +
            getString(R.string.dar_decrease_rating, decreaseRatingPercent) + "%!"
      }
      else -> {
        tvAreYouSure.text = tvAreYouSure.text
      }
    }

    view.findViewById<Button>(R.id.bt_dar_yes).setOnClickListener(this)
    view.findViewById<Button>(R.id.bt_dar_no).setOnClickListener(this)

    return view
  }

  private fun getDaysHoursStr(): String {
      if (bannDaysCount == 0)
        return getString(R.string.dar_bann_hours, bannHoursCount)

    return if (bannHoursCount > 0) {
      getString(R.string.dar_bann_days_hours, bannDaysCount, bannHoursCount)
    }
    else {
      getString(R.string.dar_bann_days, bannDaysCount)
    }
  }

  override fun onClick(v: View?) {
    if (v?.id == R.id.bt_dar_yes) {
      if (consequences != Consequences.NOTHING) {
        App.dbUser?.let { user ->
          val now = Date().time
          user.endOfBannDatetime = now + bannDaysCount * DAY + bannHoursCount * HOUR
          App.repository.updateUser(user)
        }
      }

      val bundle = Bundle().apply {
        putBoolean(OK_KEY, true)
        if (consequences == Consequences.NOTHING) {
          putBoolean(NO_CONSEQUENCES_KEY, true)
        }
        else {
          putBoolean(NO_CONSEQUENCES_KEY, false)
        }
      }
      requireActivity().supportFragmentManager.setFragmentResult(OK_KEY, bundle)
    }

    dismiss()
  }
}