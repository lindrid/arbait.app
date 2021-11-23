package `in`.arbait.commission

import `in`.arbait.*
import `in`.arbait.http.ReactionOnResponse
import `in`.arbait.http.Server
import `in`.arbait.http.response.*
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

const val COMMISSION_ARG = "commission"
const val PAY_TYPE_ARG = "payType"
const val VERTICAL_ITEM_SPACE = 20

private const val SBER_POS = 0
private const val ANOTHER_BANK_POS = 1
private const val TAG = "ConfirmationFragment"

class ConfirmationFragment( private val commission: Int,
                            private val payType: Int) : Fragment()
{
  private lateinit var rootView: View

  private lateinit var btConfirm: AppCompatButton
  private lateinit var btBack: AppCompatButton
  private lateinit var etTime: MonitoringEditText
  private lateinit var etFio: AppCompatEditText
  private lateinit var etCard: MonitoringEditText
  private lateinit var rvBanks: RecyclerView

  private lateinit var server: Server

  private val lvdBankPos = MutableLiveData<Int>(0)
  private var btConfirmIsClicked = false

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_confirmation, container, false)
    rootView = view
    server = Server(requireContext())

    setViews(view)
    setHasOptionsMenu(true)
    setBanksObserver()

    return view
  }

  private fun setViews(view: View) {
    btConfirm = view.findViewById(R.id.bt_conf_confirm)
    btBack = view.findViewById(R.id.bt_conf_back)
    etTime = view.findViewById(R.id.et_conf_time)
    etFio = view.findViewById(R.id.et_conf_fio)
    etCard = view.findViewById(R.id.et_conf_card)
    rvBanks = view.findViewById(R.id.rv_conf_banks)

    etTime.addTextChangedListener(TimeFormatWatcher(etTime, viewLifecycleOwner))

    when (payType) {
      SBER_PAY_TYPE -> {
        rvBanks.layoutManager = LinearLayoutManager(context)
        rvBanks.adapter = BankAdapter(listOf(getString(R.string.sber),
          getString(R.string.another_bank)))

        rvBanks.addItemDecoration(VerticalSpaceItemDecoration(VERTICAL_ITEM_SPACE))
        etTime.visibility = View.INVISIBLE
        setConfirmBtClickListenerForBank()

        App.dbUser?.let { user ->
          if (user.sberFio.isNotEmpty()) {
            etFio.setText(user.sberFio)
          }

          if (user.anotherBank4Digits.isNotEmpty()) {
            etCard.setText(user.anotherBank4Digits)
          }
        }
      }

      BALANCE_PAY_TYPE -> {
        setConfirmBtClickListenerForBalance()
        etFio.visibility = View.INVISIBLE
        etCard.visibility = View.INVISIBLE
      }
    }

    btBack.setOnClickListener {
      val mainActivity = context as MainActivity
      val args = Bundle().apply {
        putInt(COMMISSION_ARG, commission)
      }
      mainActivity.replaceOnFragment("Commission", args)
    }
  }

  private fun setConfirmBtClickListenerForBank() {
    btConfirm.setOnClickListener {
      if (chooseSber()) {
        if (etFio.text.isNullOrEmpty()) {
          showValidationError(requireContext(), etFio, getString(R.string.conf_empty_fio))
        }
        else {
          confirmPayedCommission(etFio.text.toString())
        }
      }
      else {
        val fourDigitsIsValid = validate4Digits()
        if (fourDigitsIsValid) {
          confirmPayedCommission(etCard.text.toString())
        }
        else {
          showValidationError(requireContext(), etCard, getString(R.string.conf_wrong_card))
        }
      }
    }
  }

  private fun setConfirmBtClickListenerForBalance() {
    btConfirm.setOnClickListener {
      if (etTime.text.isNullOrEmpty() || etTime.text.toString().length < 8) {
        showValidationError(requireContext(), etTime, getString(R.string.conf_wrong_time))
      }
      else {
        confirmPayedCommission(etTime.text.toString())
      }
    }
  }

  private fun confirmPayedCommission(s: String) {
    btConfirm.isEnabled = false
    server.commissionPayed(s, commission, payType) { response ->
      when (response.type) {
        SERVER_OK     -> ConfirmReaction(response).doOnServerOkResult(s, payType)
        SYSTEM_ERROR  -> ConfirmReaction(response).doOnSystemError()
        SERVER_ERROR  -> ConfirmReaction(response).doOnServerError()
      }
    }
  }

  private inner class ConfirmReaction (val response: Response):
    ReactionOnResponse(TAG, requireContext(), rootView, response)
  {
    override fun doOnServerOkResult() {}

    fun doOnServerOkResult(s: String, payType: Int) {
      if (payType == SBER_PAY_TYPE) {
        if (chooseSber())
          App.dbUser?.sberFio = s
        else
          App.dbUser?.anotherBank4Digits = s

        App.dbUser?.let {
          App.repository.updateUser(it)
        }
      }

      btConfirm.isEnabled = true
      val mainActivity = context as MainActivity
      mainActivity.replaceOnFragment("Applications")
    }
    override fun doOnServerFieldValidationError(response: Response) {}
    override fun doOnEndSessionError() {}
  }

  private fun setBanksObserver() {
    lvdBankPos.observe(viewLifecycleOwner,
      Observer { bankPos ->
        if (bankPos == SBER_POS) {
          etFio.visibility = View.VISIBLE
          etCard.visibility = View.INVISIBLE
        }

        if (bankPos == ANOTHER_BANK_POS) {
          etFio.visibility = View.INVISIBLE
          etCard.visibility = View.VISIBLE
        }
      }
    )
  }

  private fun chooseSber(): Boolean {
    return (lvdBankPos.value == SBER_POS)
  }

  private fun validate4Digits(): Boolean {
    val fourDigits = etCard.text
    if (fourDigits.isNullOrEmpty())
      return false

    if (fourDigits.length < 4)
      return false

    return true
  }

  private inner class BankAdapter (private val banks: List<String>):
    RecyclerView.Adapter<BankAdapter.BankHolder> ()
  {
    private var selectedPosition: Int = 0
    private val holders: MutableList<BankAdapter.BankHolder> = mutableListOf()

    private inner class BankHolder (val view: View): RecyclerView.ViewHolder(view), View.OnClickListener
    {
      private val tvBank: TextView = view.findViewById(R.id.tv_bank)

      init {
        view.setOnClickListener(this)
      }

      fun bind(bank: String) {
        tvBank.text = bank
      }

      override fun onClick(v: View?) {
        Log.i ("BankHolder", "onClick()")
        if (adapterPosition == RecyclerView.NO_POSITION) return;

        Log.i ("BankHolder", "old selectedPosition = $selectedPosition")
        notifyItemChanged(selectedPosition)
        selectedPosition = adapterPosition
        lvdBankPos.value = selectedPosition
        notifyItemChanged(selectedPosition)
        Log.i ("DebitCardHolder", "new selectedPosition = $selectedPosition")
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankHolder {
      val view = layoutInflater.inflate(R.layout.list_item_bank, parent, false)
      return BankHolder(view)
    }

    override fun getItemCount() = banks.size

    override fun onBindViewHolder(holder: BankHolder, position: Int) {
      Log.i (TAG, "banks[position] = ${banks[position]}")
      holder.bind(banks[position])
      holder.itemView.setBackgroundColor (
        if (selectedPosition == position)
          Color.GRAY
        else
          Color.TRANSPARENT
      )
      holders.add(holder)
    }
  }
}