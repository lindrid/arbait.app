package `in`.arbait

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class StartActivityReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val i = Intent(context, MainActivity::class.java)
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(i)
  }

}