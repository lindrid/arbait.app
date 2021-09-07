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
    // Remove spacing char
    if ((s.isNotEmpty() && s.length % 5 == 0) || (s.length == 20)) {
      val c = s[s.length - 1]
      if (space == c) {
        s.delete(s.length - 1, s.length)
      }
    }
    // Insert char where needed.
    if ((s.isNotEmpty() && s.length % 5 == 0) || (s.length == 20)) {
      val c = s[s.length - 1]
      // Only if its a digit where there should be a space we insert a space
      if (Character.isDigit(c) && TextUtils.split(s.toString(), space.toString()).size <= 4) {
        s.insert(s.length - 1, space.toString())
      }
    }
  }

  companion object {
    // Change this to what you want... ' ', '-' etc..
    private const val space = ' '
  }
}