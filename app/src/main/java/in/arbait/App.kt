package `in`.arbait

import android.app.Application
import android.content.res.Resources

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    instance = this
    res = resources
    UserRepository.initialize(this)
  }

  companion object {
    var instance: App? = null
      private set
    var res: Resources? = null
      private set
  }
}
