package `in`.arbait.http

import `in`.arbait.getDiffDays
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