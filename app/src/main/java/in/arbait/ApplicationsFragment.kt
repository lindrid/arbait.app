package `in`.arbait

import `in`.arbait.http.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import java.util.*

private const val TAG = "ApplicationsFragment"
private const val HOURLY_TITLE = "р/ч"
private const val DAILY_TITLE = "8ч"
private const val DATE_FORMAT = "yyyy-MM-dd"

private const val HEADER_TEXT_SIZE = 24f
private const val TEXT_SIZE = 18f

private const val APP_OPEN_STATE = 0
private const val APP_CLOSED_STATE = 1
private const val APP_FINISHED_STATE = 6

class ApplicationsFragment: Fragment() {

  private lateinit var server: Server
  private lateinit var appsResponse: LiveData<ApplicationsResponse>
  private var adapter: AppAdapter = AppAdapter(emptyList())

  private lateinit var rootView: View
  private lateinit var rvApps: RecyclerView

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_applications, container, false)
    rootView = view

    rvApps = view.findViewById(R.id.rv_app_list)
    rvApps.layoutManager = LinearLayoutManager(context)
    rvApps.adapter = adapter

    val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
    rvApps.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    rvApps.addItemDecoration(divider)

    server = Server(requireContext())
    appsResponse = server.getAppsResponseList(requireContext(), rootView)

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    Log.i (TAG, "updateUI(appItems) = $appsResponse")
    appsResponse.observe(viewLifecycleOwner,
      Observer { appsResponse ->
        appsResponse?.let {
          Log.i(TAG, "Open apps size is ${appsResponse.openApps.size}")
          Log.i(TAG, "Closed apps size is ${appsResponse.closedApps.size}")
          Log.i(TAG, "Finished apps size is ${appsResponse.finishedApps.size}")
          updateUI(appsResponse)
        }
      }
    )
  }

  private fun updateUI(appsResponse: ApplicationsResponse) {
    val openAdapter = getConcatAdapter(appsResponse.openApps, APP_OPEN_STATE)
    val closedAdapter = getConcatAdapter(appsResponse.closedApps, APP_CLOSED_STATE)
    val finishedAdapter = getConcatAdapter(appsResponse.finishedApps, APP_FINISHED_STATE)

    rvApps.adapter = ConcatAdapter(openAdapter, closedAdapter, finishedAdapter)
  }

  private fun getConcatAdapter(apps: List<ApplicationItem>, appState: Int): ConcatAdapter {
    var concatAdapter = ConcatAdapter()
    val todayApps = mutableListOf<ApplicationItem>()
    val tomorrowApps = mutableListOf<ApplicationItem>()

    val tomorrowHeaderText = when (appState) {
      APP_OPEN_STATE -> getString(R.string.apps_open_apps_tomorrow_header)
      APP_CLOSED_STATE -> getString(R.string.apps_closed_apps_tomorrow_header)
      else -> ""
    }

    val todayHeaderText = when (appState) {
      APP_OPEN_STATE -> getString(R.string.apps_open_apps_today_header)
      APP_CLOSED_STATE -> getString(R.string.apps_closed_apps_today_header)
      APP_FINISHED_STATE -> getString(R.string.apps_finished_apps_today_header)
      else -> ""
    }

    setTodayAndTomorrowApps(apps, todayApps, tomorrowApps)

    if (tomorrowApps.isNotEmpty() && appState != APP_FINISHED_STATE) {
      val tomorrowHeaderAdapter = HeaderAdapter(tomorrowHeaderText, HEADER_TEXT_SIZE)
      val intermediateAdapter = ConcatAdapter(concatAdapter, tomorrowHeaderAdapter)
      concatAdapter = ConcatAdapter(intermediateAdapter, AppAdapter(tomorrowApps))
    }

    val todayHeaderAdapter = HeaderAdapter(todayHeaderText, HEADER_TEXT_SIZE)

    val noAppsAdapter = when (appState) {
      APP_OPEN_STATE -> {
        ConcatAdapter(todayHeaderAdapter, HeaderAdapter(getString(R.string.apps_no_open_apps),
          TEXT_SIZE))
      }
      else -> ConcatAdapter()
    }

    val addAdapter = when (todayApps.isNotEmpty()) {
      true ->   ConcatAdapter(todayHeaderAdapter, AppAdapter(todayApps))
      false ->  noAppsAdapter
    }

    concatAdapter = ConcatAdapter(concatAdapter, addAdapter)

    return concatAdapter
  }

  private fun setTodayAndTomorrowApps (
    apps: List<ApplicationItem>,
    todayApps: MutableList<ApplicationItem>,
    tomorrowApps: MutableList<ApplicationItem>
  ) {
    val now = Calendar.getInstance().time
    for (i in apps.indices) {
      strToDate(apps[i].date, DATE_FORMAT)?.let {
        when (getDiffDays(now, it)) {
          0 -> todayApps.add(apps[i])
          1 -> tomorrowApps.add(apps[i])
          else -> {}
        }
      }
    }
  }

  private inner class AppHolder (view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
    private val tvTime: TextView = view.findViewById(R.id.tv_app_time)
    private val tvAddress: TextView = view.findViewById(R.id.tv_app_address)
    private val tvIncome: TextView = view.findViewById(R.id.tv_app_worker_income)
    private lateinit var app: ApplicationItem

    fun bind (app: ApplicationItem) {
      this.app = app
      tvTime.text = app.time
      tvAddress.text = app.address

      val price = app.priceForWorker.toString()
      tvIncome.text = when (app.hourlyJob) {
        true -> "$price $HOURLY_TITLE"
        false -> "$price/$DAILY_TITLE"
      }
    }

    override fun onClick(v: View?) {
      //callbacks?.onCrimeSelected(crime.id)
    }
  }

  private inner class AppAdapter (var apps: List<ApplicationItem>):
    RecyclerView.Adapter<AppHolder> ()
  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
      val view = layoutInflater.inflate(R.layout.list_item_app, parent, false)
      return AppHolder(view)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: AppHolder, position: Int) {
      Log.i (TAG, "apps[position] = ${apps[position]}")
      holder.bind(apps[position])
    }
  }


  private inner class HeaderHolder (view: View) : RecyclerView.ViewHolder(view) {
    private val tvAppsHeader: TextView = view.findViewById(R.id.tv_apps_header)

    fun bind(headerText: String, textSize: Float) {
      tvAppsHeader.text = headerText
      tvAppsHeader.textSize = textSize
    }
  }

  private inner class HeaderAdapter (val headerText: String, val textSize: Float):
    RecyclerView.Adapter<HeaderHolder>()
  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.list_header_app,
        parent, false)
      return HeaderHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
      holder.bind(headerText, textSize)
    }

    override fun getItemCount(): Int {
      return 1
    }
  }

}