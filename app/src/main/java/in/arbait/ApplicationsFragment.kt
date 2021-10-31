package `in`.arbait

import `in`.arbait.http.PollServerViewModel
import `in`.arbait.http.poll_service.*
import `in`.arbait.http.items.ApplicationItem
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration

const val DATE_FORMAT = "yyyy-MM-dd"
const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
const val APP_ID_ARG = "applicationId"
const val APPLICATION_FRAGMENT_NAME = "Application"

private const val TAG = "ApplicationsFragment"
val OPEN_HEADER_COLOR = Color.parseColor("#2E8B57")

/* Headers and messages (those that are instead of applications) */
private const val MAIN_HEADER = 0
private const val DAY_HEADER = 1
private const val TEXT = 2

const val MAIN_HEADER_TEXT_SIZE = 28f
const val HEADER_TEXT_SIZE = 24f
const val TEXT_SIZE = 18f

class ApplicationsFragment: Fragment()
{
  private var openAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  private var takenAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()

  private var todayApps = mutableListOf<ApplicationItem>()
  private var tomorrowApps = mutableListOf<ApplicationItem>()
  private var showTomorrowApps = false

  private lateinit var rootView: View
  private lateinit var rvOpenApps: RecyclerView
  private lateinit var rvTakenApps: RecyclerView
  private lateinit var llTakenApps: LinearLayout

