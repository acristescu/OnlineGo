package io.zenandroid.onlinego.ui.screens.newchallenge

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.reactivex.Completable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.*
import kotlinx.android.synthetic.main.view_new_challenge.view.*


/**
 * Created by alex on 22/02/2018.
 */
class NewChallengeView : FrameLayout {

    private var fabMiniSize: Float = 0f
    var subMenuVisible = false
        private set

    private val analytics = OnlineGoApplication.instance.analytics

    var onAutomatchClicked: (() -> Unit)? = null
    var onOnlineCustomClicked: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val view = View.inflate(context, R.layout.view_new_challenge, this)

        fab.setOnClickListener {
            analytics.logEvent("new_game_fab_clicked", null)
            toggleSubMenu()
        }
        onlineFab.setOnClickListener {
            analytics.logEvent("automatch_fab_clicked", null)
            toggleSubMenu()
            onAutomatchClicked?.invoke()
        }
        customFab.setOnClickListener {
            analytics.logEvent("custom_fab_clicked", null)
            toggleSubMenu()
            onOnlineCustomClicked?.invoke()
        }
        fabMiniSize = resources.getDimension(R.dimen.fab_mini_with_margin)
    }

    fun toggleSubMenu() {
        subMenuVisible = !subMenuVisible
        if(subMenuVisible) {
            showSubMenu().subscribe()
        } else {
            hideSubMenu().subscribe()
        }
        scrim.showIf(subMenuVisible)
    }

    fun showSubMenu() = Completable.mergeArray(
            fab.rotate(45f),
            onlineFab.slideIn(fabMiniSize).andThen(onlineLabel.fadeIn()),
            customFab.slideIn(2 * fabMiniSize).andThen(customLabel.fadeIn())
    )

    fun hideSubMenu() = Completable.mergeArray(
            fab.rotate(0f),
            onlineLabel.fadeOut().andThen(onlineFab.slideOut(fabMiniSize)),
            customLabel.fadeOut().andThen(customFab.slideOut(2 * fabMiniSize))
    )

    fun hideFab(): Completable =
            fab.slideOut(0f)

    fun showFab() : Completable =
        fab.slideIn(0f, 700, 3f)

}