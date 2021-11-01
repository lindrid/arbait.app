package `in`.arbait.http.poll_service

import `in`.arbait.*
import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.response.ServiceDataResponse
import `in`.arbait.http.response.SERVER_OK
import `in`.arbait.http.Server
import android.app.*
import android.app.NotificationManager.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import android.app.NotificationManager
import android.app.PendingIntent

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.net.Uri
import java.io.Serializable
import android.media.AudioAttributes
import androidx.lifecycle.MutableLiveData
import java.util.*


private const val TAG = "PollingService"
private const val SERVICE_DELAY_SECONDS: Long = 5
private const val MAX_PORTER_RATING_BY_DEFAULT = 5

private const val ONE_HOUR = 60 * 60 * 1000
private const val ONE_MINUTE = 60 * 1000
private const val APP_MILLISECONDS_ADDITION_BY_DEFAULT = 2 * ONE_HOUR

private const val SERVICE_NOTIFICATION_ID = 1

private val SERVICE_NOTIFICATION_CHANNEL_ID = App.res!!.getString(R.string.poll_service_channel_id)
private val NEW_APP_CHANNEL_ID = App.res!!.getString(R.string.poll_new_app_channel_id)
private val NEW_APP_WITHOUT_SOUND_CHANNEL_ID = App.res!!.getString(R.string.poll_new_app_without_sound_channel_id)

// https://robertohuertas.com/2019/06/29/android_foreground_services/
// poll the server for applications
class PollService : LifecycleService(), Serializable
{
  lateinit var dataResponse: LiveData<ServiceDataResponse>
    private set

  var serviceIsStarted = false
    private set

  val openAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val takenAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()

  private var openApps: MutableList<ApplicationItem> = mutableListOf()
  private var takenApps: MutableList<ApplicationItem> = mutableListOf()

  private var wakeLock: PowerManager.WakeLock? = null

  private lateinit var server: Server
  private val binder: IBinder = PollBinder()

  inner class PollBinder : Binder() {
    // Return this instance of MyService so clients can call public methods
    val service: PollService
      get() = this@PollService
  }

  override fun onBind(intent: Intent): IBinder? {
    super.onBind(intent)
    log( "Some component want to bind with the service")
    return binder
  }

  override fun onCreate() {
    super.onCreate()
    log("The service has been created".toUpperCase())
    // уведомление о запуске сервиса - без звука и вибрации
    val notification = createNotification (SERVICE_NOTIFICATION_CHANNEL_ID, IMPORTANCE_NONE)
    startForeground(SERVICE_NOTIFICATION_ID, notification)

    server = Server(this)
    server.updateApplicationsResponse(cache = false)
    dataResponse = server.serviceDataResponse

    var firstTime = true
    dataResponse.observe(this,
      Observer { appsResponse ->
        appsResponse?.let {
          log("OBSERVER , firstTime = $firstTime")
          val response = it.response
          if (response.type == SERVER_OK) {
            App.noInternetErrorCouldShown = true
            val openAppsFromServer = it.openApps
            log("openAppsFromServer = $openAppsFromServer")

            it.user?.let {  user ->
              App.userItem = user
              log("App.userItem = $user")
            }

            Log.i(TAG, "appsFromANotInB(openAppsFromServer, $openApps)")
            val newApps = appsFromANotInB(openAppsFromServer, openApps)
            val closedApps = appsFromANotInB(openApps, openAppsFromServer)
            logApps(newApps, closedApps)

            setOpenApps(openAppsFromServer, closedApps)

            var serverTime = Date().time
            it.serverTime?.let { serverTimeStr ->
              strToDate(serverTimeStr, DATE_TIME_FORMAT)?.let { time ->
                serverTime = time.time
              }
            }

            var maxPorterRating = MAX_PORTER_RATING_BY_DEFAULT
            it.maxPorterRating?.let { mpr ->
              maxPorterRating = mpr
            }

            if (newApps.isNotEmpty()) {
                Log.i (TAG, "newApps.indices=${newApps.indices}")
                for (i in newApps.indices) {
                  newApps[i].hideUntilTime = getAppWaitingTime(newApps[i], serverTime,
                    maxPorterRating)
                  Log.i(TAG, "app.id = ${newApps[i].id}, hideUntilTime = " +
                      "${newApps[i].hideUntilTime}")
                }
            }

            for (i in openApps.indices) {
              val notificationHasNotShown = !(openApps[i].notificationHasShown)
              if (notificationHasNotShown) {
                if (serverTime >= openApps[i].hideUntilTime) {
                  Log.i(TAG, "Notify: app.id = ${openApps[i].id}")
                  openApps[i].notificationHasShown = true

                  Log.i (TAG, "notificationsOff = ${App.dbUser?.notificationsOff}")
                  if (App.dbUser?.notificationsOff == false && !firstTime) {
                    val n = createNewAppNotification(openApps[i])
                    showNotification(openApps[i].id, n)
                  }
                }
              }
            }

            if (it.takenApps == null)
              this.takenApps = mutableListOf()

            it.takenApps?.let { takenAppsFromServer ->
              setTakenApps(takenAppsFromServer)
            }

            openAppsLvdList.value = openApps
            takenAppsLvdList.value = takenApps

            if (closedApps.isNotEmpty()) {
              for (j in closedApps.indices) {
                removeNotification(closedApps[j].id)
              }
            }

            if (firstTime) firstTime = false
          }
        }
      }
    )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    log( "onStartCommand executed with startId: $startId")
    if (intent != null) {
      val action = intent.action

      log( "using an intent with action $action")
      when (action) {
        Action.START.name -> startService()
        Action.STOP.name -> stopService()
        else -> Log.i(TAG,"This should never happen. No action in the received intent")
      }
    }
    else {
      Log.i(TAG, "with a null intent. It has been probably restarted by the system.")
    }

    // by returning this we make sure the service is restarted if the system kills the service
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    log("The service has been destroyed".toUpperCase())
  }


