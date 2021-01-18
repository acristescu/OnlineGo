package io.zenandroid.onlinego.ui.screens.newchallenge

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import io.reactivex.Completable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.databinding.ViewNewChallengeBinding
import io.zenandroid.onlinego.utils.*


/**
 * Created by alex on 22/02/2018.
 */
class NewChallengeView : FrameLayout {

    private var fabMiniSize: Float = 0f
    var subMenuVisible = false
        private set

    private val analytics = OnlineGoApplication.instance.analytics

    private lateinit var binding: ViewNewChallengeBinding

    var onAutomatchClicked: (() -> Unit)? = null
    var onOnlineCustomClicked: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewNewChallengeBinding.inflate(inflater, this, true)

        binding.fab.setOnClickListener {
            analytics.logEvent("new_game_fab_clicked", null)
            toggleSubMenu()
        }
        binding.onlineFab.setOnClickListener {
            analytics.logEvent("automatch_fab_clicked", null)
            toggleSubMenu()
            onAutomatchClicked?.invoke()
        }
        binding.customFab.setOnClickListener {
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
        binding.scrim.showIf(subMenuVisible)
    }

    fun showSubMenu() = Completable.mergeArray(
            binding.fab.rotate(45f),
            binding.onlineFab.slideIn(fabMiniSize).andThen(binding.onlineLabel.fadeIn()),
            binding.customFab.slideIn(2 * fabMiniSize).andThen(binding.customLabel.fadeIn())
    )

    fun hideSubMenu() = Completable.mergeArray(
            binding.fab.rotate(0f),
            binding.onlineLabel.fadeOut().andThen(binding.onlineFab.slideOut(fabMiniSize)),
            binding.customLabel.fadeOut().andThen(binding.customFab.slideOut(2 * fabMiniSize))
    )

    fun hideFab(): Completable =
            binding.fab.slideOut(0f)

    fun showFab() : Completable =
            binding.fab.slideIn(0f, 700, 3f)

}