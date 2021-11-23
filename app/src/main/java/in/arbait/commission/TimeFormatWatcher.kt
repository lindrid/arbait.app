package `in`.arbait.commission

import `in`.arbait.MonitoringEditText
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

private const val LENGTH_OF_PHONE_NUMBER_WITHOUT_PLUS = 11
private const val MAX_FORMATTED_TIME_LENGTH = 8

private const val TAG = "TimeFormatWatcher"

class TimeFormatWatcher (private val editText: MonitoringEditText,
                         private val viewLifecycleOwner: LifecycleOwner) : TextWatcher
{
  private var newString = "" //: MutableLiveData<String> = MutableLiveData()

  private var onTextChangedString = ""

  private var textWasPasted: LiveData<Boolean> = editText.textWasPasted

  private var sWasChangedByMe = false

  private var firstDigit: String = ""

  private var beforeText = ""

  private var itWasDelete = false

  private var needsToInsertFormatSymbol = false

  private var value = ""

  init {
    textWasPasted.observe(viewLifecycleOwner,
      Observer { textWasPasted ->
        if (textWasPasted) {
          doOnPasteText()
          sWasChangedByMe = true
          editText.setText(newString)
          editText.setSelection(editText.length())
          editText.resetLiveData()
        }
      }
    )
  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    if (sWasChangedByMe) return

    onTextChangedString = s.toString()
  }
  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    if (sWasChangedByMe) return

    beforeText = s.toString()
  }

  override fun afterTextChanged(s: Editable) {
    if (sWasChangedByMe) {
      sWasChangedByMe = false
      return
    }

    val wrongSymbolIndex = getWrongSymbolIndex(s.toString())
    if (wrongSymbolIndex != -1) {
      sWasChangedByMe = true
      s.delete(wrongSymbolIndex, wrongSymbolIndex + 1)
      return
    }

    val length = s.length

    if (length >= MAX_FORMATTED_TIME_LENGTH + 1) {
      sWasChangedByMe = true
      s.delete(MAX_FORMATTED_TIME_LENGTH, length)
      return
    }

    if (!symbolsWasDeleted(s.toString())) {
      when (length) {
        // hours
        1 -> {
          firstDigitValidation(s, 0, '2')

        }
        2 -> {
          if (secondDigitIsValid(s, 1, 23))
            s.insert(length, ":")
        }

        // minutes
        4 -> {
          firstDigitValidation(s, 3, '5')
        }
        5 -> {
          if (secondDigitIsValid(s, 4, 59))
            s.insert(length, ":")
        }

        // seconds
        7 -> {
          firstDigitValidation(s, 6, '5')
        }
        8 -> {
          if (secondDigitIsValid(s, 7, 59))
            s.insert(length, ":")
        }
      }
    }
  }

  private fun firstDigitValidation(s: Editable, index: Int, maxChar: Char) {
    if (s[index] > maxChar) {
      s.delete(index, index + 1)
    }
  }

  private fun secondDigitIsValid(s: Editable, index: Int, maxVal: Int) : Boolean {
    val str = "${s[index-1]}${s[index]}"
    val v = str.toInt()
    if (v > maxVal) {
      s.delete(index, index + 1)
      return false
    }
    return true
  }

  private fun doOnPasteText() {
    Log.i("doOnPasteText", "!!!!!!!!!!!!!!!!!")

    emptyVars()
    Log.i ("doOnPasteText", "pasted = $onTextChangedString")
    val pasted = onTextChangedString.toString()
    newString = pasted
  }

  private fun addedSymbolIsDigit(s: String): Boolean {
    if (beforeText.length >= s.length) return false

    return s.isNotEmpty() && Character.isDigit(s[s.length - 1])
  }

  private fun symbolsWasDeleted(afterText: String): Boolean {
    return beforeText.length > afterText.length
  }

  private fun getFirstDigitIndex(s: String): Int {
    if (s.isEmpty()) return -1

    for (i in s.indices) {
      if (Character.isDigit(s[i]))
        return i
    }

    return -1
  }

  private fun getWrongSymbolIndex(s: String): Int {
    if (s.isEmpty()) return -1

    for (i in s.indices) {
      if (!Character.isDigit(s[i]) && s[i] != ':') {
        return i
      }
    }

    return -1
  }

  private fun emptyVars() {
    needsToInsertFormatSymbol = false
    value = ""
    firstDigit = ""
    itWasDelete = false
    beforeText = ""
    newString = ""
  }
}