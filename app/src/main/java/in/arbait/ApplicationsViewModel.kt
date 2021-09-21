package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.*
import `in`.arbait.http.polling_service.*
import `in`.arbait.models.ApplicationItem
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable

private const val TAG = "ApplicationsViewModel"

class ApplicationsViewModel: ViewModel(), Serializable {

  lateinit var mainActivity: MainActivity
  lateinit var viewLifecycleOwner: LifecycleOwner
  lateinit var doOnFailure: (Response) -> Unit

  var user: User? = null
  val repository = UserRepository.get()

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
      setObservers()
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
      Log.d(TAG, "ServiceConnection: disconnected from service.")
      serviceIsBound = false
    }
  }

  init {
    GlobalScope.launch {
      user = repository.getUserLastByDate()
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

  fun setObservers() {
    appsResponse.observe(viewLifecycleOwner,
      Observer { appsResponse ->
        Log.i (TAG, "response")
        appsResponse?.let {
          val response = it.response
          Log.i (TAG, "CODE: ${response.code}")
          if (response.code == SERVER_OK) {
            if ((openApps.value == null) || openAppsDifferFrom(it.openApps)) {
              openApps.value = it.openApps
            }
            for (i in it.openApps.indices) {
              val appId = it.openApps[i].id
              if (lvdOpenApps.containsKey(appId)) {
                lvdOpenApps[appId]?.let { lvdApp ->
                  Log.i(TAG, "LIVE_DATA, appId = $appId, liveData = $lvdApp, " +
                      "value = ${lvdApp.value}")
                  lvdApp.value = it.openApps[i]
                  Log.i (TAG, "NEW_LIVE_DATA, appId = $appId, liveData = $lvdApp, " +
                      "value = ${lvdApp.value}")
                }
              }
              else {
                val lvdValue = MutableLiveData<ApplicationItem>(it.openApps[i])
                lvdOpenApps[appId] = lvdValue
              }
            }
          }
          else {
            doOnFailure(response)
          }
        }
      }
    )
  }

  private fun openAppsDifferFrom (openApps: List<ApplicationItem>): Boolean {
    return listsAreDifferent(this.openApps.value!!, openApps)
  }
}