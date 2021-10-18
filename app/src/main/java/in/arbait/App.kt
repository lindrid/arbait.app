package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.items.UserItem
import android.app.Application
import android.content.res.Resources

const val APP_VERSION = 1.0

class App : Application() {
  override fun onCreate() {
    super.onCreate()

    //instance = this
    res = resources
    Repository.initialize(this)
    repository = Repository.get()
  }

  companion object {
    lateinit var repository: Repository
      private set

    var noInternetErrorCouldShown = true

    var dbUser: User? = null

    var userItem: UserItem? = null

    //var instance: App? = null
     // private set

    var res: Resources? = null
      private set
  }
}