  private fun setOpenApps(openAppsFromServer: List<ApplicationItem>, closedApps: List<ApplicationItem>)
  {
    if (openApps.isEmpty()) {
      openApps = openAppsFromServer.toMutableList()
    }
    else {
      for (i in openAppsFromServer.indices) {
        val appIsNewOrUpdated = (openAppsFromServer[i].address != null)
        var openAppsContainThisApp = false

        for (j in openApps.indices) {
          if (openApps[j].id == openAppsFromServer[i].id) {
            openAppsContainThisApp = true
            if (appIsNewOrUpdated) {
              val hut = openApps[j].hideUntilTime
              val nhs = openApps[j].notificationHasShown
              openApps[j] = openAppsFromServer[i]
              openApps[j].hideUntilTime = hut
              openApps[j].notificationHasShown = nhs
            }
          }
        }

        if (!openAppsContainThisApp) {
          openApps.add(openAppsFromServer[i])
        }
      }

      openApps.removeAll { app->
        var appIsClosedOrDeleted = false
        for (i in closedApps.indices) {
          if (app.id == closedApps[i].id) {
            appIsClosedOrDeleted = true
            break
          }
        }
        appIsClosedOrDeleted
      }
    }
  }

  private fun setTakenApps(takenAppsFromServer: List<ApplicationItem>) {
    if (takenApps.isEmpty()) {
      takenApps = takenAppsFromServer.toMutableList()
    }
    else {
      for (i in takenAppsFromServer.indices) {
        val appIsNewOrUpdated = (takenAppsFromServer[i].address != null)
        var takenAppsContainThisApp = false

        for (j in takenApps.indices) {
          if (takenApps[j].id == takenAppsFromServer[i].id) {
            takenAppsContainThisApp = true
            if (appIsNewOrUpdated) {
              takenApps[j] = takenAppsFromServer[i]
            }
          }
        }

        if (!takenAppsContainThisApp) {
          takenApps.add(takenAppsFromServer[i])
        }
      }
    }
  }

  private fun logApps(newApps: List<ApplicationItem>, closedApps: List<ApplicationItem>) {
    log("newApps = $newApps")
    log("newApps.size = ${newApps.size}")
    log("closed/remove Apps = $closedApps")
    log("closed/remove Apps.size = ${closedApps.size}")
  }

  private fun startService() {
    if (serviceIsStarted) return
    log("Starting the foreground service task")
    serviceIsStarted = true
    setServiceState(this, ServiceState.STARTED)

    // we need this lock so our service gets not affected by Doze Mode
    wakeLock =
      (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
          acquire()
        }
      }

