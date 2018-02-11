package io.zenandroid.onlinego.extensions

/**
 * Created by alex on 21/11/2017.
 */
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewPropertyAnimator
import android.view.animation.OvershootInterpolator
import io.zenandroid.onlinego.R

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

fun View.fadeInAndSlideUp(offset: Float): ViewPropertyAnimator {
    visibility = View.VISIBLE
    alpha = 0f
    translationY = offset
    return animate()
            .alpha(1f)
            .translationY(0f)
            .setInterpolator(OvershootInterpolator())
}

fun View.fadeOutAndSlideDown(offset: Float): ViewPropertyAnimator {
    return animate().alpha(0f).translationY(offset).withEndAction {
        visibility = View.GONE
    }
}