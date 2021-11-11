package `in`.arbait.commission

import `in`.arbait.MainActivity
import `in`.arbait.R
import `in`.arbait.database.User
import `in`.arbait.http.*
import `in`.arbait.http.response.Response
import `in`.arbait.http.response.SERVER_ERROR
import `in`.arbait.http.response.SERVER_OK
import `in`.arbait.http.response.SYSTEM_ERROR
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import java.util.*

private const val TAG = "CommissionFragment"
private const val SBERBANK_TAB_POSITION = 0

class CommissionFragment(private val commission: Int): Fragment()
{
  private lateinit var rootView: View

  private lateinit var tlTabs: TabLayout
  private lateinit var flPay: FrameLayout


  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_commission, container, false)
    rootView = view

    setViews(view)
    addOnTabSelectionListener()
    activateFragment(SBERBANK_TAB_POSITION)
    setHasOptionsMenu(true)

    return view
  }

  private fun addOnTabSelectionListener() {
    tlTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

      override fun onTabSelected(tab: TabLayout.Tab?) {
        // Handle tab select
        activateFragment(tab!!.position)
      }

      override fun onTabReselected(tab: TabLayout.Tab?) {
        // Handle tab reselect
      }

      override fun onTabUnselected(tab: TabLayout.Tab?) {
        // Handle tab unselect
      }
    })
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    actionBar?.title = appName
  }

  private fun setViews(view: View) {
    tlTabs = view.findViewById(R.id.tl_com_tabs)
    flPay = view.findViewById(R.id.fl_com_pay)
  }

  private fun activateFragment(position: Int) {
    var fragment: Fragment? = null
    when (position) {
      0 -> fragment = SberbankFragment(commission)
      1 -> fragment = BalanceFragment(commission)
    }
    val mainActivity = context as MainActivity
    val fm = mainActivity.supportFragmentManager
    val ft: FragmentTransaction = fm.beginTransaction()
    fragment?.let {
      ft.replace(R.id.fl_com_pay, it)
      ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
      ft.commit()
    }
  }
}