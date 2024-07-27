package io.zenandroid.onlinego.ui.screens.newchallenge

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.button.MaterialButton
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.SeekGraphChallenge
import io.zenandroid.onlinego.data.repositories.PlayersRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.ui.screens.game.GameFragment
import io.zenandroid.onlinego.ui.views.ClickableMarkerView
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.formatMillis
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.timeControlDescription
import io.zenandroid.onlinego.R
import org.koin.core.context.GlobalContext.get

class ChallengeMarkerView(context: Context, onProfile: (Player) -> Unit, onAccept: (Long) -> Unit) : ClickableMarkerView(context, R.layout.challenge_markerview) {

    private val subscriptions = CompositeDisposable()
    private val containerView: LinearLayout = findViewById(R.id.containerView)
    private val rankTextView: TextView = findViewById(R.id.rankTextView)
    private val tpmTextView: TextView = findViewById(R.id.tpmTextView)
    private val userTextView: TextView = findViewById(R.id.userTextView)
    private val profileButton: MaterialButton = findViewById(R.id.profileButton)
    private val acceptButton: MaterialButton = findViewById(R.id.acceptButton)

    private val playersRepository = get().get<PlayersRepository>()
    private val restService = get().get<OGSRestService>()
    private val currentRating = get().get<UserSessionRepository>().uiConfig?.user?.ranking ?: 0

    lateinit var challenge: SeekGraphChallenge
        private set

    init {
        listOf(profileButton, acceptButton).forEach {
            it.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
                true
            }
        }
        profileButton.setOnClickListener {
            playersRepository.searchPlayers(this.challenge.username)
                .observeOn(Schedulers.single())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Handler(Looper.getMainLooper()).post {
                        it.firstOrNull()?.let { onProfile(it) }
                    }
                }, {
                    Log.e("ChallengeMarkerView", it.toString())
                })
                .addToDisposable(subscriptions)
        }
        acceptButton.setOnClickListener {
            restService.acceptOpenChallenge(this.challenge.challenge_id!!)
                .observeOn(Schedulers.single())
                .subscribe({
                    Handler(Looper.getMainLooper()).post {
                        onAccept(this.challenge.challenge_id!!)
                    }
                }, {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Not Eligible", Toast.LENGTH_SHORT).show()
                    }
                })
                .addToDisposable(subscriptions)
        }
    }

    override fun onClick(event: MotionEvent) = containerView.dispatchTouchEvent(event)

    // runs every time the MarkerView is redrawn
    override fun refreshContent(e: Entry, highlight: Highlight) {
        (e.data as? SeekGraphChallenge)?.let { challenge = it }
        challenge.let {
            val timePerMove = it.time_per_move?.toLong()?.times(1000)?.let(::formatMillis) ?: ""
            val params = it.time_control_parameters?.let(::timeControlDescription)
            val size = "${it.width}x${it.height}"
            val ranked = "${if (it.ranked) "R" else "Unr"}anked"
            val handicap = "${if (it.handicap == 0) "no" else it.handicap.toString()} handicap"
            val minRank = formatRank(it.min_rank).let { if (it == "") null else ">=$it" }
            val maxRank = formatRank(it.max_rank).let { if (it == "") null else "<=$it" }
            val ranks = when {
                minRank != null && maxRank != null -> "$minRank and $maxRank"
                minRank != null -> maxRank
                maxRank != null -> minRank
                else -> "any"
            }
            acceptButton.visibility = when {
                it.ranked && (e.y - currentRating) > 9 -> View.INVISIBLE
                it.min_rank > currentRating -> View.INVISIBLE
                it.max_rank < currentRating -> View.INVISIBLE
                else -> View.VISIBLE
            }
            rankTextView.text = "${it.username} [${formatRank(it.rank)}]"
            tpmTextView.text = "~$timePerMove / move"
            userTextView.text = "\"${it.name}\": $ranked $size, $handicap, $params\nRanks: $ranks"
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width.toFloat() / 2), -height.toFloat() - 10)
    }
}
