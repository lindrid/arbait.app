package `in`.arbait

import `in`.arbait.models.ApplicationItem
import `in`.arbait.models.DebitCardItem
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "ApplicationFragment"

const val PHONE_CALL = 1
const val PHONE_WHATSAPP = 2
const val PHONE_CALL_AND_WHATSAPP = 3

const val PM_CARD = 1
const val PM_CASH = 2

class ApplicationFragment (private val appId: Int): Fragment() {

  private var porter: PorterItem? = null
  private var enroll = false
  private lateinit var lvdAppItem: MutableLiveData<ApplicationItem>

  private lateinit var tvEnrolled: AppCompatTextView
  private lateinit var tvAddress: AppCompatTextView
  private lateinit var tvTime: AppCompatTextView
  private lateinit var tvIncome: AppCompatTextView
  private lateinit var tvDescription: AppCompatTextView
  private lateinit var tvPayMethod: AppCompatTextView
  private lateinit var tvPortersCount: AppCompatTextView
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

    setViews(view)

    vm.rootView = view
    vm.lvdOpenApps[appId]?.let {
      lvdAppItem = it
      it.value?.let { appItem ->
        Log.i (TAG, "set:lvdAppItem")
        porter = getThisUserPorter(appItem)
        updateUI()
      }
    }

    Log.i (TAG, "Porter= $porter")

    rvPorters.layoutManager = LinearLayoutManager(context)
    setAppObserver()
    setVisibility(enroll, view)

    btEnrollRefuse.setOnClickListener {
      onEnrollRefuseBtnClick(view)
    }

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    val appName = getString(R.string.app_name)
    val app = getString(R.string.app_action_bar_title)
    actionBar?.title = "$appName - $app"
  }


  private fun onEnrollRefuseBtnClick(view: View) {
    enroll = !enroll
    setVisibility(enroll, view)
    btEnrollRefuse.text = if (enroll)
       getString(R.string.app_refuse)
    else
      getString(R.string.app_enroll)
  }

  private fun setVisibility(enroll: Boolean, view: View) {
    if (enroll) {
      tvEnrolled.visibility = View.VISIBLE
      rvPorters.visibility = View.VISIBLE
      btCallClient.visibility = View.VISIBLE

      val ap = tvAddress.layoutParams as ConstraintLayout.LayoutParams
      ap.topToTop = ConstraintLayout.LayoutParams.UNSET
      ap.topToBottom = tvEnrolled.id
      tvAddress.layoutParams = ap

      val dp = tvDescription.layoutParams as ConstraintLayout.LayoutParams
      dp.topToBottom = rvPorters.id
      tvDescription.layoutParams = dp

      val np = nsvApp.layoutParams as ConstraintLayout.LayoutParams
      np.bottomToTop = btCallClient.id
      nsvApp.layoutParams = np

    }
    else {
      tvEnrolled.visibility = View.INVISIBLE
      rvPorters.visibility = View.INVISIBLE
      btCallClient.visibility = View.INVISIBLE

      val ap = tvAddress.layoutParams as ConstraintLayout.LayoutParams
      ap.topToTop = view.id
      ap.topToBottom = ConstraintLayout.LayoutParams.UNSET
      tvAddress.layoutParams = ap

      val dp = tvDescription.layoutParams as ConstraintLayout.LayoutParams
      dp.topToBottom = tvPortersCount.id
      tvDescription.layoutParams = dp

      val np = nsvApp.layoutParams as ConstraintLayout.LayoutParams
      np.bottomToTop = btBack.id
      nsvApp.layoutParams = np
    }
    //dp.endToEnd = ConstraintLayout.LayoutParams.UNSET
    //dp.marginStart = HEADER_MARGIN_START
  }

  private fun setAppObserver() {
    lvdAppItem.observe(viewLifecycleOwner,
      Observer { appItem ->
        Log.i ("appItem", "updateUI()")
        appItem?.let {
          porter = getThisUserPorter(it)
          updateUI()
        }
      }
    )
  }

  private fun getThisUserPorter(app: ApplicationItem): PorterItem? {
    var porter: PorterItem? = null
    App.user?.let { user ->
      for (i in app.porters.indices) {
        val phones = app.porters[i].user.phones
        var mainPhone = phones[0]
        for (j in phones.indices) {
          if (phones[j].usedInRegistration) {
            mainPhone = phones[j]
            break
          }
        }
        Log.i (TAG, "user.phone=${user.phone}, mainPhone=$mainPhone")
        if (user.phone == mainPhone.number) {
          porter = app.porters[i]
          break
        }
      }
    }
    return porter
  }

  private fun updateUI() {
    setViewsTexts()
    updatePorters()
  }

  private fun updatePorters() {
    lvdAppItem.value?.let {
      rvPorters.adapter = PortersAdapter(it.porters)
    }
  }

  private fun setViews(view: View) {
    tvEnrolled = view.findViewById(R.id.tv_app_enrolled)
    tvAddress = view.findViewById(R.id.tv_app_address)
    tvTime = view.findViewById(R.id.tv_app_time)
    tvIncome = view.findViewById(R.id.tv_app_worker_income)
    tvDescription = view.findViewById(R.id.tv_app_description)
    tvPayMethod = view.findViewById(R.id.tv_app_pay_method)
    tvPortersCount = view.findViewById(R.id.tv_app_porters)
    rvPorters = view.findViewById(R.id.rv_app_porters)
    btCallClient = view.findViewById(R.id.bt_app_call_client)
    btEnrollRefuse = view.findViewById(R.id.bt_app_enroll_refuse)
    btBack = view.findViewById(R.id.bt_app_back)
    nsvApp = view.findViewById(R.id.nsv_app)
  }

  private fun setViewsTexts() {
    val appItem = this.lvdAppItem.value

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

      val debitCardNumber = getDebitCardNumber()
      val payMethod = if (appItem.payMethod == PM_CARD)
        "${getString(R.string.app_on_card)} $debitCardNumber"
      else
        getString(R.string.app_cash)
      tvPayMethod.text = Html.fromHtml(getString(R.string.app_pay_method, payMethod))

      val workers = "${appItem.workerCount} / ${appItem.workerTotal}"
      tvPortersCount.text = Html.fromHtml(getString(R.string.app_worker_count, workers))

      btEnrollRefuse.text = if (enroll)
        getString(R.string.app_refuse)
      else
        getString(R.string.app_enroll)
      //btCallClient.visibility = View.INVISIBLE
    }
  }

  private fun getDebitCardNumber(): String {
    val dc = getMainDebitCard()
    var dcStr = getString(R.string.app_no_card)
    dc?.let {
      dcStr = it.number
    }
    return dcStr
  }

  private fun getMainDebitCard(): DebitCardItem? {
    var dc: DebitCardItem? = null
    porter?.let {
      val cards = it.user.debitCards
      for (i in cards.indices) {
        if (cards[i].main) {
          dc = cards[i]
          break
        }
      }
    }
    return dc
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