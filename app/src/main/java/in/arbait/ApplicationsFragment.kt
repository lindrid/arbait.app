package `in`.arbait

import `in`.arbait.commission.COMMISSION_ARG
import `in`.arbait.http.PollServerViewModel
import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.poll_service.Action
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

const val DATE_FORMAT = "yyyy-MM-dd"
const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
const val TIME_FORMAT = "HH:mm"
const val APP_ID_ARG = "applicationId"
const val APPLICATION_FRAGMENT_NAME = "Application"
const val COMMISSION_FRAGMENT_NAME = "Commission"
const val MAIN_HEADER_TEXT_SIZE = 28f
const val HEADER_TEXT_SIZE = 24f
const val TEXT_SIZE = 18f

val OPEN_HEADER_COLOR = Color.parseColor("#2E8B57")

/* Headers and messages (those that are instead of applications) */
private const val MAIN_HEADER = 0
private const val DAY_HEADER = 1
private const val TEXT = 2

private const val TAG = "ApplicationsFragment"

class ApplicationsFragment: Fragment()
{
  private var todayApps = mutableListOf<ApplicationItem>()
  private var tomorrowApps = mutableListOf<ApplicationItem>()
  private var showTomorrowApps = true

  private lateinit var rootView: View
  private lateinit var rvOpenApps: RecyclerView
  private lateinit var rvTakenApps: RecyclerView
  private lateinit var llTakenApps: LinearLayout
  private lateinit var tvUserCannot: AppCompatTextView
  private lateinit var tvCommission: AppCompatTextView
  private lateinit var tvCommissionConfirmation: AppCompatTextView
  private lateinit var btPayCommission: AppCompatButton

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

    tvUserCannot = view.findViewById(R.id.tv_apps_cannot)
    tvCommission = view.findViewById(R.id.tv_apps_commission)
    tvCommissionConfirmation = view.findViewById(R.id.tv_apps_commission_confirmation)
    btPayCommission = view.findViewById(R.id.bt_apps_pay_commission)

    vm.rootView = rootView

    doOnOpenAppsChange()
    doOnTakenAppsChange()

    // заявки считываются с сервера нашим бесконечным PollService'ом
    vm.serviceDoAction(Action.START)
    vm.bindService()

