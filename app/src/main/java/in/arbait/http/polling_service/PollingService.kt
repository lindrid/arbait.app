package `in`.arbait.http.polling_service

import `in`.arbait.CONTEXT_ARG
import `in`.arbait.MainActivity
import `in`.arbait.R
import `in`.arbait.VIEW_ARG
import `in`.arbait.http.ApplicationsResponse
import `in`.arbait.http.Server
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import com.google.gson.Gson

private const val TAG = "PollingService"
private const val SERVICE_DELAY_SECONDS: Long = 5

// https://robertohuertas.com/2019/06/29/android_foreground_services/
// poll the server for applications
class PollingService : LifecycleService()
{
  private var wakeLock: PowerManager.WakeLock? = null
  private var serviceIsStarted = false

  private lateinit var server: Server
  private lateinit var appsResponse: LiveData<ApplicationsResponse>
  //private lateinit var context: Context
  //private lateinit var view: View

  override fun onBind(intent: Intent): IBinder? {
    log( "Some component want to bind with the service")
    // We don't provide binding, so return null
    return null
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

  override fun onCreate() {
    super.onCreate()
    log("The service has been created".toUpperCase())
    val notification = createNotification()
    startForeground(1, notification)

    //server = Server(this)
    //appsResponse = server.getAppsResponseList()

    /*appsResponse.observe(this,
      Observer { appsResponse ->
        appsResponse?.let {
          log("appsResponse WAS CHANGED!")
          log( "Open apps size is ${appsResponse.openApps.size}")
        }
      }
    )*/
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
         //appsResponse = server.getAppsResponseList()
         // appsResponse.value?.let {
         //   log("appsResponse.size = ${it.openApps.size}")
         // }
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

  private fun createNotification(): Notification {
    val notificationChannelId = "ENDLESS SERVICE CHANNEL"

    // depending on the Android API that we're dealing with we will have
    // to use a specific method to create the notification
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      log ("---------------------------------asdasdasdasd")
      val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        notificationChannelId,
        "Endless Service notifications channel",
        NotificationManager.IMPORTANCE_HIGH
      ).let {
        it.setSound(null, null)
        it.description = "Endless Service channel"
        it.enableLights(true)
        it.lightColor = Color.RED
        //it.enableVibration(false)
        //it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
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
      .setContentTitle("Мониторинг заявок")
      .setContentText("не отключайте, если не хотите пропустить новые заявки")
      .setContentIntent(pendingIntent)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setTicker("Ticker text")
      .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
      .build()
  }
}
