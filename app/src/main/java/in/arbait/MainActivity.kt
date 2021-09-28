package `in`.arbait

import `in`.arbait.database.User
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


private const val TAG = "MainActivity"


class MainActivity : AppCompatActivity() {

  val pollServerViewModel: PollServerViewModel by lazy {
    ViewModelProvider(this).get(PollServerViewModel::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    pollServerViewModel.mainActivity = this
    pollServerViewModel.context = this
    pollServerViewModel.viewLifecycleOwner = this

    GlobalScope.launch {
      App.dbUser = App.repository.getUserLastByDate()
      val lastUser: User? = App.dbUser

      Log.i (TAG, "Repository.getUserLastByDate(): $lastUser")

      /*App.dbUser?.let {
        it.isConfirmed = false
        it.login = false
        App.repository.updateUser(it)
      }*/

      val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

      if (currentFragment == null) {
        var fragment: Fragment = RegistrationFragment()

        if (lastUser != null) {
          if (lastUser.login) {
            fragment =
              if (lastUser.isConfirmed)
                ApplicationsFragment()
              else
                PhoneConfirmationFragment(true)
          }
          else {
            App.dbUser?.let { user ->
              fragment =
                if (user.isItRegistration)
                  PhoneConfirmationFragment()
                else
                  LoginFragment()
            }
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
      "PhoneConfirmation" -> {
        val verifyForLogin = args.getBoolean(VERIFY_FOR_LOGIN_ARG)
        PhoneConfirmationFragment(verifyForLogin)
      }
      "Application" -> {
        val appId = args.getInt(APP_ID_ARG)
        ApplicationFragment(appId)
      }
      "Applications" -> ApplicationsFragment()
      "Login" -> LoginFragment()
       else -> RegistrationFragment()
    }

    supportFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, fragment)
      .addToBackStack(null)
      .commit()
  }
}