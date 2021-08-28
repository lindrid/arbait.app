package `in`.arbait

import `in`.arbait.http.*
import android.graphics.Color
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

/* Headers and messages (those that are instead of applications) */
private const val MAIN_HEADER = 0
private const val DAY_HEADER = 1
private const val TEXT = 2

private       val OPEN_HEADER_COLOR = Color.parseColor("#2E8B57")
private const val CLOSED_HEADER_COLOR = Color.RED
private const val FINISHED_HEADER_COLOR = Color.BLUE

private const val MAIN_HEADER_TEXT_SIZE = 28f
private const val HEADER_TEXT_SIZE = 24f
private const val TEXT_SIZE = 18f
/* ************************************************************* */

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
    //val closedAdapter = getConcatAdapter(appsResponse.closedApps, APP_CLOSED_STATE)
    //val finishedAdapter = getConcatAdapter(appsResponse.finishedApps, APP_FINISHED_STATE)

    rvApps.adapter = ConcatAdapter(openAdapter)// closedAdapter, finishedAdapter)
  }

  private fun getConcatAdapter(apps: List<ApplicationItem>, appState: Int): ConcatAdapter {
    var concatAdapter = ConcatAdapter()
    val todayApps = mutableListOf<ApplicationItem>()
    val tomorrowApps = mutableListOf<ApplicationItem>()

    setTodayAndTomorrowApps(apps, todayApps, tomorrowApps)

    if (apps.isNotEmpty()) {
      val mainHeaderText = when (appState) {
        APP_OPEN_STATE -> getString(R.string.apps_open)
        APP_CLOSED_STATE -> getString(R.string.apps_closed)
        APP_FINISHED_STATE -> getString(R.string.apps_finished)
        else -> ""
      }

      concatAdapter = ConcatAdapter(HeaderAdapter(mainHeaderText, MAIN_HEADER, appState))

      if (tomorrowApps.isNotEmpty()) {
        val tomorrowHeaderText = getString(R.string.apps_tomorrow)
        val tomorrowHeaderAdapter = HeaderAdapter(tomorrowHeaderText, DAY_HEADER, appState)
        val intermediateAdapter = ConcatAdapter(concatAdapter, tomorrowHeaderAdapter)
        concatAdapter = ConcatAdapter(intermediateAdapter, AppAdapter(tomorrowApps))
      }

      val todayHeaderText = getString(R.string.apps_today)
      val todayHeaderAdapter = HeaderAdapter(todayHeaderText, DAY_HEADER, appState)

      val addAdapter = when (todayApps.isNotEmpty()) {
        true -> {
          ConcatAdapter(todayHeaderAdapter, AppAdapter(todayApps))
        }
        false -> {
          if (appState == APP_OPEN_STATE) {
            ConcatAdapter(todayHeaderAdapter, HeaderAdapter(getString(R.string.apps_no_open_apps),
              TEXT, appState))
          }
          else ConcatAdapter()
        }
      }

      concatAdapter = ConcatAdapter (concatAdapter, addAdapter)
    }
    else if (appState == APP_OPEN_STATE) {
      val mainHeaderText = getString(R.string.apps_open)
      val mainHeader = HeaderAdapter(mainHeaderText, MAIN_HEADER, appState)
      val todayHeaderText = getString(R.string.apps_today)
      val todayHeaderAdapter = HeaderAdapter(todayHeaderText, DAY_HEADER, appState)
      concatAdapter = ConcatAdapter(mainHeader, todayHeaderAdapter)
      concatAdapter = ConcatAdapter (
        concatAdapter, HeaderAdapter(getString(R.string.apps_no_open_apps), TEXT, appState)
      )
    }

    return concatAdapter
  }

  private fun setTodayAndTomorrowApps (
    apps: List<ApplicationItem>,
    todayApps: MutableList<ApplicationItem>,
    tomorrowApps: MutableList<ApplicationItem>
  ) {
    for (i in apps.indices) {
      strToDate(apps[i].date, DATE_FORMAT)?.let {
        when (isItToday(it)) {
          true -> todayApps.add(apps[i])
          false -> tomorrowApps.add(apps[i])
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

    fun bind(headerText: String, textSize: Float, textColor: Int) {
      tvAppsHeader.text = headerText
      tvAppsHeader.textSize = textSize
      tvAppsHeader.setTextColor(textColor)
    }
  }

  private inner class HeaderAdapter (val headerText: String, val headerType: Int, val appState: Int):
    RecyclerView.Adapter<HeaderHolder>()
  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.list_header_app,
        parent, false)
      return HeaderHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
      val textSize = when (headerType) {
        MAIN_HEADER -> MAIN_HEADER_TEXT_SIZE
        DAY_HEADER -> HEADER_TEXT_SIZE
        else -> TEXT_SIZE
      }
      var textColor = when (appState) {
        APP_OPEN_STATE -> OPEN_HEADER_COLOR
        APP_CLOSED_STATE -> CLOSED_HEADER_COLOR
        APP_FINISHED_STATE -> FINISHED_HEADER_COLOR
        else -> Color.BLACK
      }
      if (headerType == TEXT) textColor = Color.BLACK
      holder.bind(headerText, textSize, textColor)
    }

    override fun getItemCount(): Int {
      return 1
    }
  }

}