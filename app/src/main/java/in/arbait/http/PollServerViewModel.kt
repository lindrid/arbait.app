package `in`.arbait.http

import `in`.arbait.*
import `in`.arbait.http.poll_service.*
import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.response.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import java.io.Serializable

private const val TAG = "PollServerViewModel"

class PollServerViewModel: ViewModel(), Serializable
{
  /* инициализируются в MainActivity */

  lateinit var mainActivity: MainActivity
  lateinit var context: Context
  lateinit var viewLifecycleOwner: LifecycleOwner

  /* инициализируются в текущем фрагменте */

  lateinit var rootView: View
  lateinit var doOnOpenAppsChange: () -> Unit
  lateinit var doOnTakenAppsChange: () -> Unit

  /* ******************************* */

  var pollService: PollService? = null
  var serviceIsBound: Boolean? = null

  var lvdDispatcherWhatsapp = MutableLiveData<String>()

  var openAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val openAppsLvdItems = mutableMapOf<Int, MutableLiveData<ApplicationItem>>()

  var takenAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val takenAppsLvdItems = mutableMapOf<Int, MutableLiveData<ApplicationItem>>()

  private lateinit var appsResponse: LiveData<ServiceDataResponse>

  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
      Log.i(TAG, "ServiceConnection: connected to service.")
      // We've bound to MyService, cast the IBinder and get MyBinder instance
      val binder = iBinder as PollService.PollBinder
      pollService = binder.service
      serviceIsBound = true
      pollService?.let {
        appsResponse = it.dataResponse
        openAppsLvdList = it.openAppsLvdList
        takenAppsLvdList = it.takenAppsLvdList
        lvdDispatcherWhatsapp = it.lvdDispatcherWhatsapp
      }
      setAppsResponseObserver()
      setAppsObservers()
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
      Log.d(TAG, "ServiceConnection: disconnected from service.")
      serviceIsBound = false
    }
  }

  fun serviceDoAction (action: Action) {
    if (getServiceState(mainActivity) == ServiceState.STOPPED && action == Action.STOP) return

    Intent(mainActivity, PollService::class.java).also {
      it.action = action.name

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        log("Starting the service in >=26 Mode")
        mainActivity.startForegroundService(it)
        return
      }
      log("Starting the service in < 26 Mode")
      mainActivity.startService(it)
    }
  }

  fun bindService() {
    val intent = Intent(mainActivity, PollService::class.java)
    serviceIsBound = mainActivity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
  }

  fun unbindService() {
    serviceIsBound?.let {
      if (it && getServiceState(context) == ServiceState.STARTED) {
        mainActivity.unbindService(serviceConnection)
      }
    }
  }

  fun setAppsResponseObserver() {
    appsResponse.observe(viewLifecycleOwner,
      Observer { appsResponse ->
        Log.i (TAG, "response")
        appsResponse?.let {
          val response = it.response
          Log.i (TAG, "CODE: ${response.type}")

          when (response.type) {
            SYSTEM_ERROR  -> PollServerReaction(response).doOnSystemError()
            SERVER_ERROR  -> {
              if (response.isItErrorWithCode && response.code == OLD_VERSION_ERROR_CODE) {
                unbindService()
                serviceDoAction(Action.STOP)
                response.message?.let { msg ->
                  showErrorBalloon(context, rootView, msg)
                }
              }
              else {
                PollServerReaction(response).doOnServerError()
              }
            }
          }
        }
      }
    )
  }

  fun setAppsObservers() {
    openAppsLvdList.observe(viewLifecycleOwner,
      Observer { openApps ->
        Log.i (TAG, "openApps list changed")
        setOpenAppsLvdItems(openApps)
        doOnOpenAppsChange()
      }
    )

    takenAppsLvdList.observe(viewLifecycleOwner,
      Observer { takenApps ->
        Log.i (TAG, "takenApps list changed")
        setTakenAppsLvdItems(takenApps)
        doOnTakenAppsChange()
      }
    )
  }


  private inner class PollServerReaction (response: Response):
    ReactionOnResponse (TAG, context, rootView, response)
  {
    override fun doOnServerOkResult() {}

    override fun doOnServerFieldValidationError(response: Response) {}

    override fun doOnEndSessionError() {
      Log.i(TAG, "doOnEndSessionError()")

      App.dbUser?.let {
        it.login = false
        it.isConfirmed = false
        //it.callReceived = false //TODO: УБРАТЬ КОММЕНТ!
        it.isItRegistration = false
        App.repository.updateUser(it)
      }

      unbindService()
      serviceDoAction(Action.STOP)

      /* Выясняем запущен ли уже LoginFragment или нет
      val myFragment: LoginFragment =
        mainActivity.supportFragmentManager.findFragmentByTag("MY_FRAGMENT") as LoginFragment
      if (myFragment != null && myFragment.isVisible()) {
        // add your code here
      }*/
      mainActivity.replaceOnFragment("Login")
    }
  }

  fun calcCommissions(takenApps: List<ApplicationItem>): Pair<Int, Int> {
    var commission = 0
    var notConfirmedCommission = 0

    App.userItem?.let { user ->
      for (i in takenApps.indices) {
        val appIsEnded = takenApps[i].state > CLOSED_STATE
        if (appIsEnded) {
          takenApps[i].porters?.let { porters ->
            for (j in porters.indices) {
              if (user.id == porters[j].user.id) {
                val pivot = porters[j].pivot
                if (!pivot.payed) {
                  commission += if (pivot.residue > 0) pivot.residue
                  else pivot.commission
                } else if (!pivot.confirmed) {
                  notConfirmedCommission += if (pivot.residue > 0) pivot.residue
                  else pivot.commission
                }
                break
              }
            }
          }
        }
      }
    }

    return Pair(commission, notConfirmedCommission)
  }


  private fun setOpenAppsLvdItems(openApps: List<ApplicationItem>) {
    for (i in openApps.indices) {
      val appId = openApps[i].id
      if (openAppsLvdItems.containsKey(appId))
        openAppsLvdItems[appId]?.value = openApps[i]
      else
        openAppsLvdItems[appId] = MutableLiveData(openApps[i])
    }
  }

  private fun setTakenAppsLvdItems(takenApps: List<ApplicationItem>) {
    if (takenApps.isEmpty()) {
      takenAppsLvdItems.clear()
    }

    for (i in takenApps.indices) {
      val appId = takenApps[i].id
      if (takenAppsLvdItems.containsKey(appId))
        takenAppsLvdItems[appId]?.value = takenApps[i]
      else
        takenAppsLvdItems[appId] = MutableLiveData(takenApps[i])
      Log.i ("setLiveDataTakenApps", "lvdAppItem=${takenAppsLvdItems[appId]}, ${takenAppsLvdItems[appId]?.value}")
    }
  }
}