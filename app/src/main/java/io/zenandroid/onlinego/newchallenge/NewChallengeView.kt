package io.zenandroid.onlinego.newchallenge

import android.app.ProgressDialog
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import io.reactivex.Completable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.*
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.ogs.Size
import io.zenandroid.onlinego.ogs.Speed
import kotlinx.android.synthetic.main.view_new_challenge.view.*


/**
 * Created by alex on 22/02/2018.
 */
class NewChallengeView : FrameLayout, NewChallengeContract.View {

    private lateinit var presenter: NewChallengeContract.Presenter
    private var fabMiniSize: Float = 0f
    private lateinit var searchDialog: ProgressDialog

    private val analytics = OnlineGoApplication.instance.analytics

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val view = View.inflate(context, R.layout.view_new_challenge, this)

        fab.setOnClickListener { onFabClicked() }
        arrayOf(blitzFab, longFab, normalFab).forEach { it.setOnClickListener { onSpeedClicked(it) } }
        arrayOf(smallFab, mediumFab, largeFab).forEach { it.setOnClickListener { onSizeClicked(it) } }
        fabMiniSize = resources.getDimension(R.dimen.fab_mini_with_margin)
        presenter = NewChallengePresenter(this, analytics)
    }

    fun onFabClicked() {
        NewChallengeBottomSheet(context) { speed: Speed, sizes: List<Size> ->
            presenter.onStartSearch(sizes, speed)
        }.show()
    }

    override fun setFadeOutState(fadedOut: Boolean) {
        scrim.showIf(fadedOut)
    }

    override fun showSpeedMenu() = Completable.mergeArray(
            fab.rotate(45f),
            longFab.slideIn(fabMiniSize).andThen(longLabel.fadeIn()),
            normalFab.slideIn(2 * fabMiniSize).andThen(normalLabel.fadeIn()),
            blitzFab.slideIn(3 * fabMiniSize).andThen(blitzLabel.fadeIn())
    )

    override fun showSizeMenu() = Completable.mergeArray(
            fab.rotate(45f),
            largeFab.slideIn(fabMiniSize).andThen(largeLabel.fadeIn()),
            mediumFab.slideIn(2 * fabMiniSize).andThen(mediumLabel.fadeIn()),
            smallFab.slideIn(3 * fabMiniSize).andThen(smallLabel.fadeIn())
    )

    override fun hideSpeedMenu() = Completable.mergeArray(
            fab.rotate(0f),
            longLabel.fadeOut().andThen(longFab.slideOut(fabMiniSize)),
            normalLabel.fadeOut().andThen(normalFab.slideOut(2 * fabMiniSize)),
            blitzLabel.fadeOut().andThen(blitzFab.slideOut(3 * fabMiniSize))
    )

    override fun hideSizeMenu() = Completable.mergeArray(
            fab.rotate(0f),
            largeLabel.fadeOut().andThen(largeFab.slideOut(fabMiniSize)),
            mediumLabel.fadeOut().andThen(mediumFab.slideOut(2 * fabMiniSize)),
            smallLabel.fadeOut().andThen(smallFab.slideOut(3 * fabMiniSize))
    )

    private fun onSpeedClicked(view : View) {
        when(view) {
            blitzFab -> presenter.onSpeedSelected(Speed.BLITZ)
            normalFab -> presenter.onSpeedSelected(Speed.NORMAL)
            longFab -> presenter.onSpeedSelected(Speed.LONG)
            else -> Log.e("NewChallengeView", "Illegal state")
        }
    }

    fun onSizeClicked(view : View) {
        when(view) {
            smallFab -> presenter.onSizeSelected(Size.SMALL)
            mediumFab -> presenter.onSizeSelected(Size.MEDIUM)
            largeFab -> presenter.onSizeSelected(Size.LARGE)
            else -> Log.e("NewChallengeView", "Illegal state")
        }
    }

    override fun hideFab(): Completable =
            fab.slideOut(0f)

    override fun showSearchDialog() {
        searchDialog = ProgressDialog.show(context, "Searching", null, true, true) {
            presenter.onDialogCancelled()
        }
    }

    override fun navigateToGame(gameId: Long) {
        (context as? MainActivity)?.navigateToGameScreenById(gameId)
    }

    override fun cancelDialog() {
        searchDialog.cancel()
    }

    override fun updateDialogText(message: CharSequence) {
        searchDialog.setMessage(message)
    }

    override fun showFab() : Completable =
        fab.slideIn(0f, 700, 3f)

}