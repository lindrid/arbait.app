package `in`.arbait

import android.R
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.MutableLiveData

private const val TAG = "MonitoringEditText"

/**
 * An EditText, which notifies when something was cut/copied/pasted inside it.
 * @author Lukas Knuth
 * @version 1.0
 */
class MonitoringEditText : AppCompatEditText {
  private val context2: Context

  var textWasPasted: MutableLiveData<Boolean> = MutableLiveData(false)
    private set

  fun resetLiveData() {
    textWasPasted.value = false
  }

  /*
   * Just the constructors to create a new EditText...
   */

  constructor(context: Context) : super(context) {
    this.context2 = context
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    this.context2 = context
  }

  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  ) {
    this.context2 = context
  }

  /**
   *
   * This is where the "magic" happens.
   *
   * The menu used to cut/copy/paste is a normal ContextMenu, which allows us to
   * overwrite the consuming method and react on the different events.
   * @see [Original Implementation](http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3_r1/android/widget/TextView.java.TextView.onTextContextMenuItem%28int%29)
   */
  override fun onTextContextMenuItem(id: Int): Boolean {
    // Do your thing:
    val consumed = super.onTextContextMenuItem(id)
    Log.i (TAG, "$consumed")
    when (id) {
      R.id.cut -> onTextCut()
      R.id.paste -> onTextPaste()
      R.id.copy -> onTextCopy()
    }
    return consumed
  }

  /**
   * Text was cut from this EditText.
   */
  private fun onTextCut() {
    //Toast.makeText(context2, "Cut!", Toast.LENGTH_SHORT).show()
  }

  /**
   * Text was copied from this EditText.
   */
  private fun onTextCopy() {
    //Toast.makeText(context2, "Copy!", Toast.LENGTH_SHORT).show()
  }

  /**
   * Text was pasted into the EditText.
   */
  private fun onTextPaste() {
    Log.i (TAG, "TEXT WAS PASTED")
    textWasPasted.value = true
  }
}