  private val vm: PollServerViewModel by lazy {
    val mainActivity = requireActivity() as MainActivity
    mainActivity.pollServerViewModel
  }


  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_applications, container, false)
    rootView = view

    llTakenApps = view.findViewById(R.id.app_linear_layout)
    rvOpenApps = view.findViewById(R.id.rv_app_list)
    rvOpenApps.layoutManager = LinearLayoutManager(context)
    rvOpenApps.adapter = OpenAppAdapter(emptyList())

    rvTakenApps = view.findViewById(R.id.rv_app_taken_apps)
    rvTakenApps.layoutManager = LinearLayoutManager(context)
    rvTakenApps.adapter = TakenAppAdapter(emptyList())

    val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
    rvOpenApps.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    rvOpenApps.addItemDecoration(divider)

    vm.rootView = rootView
    vm.doOnOpenAppsChange = {
      val openApps = vm.openAppsLvdList.value
      openApps?.let {
        Log.i(TAG, "Open apps size is ${it.size}")
        Log.i(TAG, "openApps is $it")
        setTodayAndTomorrowApps(it)
        showTomorrowApps = todayApps.isEmpty()
        updateOpenAppsUI(it)
      }
    }

    vm.doOnTakenAppsChange = {
      val takenApps = vm.takenAppsLvdList.value
      takenApps?.let {
        Log.i(TAG, "Taken apps size is ${it.size}")
        Log.i(TAG, "takenApps is $it")
        updateTakenAppsUI(it)

        if (it.isEmpty() && llTakenApps.visibility == View.VISIBLE)
          llTakenApps.visibility = View.INVISIBLE

        if (it.isNotEmpty() && llTakenApps.visibility == View.INVISIBLE)
          llTakenApps.visibility = View.VISIBLE
      }
    }

    // заявки считываются с сервера нашим бесконечным PollService'ом
    vm.serviceDoAction(Action.START)
    vm.bindService()

    setHasOptionsMenu(true)

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    val apps = getString(R.string.apps_action_bar_title)
    actionBar?.title = "$appName - $apps"
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.main_menu, menu)

    val soundItem = menu.findItem(R.id.bt_menu_sound)
    val notificationsItem = menu.findItem(R.id.bt_menu_notifications)

    if (App.dbUser?.soundOff == true)
      soundItem.setIcon(R.drawable.outline_volume_off_24)

    if (App.dbUser?.notificationsOff == true)
      notificationsItem.setIcon(R.drawable.outline_notifications_off_24)

    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.bt_menu_sound) {
      if (App.dbUser?.soundOff == false)
        setSound(true, item, R.drawable.outline_volume_off_24)
      else if (App.dbUser?.soundOff == true)
        setSound(false, item, R.drawable.outline_volume_up_24)
    }

    if (item.itemId == R.id.bt_menu_notifications) {
      if (App.dbUser?.notificationsOff == false)
        setNotifications(true, item, R.drawable.outline_notifications_off_24)
      else if (App.dbUser?.notificationsOff == true)
        setNotifications(false, item, R.drawable.outline_notifications_active_24)
    }

    return super.onOptionsItemSelected(item)
  }

  fun updateOpenAppsUI(openApps: List<ApplicationItem>) {
    rvOpenApps.adapter = getConcatOpenAdapter(openApps)
  }


  private fun setSound(soundOff: Boolean, item: MenuItem, @DrawableRes iconRes: Int) {
    App.dbUser?.let { user->
      user.soundOff = soundOff
      App.repository.updateUser(user)
    }
    item.setIcon(iconRes)
  }

  private fun setNotifications(notificationsOff: Boolean, item: MenuItem,
                               @DrawableRes iconRes: Int)
  {
    App.dbUser?.let { user->
      user.notificationsOff = notificationsOff
      App.repository.updateUser(user)
    }
    item.setIcon(iconRes)
  }

  private fun updateTakenAppsUI(takenApps: List<ApplicationItem>) {
    rvTakenApps.adapter = TakenAppAdapter(takenApps)
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
        val tomorrowHeaderText = when (App.dbUser?.headerWasPressed) {
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
            ConcatAdapter(intermediateAdapter, OpenAppAdapter(tomorrowApps, true))
          }
          false -> intermediateAdapter
        }
      }

      val todayHeaderText = getString(R.string.apps_today)
      val todayHeaderAdapter = HeaderAdapter(todayHeaderText, DAY_HEADER)

      val addAdapter = when (todayApps.isNotEmpty()) {
        true -> ConcatAdapter(todayHeaderAdapter, OpenAppAdapter(todayApps, !headerIsSet))
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
        if (apps[i].notificationHasShown) {
          when (isItToday(it)) {
            true -> todayApps.add(apps[i])
            false -> tomorrowApps.add(apps[i])
          }
        }
      }
    }
  }

  private inner class OpenAppHolder (view: View): RecyclerView.ViewHolder(view),
    View.OnClickListener
  {
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
        false -> "$price ${getString(R.string.daily_suffix)}"
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
        mainActivity.replaceOnFragment(APPLICATION_FRAGMENT_NAME, args)
      }
    }
  }

  private inner class OpenAppAdapter (apps: List<ApplicationItem?>, val hasHeader: Boolean = false):
    RecyclerView.Adapter<OpenAppHolder> ()
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpenAppHolder {
      val view = if (viewType == headerType)
        layoutInflater.inflate(R.layout.list_item_app_header, parent, false)
      else
        layoutInflater.inflate(R.layout.list_item_app, parent, false)

      return OpenAppHolder(view)
    }

    override fun getItemCount() = apps.size

    override fun getItemViewType(position: Int): Int {
      if (position == 0 && hasHeader)
        return headerType

      return itemType
    }

    override fun onBindViewHolder(holder: OpenAppHolder, position: Int) {
      Log.i (TAG, "apps[position] = ${apps[position]}")
      holder.bind(apps[position])
      holder.itemView.setOnClickListener(holder)
    }
  }


  private inner class TakenAppHolder (view: View): RecyclerView.ViewHolder(view),
    View.OnClickListener
  {
    private val tvTime: TextView = view.findViewById(R.id.tv_apps_time)
    private val tvAddress: TextView = view.findViewById(R.id.tv_apps_address)
    private lateinit var app: ApplicationItem

    @SuppressLint("SetTextI18n")
    fun bind (app: ApplicationItem) {
      this.app = app
      Log.i (TAG, "taken app is $app")

      strToDate(app.date, DATE_FORMAT)?.let {
        if (isItToday(it))
          tvTime.text = app.time
        else
          tvTime.text = "${getDateStr(it)} ${app.time}"
      }
      tvAddress.text = app.address

      if (app.state == PAYED_STATE) {
        tvTime.setTextColor(Color.GRAY)
        tvAddress.setTextColor(Color.GRAY)
      }
    }

    override fun onClick(v: View?) {
      Log.i ("TakenAppHolder", "onClick()")
      val args = Bundle().apply {
        putInt(APP_ID_ARG, app.id)
      }
      val mainActivity = context as MainActivity
      mainActivity.replaceOnFragment("Application", args)
    }
  }

  private inner class TakenAppAdapter (val apps: List<ApplicationItem>):
    RecyclerView.Adapter<TakenAppHolder> ()
  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TakenAppHolder {
      val view = layoutInflater.inflate(R.layout.list_item_taken_app, parent, false)
      return TakenAppHolder(view)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: TakenAppHolder, position: Int) {
      Log.i (TAG, "takenApps[position] = ${apps[position]}")
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
          vm.openAppsLvdList.value?.let {
            updateOpenAppsUI(it)
            App.dbUser?.let { user ->
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