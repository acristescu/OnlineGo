package io.zenandroid.onlinego.newchallenge

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.reactivex.Completable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.*
import kotlinx.android.synthetic.main.view_new_challenge.view.*


/**
 * Created by alex on 22/02/2018.
 */
class NewChallengeView : FrameLayout {

    private var fabMiniSize: Float = 0f
    var subMenuVisible = false
        private set

    private val analytics = OnlineGoApplication.instance.analytics

    public var onAutomatchClicked: (() -> Unit)? = null
    public var onOnlineBotClicked: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val view = View.inflate(context, R.layout.view_new_challenge, this)

        fab.setOnClickListener { toggleSubMenu() }
        onlineFab.setOnClickListener {
            toggleSubMenu()
            onAutomatchClicked?.invoke()
        }
        botFab.setOnClickListener {
            toggleSubMenu()
            onOnlineBotClicked?.invoke()
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
            botFab.slideIn(2 * fabMiniSize).andThen(botLabel.fadeIn())
    )

    fun hideSubMenu() = Completable.mergeArray(
            fab.rotate(0f),
            onlineLabel.fadeOut().andThen(onlineFab.slideOut(fabMiniSize)),
            botLabel.fadeOut().andThen(botFab.slideOut(2 * fabMiniSize))
    )

    fun hideFab(): Completable =
            fab.slideOut(0f)

    fun showFab() : Completable =
        fab.slideIn(0f, 700, 3f)

}