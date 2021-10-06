package `in`.arbait

import `in`.arbait.http.*
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


private const val TAG = "ApplicationsViewModel"

class PollServerViewModel: ViewModel(), Serializable
{
  /* инициализируются в MainActivity */

  lateinit var mainActivity: MainActivity
  lateinit var context: Context
  lateinit var viewLifecycleOwner: LifecycleOwner

  /* инициализируются в текущем фрагменте */

  lateinit var rootView: View

  /* ******************************* */

  var pollService: PollService? = null
  var serviceIsBound: Boolean? = null

  val openApps: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val lvdOpenApps = mutableMapOf<Int, MutableLiveData<ApplicationItem>>()

  val takenApps: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val lvdTakenApps = mutableMapOf<Int, MutableLiveData<ApplicationItem>>()

  var clickedNotificationAppId: Int? = null

  private var lvdClickedNotificationAppId = MutableLiveData<Int?>(null)
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
        clickedNotificationAppId = it.clickedNotificationAppId
      }
      setAppsResponseObserver()
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
      Log.d(TAG, "ServiceConnection: disconnected from service.")
      serviceIsBound = false
    }
  }

  fun serviceDoAction (action: Actions) {
    if (getServiceState(mainActivity) == ServiceState.STOPPED && action == Actions.STOP) return

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
    mainActivity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    serviceIsBound = true
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
            SERVER_OK     -> PollServerReaction(response).doOnServerOkResult(it)
            SYSTEM_ERROR  -> PollServerReaction(response).doOnSystemError()
            SERVER_ERROR  -> PollServerReaction(response).doOnServerError()
          }
        }
      }
    )
  }


  private inner class PollServerReaction (response: Response):
    ReactionOnResponse (TAG, context, rootView, response) {

    fun doOnServerOkResult(appsResponse: ServiceDataResponse) {
      /*openApps.value?.let { value ->
        Log.i (TAG, "openApps.size = ${value.size}")
        Log.i (TAG, "appsResponse.openApps.size = ${appsResponse.openApps.size}")
      }*/

      setOpenApps(appsResponse)
      setLiveDataOpenApps(appsResponse)

      setTakenApps(appsResponse)
      setLiveDataTakenApps(appsResponse)
    }

    override fun doOnServerOkResult() {}

    override fun doOnServerFieldValidationError(response: Response) {}

    override fun doOnEndSessionError() {
      Log.i (TAG, "doOnEndSessionError()")

      App.dbUser?.let {
        it.login = false
        it.isConfirmed = false
        //it.callReceived = false //TODO: УБРАТЬ КОММЕНТ!
        it.isItRegistration = false
        App.repository.updateUser(it)
      }

      unbindService()
      serviceDoAction(Actions.STOP)

      /* Выясняем запущен ли уже LoginFragment или нет
      val myFragment: LoginFragment =
        mainActivity.supportFragmentManager.findFragmentByTag("MY_FRAGMENT") as LoginFragment
      if (myFragment != null && myFragment.isVisible()) {
        // add your code here
      }*/
      mainActivity.replaceOnFragment("Login")
    }


    private fun setOpenApps(appsResponse: ServiceDataResponse) {
      if ((openApps.value == null) || openAppsDifferFrom(appsResponse.openApps)) {
        openApps.value = appsResponse.openApps
      }
    }

    private fun setLiveDataOpenApps(appsResponse: ServiceDataResponse) {
      for (i in appsResponse.openApps.indices) {
        val appId = appsResponse.openApps[i].id
        if (lvdOpenApps.containsKey(appId)) {
          lvdOpenApps[appId]?.let { lvdApp ->
            lvdApp.value = appsResponse.openApps[i]
          }
        }
        else {
          val lvdValue = MutableLiveData<ApplicationItem>(appsResponse.openApps[i])
          lvdOpenApps[appId] = lvdValue
        }
      }
    }

    private fun openAppsDifferFrom (openApps: List<ApplicationItem>): Boolean {
      return listsAreDifferent(this@PollServerViewModel.openApps.value!!, openApps)
    }


    private fun setTakenApps(appsResponse: ServiceDataResponse) {
      appsResponse.takenApps?.let { responseTakenApps ->
        if ((takenApps.value == null) || takenAppsDifferFrom(responseTakenApps)) {
          takenApps.value = responseTakenApps
        }
      }

    }

    private fun setLiveDataTakenApps(appsResponse: ServiceDataResponse) {
      appsResponse.takenApps?.let { responseTakenApps ->
        for (i in responseTakenApps.indices) {
          val appId = responseTakenApps[i].id
          if (lvdTakenApps.containsKey(appId)) {
            lvdTakenApps[appId]?.let { lvdApp ->
              lvdApp.value = responseTakenApps[i]
            }
          }
          else {
            val lvdValue = MutableLiveData<ApplicationItem>(responseTakenApps[i])
            lvdTakenApps[appId] = lvdValue
          }
        }
      }
    }

    private fun takenAppsDifferFrom (takenApps: List<ApplicationItem>): Boolean {
      return listsAreDifferent(this@PollServerViewModel.takenApps.value!!, takenApps)
    }
  }

}