package `in`.arbait

import `in`.arbait.database.User
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

  private lateinit var repository: UserRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    repository = UserRepository.get()

    GlobalScope.launch {
      var lastUser: User? = repository.getUserLastByDate(true)
      Log.i (TAG, "DATABASE: $lastUser")

      val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

      if (currentFragment == null) {
        var fragment: Fragment = RegistrationFragment()

        if (lastUser != null) {
          val now = Calendar.getInstance().time
          val diffDays = getDiffDays(lastUser.createdAt, now)
          if (diffDays != -1 && diffDays <= 7) {
            fragment = PhoneConfirmationFragment()
          }
        }

        supportFragmentManager
          .beginTransaction()
          .add(R.id.fragment_container, fragment)
          .commit()
      }
    }
  }
}