package `in`.arbait.http.items

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserItem (
  val id: Int,
  @SerializedName("first_name") val name: String,
  @SerializedName("birth_date") val birthDate: String,
  @SerializedName("phones") val phones: List<PhoneItem>,
  @SerializedName("debit_cards") val debitCards: List<DebitCardItem>
): Serializable {}