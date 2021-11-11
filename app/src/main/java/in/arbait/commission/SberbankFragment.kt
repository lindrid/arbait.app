package `in`.arbait.commission

import `in`.arbait.MainActivity
import `in`.arbait.R
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment

private const val TAG = "CommissionFragment"

class SberbankFragment(private val commission: Int): Fragment()
{
  private lateinit var rootView: View
  private lateinit var tvTel: AppCompatTextView
  private lateinit var tvCard: AppCompatTextView
  private lateinit var tvSberTransfer: AppCompatTextView
  private lateinit var tvBankTransfer: AppCompatTextView
  private lateinit var btPayed: AppCompatButton
  private lateinit var btBack: AppCompatButton

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_sberbank, container, false)
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
    tvTel = view.findViewById(R.id.tv_comsber_tel)
    tvCard = view.findViewById(R.id.tv_comsber_card)
    tvSberTransfer = view.findViewById(R.id.tv_comsber_sber_transfer)
    tvBankTransfer = view.findViewById(R.id.tv_comsber_bank_transfer)
    btPayed = view.findViewById(R.id.bt_comsber_payed)
    btBack = view.findViewById(R.id.bt_comsber_back)

    tvTel.text = getString(R.string.comsber_tel, "+79240078897")
    tvCard.text = getString(R.string.comsber_card, "4232 5000 3456 7890")

    val s1 = "$commission р."
    tvSberTransfer.text = getString(R.string.comsber_sber_transfer, s1)

    val s2 = "${commission + 30} р."
    tvBankTransfer.text = getString(R.string.comsber_bank_transfer, s2)
  }
}