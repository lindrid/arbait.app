package `in`.arbait

import android.text.TextUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

private const val TAG = "PhoneFormatWatcher"

private const val LENGTH_OF_PHONE_NUMBER_WITHOUT_PLUS = 11
private const val MAX_FORMATTED_PHONE_LENGTH = 18

class PhoneFormatWatcher (private val editText: MonitoringEditText,
  private val viewLifecycleOwner: LifecycleOwner) : TextWatcher {

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

    if (length >= MAX_FORMATTED_PHONE_LENGTH + 1) {
      sWasChangedByMe = true
      s.delete(MAX_FORMATTED_PHONE_LENGTH, length)
      return
    }

    needsToInsertFormatSymbol = itWasDelete && addedSymbolIsDigit(s.toString())
    itWasDelete = symbolsWasDeleted(s.toString())
    value = getValue(s.toString())

    Log.i (TAG, "length = $length, itWasDelete = $itWasDelete" +
        ", value = $value, firstDigit = $firstDigit")

    if (s.isNotEmpty() && !itWasDelete) {
        if (Character.isDigit(s[0]) || s[0] == '+') {
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
            firstDigit = c.toString()
            if (c == '7' || c == '8') {
              sWasChangedByMe = true
              s.replace(0, length, "+7")
            }
            else {
              sWasChangedByMe = true
              s.replace(0, length, "+7 ($c")
            }
          }
        }
    }
    else if (itWasDelete) {
      if (s.isEmpty()) {
        emptyVars()
      }
    }
  }

  private fun doOnPasteText() {
    Log.i("doOnPasteText", "!!!!!!!!!!!!!!!!!")

    emptyVars()
    Log.i ("doOnPasteText", "onTextChangedString = $onTextChangedString")
    value = onTextChangedString.toString()
    var index = getNotDigitIndex(value)
    while (index != -1) {
      value = value.removeRange(index, index + 1)
      index = getNotDigitIndex(value)
    }
    firstDigit = value[0].toString()
    // 9510002234
    // +7 (951) 000-22-34
    Log.i ("doOnPasteText", "value = $value, firstDigit = $firstDigit")

    val funL1: () -> Unit = {
      Log.i ("doOnPasteText", "funL1()")
      newString = if ((firstDigit == "7") || (firstDigit == "8")) "+7" else "+7 ($firstDigit"
    }

    val funL2: () -> Unit = {
      Log.i ("doOnPasteText", "funL2()")
      funL1()
      if (firstDigit == "9")
        newString += value[1]
      else
        newString += " (${value[1]}"
    }

    val funL3: () -> Unit = {
      Log.i ("doOnPasteText", "funL3()")
      funL2()
      newString +=
        if (firstDigit == "9")
          value[2] + ")"
        else
          value[2]
    }

    val funL4: () -> Unit = {
      Log.i ("doOnPasteText", "funL4()")
      funL3()
      newString +=
        if (firstDigit == "9")
          " " + value[3]
        else
          value[3] + ")"
    }

    val funL5: () -> Unit = {
      Log.i ("doOnPasteText", "funL5()")
      funL4()
      newString +=
        if (firstDigit == "9")
          value[4]
        else
          " " + value[4]
    }

    val funL6: () -> Unit = {
      Log.i ("doOnPasteText", "funL6()")
      funL5()
      newString += value[5]
    }

    val funL7: () -> Unit = {
      Log.i ("doOnPasteText", "funL7()")
      funL6()
      newString +=
        if (firstDigit == "9")
          "-" + value[6]
        else
          value[6]
    }

    val funL8: () -> Unit = {
      Log.i ("doOnPasteText", "funL8()")
      funL7()
      newString +=
        if (firstDigit == "9")
          value[7]
        else
          "-" + value[7]
    }

    val funL9: () -> Unit = {
      Log.i ("doOnPasteText", "funL9()")
      funL8()
      newString +=
        if (firstDigit == "9")
          "-" + value[8]
        else
          value[8]
    }

    val funL10: () -> Unit = {
      Log.i ("doOnPasteText", "funL10()")
      funL9()
      newString +=
        if (firstDigit == "9")
          value[9]
        else
          "-" + value[9]
    }

    val funL11: () -> Unit = {
      Log.i ("doOnPasteText", "funL11()")
      funL10()
      newString += value[10]
    }

    if (value.length > LENGTH_OF_PHONE_NUMBER_WITHOUT_PLUS)
      value = value.substring(0, LENGTH_OF_PHONE_NUMBER_WITHOUT_PLUS)

    if (value.isNotEmpty() && (firstDigit == "7") || (firstDigit == "8") || (firstDigit == "9"))
    {       // это номер телефона
      when (value.length) {
        1 -> funL1()
        2 -> funL2()
        3 -> funL3()
        4 -> funL4()
        5 -> funL5()
        6 -> funL6()
        7 -> funL7()
        8 -> funL8()
        9 -> funL9()
        10 -> funL10()
        11 -> funL11()
      }
    }

    Log.i ("doOnPasteText", "newString = $newString")
    Log.i ("doOnPasteText", "!!!!!!!!!!!!!!!!!")
  }

  private fun addedSymbolIsDigit(s: String): Boolean {
    if (beforeText.length >= s.length) return false

    return s.isNotEmpty() && Character.isDigit(s[s.length - 1])
  }

  private fun symbolsWasDeleted(afterText: String): Boolean {
    return beforeText.length > afterText.length
  }

  private fun getNotDigitIndex(s: String): Int {
    if (s.isEmpty()) return -1

    for (i in s.indices) {
      if (!Character.isDigit(s[i]))
        return i
    }

    return -1
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
    needsToInsertFormatSymbol = false
    value = ""
    firstDigit = ""
    itWasDelete = false
    beforeText = ""
    newString = ""
  }

  private fun getValue(s: String): String {
    if (s.isEmpty()) return value

    if (firstDigit.isEmpty() || !Character.isDigit(firstDigit[0])) {
      firstDigit = if (s.length > 1 && s[0] == '+' && s[1] == '7') "7" else s[0].toString()
      if (!Character.isDigit(firstDigit[0])) {
        firstDigit = ""
      }
    }
    value = firstDigit

    val fdIndex = when {
      firstDigit == "8" -> s.indexOf("7")
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
}