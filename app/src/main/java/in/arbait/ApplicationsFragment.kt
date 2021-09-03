package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.*
import `in`.arbait.http.polling_service.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val CONTEXT_ARG = "context"
const val VIEW_ARG = "view"

private const val TAG = "ApplicationsFragment"

private const val HOURLY_TITLE = "р/ч"
private const val DAILY_TITLE = "8ч"
private const val DATE_FORMAT = "yyyy-MM-dd"

/* Headers and messages (those that are instead of applications) */
private const val MAIN_HEADER = 0
private const val DAY_HEADER = 1
private const val TEXT = 2

private       val OPEN_HEADER_COLOR = Color.parseColor("#2E8B57")

private const val MAIN_HEADER_TEXT_SIZE = 28f
private const val HEADER_TEXT_SIZE = 24f
private const val TEXT_SIZE = 18f

class ApplicationsFragment: Fragment() {

  private lateinit var server: Server
  private lateinit var appsResponse: LiveData<ApplicationsResponse>
  private val openApps: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  private var adapter: AppAdapter = AppAdapter(emptyList())

  private var user: User? = null
  private lateinit var repository: UserRepository
  private var mainActivity = requireActivity() as MainActivity

  private var todayApps = mutableListOf<ApplicationItem>()
  private var tomorrowApps = mutableListOf<ApplicationItem>()
  private var showTomorrowApps = false

  private lateinit var rootView: View
  private lateinit var rvApps: RecyclerView

  private var pollService: PollService? = null
  private var serviceIsBound: Boolean? = null

  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
      Log.i(TAG, "ServiceConnection: connected to service.")
      // We've bound to MyService, cast the IBinder and get MyBinder instance
      val binder = iBinder as PollService.PollBinder
      pollService = binder.service
      serviceIsBound = true
      pollService?.let {
        appsResponse = it.appsResponse
      }
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
      Log.d(TAG, "ServiceConnection: disconnected from service.")
      serviceIsBound = false
    }
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

    /*
    server = Server(requireContext())
    server.getAppsResponseList(requireContext(), rootView)
    appsResponse = server.applicationsResponse
     */

    serviceDoAction(Actions.START)
    bindService()

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    appsResponse.observe(viewLifecycleOwner,
      Observer { appsResponse ->
        appsResponse?.let {
          if ((openApps.value == null) ||
              (listsAreDifferent(openApps.value!!, it.openApps)))
          {
            openApps.value = it.openApps
          }
        }
      }
    )

    openApps.observe(viewLifecycleOwner,
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

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val title = actionBar?.title
    val apps = getString(R.string.apps_action_bar_title)
    actionBar?.title = "$title - $apps"

    repository = UserRepository.get()
    GlobalScope.launch {
      user = repository.getUserLastByDate()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    unbindService()
    serviceDoAction(Actions.STOP)
  }

  private fun updateUI(openApps: List<ApplicationItem>) {
      rvApps.adapter = getConcatOpenAdapter(openApps.isNotEmpty(), this.showTomorrowApps)
  }

  private fun getConcatOpenAdapter(appsIsNotEmpty: Boolean, showTomorrowApps: Boolean):
      ConcatAdapter
  {
    var concatAdapter = ConcatAdapter()

    if (appsIsNotEmpty) {
      concatAdapter = ConcatAdapter()

      if (tomorrowApps.isNotEmpty()) {
        val appsCount = tomorrowApps.size
        val tomorrowHeaderText = when (user?.headerWasPressed) {
          true -> getString(R.string.apps_tomorrow_no_press, appsCount)
          false -> getString(R.string.apps_tomorrow, appsCount)
          null -> ""
        }
        val tomorrowHeaderAdapter = HeaderAdapter(tomorrowHeaderText, DAY_HEADER, true)
        val intermediateAdapter = ConcatAdapter(concatAdapter, tomorrowHeaderAdapter)
        Log.i (TAG, "showTomorrowApps = $showTomorrowApps")
        concatAdapter = when (showTomorrowApps) {
          true  -> ConcatAdapter(intermediateAdapter, AppAdapter(tomorrowApps))
          false -> intermediateAdapter
        }
      }

      val todayHeaderText = getString(R.string.apps_today)
      val todayHeaderAdapter = HeaderAdapter(todayHeaderText, DAY_HEADER)

      val addAdapter = when (todayApps.isNotEmpty()) {
        true -> {
          ConcatAdapter(todayHeaderAdapter, AppAdapter(todayApps))
        }
        false -> {
            ConcatAdapter(todayHeaderAdapter, HeaderAdapter(getString(R.string.apps_no_open_apps),
              TEXT))
        }
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
    private val tvTime: TextView = view.findViewById(R.id.tv_app_time)
    private val tvAddress: TextView = view.findViewById(R.id.tv_app_address)
    private val tvIncome: TextView = view.findViewById(R.id.tv_app_worker_income)
    private lateinit var app: ApplicationItem

    fun bind (app: ApplicationItem) {
      this.app = app
      Log.i (TAG, "app is $app")

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

  private inner class AppAdapter (var apps: List<ApplicationItem>): RecyclerView.Adapter<AppHolder> ()
  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
      val view = layoutInflater.inflate(R.layout.list_item_app, parent, false)
      return AppHolder(view)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: AppHolder, position: Int) {
      Log.i (TAG, "apps[position] = ${apps[position]}")
      holder.bind(apps[position])

      holder.itemView.setOnClickListener {
        server.getAppsResponseList(requireContext(), rootView)
        Log.i (TAG, "asdasdasdas")
      }
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

  private inner class HeaderAdapter (val headerText: String, val headerType: Int,
                                     val dayIsTomorrow: Boolean = false):
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
      var textColor = OPEN_HEADER_COLOR
      if (headerType == TEXT) textColor = Color.BLACK
      holder.bind(headerText, textSize, textColor)

      if (dayIsTomorrow) {
        holder.itemView.setOnClickListener {
          showTomorrowApps = !showTomorrowApps
          openApps.value?.let {
            updateUI(it)
            user?.let { user ->
              if (!user.headerWasPressed) {
                user.headerWasPressed = true
                repository.updateUser(user)
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

  private fun serviceDoAction (action: Actions) {
    if (getServiceState(mainActivity) == ServiceState.STOPPED && action == Actions.STOP) return

    Intent(mainActivity, PollService::class.java).also {
      it.action = action.name

      //val contextJson = Gson().toJson(requireContext())
      //val viewJson = Gson().toJson(rootView)

      //it.putExtra(CONTEXT_ARG, contextJson)
      //it.putExtra(VIEW_ARG, viewJson)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        log("Starting the service in >=26 Mode")
        mainActivity.startForegroundService(it)
        return
      }
      log("Starting the service in < 26 Mode")
      mainActivity.startService(it)
    }
  }

  private fun bindService() {
    val intent = Intent(mainActivity, PollService::class.java)
    mainActivity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    serviceIsBound = true
  }

  private fun unbindService() {
    val mainActivity = requireActivity() as MainActivity
    serviceIsBound?.let {
      if (it) {
        mainActivity.unbindService(serviceConnection)
      }
    }
  }
}