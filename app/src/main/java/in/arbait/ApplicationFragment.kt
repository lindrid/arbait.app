package `in`.arbait

import `in`.arbait.database.AppState
import `in`.arbait.database.EnrollingPermission
import `in`.arbait.http.PollServerViewModel
import `in`.arbait.http.Server
import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.items.DebitCardItem
import `in`.arbait.http.items.PhoneItem
import `in`.arbait.http.items.PorterItem
import `in`.arbait.http.response.SERVER_OK
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.abs


private const val TAG = "ApplicationFragment"

const val CLOSED_STATE = 2
const val PAYED_STATE = 6


private const val DISABLE_BTN_BASE_RANGE = 10 * 1000              // 10 sec
private const val PENALTY_TIME_RANGE = 2 * 60 * 1000              // 2 min
private const val MIN_TIME_RANGE_BETWEEN_APPS = 60 * 60 * 1000    // 1 hour

private const val CAUSE_FREQUENT_APP_REFUSING = 0
private const val CAUSE_SMALL_TIME_INTERVAL = 1

const val ENROLL_REFUSE_WITHOUT_PENALTY_MAX_AMOUNT = 2

const val APP_REFUSE_DIALOG_TAG = "ApplicationRefuseDialog"
const val DEBIT_CARD_DIALOG_TAG = "DebitCardDialog"
const val APPLICATION_KEY = "application"

const val PHONE_CALL = 1
const val PHONE_WHATSAPP = 2
const val PHONE_CALL_AND_WHATSAPP = 3

const val PM_CARD = 1
const val PM_CASH = 2

class ApplicationFragment (private val appId: Int): Fragment()
{
  private var couldEnroll = false
  private var couldNotEnrollCause = -1

  private var porter: PorterItem? = null
  private var porterIsEnrolled = false
  private lateinit var lvdAppItem: MutableLiveData<ApplicationItem>

  private lateinit var server: Server
  private lateinit var supportFragmentManager: FragmentManager

  private lateinit var tvEnrolled: AppCompatTextView
  private lateinit var tvAddress: AppCompatTextView
  private lateinit var tvTime: AppCompatTextView
  private lateinit var tvIncome: AppCompatTextView
  private lateinit var tvDescription: AppCompatTextView
  private lateinit var tvPayMethod: AppCompatTextView
  private lateinit var tvPortersCount: AppCompatTextView
  private lateinit var rvPorters: RecyclerView
  private lateinit var btCallClient: AppCompatButton
  private lateinit var btEnrollRefuse: AppCompatButton
  private lateinit var btBack: AppCompatButton
  private lateinit var btChangeDebitCard: AppCompatButton
  private lateinit var nsvApp: NestedScrollView
  private lateinit var tvWhenCall: AppCompatTextView

  private val vm: PollServerViewModel by lazy {
    val mainActivity = requireActivity() as MainActivity
    mainActivity.pollServerViewModel
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_application, container, false)
    vm.rootView = view

    supportFragmentManager = requireActivity().supportFragmentManager
    server = Server(requireContext())

    Log.i (TAG, "OnCreate()")

    Log.i (TAG, "vm.lvdOpenApps = ${vm.lvdOpenApps}")
    Log.i (TAG, "vm.lvdTakenApps = ${vm.lvdTakenApps}")
    setViews(view)
    setAppItem()
    setPorter()
    updateUI()

    rvPorters.layoutManager = LinearLayoutManager(context)
    setAppObserver()
    setVisibilityToViews(porterIsEnrolled, view)

    btEnrollRefuse.setOnClickListener {
      onEnrollRefuseBtnClick(view)
    }

    btChangeDebitCard.setOnClickListener {
      onChangeDebitCardClick()
    }

    btBack.setOnClickListener {
      vm.mainActivity.replaceOnFragment("Applications")
    }

    btCallClient.setOnClickListener {
      onCallClientBtnClick()
    }

