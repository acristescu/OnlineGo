package io.zenandroid.onlinego.views

import android.content.Context
import android.support.text.emoji.widget.EmojiAppCompatTextView
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.ogs.Player
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import java.lang.Math.*
import java.util.regex.Pattern


/**
 * Created by alex on 17/11/2017.
 */
class PlayerDetailsView : FrameLayout {

    private val unbinder: Unbinder
    private val gravatarRegex = Pattern.compile("(.*gravatar.com\\/avatar\\/[0-9a-fA-F]*+).*")
    private val rackcdnRegex = Pattern.compile("(.*rackcdn.com.*)-\\d*\\.png")

    @BindView(R.id.name) lateinit var nameView: TextView
    @BindView(R.id.rank) lateinit var rankView: TextView
    @BindView(R.id.icon) lateinit var iconView: AppCompatImageView
    @BindView(R.id.flag) lateinit var flagView: EmojiAppCompatTextView
    @BindView(R.id.score) lateinit var scoreView: TextView
    @BindView(R.id.timerFirstLine) lateinit var timerFirstLineView: TextView
    @BindView(R.id.timerSecondLine) lateinit var timerSecondLineView: TextView
    @BindView(R.id.colorIndicatorBlack) lateinit var colorIndicatorBlack: AppCompatImageView
    @BindView(R.id.colorIndicatorWhite) lateinit var colorIndicatorWhite: AppCompatImageView
    @BindView(R.id.clockIcon) lateinit var clockIcon: AppCompatImageView

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    var player: Player? = null
        set(value) {
            nameView.text = value?.username
            rankView.text = formatRank(egfToRank(value?.ratings?.overall?.rating ?: value?.egf))
            value?.country?.let {
                flagView.text = convertCountryCodeToEmojiFlag(it)
            }
            value?.icon?.let {
                Glide.with(this)
                        .load(processGravatarURL(it))
                        .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                        .apply(RequestOptions().circleCrop())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(iconView)
            }
        }

    var nextToMove: Boolean = false
        set(value) {
            clockIcon.showIf(value)
            if(value && !field) {
                clockIcon.scaleX = .75f
                clockIcon.scaleY = .75f
                clockIcon.alpha = 0f
                clockIcon.animate().scaleX(1f).scaleY(1f).alpha(1f).interpolator = OvershootInterpolator(10f)
            }
            field = value
        }

    var timerFirstLine: String? = null
        set(value) {
            timerFirstLineView.text = value
        }

    var timerSecondLine: String? = null
        set(value) {
            timerSecondLineView.text = value
            timerSecondLineView.showIf(value?.isNotEmpty() ?: false)
        }

    var captured: Int? = null
        set(value) {
            if(value != null && value != 0) {
                scoreView.text = "$value capture${if (value > 1) "s" else ""}"
            }
        }

    var color: StoneType = StoneType.BLACK
        set(value) {
            field = value
            colorIndicatorBlack.showIf(value == StoneType.BLACK)
            colorIndicatorWhite.showIf(value == StoneType.WHITE)
        }

    private fun processGravatarURL(url: String): String {
        var matcher = gravatarRegex.matcher(url)
        if(matcher.matches()) {
            return "${matcher.group(1)}?s=${iconView.width}&d=404"
        }

        matcher = rackcdnRegex.matcher(url)
        if(matcher.matches()) {
            val desired = max(512.0, pow(2.0, round(log(iconView.width.toDouble()) / log(2.0)).toDouble())).toInt()
            return "${matcher.group(1)}-${desired}.png"
        }
        return url
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val view = View.inflate(context, R.layout.view_player_details, this)
        unbinder = ButterKnife.bind(view)
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        unbinder.unbind()
    }
}