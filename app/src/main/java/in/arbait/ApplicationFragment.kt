package `in`.arbait

import `in`.arbait.models.ApplicationItem
import `in`.arbait.models.PhoneItem
import `in`.arbait.models.PorterItem
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "ApplicationFragment"
private const val MAX_NOT_EXPANDABLE_SIZE = 3
private const val HEADER_MARGIN_START = 15

const val PHONE_CALL = 1
const val PHONE_WHATSAPP = 2
const val PHONE_CALL_AND_WHATSAPP = 3

const val PM_CARD = 1
const val PM_CASH = 2

class ApplicationFragment (private val appItem: ApplicationItem): Fragment() {
  private lateinit var tvAddress: AppCompatTextView
  private lateinit var tvTime: AppCompatTextView
  private lateinit var tvIncome: AppCompatTextView
  private lateinit var tvDescription: AppCompatTextView
  private lateinit var tvPayMethod: AppCompatTextView
  private lateinit var tvPorters: AppCompatTextView
  private lateinit var rvPorters: RecyclerView
  private lateinit var btCallClient: AppCompatButton
  private lateinit var btEnrollRefuse: AppCompatButton
  private lateinit var btBack: AppCompatButton

  private var rvPortersIsExpandable = false
  private var rvPortersIsExpanded = false

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_application, container, false)

    Log.i (TAG, "OnCreate()")
    Log.i (TAG, "app.porters = ${appItem.porters}")
    setViews(view)
    setViewsTexts()

    if (appItem.porters.size > MAX_NOT_EXPANDABLE_SIZE)
      rvPortersIsExpandable = true

    rvPorters.layoutManager = LinearLayoutManager(context)
    updatePorters()

    return view
  }


  private fun updatePorters() {
    if (rvPortersIsExpandable) {
      rvPorters.adapter = getConcatPortersAdapter()
    }
    else {
      rvPorters.adapter = PortersAdapter(appItem.porters)
    }
  }

  private fun getConcatPortersAdapter(): ConcatAdapter {
    val str = if (rvPortersIsExpanded)
      getString(R.string.collapse)
    else
      getString(R.string.show)

    var concatAdapter = ConcatAdapter(HeaderAdapter(str))
    if (rvPortersIsExpanded)
      concatAdapter = ConcatAdapter(concatAdapter, PortersAdapter(appItem.porters))

    return concatAdapter
  }

  private inner class HeaderHolder (view: View) : RecyclerView.ViewHolder(view) {
    private val tvHeader: TextView = view.findViewById(R.id.tv_header)

    fun bind(headerText: String) {
      tvHeader.text = headerText
      tvHeader.textSize = HEADER_TEXT_SIZE
      tvHeader.setTextColor(Color.BLACK)

      val params = tvHeader.layoutParams as ConstraintLayout.LayoutParams
      params.endToEnd = ConstraintLayout.LayoutParams.UNSET
      params.marginStart = HEADER_MARGIN_START
    }
  }

  private inner class HeaderAdapter (val headerText: String):
    RecyclerView.Adapter<HeaderHolder>()
  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
      val view = LayoutInflater.from(parent.context).inflate(
        R.layout.list_item_header,
        parent, false
      )
      return HeaderHolder(view)
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
      holder.bind(headerText)
      holder.itemView.setBackgroundColor(Color.GRAY)

      holder.itemView.setOnClickListener {
        rvPortersIsExpanded = !rvPortersIsExpanded
        updatePorters()
      }
    }
  }

  private fun setViews(view: View) {
    tvAddress = view.findViewById(R.id.tv_app_address)
    tvTime = view.findViewById(R.id.tv_app_time)
    tvIncome = view.findViewById(R.id.tv_app_worker_income)
    tvDescription = view.findViewById(R.id.tv_app_description)
    tvPayMethod = view.findViewById(R.id.tv_app_pay_method)
    tvPorters = view.findViewById(R.id.tv_app_porters)
    rvPorters = view.findViewById(R.id.rv_app_porters)
    btCallClient = view.findViewById(R.id.bt_app_call_client)
    btEnrollRefuse = view.findViewById(R.id.bt_app_enroll_refuse)
    btBack = view.findViewById(R.id.bt_app_back)
  }

  private fun setViewsTexts() {
    tvAddress.text = Html.fromHtml(getString(R.string.app_address, appItem.address))

    val date = strToDate(appItem.date, DATE_FORMAT)
    date?.let {
      val time = if (isItToday(it))
        "${appItem.time}"
      else
        "${getString(R.string.tomorrow)} ${appItem.time}"
      tvTime.text = Html.fromHtml(getString(R.string.app_time, time))
    }

    val suffix = if (appItem.hourlyJob)
      " " + getString(R.string.hourly_suffix)
    else
      getString(R.string.daily_suffix)
    val income = "${appItem.priceForWorker}$suffix"
    tvIncome.text = Html.fromHtml(getString(R.string.app_worker_income, income))

    tvDescription.text = Html.fromHtml(getString(R.string.app_description, appItem.whatToDo))

    val payMethod = if (appItem.payMethod == PM_CARD)
      getString(R.string.app_on_card)
    else
      getString(R.string.app_cash)
    tvPayMethod.text = Html.fromHtml(getString(R.string.app_pay_method, payMethod))

    val workers = "${appItem.workerCount} / ${appItem.workerTotal}"
    tvPorters.text = Html.fromHtml(getString(R.string.app_worker_count, workers))

    btEnrollRefuse.text = getString(R.string.app_enroll)
    //btCallClient.visibility = View.INVISIBLE
  }

  private inner class PorterHolder (view: View) : RecyclerView.ViewHolder(view) {
    private val tvName: TextView = view.findViewById(R.id.tv_porter_name)
    private val ivCall: ImageView = view.findViewById(R.id.iv_porter_call)

    fun bind(porterName: String, phones: List<PhoneItem>) {
      tvName.text = porterName

      var phoneCall = ""
      for (i in phones.indices)
        if (phones[i].type == PHONE_CALL || phones[i].type == PHONE_CALL_AND_WHATSAPP)
          phoneCall = phones[i].number

      ivCall.setOnClickListener {
        Toast.makeText(context, "Call to client: $phoneCall", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private inner class PortersAdapter (val porters: List<PorterItem>):
    RecyclerView.Adapter<PorterHolder>()
  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PorterHolder {
      val view = LayoutInflater.from(parent.context).inflate(
        R.layout.list_item_porter, parent, false
      )
      return PorterHolder(view)
    }

    override fun getItemCount() = porters.size

    override fun onBindViewHolder(holder: PorterHolder, position: Int) {
      holder.bind(porters[position].user.name, porters[position].user.phones)
    }
  }
}