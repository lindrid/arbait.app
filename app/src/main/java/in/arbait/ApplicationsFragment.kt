package `in`.arbait

import `in`.arbait.http.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import androidx.core.content.ContextCompat

import androidx.recyclerview.widget.DividerItemDecoration




private const val TAG = "ApplicationsFragment"
private const val HOURLY_TITLE = "р/ч"
private const val DAILY_TITLE = "8ч"

class ApplicationsFragment: Fragment() {

  private lateinit var server: Server
  private lateinit var appItems: LiveData<List<ApplicationItem>>
  private var adapter: AppAdapter = AppAdapter(emptyList())

  private lateinit var rootView: View
  private lateinit var rvApps: RecyclerView

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_applications, container, false)
    rootView = view

    rvApps = view.findViewById(R.id.rv_app_list)
    rvApps.layoutManager = LinearLayoutManager(context)
    rvApps.adapter = adapter

    val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
    rvApps.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    rvApps.addItemDecoration(divider)

    server = Server(requireContext())
    appItems = server.getAppList(requireContext(), rootView)

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    Log.i (TAG, "updateUI(appItems) = $appItems")
    appItems.observe(viewLifecycleOwner,
      Observer { apps ->
        apps?.let {
          Log.i(TAG, "Got apps ${apps.size}")
          updateUI(apps)
        }
      }
    )
  }


  private inner class AppHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
    private val tvTime: TextView = view.findViewById(R.id.tv_app_time)
    private val tvAddress: TextView = view.findViewById(R.id.tv_app_address)
    private val tvIncome: TextView = view.findViewById(R.id.tv_app_worker_income)
    private lateinit var app: ApplicationItem

    fun bind (app: ApplicationItem) {
      this.app = app
      tvTime.text = app.time
      tvAddress.text = app.address

      val price = app.priceForWorker.toString()
      tvIncome.text = when (app.hourlyJob) {
        true -> "$price $HOURLY_TITLE"
        false -> "$price/$DAILY_TITLE"
      }
    }

    override fun onClick(v: View?) {
      //callbacks?.onCrimeSelected(crime.id)
    }
  }

  private inner class AppAdapter (var apps: List<ApplicationItem>):
    RecyclerView.Adapter<AppHolder> ()
  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
      val view = layoutInflater.inflate(R.layout.list_item_app, parent, false)
      return AppHolder(view)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: AppHolder, position: Int) {
      Log.i (TAG, "apps[position] = ${apps[position]}")
      holder.bind(apps[position])
    }
  }

  private fun updateUI(apps: List<ApplicationItem>) {
    adapter = AppAdapter(apps)
    rvApps.adapter = adapter
  }

}