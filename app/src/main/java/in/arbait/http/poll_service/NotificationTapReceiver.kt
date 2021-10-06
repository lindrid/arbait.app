package `in`.arbait.http.poll_service

import `in`.arbait.APP_ID_ARG
import `in`.arbait.MainActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val APP_NO_ID = -1
private const val TAG = "NotificationTapReceiver"

class NotificationTapReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
    Log.i(TAG,"NotificationTapReceiver.onReceive()")

    val intentMain = Intent(context, MainActivity::class.java).apply {
      val fName = intent.getStringExtra(FRAGMENT_NAME_ARG)
      val appId = intent.getIntExtra(APP_ID_ARG, APP_NO_ID)
      putExtra(FRAGMENT_NAME_ARG, fName)
      putExtra(APP_ID_ARG, appId)
      action = Intent.ACTION_MAIN
    }
    context.startActivity(intentMain)

    /*
    val dataIntent = Intent("DataToMainActivity")

    val extra = intent.extras
    Log.i (TAG, "fName=$fName, appId=$appId, extras = $extra")

    LocalBroadcastManager.getInstance(context).sendBroadcast(dataIntent)*/
  }
}