package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.sessionIsAlive
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

    GlobalScope.launch {
      val lastUser: User? = repository.getUserLastByDate(false)
      Log.i (TAG, "Repository.getUserLastByDate(isConfirmed=false): $lastUser")

      val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

      if (currentFragment == null) {
        var fragment: Fragment = RegistrationFragment()

        if (lastUser != null) {
          if (sessionIsAlive(lastUser.createdAt)) {
            fragment = PhoneConfirmationFragment(lastUser)
          }
        }

        supportFragmentManager
          .beginTransaction()
          .add(R.id.fragment_container, fragment)
          .commit()
      }
    }
  }

  fun replaceOnFragment (fragmentName: String, args: Bundle = Bundle()) {
    val fragment: Fragment = when (fragmentName) {
      "PhoneConfirmationFragment" -> {
        val user = args.getSerializable(USER_ARG) as User
        PhoneConfirmationFragment(user)
      }
      else -> RegistrationFragment()
    }

    supportFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, fragment)
      .addToBackStack(null)
      .commit()
  }
}