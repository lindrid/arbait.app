package `in`.arbait

import android.text.TextUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log

private const val TAG = "DebitCardFormatWatcher"

private const val MINIMAL_LENGTH_OF_DEBIT_CARD = 11

class DebitCardFormatWatcher : TextWatcher {

  private var sWasChangedByMe = false

  private var previousPhoneNumber: String = ""

  private var firstDigit: String = ""

  private var beforeText = ""

  private var itWasDelete = false

  private var needsToInsertFormatSymbol = false
  //var needsToInsertInCard = true

  private var value = ""

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    val userPastedText = (count >= MINIMAL_LENGTH_OF_DEBIT_CARD && !sWasChangedByMe)
    if (userPastedText) {
      Log.i("b-l-a", "!!!!!!!!!!!!!!!!!")
      Log.i ("b-l-a", "sWasChangedByMe = $sWasChangedByMe")
      Log.i("b-l-a", "s = $s, start = $start, before = $before, count = $count")
      Log.i("b-l-a", "!!!!!!!!!!!!!!!!!")
    }
  }
  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    if (sWasChangedByMe) return

    beforeText = s.toString()

    if (isItPhoneNumber) {
      previousPhoneNumber = s.toString()
    }
  }

  override fun afterTextChanged(s: Editable) {
    if (sWasChangedByMe) {
      sWasChangedByMe = false
      return
    }

    val length = s.length

    val wrongSymbolIndex = getWrongSymbolIndex(s.toString())
    if (wrongSymbolIndex != -1) {
      sWasChangedByMe = true
      s.delete(wrongSymbolIndex, wrongSymbolIndex + 1)
      return
    }

    needsToInsertFormatSymbol = itWasDelete && addedSymbolIsDigit(s.toString())
    itWasDelete = symbolsWasDeleted(s.toString())
    value = getValue(s.toString())

    Log.i (TAG, "length = $length, isItPhoneNumber = $isItPhoneNumber, itWasDelete = $itWasDelete" +
        ", value = $value, previousPhoneNumber = $previousPhoneNumber, firstDigit = $firstDigit")

    if (s.isNotEmpty() && !itWasDelete) {
      Log.i (TAG, "AAA")
      if (isItPhoneNumber || length == 1) {
        if (s[0] == '7' || s[0] == '8' || s[0] == '9' || s[0] == '+') {
          isItPhoneNumber = true
          Log.i (TAG, "isItPhoneNumber = $isItPhoneNumber")

          if (length == 19) {
            isItPhoneNumber = false
            val n = value.length
            sWasChangedByMe = true
            s.replace(0, length, "${value.subSequence(0, 4)} ${value.subSequence(4, 8)}" +
                " ${value.subSequence(8, n)}")

            Log.i(TAG, "value = $value")
            Log.i(TAG, "s = ${s.toString()}")
          }
          else {
            if (s[0] == '+') {
              when (length) {
                2 -> if (s[1] != '7') {
                  s.replace(1, 1, "7 (")
                  sWasChangedByMe = true
                }
                3 -> {
                  s.insert(2, " (")
                  sWasChangedByMe = true
                }
                4 -> if (needsToInsertFormatSymbol) {
                  s.insert(3, "(")
                  sWasChangedByMe = true
                }
                8 -> {
                  s.insert(7, ") ")
                  sWasChangedByMe = true
                }
                9 -> if (needsToInsertFormatSymbol) {
                  s.insert(8," ")
                  sWasChangedByMe = true
                }
                13 -> {
                  s.insert(12, "-")
                  sWasChangedByMe = true
                }
                16 -> {
                  s.insert(15, "-")
                  sWasChangedByMe = true
                }
              }
            }
            else {
              val c = s[0]
              if (c == '7' || c == '8') {
                firstDigit = c.toString()
                sWasChangedByMe = true
                s.replace(0, length, "+7")
              }
              else {
                firstDigit = "9"
                sWasChangedByMe = true
                s.replace(0, length, "+7 ($c")
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
            sWasChangedByMe = true
            s.insert(length - 1, space.toString())
          }
        }
      }
    }
    else if (itWasDelete) {
      if (s.isEmpty()) {
        emptyVars()
      }

      //Log.i (TAG, "previousPhoneNumber = $previousPhoneNumber")
      if (
        (
          (length == 12 && (firstDigit == "9")) ||
          (length == 13 && ((firstDigit == "7") || (firstDigit == "8")))
        ) &&
          !isItPhoneNumber && previousPhoneNumber != "")
      {
        isItPhoneNumber = true
        sWasChangedByMe = true
        s.replace(0, length, previousPhoneNumber)
        return
      }
    }
  }

  private fun addedSymbolIsDigit(s: String): Boolean {
    if (beforeText.length >= s.length) return false

    return s.isNotEmpty() && Character.isDigit(s[s.length - 1])
  }

  private fun symbolsWasDeleted(afterText: String): Boolean {
    return beforeText.length > afterText.length
  }

  private fun getWrongSymbolIndex(s: String): Int {
    if (s.isEmpty()) return -1

    for (i in s.indices) {
      if (!Character.isDigit(s[i]) && s[i] != ' ' && s[i] != '+' && s[i] != '(' && s[i] != ')'
          && s[i] != '-')
      {
        return i
      }
    }

    return -1
  }

  private fun emptyVars() {
    isItPhoneNumber = false
    previousPhoneNumber = ""
    needsToInsertFormatSymbol = false
    value = ""
    firstDigit = ""
    itWasDelete = false
    beforeText = ""
  }

  private fun getValue(s: String): String {
    if (s.isEmpty()) return value

    if (firstDigit.isEmpty() || !Character.isDigit(firstDigit[0])) {
      if (isItPhoneNumber) {
        firstDigit = if (s.length > 1 && s[0] == '+' && s[1] == '7') "7" else s[0].toString()
        if (!Character.isDigit(firstDigit[0])) {
          firstDigit = ""
        }
      }
      else firstDigit = s[0].toString()
    }
    value = firstDigit

    val fdIndex = when {
      isItPhoneNumber && firstDigit == "8" -> s.indexOf("7")
      firstDigit.isEmpty() -> -1
      else -> s.indexOf(firstDigit)
    }

    for (i in s.indices) {
      if (Character.isDigit(s[i]) && (fdIndex != -1 && i > fdIndex))
        value += s[i]
    }
    Log.i (TAG, "value = $value, s = $s")
    return value
  }

  companion object {
    // +7 (924) 007-88-97
    // 8924 0078 8978 - 12
    // 9240 0788 978 - 11
    var isItPhoneNumber: Boolean = false
      private set

    // Change this to what you want... ' ', '-' etc..
    private const val space = ' '
  }
}