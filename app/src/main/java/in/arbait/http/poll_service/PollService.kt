package `in`.arbait.http.poll_service

import `in`.arbait.*
import `in`.arbait.http.Server
import `in`.arbait.http.appIsConfirmed
import `in`.arbait.http.items.ApplicationItem
import `in`.arbait.http.response.SERVER_OK
import `in`.arbait.http.response.ServiceDataResponse
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_NONE
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.*

const val MINUTE = 60 * 1000
const val ONE_HOUR = 60 * MINUTE
const val APP_MILLISECONDS_ADDITION_BY_DEFAULT = 2 * ONE_HOUR

private const val TAG = "PollingService"
private const val SERVICE_DELAY_SECONDS: Long = 5
private const val MAX_PORTER_RATING_BY_DEFAULT = 5

private const val TWO_HOURS_AND_FIVE_MINUTES = 2 * ONE_HOUR + 5 * MINUTE

private const val SERVICE_NOTIFICATION_ID = 1
private const val SERVICE_DELETED_APP_NOTIFICATION_ID = 2
private const val SERVICE_REMOVED_FROM_APP_NOTIFICATION_ID = 3
private const val SERVICE_APP_CONFIRMATION_NOTIFICATION_ID = 4

private val NEW_APP_CHANNEL_ID = App.res!!.getString(R.string.poll_new_app_channel_id)
private val NEW_APP_WITHOUT_SOUND_CHANNEL_ID = App.res!!.getString(R.string.poll_new_app_without_sound_channel_id)
private val REMOVED_FROM_APP_WITHOUT_SOUND_CHANNEL_ID = App.res!!.getString(R.string.poll_removed_from_app_without_sound_channel_id)
private val DELETED_APP_WITHOUT_SOUND_CHANNEL_ID = App.res!!.getString(R.string.poll_deleted_app_without_sound_channel_id)

// https://robertohuertas.com/2019/06/29/android_foreground_services/
// poll the server for applications
class PollService : LifecycleService(), Serializable
{
  lateinit var dataResponse: LiveData<ServiceDataResponse>
    private set

  var serviceIsStarted = false
    private set

  val lvdServerDateTime: MutableLiveData<Date> = MutableLiveData()
  val lvdServerTimeMlsc: MutableLiveData<Long> = MutableLiveData()
  val lvdDispatcherWhatsapp: MutableLiveData<String> = MutableLiveData()
  val lvdDispatcherPhoneCall: MutableLiveData<String> = MutableLiveData()

  val openAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val takenAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val deletedAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()
  val removedFromAppsLvdList: MutableLiveData<List<ApplicationItem>> = MutableLiveData()

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
    val notification = createNotification (
      getString(R.string.poll_service_channel_id),
      Uri.EMPTY,
      IMPORTANCE_NONE
    )
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

            it.user?.let { user ->
              App.userItem = user
              log("App.userItem = $user")
            }

            it.dispatcherWhatsapp?.let { phone ->
              lvdDispatcherWhatsapp.value = phone
            }

            it.dispatcherPhoneCall?.let { phone ->
              lvdDispatcherPhoneCall.value = phone
            }

            deletedAppsLvdList.value = null
            it.deletedApps?.let { deletedApps ->
              for (i in deletedApps.indices) {
                if (App.dbUser?.notificationsOff == false) {
                  val n = createDeletedAppNotification(deletedApps[i])
                  showNotification(applicationContext, deletedApps[i].id, n)
                }
              }
              deletedAppsLvdList.value = deletedApps
            }

            removedFromAppsLvdList.value = null
            it.removedFromApps?.let { removedFromApps ->
              for (i in removedFromApps.indices) {
                if (App.dbUser?.notificationsOff == false) {
                  val n = createRemovedFromAppNotification(removedFromApps[i])
                  showNotification(applicationContext, removedFromApps[i].id, n)
                }
              }
              removedFromAppsLvdList.value = removedFromApps
            }

            Log.i(TAG, "appsFromANotInB(openAppsFromServer, $openApps)")
            val newApps = appsFromANotInB(openAppsFromServer, openApps)
            val closedApps = appsFromANotInB(openApps, openAppsFromServer)
            logApps(newApps, closedApps)

