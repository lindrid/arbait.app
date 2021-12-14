package `in`.arbait.http

import `in`.arbait.App
import `in`.arbait.getDiffDays
import `in`.arbait.http.items.ApplicationItem
import java.util.*

const val SESSION_LIFETIME_DAYS = 7

fun sessionIsAlive(lastCreatedUserDatetime: Date): Boolean {
  val now = Calendar.getInstance().time
  val diffDays = getDiffDays(lastCreatedUserDatetime, now)
  if (diffDays != -1 && diffDays <= SESSION_LIFETIME_DAYS) {
    return true
  }

  return false
}

fun participationIsConfirmed(takenApp: ApplicationItem): Boolean {
  App.userItem?.let { user ->
    takenApp.porters?.let { porters ->
      for (i in porters.indices) {
        if (porters[i].user.id == user.id) {
          if (porters[i].pivot.participationIsConfirmed)
            return true
        }
      }
    }
  }

  return false
}