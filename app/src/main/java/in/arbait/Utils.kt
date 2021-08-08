package `in`.arbait

import android.os.Build

import android.util.Log
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "Utils"

val MANUFACTURER: String = Build.MANUFACTURER
val VERSION: String = Build.VERSION.RELEASE

fun isSamsung(): Boolean {
  if (MANUFACTURER.indexOf("samsung") != -1 ||
      MANUFACTURER.indexOf("Samsung") != -1)
  {
    return true
  }

  return false
}

fun versionIsNineOrGreater(): Boolean {
  var end = VERSION.length
  if (VERSION.indexOf('.') != -1) {
    end = VERSION.indexOf('.')
  }
  val ver = VERSION.substring(0, end)
  Log.i (TAG, "Version: $ver")
  if (ver.toInt() >= 9) {
    return true
  }

  return false
}

fun getDiffYears (first: Date?, last: Date?): Int {
  val a = getCalendar(first)
  val b = getCalendar(last)
  var diff = b[Calendar.YEAR] - a[Calendar.YEAR]
  if (a[Calendar.MONTH] > b[Calendar.MONTH] || a[Calendar.MONTH] == b[Calendar.MONTH] && a[Calendar.DATE] > b[Calendar.DATE]) {
    diff--
  }

  return diff
}

fun strToDate (dateStr: String, dateFormat: String): Date? {
  var date: Date? = null
  val sdf: DateFormat = SimpleDateFormat(dateFormat)

  sdf.isLenient = false
  try {
    date = sdf.parse(dateStr)
  }
  catch (e: ParseException) {
    return null
  }

  return date
}


fun getCalendar (date: Date?): Calendar {
  val cal = Calendar.getInstance()
  cal.time = date!!
  return cal
}