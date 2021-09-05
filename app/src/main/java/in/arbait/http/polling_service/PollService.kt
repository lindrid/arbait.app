package `in`.arbait.http.polling_service

import `in`.arbait.*
import `in`.arbait.http.ApplicationsResponse
import `in`.arbait.http.SERVER_OK
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
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import android.app.NotificationManager

private const val TAG = "PollingService"
private const val SERVICE_DELAY_SECONDS: Long = 5

private const val SERVICE_NOTIFICATION_ID = 1
private const val SERVICE_NOTIFICATION_CHANNEL_ID = "Мониторинг заявок"

private const val NEW_APP_NOTIFICATION_ID = 2
private const val NEW_APP_NOTIFICATION_CHANNEL_ID = "Новая заявка"

private const val TEXT_IN = "в"
private const val TEXT_TOMORROW = "Завтра"

// https://robertohuertas.com/2019/06/29/android_foreground_services/
// poll the server for applications
class PollService : LifecycleService() {
  lateinit var appsResponse: LiveData<ApplicationsResponse>
    private set

  private var openApps: List<ApplicationItem> = emptyList()

  private var wakeLock: PowerManager.WakeLock? = null
  private var serviceIsStarted = false
  private lateinit var server: Server
  private val binder: IBinder = PollBinder()

  private var notificationId = NEW_APP_NOTIFICATION_ID

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
    val notification = createNotification (SERVICE_NOTIFICATION_CHANNEL_ID, IMPORTANCE_DEFAULT)
    startForeground(SERVICE_NOTIFICATION_ID, notification)

    server = Server(this)
    server.updateApplicationsResponse()
    appsResponse = server.applicationsResponse

    var firstTime = true
    appsResponse.observe(this,
      Observer { appsResponse ->
        appsResponse?.let {
          log("OBSERVER , firstTime = $firstTime")
          val response = it.response
          if (response.code == SERVER_OK) {
            val openAppsFromServer = it.openApps
            log("openAppsFromServer = $openAppsFromServer")
            if (openAppsFromServer.isNotEmpty()) {
              val newApps = elementsFromANotInB(openAppsFromServer, openApps)
              val closedApps = elementsFromANotInB(openApps, openAppsFromServer)
              log("newApps = $newApps")
              log("newApps.size = ${newApps.size}")
              openApps = openAppsFromServer
              if (newApps.isNotEmpty() && !firstTime) {
                for (i in newApps.indices) {
                  val n = createNewAppNotification(newApps[i])
                  showNotification(newApps[i].id, n)
                }
              }
              if (closedApps.isNotEmpty()) {
                for (j in closedApps.indices) {
                  removeNotification(closedApps[j].id)
                }
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

      //context =  Gson().fromJson(intent.getStringExtra(CONTEXT_ARG), Context::class.java)
      //view =  Gson().fromJson(intent.getStringExtra(VIEW_ARG), View::class.java)

      log( "using an intent with action $action")
      when (action) {
        Actions.START.name -> startService()
        Actions.STOP.name -> stopService()
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
    Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
  }


  private fun startService() {
    if (serviceIsStarted) return
    log("Starting the foreground service task")
    Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
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
    Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
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

  private fun createNewAppNotification (newApp: ApplicationItem): Notification {
    val title = newApp.address

    var word: String = ""
    strToDate(newApp.date, DATE_FORMAT)?.let {
      word = if (isItToday(it)) TEXT_IN.uppercase() else "$TEXT_TOMORROW $TEXT_IN"
    }
    val suffix = if (newApp.hourlyJob) " $TEXT_HOURLY_PAYMENT" else "/$TEXT_DAILY_PAYMENT"
    val price = "${newApp.priceForWorker}$suffix"
    val people = "${newApp.workerTotal} $TEXT_PEOPLE"
    val text = "$word ${newApp.time}, $price, $people"

    return createNotification ( NEW_APP_NOTIFICATION_CHANNEL_ID,
                                IMPORTANCE_HIGH,
                                true,
                                title,
                                text)
  }

  private fun createNotification (notificationChannelId: String,
                                  importance: Int,
                                  vibration: Boolean = false,
                                  title: String = notificationChannelId,
                                  text: String = "",
                                  name: String = notificationChannelId): Notification
  {
    // depending on the Android API that we're dealing with we will have
    // to use a specific method to create the notification
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        notificationChannelId,
        name,
        importance
      ).let {
        it.enableVibration(vibration)
        it.description = notificationChannelId
        it.enableLights(true)
        it.lightColor = Color.RED
        it
      }
      notificationManager.createNotificationChannel(channel)
    }

    val pendingIntent: PendingIntent =
      Intent(this, MainActivity::class.java).let { notificationIntent ->
        PendingIntent.getActivity(this, 0, notificationIntent, 0)
      }

    val builder: Notification.Builder =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
        this,
        notificationChannelId
      ) else Notification.Builder(this)

    return builder
      .setContentTitle(title)
      .setContentText(text)
      .setContentIntent(pendingIntent)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setTicker("Ticker text")
      .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
      .build()
  }
}