    setHasOptionsMenu(true)
    setBtPayCommissionClickListener()

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    actionBar?.title = "$appName"
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.main_menu, menu)

    val soundItem = menu.findItem(R.id.bt_menu_sound)
    val notificationsItem = menu.findItem(R.id.bt_menu_notifications)

    if (App.dbUser?.soundOff == true)
      soundItem.setIcon(R.drawable.outline_volume_off_black_24)

    if (App.dbUser?.notificationsOff == true)
      notificationsItem.setIcon(R.drawable.outline_notifications_off_black_24)

    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.bt_menu_sound) {
      if (App.dbUser?.soundOff == false)
        setSound(true, item, R.drawable.outline_volume_off_black_24)
      else if (App.dbUser?.soundOff == true)
        setSound(false, item, R.drawable.outline_volume_up_black_24)
    }

    if (item.itemId == R.id.bt_menu_notifications) {
      if (App.dbUser?.notificationsOff == false)
        setNotifications(true, item, R.drawable.outline_notifications_off_black_24)
      else if (App.dbUser?.notificationsOff == true)
        setNotifications(false, item, R.drawable.outline_notifications_active_black_24)
    }

    if (item.itemId == R.id.bt_menu_whatsapp) {
      vm.lvdDispatcherWhatsapp.value?.let { phone ->
        openWhatsappContact(requireActivity(), phone)
      }
    }

    if (item.itemId == R.id.bt_menu_phone) {
      vm.lvdDispatcherPhoneCall.value?.let { phone ->
        phoneCall(requireContext(), phone)
      }
    }

    return super.onOptionsItemSelected(item)
  }

  fun updateOpenAppsUI(openApps: List<ApplicationItem>) {
    rvOpenApps.adapter = getConcatOpenAdapter(openApps)
  }


  private fun setBtPayCommissionClickListener() {
    btPayCommission.setOnClickListener {
      vm.takenAppsLvdList.value?.let { takenApps ->
        vm.calcCommissions(takenApps).also {
          val commission = it.first

          val args = Bundle().apply {
            putInt(COMMISSION_ARG, commission)
          }
          val mainActivity = context as MainActivity
          mainActivity.replaceOnFragment(COMMISSION_FRAGMENT_NAME, args)
        }
      }
    }
  }

  private fun updateCommissionUI (takenApps: List<ApplicationItem>) {
    val commissions = vm.calcCommissions(takenApps)
    val commission = commissions.first
    val notConfirmedCommission = commissions.second

    if (commission == 0) {
      setCommissionUiVisibility (View.INVISIBLE, View.INVISIBLE,
        View.INVISIBLE, View.INVISIBLE)

      if (notConfirmedCommission > 0) {
        tvCommissionConfirmation.text = getString(R.string.apps_commission_confirmation, notConfirmedCommission)
        setCommissionConfirmationToTakenAppsTop()
        setUserCannotToConfirmationTop()
        tvUserCannot.visibility = View.VISIBLE
        tvCommissionConfirmation.visibility = View.VISIBLE
      }
    }
    else {
      tvCommission.text = getString(R.string.apps_commission, commission)
      setCommissionUiVisibility (View.VISIBLE, View.VISIBLE,
        View.VISIBLE, View.VISIBLE)

      if (notConfirmedCommission > 0) {
        tvCommissionConfirmation.text = getString(R.string.apps_commission_confirmation,
          notConfirmedCommission)
        setUserCannotToConfirmationTop()
        setCommissionConfirmationToCommissionTop()
      }
      else {
        setUserCannotToCommissionTop()
        tvCommissionConfirmation.visibility = View.INVISIBLE
      }
    }
  }

  private fun setUserCannotToCommissionTop() {
    val lp = tvUserCannot.layoutParams as ConstraintLayout.LayoutParams
    lp.bottomToTop = tvCommission.id
    tvUserCannot.layoutParams = lp
  }

  private fun setUserCannotToConfirmationTop() {
    val lp = tvUserCannot.layoutParams as ConstraintLayout.LayoutParams
    lp.bottomToTop = tvCommissionConfirmation.id
    tvUserCannot.layoutParams = lp
  }

  private fun setCommissionConfirmationToCommissionTop() {
    val lp = tvCommissionConfirmation.layoutParams as ConstraintLayout.LayoutParams
    lp.bottomToTop = tvCommission.id
    tvCommissionConfirmation.layoutParams = lp
  }

  private fun setCommissionConfirmationToTakenAppsTop() {
    val lp = tvCommissionConfirmation.layoutParams as ConstraintLayout.LayoutParams
    lp.bottomToTop = llTakenApps.id
    tvCommissionConfirmation.layoutParams = lp
  }

  private fun setCommissionUiVisibility(userCantVis: Int, commissionVis: Int,
                                        commConfirmationVis: Int, btPayVis: Int)
  {
    tvUserCannot.visibility = userCantVis
    tvCommission.visibility = commissionVis
    tvCommissionConfirmation.visibility = commConfirmationVis
    btPayCommission.visibility = btPayVis
  }

  private fun doOnTakenAppsChange() {
    vm.doOnTakenAppsChange = {
      val takenApps = vm.takenAppsLvdList.value
      takenApps?.let {
        Log.i(TAG, "Taken apps size is ${it.size}")
        Log.i(TAG, "takenApps is $it")
        updateTakenAppsUI(it.toMutableList())
        updateCommissionUI(it)

        if (it.isEmpty() && llTakenApps.visibility == View.VISIBLE)
          llTakenApps.visibility = View.INVISIBLE

        if (it.isNotEmpty() && llTakenApps.visibility == View.INVISIBLE)
          llTakenApps.visibility = View.VISIBLE
      }
    }
    vm.doOnTakenAppsChange()
  }

  private fun doOnOpenAppsChange() {
    vm.doOnOpenAppsChange = {
      val openApps = vm.openAppsLvdList.value
      openApps?.let {
        Log.i(TAG, "Open apps size is ${it.size}")
        Log.i(TAG, "openApps is $it")
        setTodayAndTomorrowApps(it)
        showTomorrowApps = true//todayApps.isEmpty()
        updateOpenAppsUI(it)
      }
    }
    vm.doOnOpenAppsChange()
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

  private fun updateTakenAppsUI(takenApps: MutableList<ApplicationItem>) {
    takenApps.sortWith(Comparator { lhs, rhs ->
      // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
      if (lhs.date > rhs.date) 1
      else if (lhs.date == rhs.date && lhs.time > rhs.time) 1
      else -1
    })
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

      if (app.state > CLOSED_STATE) {
        tvTime.setTextColor(Color.GRAY)
        tvAddress.setTextColor(Color.GRAY)
      }
      else if (app.itIsTimeToConfirm) {
        tvTime.setTextColor(Color.RED)
        tvAddress.setTextColor(Color.RED)
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
    private val tvAppsHeader: TextView = view.findViewById (R.id.tv_header)

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
          showTomorrowApps = true
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