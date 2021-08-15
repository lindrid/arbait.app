package `in`.arbait

import `in`.arbait.database.User
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

  private lateinit var repository: UserRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    repository = UserRepository.get()
    var lastUser: User?

    GlobalScope.launch {
      lastUser = repository.getUserLastByDate(true)
      Log.i (TAG, "DATABASE: $lastUser")

      val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

      if (currentFragment == null) {
        if (lastUser == null) {
          supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, RegistrationFragment())
            .commit()
        }
      }
    }
  }
}