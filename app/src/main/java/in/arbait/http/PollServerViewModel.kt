package `in`.arbait.http

import `in`.arbait.App
import `in`.arbait.MainActivity
import `in`.arbait.elementsFromANotInB
import `in`.arbait.http.*
import `in`.arbait.http.poll_service.*
import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.response.*
import `in`.arbait.listsAreDifferent
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

  /* ******************************* */

  var pollService: PollService? = null
  var serviceIsBound: Boolean? = null

  val openApps: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val lvdOpenApps = mutableMapOf<Int, MutableLiveData<ApplicationItem>>()

  val takenApps: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val lvdTakenApps = mutableMapOf<Int, MutableLiveData<ApplicationItem>>()

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
      }
      setAppsResponseObserver()
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
      Log.i (TAG, "doOnServerOkResult")
      setOpenApps(appsResponse)
      setTakenApps(appsResponse)
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
      serviceDoAction(Action.STOP)

      /* Выясняем запущен ли уже LoginFragment или нет
      val myFragment: LoginFragment =
        mainActivity.supportFragmentManager.findFragmentByTag("MY_FRAGMENT") as LoginFragment
      if (myFragment != null && myFragment.isVisible()) {
        // add your code here
      }*/
      mainActivity.replaceOnFragment("Login")
    }


    private fun setOpenApps(appsResponse: ServiceDataResponse) {
      val prevApps = this@PollServerViewModel.openApps.value
      var goneApps = listOf<ApplicationItem>()
      prevApps?.let {
        // closed or deleted apps
        goneApps = elementsFromANotInB(it, appsResponse.openApps)
      }

      if ((openApps.value == null) || openAppsDifferFrom(appsResponse.openApps)) {
        openApps.value = appsResponse.openApps
      }

      setLiveDataOpenApps(appsResponse, goneApps)
    }

    private fun setLiveDataOpenApps(appsResponse: ServiceDataResponse,
                                    goneApps: List<ApplicationItem>)
    {
      for (i in appsResponse.openApps.indices) {
        val appId = appsResponse.openApps[i].id
        if (lvdOpenApps.containsKey(appId))
          lvdOpenApps[appId]?.value = appsResponse.openApps[i]
        else
          lvdOpenApps[appId] = MutableLiveData(appsResponse.openApps[i])
      }

      if (goneApps.isNotEmpty()) {
        for (i in goneApps.indices) {
          val appId = goneApps[i].id
          lvdOpenApps[appId]?.value = null
        }
      }
    }

    private fun openAppsDifferFrom (openApps: List<ApplicationItem>): Boolean {
      return listsAreDifferent(this@PollServerViewModel.openApps.value!!, openApps)
    }


    private fun setTakenApps(appsResponse: ServiceDataResponse) {
      if (appsResponse.takenApps == null) {
        takenApps.value = emptyList()
      }

      val prevApps = this@PollServerViewModel.takenApps.value
      var goneApps = listOf<ApplicationItem>()
      prevApps?.let { prevTakenApps ->
        appsResponse.takenApps?.let { newTakenApps ->
          // closed or deleted apps
          goneApps = elementsFromANotInB(prevTakenApps, newTakenApps)
        }
      }

      appsResponse.takenApps?.let { responseTakenApps ->
        Log.i ("setTakenApps", "responseTakenApps = $responseTakenApps")
        if ((takenApps.value == null) || takenAppsDifferFrom(responseTakenApps)) {
          takenApps.value = responseTakenApps
        }
      }

      setLiveDataTakenApps(appsResponse, goneApps)
    }

    private fun setLiveDataTakenApps(appsResponse: ServiceDataResponse,
                                     goneApps: List<ApplicationItem>)
    {
      appsResponse.takenApps?.let { responseTakenApps ->
        for (i in responseTakenApps.indices) {
          val appId = responseTakenApps[i].id
          if (lvdTakenApps.containsKey(appId))
            lvdTakenApps[appId]?.value = responseTakenApps[i]
          else {
            lvdTakenApps[appId] = MutableLiveData(responseTakenApps[i])
          }
          Log.i ("setLiveDataTakenApps", "lvdAppItem=${lvdTakenApps[appId]}, ${lvdTakenApps[appId]?.value}")
        }
      }

      if (goneApps.isNotEmpty()) {
        for (i in goneApps.indices) {
          val appId = goneApps[i].id
          lvdTakenApps[appId]?.value = null
        }
      }
    }

    private fun takenAppsDifferFrom (takenApps: List<ApplicationItem>): Boolean {
      Log.i ("takenAppsDifferFrom", "this.takenApps=${this@PollServerViewModel.takenApps.value!!}")
      Log.i ("takenAppsDifferFrom", "new takenApps = $takenApps")
      return listsAreDifferent(this@PollServerViewModel.takenApps.value!!, takenApps)
    }
  }

}