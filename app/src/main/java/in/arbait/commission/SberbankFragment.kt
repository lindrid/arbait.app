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
const val TEL_NUMBER = "+79240078897"
const val CARD_NUMBER = "4232 5000 3456 7890"

class SberbankFragment(private val commission: Int): Fragment()
{
  private lateinit var rootView: View
  private lateinit var tvTel: AppCompatTextView
  private lateinit var tvCard: AppCompatTextView
  private lateinit var tvSberTransfer: AppCompatTextView
  private lateinit var tvBankTransfer: AppCompatTextView

  private lateinit var btCopyTel: AppCompatButton
  private lateinit var btCopyCard: AppCompatButton
  private lateinit var btPayed: AppCompatButton
  private lateinit var btBack: AppCompatButton

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_sberbank, container, false)
    rootView = view

    setViews(view)
    setHasOptionsMenu(true)

    return view
  }

  private fun setViews(view: View) {
    tvTel = view.findViewById(R.id.tv_comsber_tel)
    tvCard = view.findViewById(R.id.tv_comsber_card)
    tvSberTransfer = view.findViewById(R.id.tv_comsber_sber_transfer)
    tvBankTransfer = view.findViewById(R.id.tv_comsber_bank_transfer)

    btCopyTel = view.findViewById(R.id.bt_comsber_copy_tel)
    btCopyCard = view.findViewById(R.id.bt_comsber_copy_card)
    btPayed = view.findViewById(R.id.bt_comsber_payed)
    btBack = view.findViewById(R.id.bt_comsber_back)

    tvTel.text = getString(R.string.comsber_tel, TEL_NUMBER)
    tvCard.text = getString(R.string.comsber_card, CARD_NUMBER)

    val s1 = "$commission р."
    tvSberTransfer.text = getString(R.string.comsber_sber_transfer, s1)

    val s2 = "${commission + 30} р."
    tvBankTransfer.text = getString(R.string.comsber_bank_transfer, s2)

    btBack.setOnClickListener {
      val mainActivity = context as MainActivity
      mainActivity.replaceOnFragment("Applications")
    }

    btCopyTel.setOnClickListener {
      context?.let {
        copyToClipboard(TEL_NUMBER, it)
      }
    }

    btCopyCard.setOnClickListener {
      context?.let {
        copyToClipboard(CARD_NUMBER, it)
      }
    }
  }
}