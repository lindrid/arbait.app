package `in`.arbait

import `in`.arbait.http.items.DebitCardItem
import `in`.arbait.http.items.UserItem
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


private const val TAG = "DebitCardDialog"
private const val USER_ARG = "user"

class DebitCardDialog: DialogFragment(), View.OnClickListener
{
  private lateinit var rvDebitCards: RecyclerView
  private lateinit var metAnotherDc: MonitoringEditText
  private val ldAnotherDC: MutableLiveData<Boolean> = MutableLiveData(false)

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    dialog?.setTitle(R.string.dcd_choose_debit_card)
    val view = inflater.inflate(R.layout.fragment_dialog_debit_card, container, false)

    rvDebitCards = view.findViewById(R.id.rv_dcd_debit_cards)
    metAnotherDc = view.findViewById(R.id.met_dcd_another_debit_card)
    view.findViewById<Button>(R.id.bt_dcd_dialog_ok).setOnClickListener(this)
    view.findViewById<Button>(R.id.bt_dcd_dialog_cancel).setOnClickListener(this)

    var debitCards = emptyList<DebitCardItem>()
    arguments?.let {
      val user = it.getSerializable(USER_ARG) as UserItem
      debitCards = user.debitCards
    }

    rvDebitCards.layoutManager = LinearLayoutManager(context)
    rvDebitCards.adapter = DebitCardAdapter(debitCards)

    metAnotherDc.setOnFocusChangeListener { _, hasFocus ->
      Log.i (TAG, "hasFocus = $hasFocus")
      ldAnotherDC.value = hasFocus
    }

    return view
  }

  override fun onClick(v: View?) {
    if (v?.id == R.id.bt_dcd_dialog_ok) {
      /*val bundle = Bundle().apply {
        putString(BIRTH_DATE_KEY, str)
      }
      requireActivity().supportFragmentManager.setFragmentResult(BIRTH_DATE_KEY, bundle)*/
    }

    dismiss()
  }

  private inner class DebitCardAdapter (debitCards: List<DebitCardItem>):
    RecyclerView.Adapter<DebitCardAdapter.DebitCardHolder> ()
  {
    private val itemType = 1
    private val strType = 2

    private var selectedPosition: Int = 0

    private val dcs: MutableList<DebitCardItem?> = mutableListOf()
    private val holders: MutableList<DebitCardAdapter.DebitCardHolder> = mutableListOf()

    init {
      for (i in debitCards.indices)
        this.dcs.add(debitCards[i])

      this.dcs.add(null)

      ldAnotherDC.observe(viewLifecycleOwner,
        Observer { anotherDC ->
          Log.i (TAG, "anotherDC = $anotherDC")
          if (anotherDC) {
            holders[selectedPosition].view.setBackgroundColor(Color.TRANSPARENT)
            notifyItemChanged(selectedPosition)
            selectedPosition = RecyclerView.NO_POSITION
            notifyItemChanged(selectedPosition)
            //selectedPosition = RecyclerView.NO_POSITION
            //notifyItemChanged(selectedPosition)
          }
        }
      )
    }

    private inner class DebitCardHolder (val view: View): RecyclerView.ViewHolder(view), View.OnClickListener
    {
      private val tvDebitCard: TextView = view.findViewById(R.id.tv_debit_card)

      init {
        view.setOnClickListener(this)
      }

      fun bind (dcItem: DebitCardItem?) {
        Log.i (TAG, "bind")

        if (dcItem == null) {
          tvDebitCard.text = getString(R.string.dcd_no_card)
        }
        else {
          tvDebitCard.text = dcItem.number
        }
      }

      override fun onClick(v: View?) {
        Log.i ("DebitCardHolder", "onClick()")
        if (adapterPosition == RecyclerView.NO_POSITION) return;

        clearFocusFromAnotherDCEditText()

        Log.i ("DebitCardHolder", "old selectedPosition = $selectedPosition")
        //if (selectedPosition != RecyclerView.NO_POSITION)
          notifyItemChanged(selectedPosition)
        selectedPosition = adapterPosition
        notifyItemChanged(selectedPosition)
        Log.i ("DebitCardHolder", "new selectedPosition = $selectedPosition")
      }

      private fun clearFocusFromAnotherDCEditText() {
        val mainActivity = requireActivity() as MainActivity
        mainActivity.hideKeyboard(metAnotherDc)
        metAnotherDc.clearFocus()
        ldAnotherDC.value = false
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebitCardHolder {
      val view = layoutInflater.inflate(R.layout.list_item_debit_card, parent, false)
      return DebitCardHolder(view)
    }

    override fun getItemCount() = dcs.size

    override fun getItemViewType(position: Int): Int {
      if (position == dcs.size-1)
        return strType

      return itemType
    }

    override fun onBindViewHolder(holder: DebitCardHolder, position: Int) {
      Log.i (TAG, "dcs[position] = ${dcs[position]}")
      holder.bind(dcs[position])
      holder.itemView.setBackgroundColor (
        if (selectedPosition == position) Color.GREEN
        else Color.TRANSPARENT
      )
      holders.add(holder)
    }
  }

  companion object {
    fun newInstance (user: UserItem): DebitCardDialog {
      val args = Bundle().apply {
        putSerializable(USER_ARG, user)
      }

      return DebitCardDialog().apply {
        arguments = args
      }
    }
  }
}