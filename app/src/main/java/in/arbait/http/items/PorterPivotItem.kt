package `in`.arbait.http.items

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PorterPivotItem (
  @SerializedName("application_id") val appId: Int,
  @SerializedName("app_porter_id") val appPorterId: Int,
  @SerializedName("work_hours") val workHours: Int,
  @SerializedName("money") val money: Int,
  @SerializedName("commission") val commission: Int,
  @SerializedName("residue") val residue: Int,
  @SerializedName("porter_payed") val payed: Boolean,
  @SerializedName("pay_is_confirmed") val confirmed: Boolean,
  @SerializedName("is_confirmed") val appIsConfirmed: Boolean,
  @SerializedName("removed") val removed: Boolean,
): Serializable {}

/*
"porters":[{"id":4,"app_user_id":9,"created_at":"2021-09-10 13:43:54","updated_at":"2021-09-10 13:43:54","user":{"id":9,"first_name":"\u0414\u0438\u043c\u0430\u0441","birth_date":"08.06.1987","pass_series_number":null,"pass_date":null,"pass_code":null,"inn":null,"RF_citizen":null,"foreign_pass_series_number":null,"passport_image":null,"created_at":"2021-09-10 13:43:54","updated_at":"2021-09-10 13:43:54"},"pivot":{"application_id":4744,"app_porter_id":4,"app_debit_card_id":2}},{"id":5,"app_user_id":10,"created_at":"2021-09-10 14:04:34","updated_at":"2021-09-10 14:04:34","user":{"id":10,"first_name":"\u0413\u043b\u0435\u0431","birth_date":"15.09.1997","pass_series_number":null,"pass_date":null,"pass_code":null,"inn":null,"RF_cit
"porters": [
  {
    "id":4,
    "app_user_id":9,
    "created_at":"2021-09-10 13:43:54",
    "updated_at":"2021-09-10 13:43:54",
    "user": {
      "id":9,
      "first_name":"\u0414\u0438\u043c\u0430\u0441",
      "birth_date":"08.06.1987",
      "pass_series_number":null,
      "pass_date":null,
      "pass_code":null,
      "inn":null,
      "RF_citizen":null,
      "foreign_pass_series_number":null,
      "passport_image":null,
      "created_at":"2021-09-10 13:43:54",
      "updated_at":"2021-09-10 13:43:54"
    },
    "pivot": {
      "application_id":4744,
      "app_porter_id":4,
      "app_debit_card_id":2
    }
  },
  {
    "id":5,
    "app_user_id":10,
    "created_at":"2021-09-10 14:04:34",
    "updated_at":"2021-09-10 14:04:34",
    "pivot": {
      "application_id":4744,
      "app_porter_id":5,
      "app_debit_card_id":3
    }
  }
]
 */