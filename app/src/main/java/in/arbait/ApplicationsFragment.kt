package `in`.arbait

import `in`.arbait.http.poll_service.*
import `in`.arbait.models.ApplicationItem
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration

const val DATE_FORMAT = "yyyy-MM-dd"
const val APP_ID_ARG = "application_id"
const val VIEW_MODEL_ARG = "applications_view_model"

private const val TAG = "ApplicationsFragment"
val OPEN_HEADER_COLOR = Color.parseColor("#2E8B57")

/* Headers and messages (those that are instead of applications) */
private const val MAIN_HEADER = 0
private const val DAY_HEADER = 1
private const val TEXT = 2

const val MAIN_HEADER_TEXT_SIZE = 28f
const val HEADER_TEXT_SIZE = 24f
const val TEXT_SIZE = 18f

class ApplicationsFragment: Fragment() {
  private var todayApps = mutableListOf<ApplicationItem>()
  private var tomorrowApps = mutableListOf<ApplicationItem>()
  private var showTomorrowApps = false

  private lateinit var rootView: View
  private lateinit var rvApps: RecyclerView
  private var adapter: AppAdapter = AppAdapter(emptyList())

  private val vm: PollServerViewModel by lazy {
    val mainActivity = requireActivity() as MainActivity
    mainActivity.pollServerViewModel
  }

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

    vm.rootView = rootView

    // заявки считываются с сервера нашим бесконечным PollService'ом
    vm.serviceDoAction(Actions.START)
    vm.bindService()

