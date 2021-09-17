package `in`.arbait

import `in`.arbait.models.ApplicationItem
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "ApplicationFragment"

const val PM_CARD = 1
const val PM_CASH = 2

class ApplicationFragment (private val appItem: ApplicationItem): Fragment() {
  private lateinit var tvAddress: AppCompatTextView
  private lateinit var tvTime: AppCompatTextView
  private lateinit var tvIncome: AppCompatTextView
  private lateinit var tvDescription: AppCompatTextView
  private lateinit var tvPayMethod: AppCompatTextView
  private lateinit var tvWorkers: AppCompatTextView
  private lateinit var rvWorkers: RecyclerView
  private lateinit var btCallClient: AppCompatButton
  private lateinit var btEnrollRefuse: AppCompatButton
  private lateinit var btBack: AppCompatButton

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_application, container, false)

    Log.i (TAG, "OnCreate()")
    setViews(view)
    setViewsTexts()

    return view
  }


  private fun setViews(view: View) {
    tvAddress = view.findViewById(R.id.tv_app_address)
    tvTime = view.findViewById(R.id.tv_app_time)
    tvIncome = view.findViewById(R.id.tv_app_worker_income)
    tvDescription = view.findViewById(R.id.tv_app_description)
    tvPayMethod = view.findViewById(R.id.tv_app_pay_method)
    tvWorkers = view.findViewById(R.id.tv_app_workers)
    rvWorkers = view.findViewById(R.id.rv_app_workers)
    btCallClient = view.findViewById(R.id.bt_app_call_client)
    btEnrollRefuse = view.findViewById(R.id.bt_app_enroll_refuse)
    btBack = view.findViewById(R.id.bt_app_back)
  }

  private fun setViewsTexts() {
    tvAddress.text = Html.fromHtml(getString(R.string.app_address, appItem.address))

    val date = strToDate(appItem.date, DATE_FORMAT)
    date?.let {
      val time = if (isItToday(it))
        "${getString(R.string.`in`).uppercase()} ${appItem.time}"
      else
        "${getString(R.string.tomorrow_in).uppercase()} ${appItem.time}"
      tvTime.text = Html.fromHtml(getString(R.string.app_time, time))
    }

    val suffix = if (appItem.hourlyJob)
      getString(R.string.hourly_suffix)
    else
      getString(R.string.daily_suffix)
    val income = "${appItem.priceForWorker}$suffix"
    tvIncome.text = Html.fromHtml(getString(R.string.app_worker_income, income))

    tvDescription.text = Html.fromHtml(getString(R.string.app_description, appItem.whatToDo))

    val payMethod = if (appItem.payMethod == PM_CARD)
      getString(R.string.app_on_card)
    else
      getString(R.string.app_cash)
    tvPayMethod.text = Html.fromHtml(getString(R.string.app_pay_method, payMethod))

    val workers = "${appItem.workerCount} / ${appItem.workerTotal}"
    tvWorkers.text = Html.fromHtml(getString(R.string.app_worker_count, workers))

    btEnrollRefuse.text = getString(R.string.app_enroll)
    //btCallClient.visibility = View.INVISIBLE
  }
}