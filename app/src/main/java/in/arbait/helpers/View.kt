package `in`.arbait.helpers

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

fun setLayoutParams(view: View, layoutParams: Map<String, Int>)
{
  view.layoutParams = getLayoutParams(view, layoutParams);
}

fun setVisibilityToViews(views: List<View>, visibility: Int)
{
  views.forEach {
    it.visibility = visibility
  }
}

private fun getLayoutParams(view: View, setLayoutParams: Map<String, Int>): ConstraintLayout.LayoutParams
{
  val lp = view.layoutParams as ConstraintLayout.LayoutParams
  for ((paramName, viewId) in setLayoutParams) {
    when (paramName) {
      "topToTop" -> lp.topToTop = viewId
      "topToBottom" -> lp.topToBottom = viewId
      "bottomToTop" -> lp.bottomToTop = viewId
    }
  }
  return lp;
}
