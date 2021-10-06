package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.items.UserItem
import android.app.Application
import android.content.Context
import android.content.res.Resources

class App : Application() {
  override fun onCreate() {
    super.onCreate()

    context = applicationContext
    //instance = this
    res = resources
    UserRepository.initialize(this)
    repository = UserRepository.get()
  }

  companion object {
    lateinit var repository: UserRepository
      private set

    var context: Context? = null
      private set

    var dbUser: User? = null

    var userItem: UserItem? = null

    //var instance: App? = null
     // private set

    var res: Resources? = null
      private set
  }
}
