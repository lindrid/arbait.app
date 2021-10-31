package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.items.UserItem
import android.app.Application
import android.content.Context
import android.content.res.Resources

const val APP_VERSION = "1.1.0"

class App : Application() {
  override fun onCreate() {
    super.onCreate()

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
