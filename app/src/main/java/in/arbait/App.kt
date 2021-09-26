package `in`.arbait

import `in`.arbait.database.User
import android.app.Application
import android.content.res.Resources

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    instance = this
    res = resources
    UserRepository.initialize(this)
    repository = UserRepository.get()
  }

  companion object {
    lateinit var repository: UserRepository
      private set

    var user: User? = null

    var httpUser: `in`.arbait.http.User? = null

    var instance: App? = null
      private set

    var res: Resources? = null
      private set
  }
}
