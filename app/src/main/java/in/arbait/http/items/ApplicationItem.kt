package `in`.arbait.http.items

import androidx.lifecycle.LiveData
import com.google.gson.annotations.SerializedName
import java.io.Serializable

const val FROM_DISPATCHER = 0
const val FROM_CLIENT = 1

data class ApplicationItem (
  val id: Int,
  val address: String?,
  val date: String,
  val time: String,
  //@SerializedName("pay_from_who") val payFromWho: Int = FROM_DISPATCHER,
  @SerializedName("what_to_do") val whatToDo: String,
  @SerializedName("pay_method") val payMethod: Int,
  @SerializedName("client_pay") val clientPay: Boolean,
  @SerializedName("client_phone_number") val clientPhoneNumber: String,
  @SerializedName("worker_count") var workerCount: Int,
  @SerializedName("worker_total") val workerTotal: Int,
  @SerializedName("hourly_job") val hourlyJob: Boolean,
  val price: Int,
  @SerializedName("price_for_worker") val priceForWorker: Int,
  val state: Int,
  @SerializedName("dispatcher_id") val dispatcherId: Int,
  @SerializedName("need_to_confirm") val needToConfirm: Boolean,
  @SerializedName("created_at") val createdAt: String,
  @SerializedName("updated_at") val updatedAt: String,
  @SerializedName("porters") val porters: List<PorterItem>?,

  var expanded: Boolean = true,
  var itIsTimeToConfirm: Boolean = false,
  var hideUntilTime: Long = -1L,
  var notificationHasShown: Boolean = false
): Serializable {}

data class LiveDataAppItem (val lvdAppItem: LiveData<ApplicationItem>): Serializable{}

/*
  id: 4444
  address: "Калинина 29б"
  date: "2021-08-23"
  time: "08:30"
  what_to_do: "Выгрузка контейнера с пластиковыми стаканчиками \nРаботы максимум на 1,5 часа"
  pay_method: 2
  client_phone_number: "+7 908 994 56 54"
  worker_count: 0
  worker_total: 4
  hourly_job: 1
  price: 300
  price_for_worker: 250
  state: 1
  income: 0
  income_with_nds: 0
  nds: 0
  outcome: 0
  payed_by_client: 0
  profit: 0
  total_work_hours: null
  dispatcher_id: 1
  edg: 0
  created_at: "2021-08-20 21:26:13"
  updated_at: "2021-08-20 21:26:13"
  */
