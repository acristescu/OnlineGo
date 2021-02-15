package io.zenandroid.onlinego.ui.views

import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.ColorRes
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.databinding.ViewPlayerDetailsBinding
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL
import org.koin.core.context.GlobalContext.get


/**
 * Created by alex on 17/11/2017.
 */
class PlayerDetailsView : FrameLayout {

    private val settingsRepository: SettingsRepository = get().get()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    private lateinit var binding: ViewPlayerDetailsBinding

    var onUserClickedListener: (() -> Unit)? = null

    var player: Player? = null
        set(value) {
            if(field == value) {
                return
            }
            binding.nameView.text = value?.username
            binding.rankView.text = if (settingsRepository.showRanks) formatRank(egfToRank(value?.rating)) else ""
            value?.country?.let {
                binding.flagView.text = convertCountryCodeToEmojiFlag(it)
            }
            value?.icon?.let {
                Glide.with(this)
                        .load(processGravatarURL(it, binding.iconView.width))
                        .transition(withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                        .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                        .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                        .into(binding.iconView)
            }
            field = value
        }

    fun setStatus(text: String?, @ColorRes color: Int = R.color.colorAccent) {
        text?.let {
            binding.theirTurnLabel.text = text
            ViewCompat.setBackgroundTintList(binding.theirTurnLabel, ColorStateList.valueOf(resources.getColor(color)))
            binding.theirTurnLabel.animate().alpha(1f)
        } ?: binding.theirTurnLabel.animate().alpha(0f)

    }

    var passed: Boolean = false
        set(value) {
            if(field == value) {
                return
            }
            if(value) {
                binding.passedLabel.animate().alpha(1f)
            } else {
                binding.passedLabel.animate().alpha(0f)
            }
            field = value
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        binding.iconContainer.radius = binding.iconContainer.width / 2f
    }

    var timerFirstLine: String? = null
        set(value) {
            binding.timerFirstLineView.text = value
            binding.timerFirstLineView.showIf(value?.isNotEmpty() ?: false)
            binding.timerFirstLineLabelView.showIf(binding.timerFirstLineView.visibility == View.VISIBLE)
        }

    var timerSecondLine: String? = null
        set(value) {
            binding.timerSecondLineView.text = value
            binding.timerSecondLineView.showIf(value?.isNotEmpty() ?: false)
            binding.timerSecondLineLabelView.showIf(binding.timerSecondLineView.visibility == View.VISIBLE)
        }

    var score: Float = 0f
        set(value) {
            field = value
            binding.scoreView.text = score.toString()
        }

    var color: StoneType = StoneType.BLACK
        set(value) {
            field = value
            binding.colorIndicatorBlack.showIf(value == StoneType.BLACK)
            binding.colorIndicatorWhite.showIf(value == StoneType.WHITE)
        }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewPlayerDetailsBinding.inflate(inflater, this, true)

        binding.nameView.setOnClickListener { onUserClickedListener?.invoke() }
        binding.iconView.setOnClickListener { onUserClickedListener?.invoke() }
    }

}