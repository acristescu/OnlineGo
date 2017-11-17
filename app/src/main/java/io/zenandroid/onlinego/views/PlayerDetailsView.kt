package io.zenandroid.onlinego.views

import android.content.Context
import android.support.text.emoji.widget.EmojiAppCompatTextView
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.ogs.Player
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank


/**
 * Created by alex on 17/11/2017.
 */
class PlayerDetailsView : FrameLayout {

    private val unbinder: Unbinder

    @BindView(R.id.name) lateinit var nameView: TextView
    @BindView(R.id.rank) lateinit var rankView: TextView


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    var player: Player? = null
        set(value) {
            nameView.text = value?.username
            rankView.text = formatRank(egfToRank(value?.egf))
        }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val view = View.inflate(context, R.layout.view_player_details, this)
        unbinder = ButterKnife.bind(view)
        val gb = convertCountryCodeToEmojiFlag("gb")
        view.findViewById<EmojiAppCompatTextView>(R.id.flag).text = gb
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        unbinder.unbind()
    }
}