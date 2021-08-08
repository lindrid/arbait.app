package `in`.arbait

import android.os.Build

import android.view.inputmethod.InputMethodSubtype

import android.annotation.SuppressLint
import android.util.Log

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