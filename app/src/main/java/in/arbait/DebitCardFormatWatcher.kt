package `in`.arbait

import android.text.TextUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log

private const val TAG = "DebitCardFormatWatcher"

class DebitCardFormatWatcher : TextWatcher {
  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
  override fun afterTextChanged(s: Editable) {
    Log.i (TAG, "length = ${s.length}")
    if (s.isNotEmpty() && !itWasDelete) {
      if (s[0] == '7' || s[0] == '8' || s[0] == '9' || s[0] == '+') {
        if (s[0] == '+') {
          when (s.length) {
            2 -> if (s[1] != '7') s.replace(1, 1, "7 (")
            3 -> s.replace(2, 2, " (")
          }
        }
        else {
          val c = s[0]
          s.clear()
          if (c == '7') {
            s.append("+7")
          }
          else {
            s.append("+7 ($c")
          }
        }

        isItPhoneNumber = true
      }

      if (!isItPhoneNumber) {
        // Remove spacing char
        if ((s.length % 5 == 0) || (s.length == 20)) {
          val c = s[s.length - 1]
          if (space == c) {
            s.delete(s.length - 1, s.length)
          }
        }
        // Insert char where needed.
        if ((s.length % 5 == 0) || (s.length == 20)) {
          val c = s[s.length - 1]
          // Only if its a digit where there should be a space we insert a space
          if (Character.isDigit(c) && TextUtils.split(s.toString(), space.toString()).size <= 4) {
            s.insert(s.length - 1, space.toString())
          }
        }
      }
    }
  }

  companion object {
    // +7 (924) 007-88-97
    var isItPhoneNumber: Boolean = false
      private set

    var itWasDelete = false

    // Change this to what you want... ' ', '-' etc..
    private const val space = ' '
  }
}