    vm.openApps.observe(viewLifecycleOwner,
      Observer { openApps ->
        openApps?.let {
          Log.i(TAG, "Open apps size is ${it.size}")
          Log.i(TAG, "openApps is $it")
          setTodayAndTomorrowApps(it)
          showTomorrowApps = todayApps.isEmpty()
          updateUI(it)
        }
      }
    )

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    val apps = getString(R.string.apps_action_bar_title)
    actionBar?.title = "$appName - $apps"
  }

  override fun onDestroy() {
    super.onDestroy()
    vm.unbindService()
    vm.serviceDoAction(Actions.STOP)
  }

  fun updateUI(openApps: List<ApplicationItem>) {
    rvApps.adapter = getConcatOpenAdapter(openApps)
  }

  private fun getConcatOpenAdapter(openApps: List<ApplicationItem>):
      ConcatAdapter
  {
    var concatAdapter = ConcatAdapter()
    var headerIsSet = false

    if (openApps.isNotEmpty()) {
      concatAdapter = ConcatAdapter()

      if (tomorrowApps.isNotEmpty()) {
        val appsCount = tomorrowApps.size
        val tomorrowHeaderText = when (App.user?.headerWasPressed) {
          true -> getString(R.string.apps_tomorrow_no_press, appsCount)
          false -> getString(R.string.apps_tomorrow, appsCount)
          null -> ""
        }
        val tomorrowHeaderAdapter = HeaderAdapter(tomorrowHeaderText, DAY_HEADER, true)
        val intermediateAdapter = ConcatAdapter(concatAdapter, tomorrowHeaderAdapter)
        Log.i (TAG, "showTomorrowApps = $showTomorrowApps")
        concatAdapter = when (showTomorrowApps) {
          true  -> {
            headerIsSet = true
            ConcatAdapter(intermediateAdapter, AppAdapter(tomorrowApps, true))
          }
          false -> intermediateAdapter
        }
      }

      val todayHeaderText = getString(R.string.apps_today)
      val todayHeaderAdapter = HeaderAdapter(todayHeaderText, DAY_HEADER)

      val addAdapter = when (todayApps.isNotEmpty()) {
        true -> ConcatAdapter(todayHeaderAdapter, AppAdapter(todayApps, !headerIsSet))
        false ->  ConcatAdapter(todayHeaderAdapter, HeaderAdapter(
                    getString(R.string.apps_no_open_apps), TEXT))
      }

      concatAdapter = ConcatAdapter (concatAdapter, addAdapter)
    }
    else {
      val todayHeaderText = getString(R.string.apps_today)
      val todayHeaderAdapter = HeaderAdapter(todayHeaderText, DAY_HEADER)
      concatAdapter = ConcatAdapter (todayHeaderAdapter,
        HeaderAdapter(getString(R.string.apps_no_open_apps), TEXT))
    }

    return concatAdapter
  }

  private fun setTodayAndTomorrowApps (apps: List<ApplicationItem>) {
    todayApps = mutableListOf<ApplicationItem>()
    tomorrowApps = mutableListOf<ApplicationItem>()

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
    private val tvTime: TextView = view.findViewById(R.id.tv_apps_time)
    private val tvAddress: TextView = view.findViewById(R.id.tv_apps_address)
    private val tvIncome: TextView = view.findViewById(R.id.tv_apps_worker_income)
    private val tvWorkers: TextView = view.findViewById(R.id.tv_apps_workers)
    private var app: ApplicationItem? = null

    @SuppressLint("SetTextI18n")
    fun bind (app: ApplicationItem?) {
      if (app == null) {
        tvTime.text = getString(R.string.apps_time)
        tvTime.setTextColor(OPEN_HEADER_COLOR)

        tvAddress.text = getString(R.string.apps_address)
        tvAddress.setTextColor(OPEN_HEADER_COLOR)

        tvIncome.text = getString(R.string.apps_worker_income)
        tvIncome.setTextColor(OPEN_HEADER_COLOR)

        tvWorkers.text = getString(R.string.apps_workers)
        tvWorkers.setTextColor(OPEN_HEADER_COLOR)
        return
      }

      this.app = app
      Log.i (TAG, "app is $app")

      tvTime.text = app.time
      tvAddress.text = app.address
      tvWorkers.text = "${app.workerCount} / ${app.workerTotal}"

      val price = app.priceForWorker.toString()
      tvIncome.text = when (app.hourlyJob) {
        true -> "$price ${getString(R.string.hourly_suffix)}"
        false -> "$price${getString(R.string.daily_suffix)}"
      }
    }

    override fun onClick(v: View?) {
      Log.i ("AppHolder", "onClick()")
      app?.let { app ->
        Log.i ("AppHolder", "app is not null")
        val args = Bundle().apply {
          putInt(APP_ID_ARG, app.id)
        }
        val mainActivity = context as MainActivity
        mainActivity.replaceOnFragment("Application", args)
      }
    }
  }

  private inner class AppAdapter (apps: List<ApplicationItem?>, val hasHeader: Boolean = false):
    RecyclerView.Adapter<AppHolder> ()
  {
    private val headerType = 0
    private val itemType = 1
    private val apps: MutableList<ApplicationItem?> = mutableListOf()

    init {
      if (hasHeader)
        this.apps.add(null)

      for (i in apps.indices)
        this.apps.add(apps[i])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
      val view = if (viewType == headerType)
        layoutInflater.inflate(R.layout.list_item_app_header, parent, false)
      else
        layoutInflater.inflate(R.layout.list_item_app, parent, false)

      return AppHolder(view)
    }

    override fun getItemCount() = apps.size

    override fun getItemViewType(position: Int): Int {
      if (position == 0 && hasHeader)
        return headerType

      return itemType
    }

    override fun onBindViewHolder(holder: AppHolder, position: Int) {
      Log.i (TAG, "apps[position] = ${apps[position]}")
      holder.bind(apps[position])
      holder.itemView.setOnClickListener(holder)
    }
  }

  private inner class HeaderHolder (view: View) : RecyclerView.ViewHolder(view) {
    private val tvAppsHeader: TextView = view.findViewById(R.id.tv_header)

    fun bind(headerText: String, textSize: Float, textColor: Int) {
      tvAppsHeader.text = headerText
      tvAppsHeader.textSize = textSize
      tvAppsHeader.setTextColor(textColor)
    }
  }

  private inner class HeaderAdapter (val headerText: String, val headerType: Int,
                                     val dayIsTomorrow: Boolean = false):
    RecyclerView.Adapter<HeaderHolder>()
  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_header,
        parent, false)
      return HeaderHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
      val textSize = when (headerType) {
        MAIN_HEADER -> MAIN_HEADER_TEXT_SIZE
        DAY_HEADER -> HEADER_TEXT_SIZE
        else -> TEXT_SIZE
      }
      var textColor = OPEN_HEADER_COLOR
      if (headerType == TEXT) textColor = Color.BLACK
      holder.bind(headerText, textSize, textColor)

      if (dayIsTomorrow) {
        holder.itemView.setOnClickListener {
          showTomorrowApps = !showTomorrowApps
          vm.openApps.value?.let {
            updateUI(it)
            App.user?.let { user ->
              if (!user.headerWasPressed) {
                user.headerWasPressed = true
                App.repository.updateUser(user)
              }
            }
          }
        }
      }
    }

    override fun getItemCount(): Int {
      return 1
    }
  }

}