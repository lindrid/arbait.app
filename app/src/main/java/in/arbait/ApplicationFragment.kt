package `in`.arbait

import `in`.arbait.models.ApplicationItem
import `in`.arbait.models.PhoneItem
import `in`.arbait.models.PorterItem
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "ApplicationFragment"

const val PHONE_CALL = 1
const val PHONE_WHATSAPP = 2
const val PHONE_CALL_AND_WHATSAPP = 3

const val PM_CARD = 1
const val PM_CASH = 2

class ApplicationFragment (private val appItem: LiveData<ApplicationItem>): Fragment() {

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
  private lateinit var nsvApp: NestedScrollView

  private val vm: PollServerViewModel by lazy {
    val mainActivity = requireActivity() as MainActivity
    mainActivity.pollServerViewModel
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_application, container, false)

    Log.i (TAG, "OnCreate()")
    appItem.value?.let {
      Log.i (TAG, "app.porters = ${it.porters}")
    }
    setViews(view)
    setViewsTexts()

    rvPorters.layoutManager = LinearLayoutManager(context)

    updatePorters()
    setAppObserver()
    setVisibility()

    vm.setContextValues(requireContext(), view, viewLifecycleOwner)

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    val app = getString(R.string.app_action_bar_title)
    actionBar?.title = "$appName - $app"
  }


  private fun setVisibility() {
    rvPorters.visibility = View.INVISIBLE
    btCallClient.visibility = View.INVISIBLE
    val dp = tvDescription.layoutParams as ConstraintLayout.LayoutParams
    dp.topToBottom = tvPorters.id

    val np = nsvApp.layoutParams as ConstraintLayout.LayoutParams
    np.bottomToTop = btBack.id
    //dp.endToEnd = ConstraintLayout.LayoutParams.UNSET
    //dp.marginStart = HEADER_MARGIN_START
  }

  private fun setAppObserver() {
    appItem.observe(viewLifecycleOwner,
      Observer { appItem ->
        appItem?.let {
          updateUI()
        }
      }
    )
  }

  private fun updateUI() {
    setViewsTexts()
    updatePorters()
  }

  private fun updatePorters() {
    appItem.value?.let {
      rvPorters.adapter = PortersAdapter(it.porters)
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
    nsvApp = view.findViewById(R.id.nsv_app)
  }

  private fun setViewsTexts() {
    val appItem = this.appItem.value

    appItem?.let {
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