    return view
  }

  private fun onCallClientBtnClick() {
    lvdAppItem.value?.let { appItem ->
      val intent = Intent(Intent.ACTION_DIAL)
      val uriStr = "tel:${appItem.clientPhoneNumber}"
      intent.data = Uri.parse(uriStr)
      context?.startActivity(intent)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    val app = getString(R.string.app_action_bar_title)
    actionBar?.title = "$appName - $app"
  }


  private fun setAppItem(): Boolean {
    vm.lvdOpenApps[appId]?.let {
      lvdAppItem = it
      return true
    }
    vm.lvdTakenApps[appId]?.let {
      lvdAppItem = it
      return true
    }
    lvdAppItem = MutableLiveData(
      ApplicationItem(0,"","","","",
      0,"",0,0,true,0,0,
    1,0, true, listOf<PorterItem>()))
    return false
  }

  private fun setPorter() {
    lvdAppItem.value?.let { appItem ->
      Log.i (TAG, "set:lvdAppItem")
      porter = getThisUserPorter(appItem)
      Log.i (TAG, "Porter= $porter")
      porter?.let {
        porterIsEnrolled = true
      }
    }
  }

  private fun onChangeDebitCardClick() {
    App.userItem?.let { user ->
      val dcd = DebitCardDialog.newInstance(appId, user, itIsChangingCard = true)
      dcd.show(supportFragmentManager, DEBIT_CARD_DIALOG_TAG)

      supportFragmentManager.setFragmentResultListener(APPLICATION_KEY, viewLifecycleOwner)
      { _, bundle ->
        val app = bundle.getSerializable(APPLICATION_KEY) as ApplicationItem
        Log.i (TAG, "when change card, app from dialog = $app")
        lvdAppItem.value = app
      }
    }
  }

  private fun onEnrollRefuseBtnClick(view: View) {
    val porterWantToEnroll = !porterIsEnrolled
    val porterWantToRefuse = porterIsEnrolled

    val state = if (!porterIsEnrolled) AppState.ENROLLED
                else AppState.REFUSED

    GlobalScope.launch(Dispatchers.Main) {
      App.userItem?.let { user ->
        val coroutineValue = GlobalScope.async {
          App.repository.getEnrollingPermission(user.id)
        }

        val enrollingPermission = coroutineValue.await()
        val now = Date().time

        val passTooMuchTimeAfterLastClick = (enrollingPermission != null &&
          now > enrollingPermission.lastClickTime + PENALTY_TIME_RANGE
        )

        val changeStateCount =
          if (enrollingPermission == null || passTooMuchTimeAfterLastClick) {
            if (porterWantToEnroll) 1 else 0
          }
          else {
            if (stateIsChanging(enrollingPermission, state))
              enrollingPermission.changeStateCount + 1
            else
              enrollingPermission.changeStateCount
          }

        val newEnrollingPermission = EnrollingPermission(
          userId = user.id,
          changeStateCount = changeStateCount,
          enableClickTime = now,
          lastClickTime = now,
          lastState = state
        )

        if (porterWantToEnroll) {
          val debitCardDialog = DebitCardDialog.newInstance(appId, user)
          debitCardDialog.show(supportFragmentManager, DEBIT_CARD_DIALOG_TAG)
          porterIsEnrolled = false
          reactOnDebitCardDialog(view, enrollingPermission, newEnrollingPermission)
        }

        if (porterWantToRefuse) {
          ApplicationRefuseDialog().show(supportFragmentManager, APP_REFUSE_DIALOG_TAG)
          reactOnRefuseDialog(view, enrollingPermission, newEnrollingPermission, changeStateCount)
        }
      }
    }
  }

  private fun stateIsChanging(ep: EnrollingPermission, state: AppState): Boolean {
    return ep.lastState == AppState.ENROLLED && state == AppState.REFUSED ||
        ep.lastState == AppState.REFUSED && state == AppState.ENROLLED
  }

  private fun reactOnDebitCardDialog(view: View, ep: EnrollingPermission?, newEp: EnrollingPermission)
  {
    supportFragmentManager.setFragmentResultListener(APPLICATION_KEY, viewLifecycleOwner)
    { _, bundle ->
      val app = bundle.getSerializable(APPLICATION_KEY) as ApplicationItem
      Log.i (TAG, "app from dialog = $app")

      vm.lvdOpenApps.remove(appId)
      vm.lvdTakenApps[appId] = MutableLiveData(app)
      vm.lvdTakenApps[appId]?.let {
        lvdAppItem = it
      }

      setAppObserver()

      Log.i (TAG, "newEnrollingPermission = $newEp")
      addOrUpdateEnrollPermission(ep, newEp)
      porterIsEnrolled = true

      btEnrollRefuse.text = getString(R.string.app_refuse)
      setVisibilityToViews(true, view)
      updateUI()
    }
  }

  private fun reactOnRefuseDialog(view: View,
                                  ep: EnrollingPermission?, newEp: EnrollingPermission,
                                  changeStateCount: Int)
  {
    supportFragmentManager.setFragmentResultListener(OK_KEY, viewLifecycleOwner)
    { _, bundle ->
      Log.i (TAG, "setFragmentResultListener")
      server.refuseApp(appId) { appUserResponse ->
        Log.i (TAG, "setFragmentResultListener. response.type=${appUserResponse.response.type}")
        if (appUserResponse.response.type == SERVER_OK) {
          lvdAppItem.value = appUserResponse.app

          vm.lvdTakenApps.remove(appId)
          vm.lvdOpenApps[appId] = MutableLiveData(lvdAppItem.value)
          vm.lvdOpenApps[appId]?.let {
            lvdAppItem = it
          }
          setAppObserver()

          if (changeStateCount > ENROLL_REFUSE_WITHOUT_PENALTY_MAX_AMOUNT && ep != null) {
            val now = Date().time
            val p: Int = changeStateCount / 2
            var coefficient = 0

            for (i in 1..p)
              coefficient += 3

            Log.i(TAG, "coefficient=$coefficient")

            newEp.enableClickTime = now + coefficient * DISABLE_BTN_BASE_RANGE
            btEnrollRefuse.isEnabled = false
          }

          Log.i (TAG, "newEnrollingPermission = $newEp")
          addOrUpdateEnrollPermission(ep, newEp)
          porterIsEnrolled = false

          btEnrollRefuse.text = getString(R.string.app_enroll)
          setVisibilityToViews(false, view)
          updateUI()
        }
      }
    }
  }

  private fun addOrUpdateEnrollPermission(ep: EnrollingPermission?, newEp: EnrollingPermission) {
    if (ep == null)
      App.repository.addEnrollingPermission(newEp)
    else
      App.repository.updateEnrollingPermission(newEp)
  }

  private fun setAppObserver() {
    lvdAppItem.observe(viewLifecycleOwner,
      Observer { appItem ->
        Log.i (TAG, "lvdAppItem=$lvdAppItem, observer appItem is $appItem")
        if (appItem == null) {
          tvEnrolled.text = if (porterIsEnrolled)
            getString(R.string.app_you_were_deleted)
          else
            getString(R.string.app_is_gone)

          tvEnrolled.textSize = 26.0f
          tvEnrolled.setTextColor(Color.RED)
          tvEnrolled.visibility = View.VISIBLE
          btEnrollRefuse.isEnabled = false
          btCallClient.isEnabled = false
          setLayoutConstraints(tvEnrolledIsVisible = true, btEnrollRefuse, withoutPorters = true)
        }
        appItem?.let {
          if (appItem.porters != null)
            porter = getThisUserPorter(it)
          Log.i (TAG, "porter is $porter")
          updateUI()
        }
      }
    )
  }

  private fun updateUI() {
    Log.i ("track", "updateUI")
    setViewsTexts()
    updatePorters()

    GlobalScope.launch(Dispatchers.Main)
    {
      updateBtnEnabling()

      if (!porterIsEnrolled) {
        if (couldNotEnrollCause != CAUSE_FREQUENT_APP_REFUSING) {
          couldEnroll = true
          couldNotEnrollCause = -1
        }
        val takenApps = vm.lvdTakenApps
        val thisAppTimeStr = lvdAppItem.value?.time

        for (i in takenApps.keys)
        {
          if (takenApps[i] != null && takenApps[i]?.value != null)
            if (takenApps[i]?.value?.id == lvdAppItem.value?.id)
              continue

          val takenAppTimeStr = takenApps[i]?.value?.time
          takenAppTimeStr?.let { takenStr ->
            thisAppTimeStr?.let { thisStr ->
              val takenDate = strToDate(takenStr, "HH:mm")
              val thisDate = strToDate(thisStr, "HH:mm")
              takenDate?.let {
                thisDate?.let {
                  val diff = abs(takenDate.time - thisDate.time)
                  if (diff < MIN_TIME_RANGE_BETWEEN_APPS) {
                    couldEnroll = false
                    couldNotEnrollCause = CAUSE_SMALL_TIME_INTERVAL
                  }
                }
              }
            }
          }
          if (!couldEnroll) break
        }

        if (couldEnroll) {
          tvEnrolled.visibility = View.INVISIBLE
          btEnrollRefuse.isEnabled = true
          setLayoutConstraints(tvEnrolledIsVisible = false, btEnrollRefuse)
        }
        else {
          tvEnrolled.text = when (couldNotEnrollCause) {
            CAUSE_FREQUENT_APP_REFUSING -> getString(R.string.app_could_not_enroll_cause_refuses)
            CAUSE_SMALL_TIME_INTERVAL -> getString(R.string.app_could_not_enroll_cause_interval)
            else -> getString(R.string.app_could_not_enroll_cause_unknown)
          }
          tvEnrolled.textSize = 26.0f
          tvEnrolled.setTextColor(Color.RED)
          tvEnrolled.visibility = View.VISIBLE
          btEnrollRefuse.isEnabled = false
          setLayoutConstraints(tvEnrolledIsVisible = true, btEnrollRefuse, withoutPorters = true)
        }
      }
      else {
        btEnrollRefuse.isEnabled = true
      }

      val app = lvdAppItem.value
      if (app != null && app.state > CLOSED_STATE) {
        btEnrollRefuse.isEnabled = false
      }

      if (app != null && app.state == PAYED_STATE) {
        tvEnrolled.textSize = 26.0f
        tvEnrolled.text = Html.fromHtml(getString(
          R.string.app_payed,
          porter?.pivot?.workHours,
          porter?.pivot?.money
        ))
        btCallClient.isEnabled = false
      }

      lvdAppItem.value?.let { app ->
        if (app.id == 0)
          btEnrollRefuse.isEnabled = false
      }
    }
  }

  private fun updatePorters() {
    lvdAppItem.value?.let {
      it.porters?.let { porters ->
        rvPorters.adapter = PortersAdapter(porters)
      }
    }
  }

  private suspend fun updateBtnEnabling() {
    App.userItem?.let { user ->
      val coroutineValue = GlobalScope.async {
        App.repository.getEnrollingPermission(user.id)
      }

      val ep = coroutineValue.await()
      val now = Date().time

      if (couldNotEnrollCause != CAUSE_SMALL_TIME_INTERVAL) {
        if (ep == null) {
          btEnrollRefuse.isEnabled = true
          couldEnroll = true
          couldNotEnrollCause = -1
        }
        else {
          btEnrollRefuse.isEnabled = (now >= ep.enableClickTime)
          if (now >= ep.enableClickTime) {
            couldEnroll = true
            couldNotEnrollCause = -1
          }
          else {
            couldEnroll = false
            couldNotEnrollCause = CAUSE_FREQUENT_APP_REFUSING
          }
        }
      }
    }
  }

  private fun setVisibilityToViews(porterIsEnrolled: Boolean, view: View) {
    if (porterIsEnrolled) {
      tvEnrolled.visibility = View.VISIBLE
      tvEnrolled.textSize = 34.0f
      tvEnrolled.text = getString(R.string.app_you_are_enrolled)
      tvEnrolled.setTextColor(resources.getColor(R.color.emerald))

      rvPorters.visibility = View.VISIBLE
      btCallClient.visibility = View.VISIBLE
      tvWhenCall.visibility = View.VISIBLE
      setLayoutConstraints(tvEnrolledIsVisible = true, view)
    }
    else {
      tvEnrolled.visibility = View.INVISIBLE
      rvPorters.visibility = View.INVISIBLE
      btCallClient.visibility = View.INVISIBLE
      tvWhenCall.visibility = View.INVISIBLE
      setLayoutConstraints(tvEnrolledIsVisible = false, view)
    }
  }

  private fun setLayoutConstraints(tvEnrolledIsVisible: Boolean, view: View,
                                   withoutPorters: Boolean = false)
  {
    if (tvEnrolledIsVisible) {
      val ap = tvAddress.layoutParams as ConstraintLayout.LayoutParams
      ap.topToTop = ConstraintLayout.LayoutParams.UNSET
      ap.topToBottom = tvEnrolled.id
      tvAddress.layoutParams = ap

      if (withoutPorters) {
        val dp = tvDescription.layoutParams as ConstraintLayout.LayoutParams
        dp.topToBottom = tvPortersCount.id
        tvDescription.layoutParams = dp
      }
      else {
        val dp = tvDescription.layoutParams as ConstraintLayout.LayoutParams
        dp.topToBottom = rvPorters.id
        tvDescription.layoutParams = dp
      }

      val np = nsvApp.layoutParams as ConstraintLayout.LayoutParams
      np.bottomToTop = btCallClient.id
      nsvApp.layoutParams = np
    }
    else {
      val ap = tvAddress.layoutParams as ConstraintLayout.LayoutParams
      ap.topToTop = view.id
      ap.topToBottom = ConstraintLayout.LayoutParams.UNSET
      tvAddress.layoutParams = ap

      val dp = tvDescription.layoutParams as ConstraintLayout.LayoutParams
      dp.topToBottom = tvPortersCount.id
      tvDescription.layoutParams = dp

      val np = nsvApp.layoutParams as ConstraintLayout.LayoutParams
      np.bottomToTop = btBack.id
      nsvApp.layoutParams = np
    }
  }


  private fun getThisUserPorter(app: ApplicationItem): PorterItem? {
    var porter: PorterItem? = null
    App.dbUser?.let { user ->
      Log.i (TAG, "getThisUserPorter: app = $app, porters = ${app.porters}")
      app.porters?.let {
        for (i in app.porters.indices) {
          if (user.id == app.porters[i].user.id) {
            Log.i (TAG, "user.id=${user.id}")
            porter = app.porters[i]
            break
          }
        }
      }
    }
    return porter
  }


  private fun setViews(view: View) {
    tvEnrolled = view.findViewById(R.id.tv_app_enrolled)
    tvAddress = view.findViewById(R.id.tv_app_address)
    tvTime = view.findViewById(R.id.tv_app_time)
    tvIncome = view.findViewById(R.id.tv_app_worker_income)
    tvDescription = view.findViewById(R.id.tv_app_description)
    tvPayMethod = view.findViewById(R.id.tv_app_pay_method)
    tvPortersCount = view.findViewById(R.id.tv_app_porters)
    rvPorters = view.findViewById(R.id.rv_app_porters)
    btCallClient = view.findViewById(R.id.bt_app_call_client)
    btEnrollRefuse = view.findViewById(R.id.bt_app_enroll_refuse)
    btBack = view.findViewById(R.id.bt_app_back)
    btChangeDebitCard = view.findViewById(R.id.bt_app_change_debit_card)
    nsvApp = view.findViewById(R.id.nsv_app)
    tvWhenCall = view.findViewById(R.id.tv_app_when_call)
  }

  private fun setViewsTexts() {
    lvdAppItem.value?.let { appItem ->
      tvAddress.text = Html.fromHtml(getString(R.string.app_address, appItem.address))

      val date = strToDate(appItem.date, DATE_FORMAT)
      date?.let {
        val time = if (isItToday(it))
          "${appItem.time}"
        else
          "${getDateStr(it)} ${appItem.time}"
        tvTime.text = Html.fromHtml(getString(R.string.app_time, time))
      }

      val suffix = if (appItem.hourlyJob)
        " " + getString(R.string.hourly_suffix)
      else
        getString(R.string.daily_suffix)
      val income = "${appItem.priceForWorker}$suffix"
      tvIncome.text = Html.fromHtml(getString(R.string.app_worker_income, income))

      tvDescription.text = Html.fromHtml(getString(R.string.app_description, appItem.whatToDo))

      val payMethod = when (appItem.payMethod) {
        PM_CARD -> getString(R.string.app_on_card)
        PM_CASH -> getString(R.string.app_cash)
        else -> ""
      }

      if (porterIsEnrolled) {
        val debitCardNumber = getDebitCardNumber()
        btChangeDebitCard.text = debitCardNumber
        btChangeDebitCard.visibility = View.VISIBLE
      }
      else {
        btChangeDebitCard.visibility = View.INVISIBLE
      }

      tvPayMethod.text = Html.fromHtml(getString(R.string.app_pay_method, payMethod))

      val workers = "${appItem.workerCount} / ${appItem.workerTotal}"
      tvPortersCount.text = Html.fromHtml(getString(R.string.app_worker_count, workers))

      btEnrollRefuse.text = if (porterIsEnrolled)
        getString(R.string.app_refuse)
      else
        getString(R.string.app_enroll)
      //btCallClient.visibility = View.INVISIBLE
    }
  }

  private fun getDebitCardNumber(): String {
    porter?.let { porter ->
      val dcId = porter.pivot.appDebitCardId
      Log.i (TAG, "debitCardId = $dcId")
      var dcStr = getString(R.string.app_no_card)

      if (dcId != 0) {
        val debitCards = porter.user.debitCards
        for (i in debitCards.indices)
          if (debitCards[i].id == dcId) {
            dcStr = debitCards[i].number
            break
          }
      }

      return dcStr
    }

    return ""
  }

  private fun getMainDebitCard(): DebitCardItem? {
    var dc: DebitCardItem? = null
    porter?.let {
      val cards = it.user.debitCards
      for (i in cards.indices) {
        if (cards[i].main) {
          dc = cards[i]
          break
        }
      }
    }
    return dc
  }

  private inner class PorterHolder (view: View) : RecyclerView.ViewHolder(view) {
    private val tvName: TextView = view.findViewById(R.id.tv_porter_name)
    private val ivCall: ImageView = view.findViewById(R.id.iv_porter_call)
    private val ivWhatsapp: ImageView = view.findViewById(R.id.iv_porter_whatsapp)

    fun bind(porterName: String, phones: List<PhoneItem>) {
      tvName.text = porterName

      var phoneWhatsapp = ""
      var phoneCall = ""
      for (i in phones.indices) {
        if (phones[i].type == PHONE_CALL || phones[i].type == PHONE_CALL_AND_WHATSAPP)
          phoneCall = phones[i].number
        if (phones[i].type == PHONE_WHATSAPP || phones[i].type == PHONE_CALL_AND_WHATSAPP)
          phoneWhatsapp = phones[i].number
      }

      ivCall.setOnClickListener {
        val intent = Intent(Intent.ACTION_DIAL)
        val uriStr = "tel:$phoneCall"
        intent.data = Uri.parse(uriStr)
        context?.startActivity(intent)
      }

      ivWhatsapp.setOnClickListener {
        openWhatsappContact(phoneWhatsapp)
      }
    }

    private fun openWhatsappContact(number: String) {
      Log.i ("openWhatsappContact", "https://wa.me/$number")
      val uri = Uri.parse("https://wa.me/$number")
      val intent = Intent(Intent.ACTION_VIEW, uri)
      startActivity(intent)
    }
  }

  private inner class PortersAdapter (porters: List<PorterItem>):
    RecyclerView.Adapter<PorterHolder>()
  {
    private val porters = mutableListOf<PorterItem>()

    init {
      App.userItem?.let { user ->
        for (i in porters.indices) {
          if (porters[i].user.id != user.id)
            this.porters.add(porters[i])
        }
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PorterHolder {
      val view = LayoutInflater.from(parent.context).inflate(
        R.layout.list_item_porter, parent, false
      )
      return PorterHolder(view)
    }

    override fun getItemCount() = porters.size

    override fun onBindViewHolder(holder: PorterHolder, position: Int) {
      holder.bind(porters[position].user.name, porters[position].user.phones)
    }
  }
}