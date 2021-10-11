package `in`.arbait

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Preferences
{
  companion object {
    fun putLong(context: Context?, key: String, value: Long) {
      val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
      val editor = prefs.edit()
      editor.putLong(key, value)
      editor.apply()
    }

    fun putInt(context: Context?, key: String, value: Int) {
      val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
      val editor = prefs.edit()
      editor.putInt(key, value)
      editor.apply()
    }
  }
}