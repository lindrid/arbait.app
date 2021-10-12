package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.http.poll_service.APP_NO_ID
import `in`.arbait.http.poll_service.Action
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

        if (intent != null) {
          val appId = intent.getIntExtra(APP_ID_ARG, APP_NO_ID)
          val notificationWasTapped = appId != APP_NO_ID

          if (notificationWasTapped) {
            fragment = ApplicationFragment(appId)
            pollServerViewModel.rootView = View(baseContext)
            pollServerViewModel.serviceDoAction(Action.START)
            pollServerViewModel.bindService()
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



}