            setOpenApps(openAppsFromServer, closedApps)

            lvdServerDateTime.value = Date()
            lvdServerTimeMlsc.value = Date().time
            it.serverTime?.let { serverTimeStr ->
              strToDate(serverTimeStr, DATE_TIME_FORMAT)?.let { time ->
                lvdServerDateTime.value = time
                lvdServerTimeMlsc.value = time.time
              }
            }

            var maxPorterRating = MAX_PORTER_RATING_BY_DEFAULT
            it.maxPorterRating?.let { mpr ->
              maxPorterRating = mpr
            }

            if (newApps.isNotEmpty()) {
                Log.i (TAG, "newApps.indices=${newApps.indices}")
              lvdServerTimeMlsc.value?.let {  timeMlsc ->
                for (i in newApps.indices) {
                  newApps[i].hideUntilTime = getAppWaitingTime(newApps[i], timeMlsc,
                    maxPorterRating)
                  Log.i(TAG, "app.id = ${newApps[i].id}, hideUntilTime = " +
                    "${newApps[i].hideUntilTime}")
                }
              }
            }

            for (i in openApps.indices) {
              val notificationHasNotShown = !(openApps[i].notificationHasShown)
              if (notificationHasNotShown) {
                lvdServerTimeMlsc.value?.let { timeMlsc ->
                  if (timeMlsc >= openApps[i].hideUntilTime) {
                    Log.i(TAG, "Notify: app.id = ${openApps[i].id}")
                    openApps[i].notificationHasShown = true

                    Log.i(TAG, "notificationsOff = ${App.dbUser?.notificationsOff}")
                    if (App.dbUser?.notificationsOff == false && !firstTime) {
                      if (!justRefusedOrRemovedFromThisApp(openApps[i].id)) {
                        val n = createNewAppNotification(openApps[i])
                        showNotification(applicationContext, openApps[i].id, n)
                      }
                    }
                  }
                }
              }
            }

            if (it.takenApps == null)
              this.takenApps = mutableListOf()

            var takenAppsWasChanged = false
            it.takenApps?.let { takenAppsFromServer ->
              val refusedApps = appsFromANotInB(takenApps, takenAppsFromServer)
              takenAppsWasChanged = setTakenApps(takenAppsFromServer, refusedApps)
            }

            openAppsLvdList.value = openApps

            val takenAppsFromServer = takenApps
            if (takenAppsWasChanged) {
              lvdServerDateTime.value?.let { serverDate ->
                doThingsIfItsTimeToConfirmApp(takenAppsFromServer, serverDate)
              }
              takenAppsLvdList.value = takenAppsFromServer
            }
            else {
              val mList = takenAppsLvdList.value?.toMutableList()
              mList?.let { list ->
                lvdServerDateTime.value?.let { serverDate ->
                  doThingsIfItsTimeToConfirmApp(list, serverDate)
                }
              }
              takenAppsLvdList.value = if (mList == null) emptyList() else mList
            }

            Log.i (TAG, "takenApps = ${takenAppsLvdList.value}")

            if (closedApps.isNotEmpty()) {
              for (j in closedApps.indices) {
                removeNotification(applicationContext, closedApps[j].id)
                removeOpenApp(closedApps[j].id)
              }
              openAppsLvdList.value = openApps
            }

