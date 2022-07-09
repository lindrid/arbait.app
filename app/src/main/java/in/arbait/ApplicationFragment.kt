package `in`.arbait

import `in`.arbait.commission.COMMISSION_ARG
import `in`.arbait.database.AppHistory
import `in`.arbait.database.AppState
import `in`.arbait.database.Consequences
import `in`.arbait.database.EnrollingPermission
import `in`.arbait.helpers.setLayoutParams
import `in`.arbait.helpers.setVisibilityToViews
import `in`.arbait.http.PollServerViewModel
import `in`.arbait.http.ReactionOnResponse
import `in`.arbait.http.Server
import `in`.arbait.http.appIsConfirmed
import `in`.arbait.http.items.*
import `in`.arbait.http.poll_service.DAY
import `in`.arbait.http.poll_service.HOUR
import `in`.arbait.http.poll_service.MINUTE
import `in`.arbait.http.poll_service.removeNotification
import `in`.arbait.http.response.*
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.util.Log
import android.view.*
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
import kotlin.math.roundToInt


private const val TAG = "ApplicationFragment"
private const val REFUSE_DIALOG_TAG = "REFUSE_DIALOG"
private const val RULES_DIALOG_TAG = "RULES_DIALOG"

const val CLOSED_STATE = 2
const val READY_TO_PAY = 4

private const val MAX_REFUSAL_TIME_AFTER_ENROLL_WITHOUT_CONSEQUENCES = 10 * 1000
private const val BANN_FOR_NON_APPEARANCE = 30L * 24L * HOUR.toLong()
private const val MAX_BANN_FOR_REFUSE = 15 * 24 * HOUR
private const val MAX_MIN_COUNT = 60
private const val MAX_REFUSAL_TIME_WITHOUT_CONSEQUENCES = MAX_MIN_COUNT * MINUTE

private const val BEFORE_RULES_DELAY_IN_SECONDS = 1L
private const val SHOW_RULES_DELAY_TIME: Long = 7 * 24 * 60 * 60 * 1000 // 7 days

private const val DISABLE_BTN_BASE_RANGE = 10 * 1000              // 10 sec
private const val PENALTY_TIME_RANGE = 2 * 60 * 1000              // 2 min
private const val MIN_TIME_RANGE_BETWEEN_APPS = 60 * 60 * 1000    // 1 hour

private const val CAUSE_FREQUENT_APP_REFUSING = 0
private const val CAUSE_SMALL_TIME_INTERVAL = 1
private const val CAUSE_COMMISSION_IS_NOT_PAYED = 2
private const val CAUSE_PAY_IS_NOT_CONFIRMED = 3
private const val CAUSE_BANNED = 4

const val APP_REFUSE_DIALOG_TAG = "ApplicationRefuseDialog"
const val DEBIT_CARD_DIALOG_TAG = "DebitCardDialog"

const val ENROLL_REFUSE_WITHOUT_PENALTY_MAX_AMOUNT = 2

const val APPLICATION_KEY = "application"

const val PHONE_CALL = 1
const val PHONE_WHATSAPP = 2
const val PHONE_CALL_AND_WHATSAPP = 3

const val PHONE_CALL_TEXT = "c"
const val PHONE_WHATSAPP_TEXT = "w"
const val PHONE_CALL_AND_WHATSAPP_TEXT = "cw"

const val PM_CARD = 1
const val PM_CASH = 2

class ApplicationFragment (private val appId: Int): Fragment()
{
  private var removedFromApp = false
  private var appWasDeleted = false
  private var canEnroll = false
  private var canNotEnrollCause = -1
  private var lvdEnrollResult: MutableLiveData<Boolean> = MutableLiveData(false)

  private var porter: PorterItem? = null
  private var porterIsEnrolled = false
  private lateinit var lvdAppItem: MutableLiveData<ApplicationItem>
  private var lastAppItem: ApplicationItem? = null

  private lateinit var server: Server
  private lateinit var supportFragmentManager: FragmentManager

  private lateinit var tvStatus: AppCompatTextView
  private lateinit var tvEndStatus: AppCompatTextView
  private lateinit var tvCommissionStatus: AppCompatTextView
  private lateinit var tvConfirmationStatus: AppCompatTextView
  private lateinit var btPay: AppCompatButton
  private lateinit var tvAddress: AppCompatTextView
  private lateinit var tvTime: AppCompatTextView
  private lateinit var tvMoney: AppCompatTextView
  private lateinit var tvCommission: AppCompatTextView
  private lateinit var tvPayMethod: AppCompatTextView
  private lateinit var tvPayFromWho: AppCompatTextView
  private lateinit var tvDescription: AppCompatTextView
  private lateinit var tvPortersCount: AppCompatTextView
  private lateinit var rvPorters: RecyclerView
  private lateinit var rvWorkers: RecyclerView
  private lateinit var btCallClient: AppCompatButton
  private lateinit var btClientWhatsapp: AppCompatButton
  private lateinit var btChangeDebitCard: AppCompatButton
  private lateinit var btEnrollRefuse: AppCompatButton
  private lateinit var btBack: AppCompatButton
  private lateinit var nsvAppArea: NestedScrollView
  private lateinit var tvWhenCall: AppCompatTextView

  private lateinit var btAppConfirm: AppCompatButton
  private lateinit var tvAppConfirm: AppCompatTextView

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

