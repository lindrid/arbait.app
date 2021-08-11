package `in`.arbait

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Build

import android.util.Log
import android.view.View
import android.widget.EditText
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.createBalloon
import java.io.IOException
import java.lang.Exception
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "Utils"

val MANUFACTURER: String = Build.MANUFACTURER
val VERSION: String = Build.VERSION.RELEASE

private const val CENTURY_21_YEAR_UP_TO = 21
private const val CENTURY_20_YEAR_UP_TO = 99

fun getFullYear (shortcutYear: String): Int {
  try {
    val year = shortcutYear.toInt()
    when (shortcutYear.length) {
      1 -> {
        return 2000 + year
      }
      2 -> {
        if (year <= CENTURY_21_YEAR_UP_TO) {
          return 2000 + year
        }
        else if (year <= CENTURY_20_YEAR_UP_TO) {
          return 1900 + year
        }
      }
      3 -> {
        return if (year <= CENTURY_20_YEAR_UP_TO) {
          2000 + year
        } else {
          900 + year
        }
      }
      4 -> {
        return year
      }
    }
  }
  catch (e: Exception) {
    return -1
  }

  return -1
}

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

fun phoneNumberIsValid (phoneNumber: String?, countryCode: String?, logTag: String): Boolean {
  //NOTE: This should probably be a member variable.
  val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
  try {
    val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, countryCode)
    return phoneUtil.isValidNumber(numberProto)
  }
  catch (e: NumberParseException) {
    Log.i (logTag, "NumberParseException was thrown: $e")
  }
  return false
}

@Throws(InterruptedException::class, IOException::class)
fun internetIsAvailable(): Boolean {
  val command = "ping -c 1 google.com"
  return Runtime.getRuntime().exec(command).waitFor() == 0
}

fun setUnderlineColor (editText: EditText, colorInt: Int) {
  editText.backgroundTintList = ColorStateList.valueOf(colorInt)
}

fun showValidationError (context: Context, field: EditText, textResource: Int) {
  setUnderlineColor(field, Color.RED)
  showErrorBalloon(context, field, textResource)
}

fun showValidationError (context: Context, field: EditText, text: String) {
  setUnderlineColor(field, Color.RED)
  showErrorBalloon(context, field, text)
}

fun showErrorBalloon (context: Context, whereToShow: View, textResource: Int) {
  App.res?.let {
    val str = it.getString(textResource)
    showErrorBalloon(context, whereToShow, str)
  }
}

fun showErrorBalloon(context: Context, whereToShow: View, text: String) {
  val balloon = createBalloon(context) {
    setArrowSize(10)
    setWidth(BalloonSizeSpec.WRAP)
    setHeight(65)
    setArrowPosition(0.7f)
    setCornerRadius(4f)
    setAlpha(0.9f)
    setText("&nbsp  $text  &nbsp")
    setTextSize(18f)
    setTextColorResource(R.color.white)
    setTextIsHtml(true)
    setBackgroundColor(Color.RED)
    setBalloonAnimation(BalloonAnimation.FADE)
    setLifecycleOwner(lifecycleOwner)
  }

  balloon.showAlignBottom(whereToShow)
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