package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.items.UserItem
import `in`.arbait.http.poll_service.PollService
import `in`.arbait.http.poll_service.StartReceiver
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources

const val APP_VERSION = "1.1.6"

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    val receiver = ComponentName(this, StartReceiver::class.java)
    //val receiver = if (manufacturerIsHuawei()) ComponentName(this, StartReceiver::class.java)
    //else ComponentName(this, StartActivityReceiver::class.java)
    val pm = packageManager

    pm.setComponentEnabledSetting(
      receiver,
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP
    )

    val pollService = ComponentName(this, PollService::class.java)

    pm.setComponentEnabledSetting(
      pollService,
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP
    )
    //instance = this
    res = resources
    Repository.initialize(this)
    repository = Repository.get()
  }

  override fun getApplicationContext(): Context {
    return super.getApplicationContext()
  }

  companion object {
    lateinit var repository: Repository
      private set

    var noInternetErrorCouldShown = true

    var dbUser: User? = null

    var userItem: UserItem? = null

    val appWaitingTime = mutableMapOf<Int, Long>()

    var res: Resources? = null
      private set
  }
}
