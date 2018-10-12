package io.zenandroid.onlinego.extensions

/**
 * Created by alex on 21/11/2017.
 */
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.os.Build
import android.support.design.internal.BottomNavigationItemView
import android.support.design.internal.BottomNavigationMenuView
import android.support.design.widget.BottomNavigationView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import io.reactivex.Completable





fun View.showIf(show: Boolean) {
    if (show) {
        show()
    } else {
        hide()
    }
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.circularReveal(backgroundColor: Int) {
    val showAndSetBackgroundColorFunction = {
        this.setBackgroundColor(backgroundColor)
        this.visibility = View.VISIBLE
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.post {
            val cx = this.width / 2
            val cy = this.height / 2
            val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()

            try {
                val animator = ViewAnimationUtils.createCircularReveal(this, cx, cy, 0f, finalRadius)
                animator.startDelay = 50
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        showAndSetBackgroundColorFunction.invoke()
                    }
                })
                animator.start()
            } catch(e: Exception) {
                Log.e("ViewExtensions", "Unable to perform circular reveal", e )
            }
        }
    } else {
        showAndSetBackgroundColorFunction.invoke()
    }
}

fun View.slideIn(offset: Float, duration: Long = 300L, overshootTension: Float = 2f): Completable {
    return Completable.create {
        visibility = View.VISIBLE
        alpha = 0f
        scaleX = 0f
        scaleY = 0f
        translationY = offset
        animate().alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .setInterpolator(OvershootInterpolator(overshootTension))
                .withEndAction(it::onComplete)
    }
}

fun View.slideOut(offset: Float): Completable {
    return Completable.create {
        animate().alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .translationY(offset)
                .withEndAction {
                    visibility = View.GONE
                    it.onComplete()
                }
    }
}

fun View.fadeOut(duration: Long = 30): Completable {
    return Completable.create {
        animate().setDuration(duration)
                .alpha(0f)
                .withEndAction {
                    visibility = View.GONE
                    it.onComplete()
                }
    }
}

fun View.fadeIn(): Completable {
    return Completable.create {
        visibility = View.VISIBLE
        alpha = 0f
        animate().alpha(1f)
                .setDuration(200)
                .withEndAction(it::onComplete)
    }
}

fun View.rotate(degree: Float): Completable {
    return Completable.create { animate().rotation(degree).withEndAction(it::onComplete) }
}

@SuppressLint("RestrictedApi")
fun BottomNavigationView.disableShiftMode() {
    val menuView = getChildAt(0) as BottomNavigationMenuView
    try {
        val shiftingMode = menuView.javaClass.getDeclaredField("mShiftingMode")
        shiftingMode.isAccessible = true
        shiftingMode.setBoolean(menuView, false)
        shiftingMode.isAccessible = false
        for (i in 0 until menuView.childCount) {
            val item = menuView.getChildAt(i) as BottomNavigationItemView
            item.setShiftingMode(false)
            // set once again checked value, so view will be updated
            item.setChecked(item.itemData.isChecked)
        }
    } catch (e: NoSuchFieldException) {
        //Timber.e(e, "Unable to get shift mode field");
    } catch (e: IllegalAccessException) {
        //Timber.e(e, "Unable to change value of shift mode");
    }

}

fun View.setMargins(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
        it.setMargins(
                left ?: it.leftMargin,
                top ?: it.topMargin,
                right ?: it.rightMargin,
                bottom ?: it.bottomMargin
        )
        requestLayout()
    }
}

fun View.setMarginsDP(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    val density = resources.displayMetrics.density
    setMargins(
            if(left != null) (left * density + 0.5f).toInt() else null,
            if(top != null) (top * density + 0.5f).toInt() else null,
            if(right != null) (right * density + 0.5f).toInt() else null,
            if(bottom != null) (bottom * density + 0.5f).toInt() else null
    )
}

fun EditText.onChange(cb: (String) -> Unit) {
    this.addTextChangedListener(object: TextWatcher {
        override fun afterTextChanged(s: Editable?) { cb(s.toString()) }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