    Log.i (TAG, "vm.lvdOpenApps = ${vm.openAppsLvdItems}")
    Log.i (TAG, "vm.lvdTakenApps = ${vm.takenAppsLvdItems}")
    setViews(view)
    setAppItem()
    setPorter()
    setHasOptionsMenu(true)

    rvPorters.layoutManager = LinearLayoutManager(context)
    rvWorkers.layoutManager = LinearLayoutManager(context)

    updateUI()

    setAppObserver()
    if (porterIsEnrolled)
      setUiToEnrolledState()
    else
      setUiToRefusedState()

    setDeletedAppObserver()
    setRemovedFromAppObserver()

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    actionBar?.title = "$appName"
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.app_card_menu, menu)

    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
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


  private fun setDeletedAppObserver() {
    vm.deletedAppsLvdList.observe(viewLifecycleOwner,
      Observer { deletedApps ->
        deletedApps?.let {
          deletedApps.forEach {
            if (appId == it.id) {
              setUiToDeletedState()
            }
          }
        }
      }
    )
  }

  private fun setRemovedFromAppObserver() {
    vm.removedFromAppsLvdList.observe(viewLifecycleOwner,
      Observer { removedFromApps ->
        removedFromApps?.let {
          for (i in removedFromApps.indices) {
            if (removedFromApps[i].id == appId) {
              setUiToRemovedFromAppState()
            }
          }
        }
      }
    )
  }

  private fun onCallClientBtnClick() {
    lvdAppItem.value?.let { appItem ->
      phoneCall(requireContext(), appItem.clientPhoneNumber)
    }
  }

  private fun setBtPayClickListener() {
    btPay.setOnClickListener {
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

  private fun setAppItem(): Boolean {
    vm.takenAppsLvdItems[appId]?.let {
      if (it.value != null) {
        lvdAppItem = it
        return true
      }
    }

    vm.openAppsLvdItems[appId]?.let {
      if (it.value != null) {
        lvdAppItem = it
        return true
      }
    }

    lvdAppItem = MutableLiveData(
      ApplicationItem(0,"","","","",
        0,false,"",0,0,true,0,0,
        1,0, false,"", "", listOf<PorterItem>(),
        listOf<WorkerItem>(),true))
    lastAppItem?.let {
      lvdAppItem.value = it
    }
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

  private fun onEnrollRefuseBtnClick() {
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
          btEnrollRefuse.isEnabled = false
          lvdEnrollResult.value = false

          enrollPorterWithoutCard (enrollingPermission, newEnrollingPermission)
          setEnrollResultObserver(
            enrollingPermission,
            newEnrollingPermission,
            changeStateCount,
            user
          )

          btEnrollRefuse.isEnabled = true
        }

        if (porterWantToRefuse) {
          var consequences = Consequences.NOTHING
          var bannTimeDaysCount = 0
          var bannTimeHoursCount = 0
          var decreaseRatingPercent = 0

          lvdAppItem.value?.let { appItem ->
            val appHistoryCoroutineVal = GlobalScope.async {
              App.repository.getAppHistory(appItem.id)
            }

            val appHistory = appHistoryCoroutineVal.await()
            if (appHistory == null) {
              val ah = AppHistory(
                appId = appItem.id,
                refuseTime = Date().time
              )
              App.repository.addAppHistory(ah)
            }
            else {
              appHistory.refuseTime = Date().time
              App.repository.updateAppHistory(appHistory)
            }

            appHistory?.let { ah ->
              if (
                ah.refuseTime > 0 &&
                ah.enrollTime > 0 &&
                ah.refuseTime - ah.enrollTime > MAX_REFUSAL_TIME_AFTER_ENROLL_WITHOUT_CONSEQUENCES
              ) {
                vm.serverTimeMlsc?.let { serverTimeMlsc ->
                  val appTimeMlsc = getAppTimeMlsc(appItem, serverTimeMlsc)
                  val diff = abs(appTimeMlsc - serverTimeMlsc)
                  if (diff < MAX_REFUSAL_TIME_WITHOUT_CONSEQUENCES) {
                    consequences = Consequences.DECREASE_RATING_AND_BANN
                    val diffInMins: Double = diff.toDouble() / MINUTE
                    val divider: Double = 1 - diffInMins / MAX_MIN_COUNT
                    decreaseRatingPercent = (divider * 100).roundToInt()
                    val bannTime = divider * MAX_BANN_FOR_REFUSE

                    val bannTimeInDays = bannTime / (24 * HOUR)
                    bannTimeDaysCount = bannTimeInDays.toInt()

                    if (bannTimeInDays < 1.0) {
                      bannTimeHoursCount = (bannTimeInDays * 24).roundToInt()
                    }
                    else {
                      bannTimeDaysCount = bannTimeInDays.toInt()
                      Log.i(TAG, "Days: $bannTimeDaysCount")
                      bannTimeHoursCount = ((bannTimeInDays - bannTimeDaysCount.toDouble()) * 24)
                        .roundToInt()
                      Log.i(TAG, "hours: $bannTimeHoursCount")
                    }
                  }
                }
              }
            }
          }

          val refuseDialog = ApplicationRefuseDialog(consequences, decreaseRatingPercent,
            bannTimeDaysCount, bannTimeHoursCount)

          refuseDialog.show(supportFragmentManager, REFUSE_DIALOG_TAG)
          supportFragmentManager.setFragmentResultListener(OK_KEY, viewLifecycleOwner)
          { _, bundle ->
            Log.i (TAG, "Dialog OK, refuse from app")
            if (bundle.getBoolean(NO_CONSEQUENCES_KEY)) {
              refuseFromApp(
                enrollingPermission, newEnrollingPermission,
                changeStateCount, null, null
              )
            }
            else {
              val bannUntilMlsc =
                Date().time + (bannTimeDaysCount * DAY + bannTimeHoursCount * HOUR).toLong()
              val bannUntil = dateToStr(Date(bannUntilMlsc), DATE_TIME_FORMAT)
              refuseFromApp(
                enrollingPermission, newEnrollingPermission,
                changeStateCount, bannUntil, decreaseRatingPercent
              )
            }
          }
        }
      }
    }
  }

  private suspend fun setEnrollResultObserver(enrollingPermission: EnrollingPermission?,
                                              newEnrollingPermission: EnrollingPermission,
                                              changeStateCount: Int,
                                              user: UserItem
  ) {
    lvdEnrollResult.observe(viewLifecycleOwner,
      Observer { enrollResult ->
        GlobalScope.launch(Dispatchers.Main) {
          lvdAppItem.value?.let { appItem ->
            if (enrollResult == true) {
              if (appItem.payMethod == PM_CARD) {
                val enrollWithDebitCardDialog = DebitCardDialog.newInstance(appId, user)
                enrollWithDebitCardDialog.show(supportFragmentManager, DEBIT_CARD_DIALOG_TAG)
                porterIsEnrolled = false
                view?.let {
                  reactOnEnrollWithCard(
                    it, enrollingPermission, newEnrollingPermission,
                    changeStateCount
                  )
                }
              }

              val appHistoryCoroutineVal = GlobalScope.async {
                App.repository.getAppHistory(appItem.id)
              }

              val appHistory = appHistoryCoroutineVal.await()
              if (appHistory == null) {
                val ah = AppHistory(
                  appId = appItem.id,
                  enrollTime = Date().time
                )
                App.repository.addAppHistory(ah)
              }
              else {
                appHistory.enrollTime = Date().time
                App.repository.updateAppHistory(appHistory)
              }
            }
          }
        }
      }
    )
  }

  private fun showRulesDialog(enrollingPermission: EnrollingPermission?,
                              newEnrollingPermission: EnrollingPermission,
                              changeStateCount: Int
  ) {
    var showDialog = false
    App.dbUser?.let { u ->
      val now = vm.serverTimeMlsc
      if (now != null && (now - u.rulesShowDatetime >= SHOW_RULES_DELAY_TIME)) {
        showDialog = true
      }
    }

    if (showDialog) {
      Handler().postDelayed(
        Runnable {
          val rulesDialog = RulesDialog()
          rulesDialog.show(supportFragmentManager, RULES_DIALOG_TAG)
          supportFragmentManager.setFragmentResultListener(CANCEL_KEY, viewLifecycleOwner)
          { _, bundle ->
            Log.i(TAG, "Dialog cancel, refuse from app")
            refuseFromApp(enrollingPermission, newEnrollingPermission,
              changeStateCount, null, null)
          }
        },
        BEFORE_RULES_DELAY_IN_SECONDS * 1000
      )
    }
  }

  private fun stateIsChanging(ep: EnrollingPermission, state: AppState): Boolean {
    return ep.lastState == AppState.ENROLLED && state == AppState.REFUSED ||
        ep.lastState == AppState.REFUSED && state == AppState.ENROLLED
  }

  private fun enrollPorterWithoutCard(ep: EnrollingPermission?,
                                      newEp: EnrollingPermission,
  ) {
    server.enrollPorter(appId, null, null) { appResponse: ApplicationResponse ->
      when (appResponse.response.type) {
        SERVER_OK     -> EnrollReaction(appResponse).doOnServerOkResult(ep, newEp)
        SYSTEM_ERROR  -> EnrollReaction(appResponse).doOnSystemError()
        SERVER_ERROR  -> EnrollReaction(appResponse).doOnServerError()
      }
      btEnrollRefuse.isEnabled = true
    }
  }

  private fun reactOnEnrollWithCard(view: View, ep: EnrollingPermission?,
                                    newEp: EnrollingPermission, changeStateCount: Int)
  {
    supportFragmentManager.setFragmentResultListener(APPLICATION_KEY, viewLifecycleOwner)
    { _, bundle ->
      val app = bundle.getSerializable(APPLICATION_KEY) as ApplicationItem
      Log.i (TAG, "app from dialog = $app")

      vm.openAppsLvdItems.remove(appId)
      vm.takenAppsLvdItems[appId] = MutableLiveData(app)
      vm.takenAppsLvdItems[appId]?.let {
        lvdAppItem = it
      }

      setAppObserver()

      Log.i (TAG, "newEnrollingPermission = $newEp")
      addOrUpdateEnrollPermission(ep, newEp)
      porterIsEnrolled = true

      btEnrollRefuse.text = getString(R.string.app_refuse)
      setUiToEnrolledState()
      updateUI()

      showRulesDialog(ep, newEp, changeStateCount)
    }
  }

  private inner class EnrollReaction (val appResponse: ApplicationResponse):
    ReactionOnResponse(TAG, requireContext(), vm.rootView, appResponse.response)
  {
    override fun doOnServerOkResult() {}

    fun doOnServerOkResult(ep: EnrollingPermission?, newEp: EnrollingPermission) {
      val app = appResponse.app
      lvdEnrollResult.value = true

      vm.openAppsLvdItems.remove(appId)
      vm.takenAppsLvdItems[appId] = MutableLiveData(app)
      vm.takenAppsLvdItems[appId]?.let {
        lvdAppItem = it
      }

      setAppObserver()

      Log.i (TAG, "newEnrollingPermission = $newEp")
      addOrUpdateEnrollPermission(ep, newEp)
      porterIsEnrolled = true

      btEnrollRefuse.text = getString(R.string.app_refuse)
      setUiToEnrolledState()
      updateUI()
    }

    override fun doOnServerFieldValidationError(response: Response) {}
    override fun doOnEndSessionError() {}
  }

  private fun refuseFromApp(ep: EnrollingPermission?, newEp: EnrollingPermission,
                            changeStateCount: Int, bannUntil: String?,
                            decreaseRatingPercent: Int?
  )
  {
    server.refuseApp(appId, bannUntil, decreaseRatingPercent) { appUserResponse ->
      Log.i (TAG, "setFragmentResultListener. response.type=${appUserResponse.response.type}")
      when (appUserResponse.response.type) {
        SERVER_OK     -> RefuseReaction(appUserResponse).doOnServerOkResult(ep, newEp,
          changeStateCount)
        SYSTEM_ERROR  -> RefuseReaction(appUserResponse).doOnSystemError()
        SERVER_ERROR  -> RefuseReaction(appUserResponse).doOnServerError()
      }
    }
  }

  private inner class RefuseReaction (val appResponse: ApplicationResponse):
    ReactionOnResponse(TAG, requireContext(), vm.rootView, appResponse.response)
  {
    override fun doOnServerOkResult() {}

    fun doOnServerOkResult(ep: EnrollingPermission?, newEp: EnrollingPermission,
                           changeStateCount: Int)
    {
      lvdAppItem.value = appResponse.app

      vm.openAppsLvdItems[appId] = MutableLiveData(lvdAppItem.value)
      vm.openAppsLvdItems[appId]?.let {
        lvdAppItem = it
      }
      vm.takenAppsLvdItems.remove(appId)
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
      hideConfirmationStatus()
      hideConfirmationInfo()

      btEnrollRefuse.text = getString(R.string.app_enroll)
      setUiToRefusedState()
      updateUI()
    }

    override fun doOnServerFieldValidationError(response: Response) {}
    override fun doOnEndSessionError() {}
  }

  private fun addOrUpdateEnrollPermission(ep: EnrollingPermission?, newEp: EnrollingPermission) {
    if (ep == null) {
      try {
        App.repository.addEnrollingPermission(newEp)
      }
      catch (e: SQLiteConstraintException) {
        App.repository.updateEnrollingPermission(newEp)
      }
    }
    else {
      App.repository.updateEnrollingPermission(newEp)
    }
  }

  private fun setAppObserver() {
    lvdAppItem.observe(viewLifecycleOwner,
      Observer { appItem ->
        Log.i (TAG, "lvdAppItem=$lvdAppItem, observer appItem is $appItem")

        appItem?.let {
          lastAppItem = it
          if (appItem.porters != null)
            porter = getThisUserPorter(it)
          Log.i (TAG, "porter is $porter")

          porter?.let { porter ->
            if (porter.pivot.removed) {
              setUiToRemovedFromAppState()
            }
          }
          updateUI()
        }
      }
    )
  }

  private fun setUiToDeletedState() {
    changeStatusToError(statusText = getString(R.string.app_deleted))
    setUiToRefusedState(
      tvAddressTopToTop = ConstraintLayout.LayoutParams.UNSET,
      tvAddressTopToBottom = tvStatus.id
    )
    appWasDeleted = true
    lvdAppItem.value?.let {
      updateUI()
    }
  }

  private fun setUiToRemovedFromAppState() {
    changeStatusToError(statusText = getString(R.string.app_you_were_removed))
    setUiToRefusedState(
      tvAddressTopToTop = ConstraintLayout.LayoutParams.UNSET,
      tvAddressTopToBottom = tvStatus.id
    )
    removedFromApp = true
    lvdAppItem.value?.let {
      it.workerCount--
      updateUI()
    }
  }


  private fun changeStatusToError(statusText: String) {
    tvStatus.text = statusText
    tvStatus.textSize = 26.0f
    tvStatus.setTextColor(Color.RED)
    tvStatus.visibility = View.VISIBLE
  }

  private fun changeStatusToEnrolled() {
    tvStatus.visibility = View.VISIBLE
    tvStatus.textSize = 34.0f
    tvStatus.text = getString(R.string.app_you_are_enrolled)
    tvStatus.setTextColor(resources.getColor(R.color.emerald))
  }

  private fun updateUI() {
    Log.i ("track", "updateUI")

    setViewsTexts()
    updatePorters()
    updateWorkers()

    if (appWasDeleted || removedFromApp)
      return

    lvdAppItem.value?.let { app ->
      if (porterIsEnrolled) {
        if (app.needToConfirm) {
          if (!appIsConfirmed(app)) {
            hideConfirmationInfo()
            hideConfirmationStatus()

            if (app.itIsTimeToConfirm)
              showConfirmationInfo()
            else
              showConfirmationStatus()
          }
        }
      }
    }

    GlobalScope.launch(Dispatchers.Main)
    {
      updateBtnEnabling()

      if (!porterIsEnrolled) {
        if (canNotEnrollCause != CAUSE_FREQUENT_APP_REFUSING) {
          canEnroll = true
          canNotEnrollCause = -1
        }
        val takenApps = vm.takenAppsLvdItems

        for (i in takenApps.keys) {
          takenApps[i]?.value?.let {
            val appIsEnded = it.state > CLOSED_STATE
            if (appIsEnded) {
              it.porters?.let { porters ->
                for (j in porters.indices) {
                  if (porters[j].user?.id == App.userItem?.id) {
                    val pivot = porters[j].pivot
                    if (!pivot.payed) {
                      canEnroll = false
                      canNotEnrollCause = CAUSE_COMMISSION_IS_NOT_PAYED
                      break
                    }
                    if (!pivot.confirmed) {
                      canEnroll = false
                      canNotEnrollCause = CAUSE_PAY_IS_NOT_CONFIRMED
                      break
                    }
                  }
                }
              }
            }
          }

          if (!canEnroll) break

          if (takenApps[i] != null && takenApps[i]?.value != null)
            if (takenApps[i]?.value?.id == lvdAppItem.value?.id)
              continue

          takenApps[i]?.value?.let { takenApp ->
            lvdAppItem.value?.let { thisApp ->
              val takenDate = strToDate(takenApp.date, DATE_FORMAT)
              val thisDate = strToDate(thisApp.date, DATE_FORMAT)
              takenDate?.let {
                thisDate?.let {
                  val appsHaveSameDay = (abs(takenDate.time - thisDate.time) == 0L)
                  if (appsHaveSameDay) {
                    val takenTime = strToDate(takenApp.time, TIME_FORMAT)
                    val thisTime = strToDate(thisApp.time, TIME_FORMAT)
                    takenTime?.let {
                      thisTime?.let {
                        val diffTime = abs(takenTime.time - thisTime.time)
                        if (diffTime < MIN_TIME_RANGE_BETWEEN_APPS) {
                          canEnroll = false
                          canNotEnrollCause = CAUSE_SMALL_TIME_INTERVAL
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          if (!canEnroll) break
        }

        if (userIsBanned()) {
          canEnroll = false
          canNotEnrollCause = CAUSE_BANNED
        }

        if (canEnroll) {
          setUiToAbleToEnrollState()
        } else {
          setUiToUnableToEnrollState()
        }
        //canEnroll = true
      } else {
        btEnrollRefuse.isEnabled = true
      }

      val app = lvdAppItem.value
      if (app != null && app.state > CLOSED_STATE) {
        btEnrollRefuse.isEnabled = false
      }

      if (app != null && app.state >= READY_TO_PAY) {
        tvStatus.text = getString(R.string.app_end)
        btChangeDebitCard.isEnabled = false
        porter?.pivot?.let { pivot ->
          lvdAppItem.value?.let { app ->
            if (app.clientPaysForWork) {
              renderEndResultWithCommission(pivot)
            } else {
              renderEndResult(pivot)
            }
            setTvAddressLayoutParams(app)
          }
        }
        btCallClient.isEnabled = false
        btClientWhatsapp.isEnabled = false
      }

      lvdAppItem.value?.let { app ->
        if (app.id == 0)
          btEnrollRefuse.isEnabled = false
      }
    }
  }

  private fun userIsBanned(): Boolean
  {
    App.dbUser?.let { user ->
      val now = vm.serverTimeMlsc
      if (now != null && now < user.endOfBannDatetime) {
        return true
      }
    }
    return false
  }

  private fun renderEndResultWithCommission(pivot: PorterPivotItem) {
    tvEndStatus.text = Html.fromHtml(getString(R.string.app_end_pay_by_client,
      pivot.workHours,
      pivot.money
    ))
    val commission = if (pivot.residue > 0) pivot.residue else pivot.commission
    if (pivot.payed) {
      if (pivot.confirmed) {
        tvCommissionStatus.text = getString(R.string.app_end_commission_payed, pivot.commission)
        tvCommissionStatus.setTextColor(resources.getColor(R.color.emerald))
      }
      else {
        tvCommissionStatus.text = getString(R.string.app_end_commission_confirmation,
          commission)
        tvCommissionStatus.setTextColor(resources.getColor(R.color.emerald))
      }
      btPay.visibility = View.INVISIBLE
    }
    else {
      btPay.visibility = View.VISIBLE
      tvCommissionStatus.text = if (pivot.residue > 0) getString(R.string.app_residue,
        pivot.residue)
      else getString(R.string.app_end_commission, pivot.commission)
      tvCommissionStatus.setTextColor(resources.getColor(R.color.red))
    }

    tvEndStatus.visibility = View.VISIBLE
    tvCommissionStatus.visibility = View.VISIBLE
  }

  private fun renderEndResult(pivot: PorterPivotItem) {
    tvEndStatus.text = Html.fromHtml(getString(R.string.app_end_pay_by_dispatcher,
      pivot.workHours,
      pivot.money - pivot.commission
    ))
    tvEndStatus.visibility = View.VISIBLE
  }

  private fun setTvAddressLayoutParams(app: ApplicationItem) {
    val ap = tvAddress.layoutParams as ConstraintLayout.LayoutParams
    ap.topToTop = ConstraintLayout.LayoutParams.UNSET
    ap.topToBottom = when (btPay.visibility) {
      View.VISIBLE -> btPay.id
      else -> if (app.clientPaysForWork) tvCommissionStatus.id else tvEndStatus.id
    }
    tvAddress.layoutParams = ap
  }

  private fun showConfirmationStatus() {
    val dp = tvAddress.layoutParams as ConstraintLayout.LayoutParams
    dp.topToBottom = tvConfirmationStatus.id
    tvAddress.layoutParams = dp

    tvConfirmationStatus.visibility = View.VISIBLE
  }

  private fun hideConfirmationStatus() {
    val dp = tvAddress.layoutParams as ConstraintLayout.LayoutParams
    dp.topToBottom = ConstraintLayout.LayoutParams.UNSET
    tvAddress.layoutParams = dp

    tvConfirmationStatus.visibility = View.INVISIBLE
  }

  private fun showConfirmationInfo() {
    val dp = nsvAppArea.layoutParams as ConstraintLayout.LayoutParams
    dp.bottomToTop = btAppConfirm.id
    nsvAppArea.layoutParams = dp

    btAppConfirm.visibility = View.VISIBLE
    tvAppConfirm.visibility = View.VISIBLE
    tvAppConfirm.setTextColor(Color.RED)

    btCallClient.isEnabled = false
    btClientWhatsapp.isEnabled = false
    tvWhenCall.visibility = View.INVISIBLE

    changeStatusToError(getString(R.string.app_status_need_confirm))

    val lp = tvAddress.layoutParams as ConstraintLayout.LayoutParams
    lp.topToBottom = tvStatus.id
    tvAddress.layoutParams = lp
  }

  private fun hideConfirmationInfo() {
    val dp = nsvAppArea.layoutParams as ConstraintLayout.LayoutParams
    dp.bottomToTop = btCallClient.id
    nsvAppArea.layoutParams = dp

    val lp = tvAddress.layoutParams as ConstraintLayout.LayoutParams
    if (porterIsEnrolled)
      lp.topToBottom = tvStatus.id
    else
      lp.topToBottom = ConstraintLayout.LayoutParams.UNSET
    tvAddress.layoutParams = lp

    btAppConfirm.visibility = View.INVISIBLE
    tvAppConfirm.visibility = View.INVISIBLE
    tvWhenCall.visibility = View.VISIBLE

    btCallClient.isEnabled = true
    btClientWhatsapp.isEnabled = true
  }

  private fun updatePorters() {
    lvdAppItem.value?.let {
      it.porters?.let { porters ->
        rvPorters.adapter = PortersAdapter(porters)
      }
    }
  }

  private fun updateWorkers() {
    lvdAppItem.value?.let {
      it.workers?.let { workers ->
        rvWorkers.adapter = WorkersAdapter(workers)
        rvWorkers.visibility = if (workers.size > 0) View.VISIBLE else View.INVISIBLE
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

      if (canNotEnrollCause != CAUSE_SMALL_TIME_INTERVAL) {
        if (ep == null) {
          btEnrollRefuse.isEnabled = true
          canEnroll = true
          canNotEnrollCause = -1
        }
        else {
          btEnrollRefuse.isEnabled = (now >= ep.enableClickTime)
          if (now >= ep.enableClickTime) {
            canEnroll = true
            canNotEnrollCause = -1
          }
          else {
            canEnroll = false
            canNotEnrollCause = CAUSE_FREQUENT_APP_REFUSING
          }
        }
      }
    }
  }

  private fun setUiToEnrolledState()
  {
    changeStatusToEnrolled()
    setVisibilityToViews(listOf(rvPorters, btCallClient, btClientWhatsapp, tvWhenCall), View.VISIBLE)
    setLayoutParamsToViews (
      tvAddressTopToTop = ConstraintLayout.LayoutParams.UNSET,
      tvAddressTopToBottom = tvStatus.id,
      tvDescriptionTopToBottom = rvPorters.id,
      nsvAppAreaBottomToTop = btCallClient.id
    )
  }

  private fun setUiToRefusedState(tvAddressTopToTop: Int = vm.rootView.id,
                                  tvAddressTopToBottom: Int = ConstraintLayout.LayoutParams.UNSET)
  {
    setVisibilityToViews(listOf(tvStatus, rvPorters, btCallClient, btClientWhatsapp, tvWhenCall),
      View.INVISIBLE)
    setLayoutParamsToViews (
      tvAddressTopToTop = tvAddressTopToTop,
      tvAddressTopToBottom = tvAddressTopToBottom,
      tvDescriptionTopToBottom = tvPortersCount.id,
      nsvAppAreaBottomToTop = btBack.id
    )
  }

  private fun setUiToAbleToEnrollState()
  {
    tvStatus.visibility = View.INVISIBLE
    btEnrollRefuse.isEnabled = true
    setLayoutParamsToViews (
      tvAddressTopToTop = vm.rootView.id,
      tvAddressTopToBottom = ConstraintLayout.LayoutParams.UNSET,
      tvDescriptionTopToBottom = tvPortersCount.id,
      nsvAppAreaBottomToTop = btBack.id
    )
  }

  private fun setUiToUnableToEnrollState()
  {
    App.dbUser?.let { user ->
      val endOfBannDate = Date(user.endOfBannDatetime)
      val statusText = when (canNotEnrollCause) {
        CAUSE_FREQUENT_APP_REFUSING -> getString(R.string.app_could_not_enroll_cause_refuses)
        CAUSE_SMALL_TIME_INTERVAL -> getString(R.string.app_could_not_enroll_cause_interval)
        CAUSE_COMMISSION_IS_NOT_PAYED -> getString(R.string.app_could_not_enroll_cause_commission)
        CAUSE_PAY_IS_NOT_CONFIRMED -> getString(R.string.app_could_not_enroll_cause_not_confirmed_pay)
        CAUSE_BANNED -> getString(R.string.app_can_not_enroll_cause_banned,
          dateToStr(endOfBannDate, COOL_DATE_TIME_FORMAT))
        else -> getString(R.string.app_could_not_enroll_cause_unknown)
      }
      changeStatusToError(statusText)
    }

    btEnrollRefuse.isEnabled = false
    setLayoutParamsToViews (
      tvAddressTopToTop = ConstraintLayout.LayoutParams.UNSET,
      tvAddressTopToBottom = tvStatus.id,
      tvDescriptionTopToBottom = tvPortersCount.id,
      nsvAppAreaBottomToTop = btCallClient.id
    )
  }

  private fun setLayoutParamsToViews(tvAddressTopToTop: Int, tvAddressTopToBottom: Int,
                                     tvDescriptionTopToBottom: Int, nsvAppAreaBottomToTop: Int)
  {
    val tvAddressMap: Map<String, Int> = mapOf(
      "topToTop" to tvAddressTopToTop,
      "topToBottom" to tvAddressTopToBottom
    )
    val tvDescriptionMap: Map<String, Int> = mapOf(
      "topToBottom" to tvDescriptionTopToBottom
    )
    val nsvAppAreaMap: Map<String, Int> = mapOf(
      "bottomToTop" to nsvAppAreaBottomToTop
    )

    setLayoutParams(this.tvAddress, tvAddressMap)
    setLayoutParams(this.tvDescription, tvDescriptionMap)
    setLayoutParams(this.nsvAppArea, nsvAppAreaMap)
  }

  private fun getThisUserPorter(app: ApplicationItem): PorterItem? {
    var porter: PorterItem? = null
    App.dbUser?.let { user ->
      Log.i (TAG, "getThisUserPorter: app = $app, porters = ${app.porters}")
      app.porters?.let {
        for (i in app.porters.indices) {
          if (user.id == app.porters[i].user?.id) {
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
    tvStatus = view.findViewById(R.id.tv_app_status)
    tvEndStatus = view.findViewById(R.id.tv_app_end_status)
    tvConfirmationStatus = view.findViewById(R.id.tv_app_confirm_status)
    tvCommissionStatus = view.findViewById(R.id.tv_app_status_commission)
    btPay = view.findViewById(R.id.bt_app_pay)
    tvAddress = view.findViewById(R.id.tv_app_address)
    tvTime = view.findViewById(R.id.tv_app_time)
    tvMoney = view.findViewById(R.id.tv_app_money)
    tvCommission = view.findViewById(R.id.tv_app_commission)
    tvDescription = view.findViewById(R.id.tv_app_description)
    tvPayMethod = view.findViewById(R.id.tv_app_pay_method)
    tvPayFromWho = view.findViewById(R.id.tv_app_pay_from_who)
    tvPortersCount = view.findViewById(R.id.tv_app_porters)
    rvPorters = view.findViewById(R.id.rv_app_porters)
    rvWorkers = view.findViewById(R.id.rv_app_workers)
    btCallClient = view.findViewById(R.id.bt_app_call_client)
    btClientWhatsapp = view.findViewById(R.id.bt_app_client_whatsapp)
    btEnrollRefuse = view.findViewById(R.id.bt_app_enroll_refuse)
    btBack = view.findViewById(R.id.bt_app_back)
    btChangeDebitCard = view.findViewById(R.id.bt_app_change_debit_card)
    nsvAppArea = view.findViewById(R.id.nsv_app)
    tvWhenCall = view.findViewById(R.id.tv_app_when_call)
    btAppConfirm = view.findViewById(R.id.bt_app_confirm)
    tvAppConfirm = view.findViewById(R.id.tv_app_confirm)


    btAppConfirm.setOnClickListener {
      confirmYourParticipation(appId)
    }

    btClientWhatsapp.setOnClickListener {
      lvdAppItem.value?.let { appItem ->
        openWhatsappContact(requireActivity(), appItem.clientPhoneNumber)
      }
    }

    btEnrollRefuse.setOnClickListener {
      onEnrollRefuseBtnClick()
    }

    btBack.setOnClickListener {
      vm.mainActivity.replaceOnFragment("Applications")
    }

    btCallClient.setOnClickListener {
      onCallClientBtnClick()
    }

    btChangeDebitCard.setOnClickListener {
      onChangeDebitCardClick()
    }

    setBtPayClickListener()
  }

  private fun setViewsTexts() {
    lvdAppItem.value?.let { appItem ->
      tvAddress.text = Html.fromHtml(getString(R.string.app_address, appItem.address))

      val date = strToDate(appItem.date, DATE_FORMAT)
      date?.let {
        val time = if (isItToday(it)) {
          tvTime.setTextColor(Color.BLACK)
          "${appItem.time}"
        }
        else {
          tvTime.setTextColor(Color.RED)
          "${getDateStr(it)} ${appItem.time}"
        }
        tvTime.text = Html.fromHtml(getString(R.string.app_time, time))
      }

      val suffix = if (appItem.hourlyJob)
        " " + getString(R.string.hourly_suffix)
      else
        getString(R.string.daily_suffix)
      val money = "${appItem.price}$suffix"
      tvMoney.text = Html.fromHtml(getString(R.string.app_money, money))

      val comm = appItem.price - appItem.priceForWorker
      val commission = "$comm$suffix"
      tvCommission.text = Html.fromHtml(getString(R.string.app_commission, commission))

      tvDescription.text = Html.fromHtml(getString(R.string.app_description, appItem.whatToDo))

      val payMethod = when (appItem.payMethod) {
        PM_CARD -> getString(R.string.app_on_card)
        PM_CASH -> getString(R.string.app_cash)
        else -> ""
      }

      val payFromWho = when (appItem.clientPaysForWork) {
        true -> getString(R.string.app_from_client)
        false -> getString(R.string.app_from_dispatcher)
      }

      if (porterIsEnrolled) {
        btChangeDebitCard.text = getDebitCardNumber()

        if (appItem.payMethod == PM_CARD)
          btChangeDebitCard.visibility = View.VISIBLE
        else
          btChangeDebitCard.visibility = View.INVISIBLE
      }
      else {
        btChangeDebitCard.visibility = View.INVISIBLE
      }

      tvPayMethod.text = Html.fromHtml(getString(R.string.app_pay_method, payMethod))
      tvPayFromWho.text = Html.fromHtml(getString(R.string.app_pay_from_who, payFromWho))

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
        debitCards?.let {
          for (i in debitCards.indices)
            if (debitCards[i].id == dcId) {
              dcStr = debitCards[i].number
              break
            }
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
      cards?.let {
        for (i in cards.indices) {
          if (cards[i].main) {
            dc = cards[i]
            break
          }
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
        openWhatsappContact(requireActivity(), phoneWhatsapp)
      }
    }
  }

  private inner class PortersAdapter (porters: List<PorterItem>):
    RecyclerView.Adapter<PorterHolder>()
  {
    private val porters = mutableListOf<PorterItem>()

    init {
      App.userItem?.let { user ->
        for (i in porters.indices) {
          if (porters[i].user?.id != user.id)
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

  private inner class WorkerHolder (view: View) : RecyclerView.ViewHolder(view) {
    private val tvName: TextView = view.findViewById(R.id.tv_porter_name)
    private val ivCall: ImageView = view.findViewById(R.id.iv_porter_call)
    private val ivWhatsapp: ImageView = view.findViewById(R.id.iv_porter_whatsapp)

    fun bind(workerName: String, phones: List<WorkerPhoneItem>) {
      tvName.text = workerName

      var waPhone = ""
      var callPhone = ""
      for (i in phones.indices) {
        if (phones[i].type == PHONE_CALL_TEXT ||
          phones[i].type == PHONE_CALL_AND_WHATSAPP_TEXT
        ) {
          callPhone = phones[i].number
        }
        if (phones[i].type == PHONE_WHATSAPP_TEXT ||
          phones[i].type == PHONE_CALL_AND_WHATSAPP_TEXT
        ) {
          waPhone = phones[i].number
        }
      }

      ivCall.setOnClickListener {
        val intent = Intent(Intent.ACTION_DIAL)
        val uriStr = "tel:$callPhone"
        intent.data = Uri.parse(uriStr)
        context?.startActivity(intent)
      }

      ivWhatsapp.setOnClickListener {
        openWhatsappContact(requireActivity(), waPhone)
      }
    }
  }

  private inner class WorkersAdapter (private val workers: List<WorkerItem>):
    RecyclerView.Adapter<WorkerHolder>()
  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerHolder {
      val view = LayoutInflater.from(parent.context).inflate(
        R.layout.list_item_porter, parent, false
      )
      return WorkerHolder(view)
    }

    override fun getItemCount() = workers.size

    override fun onBindViewHolder(holder: WorkerHolder, position: Int) {
      val workerName = if (itemCount > 1) "${getString(R.string.mate)} ${position + 1}"
      else getString(R.string.mate)
      holder.bind(workerName, workers[position].phones)
    }
  }

  private fun confirmYourParticipation(appId: Int) {
    server.confirmParticipationInApp(appId) { response ->
      when (response.type) {
        SERVER_OK     -> ConfirmReaction(response).doOnServerOkResult()
        SYSTEM_ERROR  -> ConfirmReaction(response).doOnSystemError()
        SERVER_ERROR  -> ConfirmReaction(response).doOnServerError()
      }
    }
  }

  private inner class ConfirmReaction (val response: Response):
    ReactionOnResponse(TAG, requireContext(), vm.rootView, response)
  {
    override fun doOnServerOkResult() {
      changeStatusToEnrolled()
      hideConfirmationInfo()
      removeNotification(requireContext(), appId)
    }

    override fun doOnServerFieldValidationError(response: Response) {}
    override fun doOnEndSessionError() {}
  }
}