            if (firstTime) firstTime = false
          }
        }
      }
    )
  }

  // срабатывает правильно ДО того, как takenApps = takenAppsFromServer
  private fun justRefusedOrRemovedFromThisApp(id: Int): Boolean {
    for (i in takenApps.indices)
      if (takenApps[i].id == id)
        return true

    return false
  }

  private fun removeOpenApp(id: Int) {
    for (i in openApps.indices) {
      if (openApps[i].id == id) {
        openApps.removeAt(i)
        break
      }
    }
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


  private fun doThingsIfItsTimeToConfirmApp(takenApps: MutableList<ApplicationItem>,
                                            serverDate: Date)
  {
    for (i in takenApps.indices) {
      if (takenApps[i].state > CLOSED_STATE)
        continue

      if (takenApps[i].needToConfirm) {
        if (!appIsConfirmed(takenApps[i])) {
          if (itIsTimeToConfirmApp(takenApps[i], serverDate)) {
            takenApps[i].itIsTimeToConfirm = true

            val notificationHasNotShown = !(takenApps[i].notificationHasShown)
            if (notificationHasNotShown && App.dbUser?.notificationsOff == false) {
              val n = createConfirmationNotification(takenApps[i].id)
              showNotification(applicationContext, takenApps[i].id, n)
              takenApps[i].notificationHasShown = true
            }
          }
          else {
            takenApps[i].itIsTimeToConfirm = false
          }
        }
      }
    }
  }

  private fun setOpenApps(openAppsFromServer: List<ApplicationItem>,
                          closedApps: List<ApplicationItem>)
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

  private fun setTakenApps(takenAppsFromServer: List<ApplicationItem>,
                           refusedApps: List<ApplicationItem>): Boolean
  {
    var takenAppsWasChanged = false

    if (takenApps.isEmpty()) {
      takenApps = takenAppsFromServer.toMutableList()
      takenAppsWasChanged = true
    }
    else {
      for (i in takenAppsFromServer.indices) {
        val appIsNewOrUpdated = (takenAppsFromServer[i].address != null)
        var takenAppsContainThisApp = false

        for (j in takenApps.indices) {
          if (takenApps[j].id == takenAppsFromServer[i].id) {
            takenAppsContainThisApp = true
            if (appIsNewOrUpdated) {
              Log.i ("PEREZAPIS", "takenAppsFromServer[i] = ${takenAppsFromServer[i]}")
              takenApps[j] = takenAppsFromServer[i]
              takenAppsWasChanged = true
            }
          }
        }

        if (!takenAppsContainThisApp) {
          val appIsNewOrUpdated = (takenAppsFromServer[i].address != null)
          if (appIsNewOrUpdated)
            takenApps.add(takenAppsFromServer[i])
        }
      }

      takenApps.removeAll { app->
        var appIsDeletedOrPorterIsRefused = false
        for (i in refusedApps.indices) {
          if (app.id == refusedApps[i].id) {
            appIsDeletedOrPorterIsRefused = true
            takenAppsWasChanged = true
            break
          }
        }
        appIsDeletedOrPorterIsRefused
      }
    }

    return takenAppsWasChanged
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

  /*private fun showNotification(id: Int, notification: Notification) {
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(id, notification)
  }

   private fun removeNotification(id: Int) {
    val notificationManager =
      applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(id)
  } */


  private fun getAppWaitingTime(newApp: ApplicationItem, serverTime: Long, maxRating: Int): Long
  {
    if (maxRating == 0)
      return serverTime

    val appDateTime = getAppTimeMlsc(newApp, serverTime)

    var appCreatedAt = serverTime
    strToDate(newApp.createdAt, DATE_TIME_FORMAT)?.let { time ->
      appCreatedAt = time.time
    }

    val timeBeforeStart = appDateTime - appCreatedAt

    val interval: Int = 1 * MINUTE /*if (timeBeforeStart <= 55 * MINUTE)
      1 * MINUTE
    else if (timeBeforeStart > 55 * MINUTE && timeBeforeStart <= 3 * ONE_HOUR)
      3 * MINUTE
    else if (timeBeforeStart > 3 * ONE_HOUR && timeBeforeStart <= 6 * ONE_HOUR)
      5 * MINUTE
    else
      10 * MINUTE*/

    /*else if (timeBeforeStart > 65 * MINUTE && timeBeforeStart <= 75 * MINUTE)
      5 * MINUTE
    else if (timeBeforeStart > 75 * MINUTE && timeBeforeStart <= 95 * MINUTE)
      10 * MINUTE
    else if (timeBeforeStart > 95 * MINUTE && timeBeforeStart <= 135 * MINUTE)
      15 * MINUTE
    else if (timeBeforeStart > 135 * MINUTE && timeBeforeStart <= 150 * MINUTE)
      20 * MINUTE
    else if (timeBeforeStart > 150 * MINUTE && timeBeforeStart <= 3 * ONE_HOUR)
      30 * MINUTE
    else if (timeBeforeStart > 3 * ONE_HOUR && timeBeforeStart <= 4 * ONE_HOUR)
      40 * MINUTE
    else if (timeBeforeStart > 4 * ONE_HOUR && timeBeforeStart <= 5 * ONE_HOUR)
      50 * MINUTE
    else if (timeBeforeStart > 5 * ONE_HOUR && timeBeforeStart <= 6 * ONE_HOUR)
      60 * MINUTE*/

    val waitMultiplier = interval / maxRating
    val porterRating = App.userItem?.porter?.rating ?: 0

    return appCreatedAt + (waitMultiplier * (maxRating - porterRating)).toLong()
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

    val sound = Uri.parse(("android.resource://" + applicationContext.packageName) + "/"
        + R.raw.horn)

    return createNotification (
      channelId,
      sound,
      IMPORTANCE_HIGH,
      true,
      title,
      text,
      channelId,
      newApp.id
    )
  }

  private fun createDeletedAppNotification (deletedApp: ApplicationItem): Notification {
    val title = getString(R.string.poll_deleted_app, deletedApp.address ?: "Oops...")
    val text = ""

    var channelId = getString(R.string.poll_deleted_app_channel_id)

    if (App.dbUser?.soundOff == true) {
      channelId = DELETED_APP_WITHOUT_SOUND_CHANNEL_ID
    }

    Log.i (TAG, "channelId = $channelId")

    val sound = Uri.parse(("android.resource://" + applicationContext.packageName) + "/"
        + R.raw.sad_trombon
    )

    return createNotification (
      channelId,
      sound,
      IMPORTANCE_HIGH,
      true,
      title,
      text,
      channelId,
      SERVICE_DELETED_APP_NOTIFICATION_ID
    )
  }

  private fun createRemovedFromAppNotification (removedFromApp: ApplicationItem): Notification {
    val title = getString(R.string.poll_removed_from_app, removedFromApp.address ?: "Oops...")
    val text = ""

    var channelId = getString(R.string.poll_removed_from_app_channel_id)

    if (App.dbUser?.soundOff == true) {
      channelId = REMOVED_FROM_APP_WITHOUT_SOUND_CHANNEL_ID
    }

    Log.i (TAG, "channelId = $channelId")

    val sound = Uri.parse(("android.resource://" + applicationContext.packageName) + "/"
        + R.raw.suddenness
    )

    return createNotification (
      channelId,
      sound,
      IMPORTANCE_HIGH,
      true,
      title,
      text,
      channelId,
      SERVICE_REMOVED_FROM_APP_NOTIFICATION_ID
    )
  }

  private fun createConfirmationNotification (appId: Int): Notification {
    val title = getString(R.string.poll_confirmation_notification)
    val text = getString(R.string.poll_confirmation_notification_text)

    var channelId = NEW_APP_CHANNEL_ID

    if (App.dbUser?.soundOff == true) {
      channelId = NEW_APP_WITHOUT_SOUND_CHANNEL_ID
    }

    Log.i (TAG, "channelId = $channelId")

    val sound = Uri.parse(("android.resource://" + applicationContext.packageName) + "/"
        + R.raw.horn)

    return createNotification (
      channelId,
      sound,
      IMPORTANCE_HIGH,
      true,
      title,
      text,
      channelId,
      SERVICE_APP_CONFIRMATION_NOTIFICATION_ID
    )
  }

  private fun createNotification (notificationChannelId: String,
                                  sound: Uri,
                                  importance: Int,
                                  vibration: Boolean = false,
                                  title: String = notificationChannelId,
                                  text: String = "",
                                  name: String = notificationChannelId,
                                  notificationId: Int? = null ): Notification
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
        if (notificationChannelId == NEW_APP_WITHOUT_SOUND_CHANNEL_ID ||
            notificationChannelId == REMOVED_FROM_APP_WITHOUT_SOUND_CHANNEL_ID ||
            notificationChannelId == DELETED_APP_WITHOUT_SOUND_CHANNEL_ID
        ) {
          it.setSound(null, null)
        }
        else {
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
        notificationIntent.putExtra(APP_ID_ARG, notificationId)
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
