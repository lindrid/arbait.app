package `in`.arbait.commission

import `in`.arbait.MainActivity
import `in`.arbait.R
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment

private const val TAG = "CommissionFragment"

class BalanceFragment(private val commission: Int): Fragment()
{
  private lateinit var rootView: View
  private lateinit var tvTel: AppCompatTextView
  private lateinit var tvPayWithoutCommission: AppCompatTextView
  private lateinit var tvPayInAtm: AppCompatTextView
  private lateinit var btPayed: AppCompatButton
  private lateinit var btBack: AppCompatButton

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_balance, container, false)
    rootView = view

    setViews(view)

    btBack.setOnClickListener {
      val mainActivity = context as MainActivity
      mainActivity.replaceOnFragment("Applications")
    }

    setHasOptionsMenu(true)

    return view
  }

  private fun setViews(view: View) {
    tvTel = view.findViewById(R.id.tv_combal_tel)
    tvPayWithoutCommission = view.findViewById(R.id.tv_combal_without_commission)
    tvPayInAtm = view.findViewById(R.id.tv_combal_atm)
    btPayed = view.findViewById(R.id.bt_combal_payed)
    btBack = view.findViewById(R.id.bt_combal_back)

    tvTel.text = getString(R.string.combal_tel, "+79240078897")

    val s1 = "$commission р."
    tvPayWithoutCommission.text = getString(R.string.combal_without_commission, s1)

    val s2 = "${commission + 50} р."
    tvPayInAtm.text = getString(R.string.combal_atm, s2)
  }
}