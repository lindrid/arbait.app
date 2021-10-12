package `in`.arbait.http.poll_service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class StartReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED && getServiceState(context) == ServiceState.STARTED) {
      Intent(context, PollService::class.java).also {
        it.action = Action.START.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          log("Starting the service in >=26 Mode from a BroadcastReceiver")
          context.startForegroundService(it)
          return
        }
        log("Starting the service in < 26 Mode from a BroadcastReceiver")
        context.startService(it)
      }
    }
  }
}