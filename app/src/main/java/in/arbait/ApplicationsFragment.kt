package `in`.arbait

import `in`.arbait.http.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "ApplicationsFragment"

class ApplicationsFragment: Fragment() {

  private lateinit var server: Server

  private lateinit var rootView: View
  private lateinit var rvApps: RecyclerView

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View?
  {
    val view = inflater.inflate(R.layout.fragment_applications, container, false)
    rootView = view

    rvApps = view.findViewById(R.id.rv_apps_list)

    server = Server(requireContext())
    server.getAppList { response ->
      val al = AppList(response)
      when (response.code) {
        SERVER_OK -> al.doOnServerOkResult()
        SERVER_ERROR -> al.doOnServerError()
        SYSTEM_ERROR -> al.doOnSystemError()
      }
    }

    return view
  }

  private inner class AppList (response: Response):
    ReactionOnServerResponse(TAG, requireContext(), rootView, response)
  {
    override fun doOnServerOkResult() {
      Log.i(TAG, "сервер отработал без ошибок и вернул список заявок")
    }

    override fun doOnServerFieldValidationError(response: Response) {

    }
  }

}