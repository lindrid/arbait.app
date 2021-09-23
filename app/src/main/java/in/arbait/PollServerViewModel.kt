package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.*
import `in`.arbait.http.poll_service.*
import `in`.arbait.models.ApplicationItem
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable

private const val TAG = "ApplicationsViewModel"

class PollServerViewModel: ViewModel(), Serializable {

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

  private lateinit var appsResponse: LiveData<ApplicationsResponse>

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
      if (it) {
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

  private fun openAppsDifferFrom (openApps: List<ApplicationItem>): Boolean {
    return listsAreDifferent(this.openApps.value!!, openApps)
  }

  private inner class PollServerReaction (response: Response):
    ReactionOnResponse (TAG, context, rootView, response) {

    fun doOnServerOkResult(appsResponse: ApplicationsResponse) {
      var openAppsIsEmpty = false
      openApps.value?.let { value ->
        openAppsIsEmpty = value.isEmpty()
        Log.i (TAG, "openApps.size = ${value.size}")
        Log.i (TAG, "appsResponse.openApps.size = ${appsResponse.openApps.size}")
      }
      if ((openApps.value == null) || openAppsDifferFrom(appsResponse.openApps)) {
        openApps.value = appsResponse.openApps
      }
      for (i in appsResponse.openApps.indices) {
        val appId = appsResponse.openApps[i].id
        if (lvdOpenApps.containsKey(appId)) {
          lvdOpenApps[appId]?.let { lvdApp ->
            Log.i(TAG, "LIVE_DATA, appId = $appId, liveData = $lvdApp, " +
                "value = ${lvdApp.value}")

            lvdApp.value = appsResponse.openApps[i]

            Log.i (TAG, "NEW_LIVE_DATA, appId = $appId, liveData = $lvdApp, " +
                "value = ${lvdApp.value}")
          }
        }
        else {
          val lvdValue = MutableLiveData<ApplicationItem>(appsResponse.openApps[i])
          lvdOpenApps[appId] = lvdValue
        }
      }
    }

    override fun doOnServerOkResult() {}

    override fun doOnServerFieldValidationError(response: Response) {}

    override fun doOnEndSessionError() {
      Log.i (TAG, "doOnEndSessionError()")

      App.user?.let {
        it.login = false
        App.repository.updateUser(it)
      }

      unbindService()
      serviceDoAction(Actions.STOP)

      mainActivity.replaceOnFragment("LoginFragment")
    }
  }

}