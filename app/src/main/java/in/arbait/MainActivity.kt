package `in`.arbait

import `in`.arbait.commission.CommissionFragment
import `in`.arbait.database.User
import `in`.arbait.http.PollServerViewModel
import `in`.arbait.http.poll_service.APP_NO_ID
import `in`.arbait.http.poll_service.Action
import `in`.arbait.http.poll_service.NOTIFICATION_ARG
import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.View
import android.view.inputmethod.InputMethodManager


private const val TAG = "MainActivity"


class MainActivity : AppCompatActivity()
{
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
            fragment =
              if (lastUser.isItRegistration)
                PhoneConfirmationFragment()
              else
                LoginFragment()
          }
        }

        if (intent != null) {
          val s = intent.getStringExtra(NOTIFICATION_ARG)
          if (s == "thisIsNotificationIntent") {
            fragment = ApplicationsFragment()
          }
          /*val appId = intent.getIntExtra(APP_ID_ARG, APP_NO_ID)
          val notificationWasTapped = appId != APP_NO_ID

          if (notificationWasTapped) {
            fragment = ApplicationFragment(appId)
            pollServerViewModel.rootView = View(baseContext)
            pollServerViewModel.serviceDoAction(Action.START)
            pollServerViewModel.bindService()
          }*/
        }

        supportFragmentManager
          .beginTransaction()
          .add(R.id.fragment_container, fragment)
          .commit()
      }
    }
  }

  fun replaceOnFragment (fragmentName: String, args: Bundle = Bundle()) {
    Log.i (TAG, "replaceOnFragment")
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
      "Commission" -> {
        val commission = args.getInt(COMMISSION_ARG)
        CommissionFragment(commission)
      }
       else -> RegistrationFragment()
    }

    supportFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, fragment)
      .addToBackStack(null)
      .commit()
  }

  fun hideKeyboard(view: View?) {
    val activity = this
    val imm: InputMethodManager =
      activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view != null) {
      imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
  }

  override fun onDestroy() {
    super.onDestroy()

  }

}