    // we're starting a loop in a coroutine
    GlobalScope.launch(Dispatchers.IO) {
      while (serviceIsStarted) {
        delay(SERVICE_DELAY_SECONDS * 1 * 1000)
        launch(Dispatchers.IO) {
          server.updateApplicationsResponse()
        }
      }
      log("End of the loop for the service")
    }
  }

  private fun stopService() {
    log("Stopping the foreground service")
    try {
      wakeLock?.let {
        if (it.isHeld) {
          it.release()
        }
      }
      stopForeground(true)
      stopSelf()
    }
    catch (e: Exception) {
      log("Service stopped without being started: ${e.message}")
    }
    serviceIsStarted = false
    setServiceState(this, ServiceState.STOPPED)
  }



  private fun showNotification(id: Int, notification: Notification) {
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(id, notification)
  }

  private fun removeNotification(id: Int) {
    val notificationManager =
      applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(id)
  }


  private fun getAppWaitingTime(newApp: ApplicationItem, serverTime: Long, maxRating: Int): Long
  {
    if (maxRating == 0)
      return serverTime

    val appDateTimeStr = "${newApp.date} ${newApp.time}:00"
    var appDateTime = serverTime + APP_MILLISECONDS_ADDITION_BY_DEFAULT
    strToDate(appDateTimeStr, DATE_TIME_FORMAT)?.let { time ->
      appDateTime = time.time
    }
    val diffTime = appDateTime - serverTime
    val interval: Int = if (diffTime <= ONE_HOUR)
      3 * ONE_MINUTE
    else if (diffTime > ONE_HOUR && diffTime <= 2 * ONE_HOUR)
      15 * ONE_MINUTE
    else if (diffTime > 2 * ONE_HOUR && diffTime <= 3 * ONE_HOUR)
      30 * ONE_MINUTE
    else if (diffTime > 3 * ONE_HOUR && diffTime <= 4 * ONE_HOUR)
      40 * ONE_MINUTE
    else if (diffTime > 4 * ONE_HOUR && diffTime <= 5 * ONE_HOUR)
      50 * ONE_MINUTE
    else if (diffTime > 5 * ONE_HOUR && diffTime <= 6 * ONE_HOUR)
      60 * ONE_MINUTE
    else
      60 * ONE_MINUTE

    val waitMultiplier = interval / maxRating
    val porterRating = App.userItem?.porter?.rating ?: 0

    return serverTime + (waitMultiplier * (maxRating - porterRating)).toLong()
  }

  private fun createNewAppNotification (newApp: ApplicationItem): Notification {
    val title = newApp.address ?: "Oops..."

    var word = ""
    strToDate(newApp.date, DATE_FORMAT)?.let {
     if (!isItToday(it))
       word = getDateStr(it) + " "
    }
    val suffix = if (newApp.hourlyJob)
      getString(R.string.hourly_suffix)
    else
      getString(R.string.daily_suffix)
    val price = "${newApp.priceForWorker}$suffix"
    val people = getString(R.string.people, newApp.workerTotal)
    val text = "$word${newApp.time}, $price, $people"

    var channelId = NEW_APP_CHANNEL_ID

    if (App.dbUser?.soundOff == true) {
      channelId = NEW_APP_WITHOUT_SOUND_CHANNEL_ID
    }

    Log.i (TAG, "channelId = $channelId")

    return createNotification ( channelId,
                                IMPORTANCE_HIGH,
                                true,
                                title,
                                text,
                                channelId,
                                newApp.id)
  }

  private fun createNotification (notificationChannelId: String,
                                  importance: Int,
                                  vibration: Boolean = false,
                                  title: String = notificationChannelId,
                                  text: String = "",
                                  name: String = notificationChannelId,
                                  appId: Int? = null ): Notification
  {
    // depending on the Android API that we're dealing with we will have
    // to use a specific method to create the notification
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Log.i (TAG, "version >= Oreo")
      val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        notificationChannelId,
        name,
        importance
      ).let {
        if (notificationChannelId == NEW_APP_WITHOUT_SOUND_CHANNEL_ID) {
          it.setSound(null, null)
        }
        else {
          val sound = Uri.parse(("android.resource://" + applicationContext.packageName) + "/"
              + R.raw.horn)
          val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
          it.setSound(sound, audioAttributes)
        }
        it.enableVibration(vibration)
        it.description = notificationChannelId
        it.enableLights(true)
        it.lightColor = Color.RED
        it
      }
      notificationManager.createNotificationChannel(channel)
    }

    val pendingIntent: PendingIntent =
      Intent(this, NotificationTapReceiver::class.java).let { notificationIntent ->
        notificationIntent.putExtra(APP_ID_ARG, appId)
        notificationIntent.action = "TAP_ON_NOTIFICATION"
        PendingIntent.getBroadcast(this, 0, notificationIntent,
          FLAG_UPDATE_CURRENT)
      }

    val builder: Notification.Builder =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        Notification.Builder(this, notificationChannelId)
      else
        Notification.Builder(this)

    return builder
      .setContentTitle(title)
      .setContentText(text)
      .setContentIntent(pendingIntent)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setTicker("Ticker text")
      .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
      .setAutoCancel(true)
      .build()
  }
}
