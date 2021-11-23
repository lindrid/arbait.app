package `in`.arbait.commission

import `in`.arbait.MainActivity
import `in`.arbait.R
import `in`.arbait.copyToClipboard
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment

private const val TAG = "CommissionFragment"

const val BALANCE_TEL_NUMBER = "+79240078897"
const val BALANCE_PAY_TYPE = 1

class BalanceFragment(private val commission: Int): Fragment()
{
  private lateinit var rootView: View
  private lateinit var tvTel: AppCompatTextView
  private lateinit var tvPayWithoutCommission: AppCompatTextView
  private lateinit var tvPayInAtm: AppCompatTextView

  private lateinit var btCopyTel: AppCompatButton
  private lateinit var btPayed: AppCompatButton
  private lateinit var btBack: AppCompatButton

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_balance, container, false)
    rootView = view

    setViews(view)
    setHasOptionsMenu(true)

    return view
  }

  private fun setViews(view: View) {
    tvTel = view.findViewById(R.id.tv_combal_tel)
    tvPayWithoutCommission = view.findViewById(R.id.tv_combal_without_commission)
    tvPayInAtm = view.findViewById(R.id.tv_combal_atm)

    btCopyTel = view.findViewById(R.id.bt_combal_copy)
    btPayed = view.findViewById(R.id.bt_combal_payed)
    btBack = view.findViewById(R.id.bt_combal_back)

    tvTel.text = getString(R.string.combal_tel, BALANCE_TEL_NUMBER)

    val s1 = "$commission р."
    tvPayWithoutCommission.text = getString(R.string.combal_without_commission, s1)

    val s2 = "${commission + 50} р."
    tvPayInAtm.text = getString(R.string.combal_atm, s2)

    btPayed.setOnClickListener {
      val mainActivity = context as MainActivity
      val args = Bundle().apply {
        putInt(COMMISSION_ARG, commission)
        putInt(PAY_TYPE_ARG, BALANCE_PAY_TYPE)
      }
      mainActivity.replaceOnFragment("PayConfirmation", args)
    }

    btBack.setOnClickListener {
      val mainActivity = context as MainActivity
      mainActivity.replaceOnFragment("Applications")
    }

    btCopyTel.setOnClickListener {
      context?.let {
        copyToClipboard(SBER_TEL_NUMBER, it)
      }
    }
  }
}