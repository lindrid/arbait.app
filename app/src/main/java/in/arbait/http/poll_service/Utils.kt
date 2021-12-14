package `in`.arbait.http.poll_service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleService

fun log (msg: String) {
  Log.i("PollService", msg)
}

fun showNotification(context: Context, id: Int, notification: Notification) {
  val notificationManager = context.getSystemService(LifecycleService.NOTIFICATION_SERVICE)
      as NotificationManager
  notificationManager.notify(id, notification)
}

fun removeNotification(context: Context, id: Int) {
  val notificationManager = context.applicationContext.getSystemService(
    LifecycleService.NOTIFICATION_SERVICE) as NotificationManager
  notificationManager.cancel(id)
}