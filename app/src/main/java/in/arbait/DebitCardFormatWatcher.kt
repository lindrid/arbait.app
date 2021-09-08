package `in`.arbait

import android.text.TextUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log

private const val TAG = "DebitCardFormatWatcher"

class DebitCardFormatWatcher : TextWatcher {
  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    if (isItPhoneNumber) {
      previousPhoneNumber = s.toString()
    }
  }

  override fun afterTextChanged(s: Editable) {
    val length = s.length
    if (!thereIsWrongSymbol(s.toString())) {
      emptyVars()
      s.clear()
      return
    }

    value = getValue(s.toString())

    Log.i (TAG, "length = $length, isItPhoneNumber = $isItPhoneNumber, itWasDelete = $itWasDelete" +
        ", value = $value, previousPhoneNumber = $previousPhoneNumber, firstDigit = $firstDigit")

    if (s.isNotEmpty() && !itWasDelete) {
      if (isItPhoneNumber || length == 1) {
        if (s[0] == '7' || s[0] == '8' || s[0] == '9' || s[0] == '+') {
          isItPhoneNumber = true

          if (length == 19) {
            isItPhoneNumber = false
            s.clear()
            val n = value.length
            s.append("${value.subSequence(0, 4)} ${value.subSequence(4, 8)}" +
                " ${value.subSequence(8, n)}")

            Log.i(TAG, "value = $value")
            Log.i(TAG, "s = ${s.toString()}")
          }
          else {
            if (s[0] == '+') {
              if (length == 0) {
                s.clear()
                return
              }

              when (length) {
                2 -> if (s[1] != '7') s.replace(1, 1, "7 (")
                3 -> s.insert(2, " (")
                4 -> if (needsToInsertInPhone) s.insert(3, "(")
                8 -> s.insert(7, ") ")
                9 -> if (needsToInsertInPhone) s.insert(8," ")
                13 -> s.insert(12, "-")
                16 -> s.insert(15, "-")
              }
            }
            else {
              val c = s[0]
              s.clear()
              if (c == '7' || c == '8') {
                firstDigit = c.toString()
                s.append("+7")
              }
              else {
                firstDigit = "9"
                s.append("+7 ($c")
              }
            }
          }
        }
        else {
          isItPhoneNumber = false
        }
      }

      if (!isItPhoneNumber) {
        // Insert char where needed.
        if ((length % 5 == 0) || (length == 20)) {
          val c = s[length - 1]
          Log.i (TAG, "insert $space")
          // Only if its a digit where there should be a space we insert a space
          if (Character.isDigit(c) && TextUtils.split(s.toString(), space.toString()).size <= 4) {
            s.insert(length - 1, space.toString())
          }
        }
      }
    }
    else if (itWasDelete) {
      if (s.isEmpty()) {
        emptyVars()
      }

      if (length == 2 && isItPhoneNumber) {
        value = value[0].toString()
      }

      //Log.i (TAG, "previousPhoneNumber = $previousPhoneNumber")
      if (((length == 12 && value[0] == '9') || (length == 13 && value[0] == '8')) &&
        !isItPhoneNumber && previousPhoneNumber != "")
      {
        isItPhoneNumber = true
        //needsToInsertInCard = true
        s.replace(0, length, previousPhoneNumber)
        return
      }
    }
  }

  private fun thereIsWrongSymbol(s: String): Boolean {
    if (s.isEmpty()) return false

    for (i in s.indices) {

    }
  }

  private fun emptyVars() {
    isItPhoneNumber = false
    previousPhoneNumber = ""
    needsToInsertInPhone = false
    value = ""
    firstDigit = ""
  }

  private fun getValue(s: String): String {
    if (firstDigit.isEmpty()) {
      firstDigit = if (s[0].toString() == "+") "7" else s[0].toString()
    }
    value = firstDigit
    for (i in s.indices) {
      if (Character.isDigit(s[i]) && ((s[0] == '+' && i > 1)) || (i > s.indexOf(firstDigit)))
        value += s[i]
    }
    return value
  }

  companion object {
    // +7 (924) 007-88-97
    // 8924 0078 8978 - 12
    // 9240 0788 978 - 11
    var isItPhoneNumber: Boolean = false
      private set

    var previousPhoneNumber: String = ""

    var firstDigit: String = ""

    var itWasDelete = false

    var needsToInsertInPhone = false
    //var needsToInsertInCard = true

    var value = ""

    // Change this to what you want... ' ', '-' etc..
    private const val space = ' '
  }
}