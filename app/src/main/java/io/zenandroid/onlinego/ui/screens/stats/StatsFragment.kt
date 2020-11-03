package io.zenandroid.onlinego.ui.screens.stats

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.scale
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.EntryXComparator
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL
import kotlinx.android.synthetic.main.fragment_stats.*
import kotlinx.android.synthetic.main.view_player_profile.*
import org.koin.android.ext.android.get
import java.text.SimpleDateFormat
import java.util.*

private const val PLAYER_ID = "PLAYER_ID"

@SuppressLint("SetTextI18n")
class StatsFragment : Fragment(R.layout.fragment_stats), StatsContract.View {

    companion object {
        fun createFragment(id: Long): StatsFragment = StatsFragment().apply {
            arguments = bundleOf(PLAYER_ID to id)
        }
    }
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    private lateinit var presenter: StatsContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics
    override var title: String? = null
        set(value) {
            (requireActivity() as MainActivity).mainTitle = value
            field = value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = StatsPresenter(this, analytics, get(), arguments!!.getLong(PLAYER_ID))
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(requireActivity(), javaClass.simpleName, javaClass.simpleName)
        presenter.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun fillPlayerDetails(playerDetails: OGSPlayer ) {
        nameView.text = playerDetails.username
        playerDetails.country?.let {
            flagView.text = convertCountryCodeToEmojiFlag(it)
        }
        playerDetails.icon?.let {
            Glide.with(this)
                    .load(processGravatarURL(it, iconView.width))
                    .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                    .into(iconView)
        }

        val glicko = playerDetails.ratings?.overall?.rating?.toInt()
        val deviation = playerDetails.ratings?.overall?.deviation?.toInt()
        rankView.text = formatRank(egfToRank(playerDetails.ratings?.overall?.rating))
        glickoView.text = "$glicko ± $deviation"
    }

    override fun mostFacedOpponent(playerDetails: OGSPlayer, total: Int, won: Int) {
        mostFacedOpponentView.text = playerDetails.username
        opponentRankView.text = "(${formatRank(egfToRank(playerDetails.ratings?.overall?.rating))})"
        playerDetails.icon?.let {
            Glide.with(this)
                    .load(processGravatarURL(it, opponentIconView.width))
                    .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                    .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                    .into(opponentIconView)
        }
        mostFacedOpponentDetailsView.text = "$total games ($won wins, ${total - won} losses)"
    }

    override fun fillHighestWin(playerDetails: OGSPlayer, winningGame: Glicko2HistoryItem) {
        val rank = formatRank(egfToRank(playerDetails.ratings?.overall?.rating))

        highestWinOpponent.text = playerDetails.username
        highestWinRank.text = "($rank)"
        playerDetails.icon?.let {
            Glide.with(this)
                    .load(processGravatarURL(it, highestWinIconView.width))
                    .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                    .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                    .into(highestWinIconView)
        }
        highestWinDetails.text = dateFormat.format(Date(winningGame.ended * 1000))
    }

    override fun fillHighestRank(highestRank: Float, highestRankTimestamp: Long) {
        highestRankView.text = formatRank(egfToRank(highestRank.toDouble()))
        highestRankDateView.text = dateFormat.format(Date(highestRankTimestamp * 1000))
    }


    override fun fillRankGraph(entries: List<Entry>) {
        if (entries.isNotEmpty()) {
            Collections.sort(entries, EntryXComparator())
            val rankDataSet = LineDataSet(entries, "Games").apply {
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 1.5f
                color = ResourcesCompat.getColor(resources, R.color.rankGraphLine, context?.theme)
                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            }
            rankGraph.data = LineData(rankDataSet)
        }
        rankGraph.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = DayAxisValueFormatter(this)
            xAxis.setLabelCount(6, true)
            xAxis.textColor = ResourcesCompat.getColor(resources, R.color.colorText, context?.theme)
            xAxis.setDrawGridLines(false)

            axisLeft.setDrawGridLines(false)
            axisLeft.textColor = ResourcesCompat.getColor(resources, R.color.colorText, context?.theme)
            axisRight.setDrawGridLines(false)
            axisRight.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return formatRank(egfToRank(value.toDouble()))
                }
            }

            axisRight.textColor = ResourcesCompat.getColor(resources, R.color.colorText, context?.theme)
            description.isEnabled = false
            setDrawMarkers(false)
            setNoDataText("No ranked games on record")
            setNoDataTextColor(ResourcesCompat.getColor(resources,R.color.colorActionableText, context?.theme))
            legend.isEnabled = false
            isDoubleTapToZoomEnabled = false

            invalidate()
        }
    }

    override fun fillOutcomePieChart(lostCount: Int, wonCount: Int) {
        if (wonCount + lostCount != 0) {
            val lostEntry = PieEntry(lostCount.toFloat(), "Lost")
            val wonEntry = PieEntry(wonCount.toFloat(), "Won")
            val dataSet = PieDataSet(listOf(lostEntry, wonEntry), "Games").apply {
                setColors(intArrayOf(R.color.chartLost, R.color.chartWon), context)
                setDrawValues(false)
            }

            chart.data = PieData(dataSet)

            val gamesWon = wonEntry.value.toInt()
            val gamesLost = lostEntry.value.toInt()
            val gamesWonPercent = (gamesWon * 100 / (gamesWon + gamesLost).toFloat())
            val gamesLostPercent = 100 - gamesWonPercent

            val gamesWonString = String.format("%.1f", gamesWonPercent)
            val gamesLostString = String.format("%.1f", gamesLostPercent)

            val wonColor = ResourcesCompat.getColor(resources, R.color.chartWon, context?.theme)
            val lostColor = ResourcesCompat.getColor(resources, R.color.chartLost, context?.theme)

            val centerText = SpannableStringBuilder()
                    .color(wonColor) {
                        scale(1.8f) { bold { append(gamesWon.toString()) } }
                        append(" $gamesWonString%")
                    }.append('\n')
                    .color(lostColor) {
                        scale(1.8f) { bold { append(gamesLost.toString()) } }
                        append(" $gamesLostString%")
                    }

            chart.centerText = centerText
        }
        chart.apply {
            offsetLeftAndRight(0)
            setTransparentCircleAlpha(0)
            holeRadius = 60f
            setHoleColor(ResourcesCompat.getColor(resources, R.color.colorTextBackground, context?.theme))
            setTouchEnabled(false)
            legend.isEnabled = false
            description.isEnabled = false
            setDrawEntryLabels(false)
            setDrawMarkers(false)
            setNoDataText("No ranked games on record")
            setNoDataTextColor(ResourcesCompat.getColor(resources, R.color.colorActionableText, context?.theme))

            chart.invalidate()
        }
    }

    override fun fillLongestStreak(length: Int, start: Long, end: Long) {
        val startDate = dateFormat.format(Date(start * 1000))
        val endDate = dateFormat.format(Date(end * 1000))
        winningStreak.text = "$length game(s)"
        winningStreakDetails.text = "$startDate to $endDate"
    }

    override fun fillCurrentForm(lastGames: List<Glicko2HistoryItem>) {
        val currentFormSpan = SpannableStringBuilder()
        val wonColor = ResourcesCompat.getColor(resources, R.color.chartWon, context?.theme)
        val lostColor = ResourcesCompat.getColor(resources, R.color.chartLost, context?.theme)
        lastGames.forEach {
            if (it.won) {
                currentFormSpan.color(wonColor) { append("W") }
            } else {
                currentFormSpan.color(lostColor) { append("L") }
            }
        }
        currentForm.text = currentFormSpan
    }
}

class DayAxisValueFormatter (private val chart: BarLineChartBase<*>) : ValueFormatter() {

    private val yearFormatter = SimpleDateFormat("yyyy", Locale.US)
    private val monthFormatter = SimpleDateFormat("MMM''yy", Locale.US)
    private val dayFormatter = SimpleDateFormat("dd MMM", Locale.US)

    override fun getFormattedValue(hoursSinceEpoch: Float): String? {
        return when {
            chart.visibleXRange > 51840 -> { // 6 years
                yearFormatter.format(Date(hoursSinceEpoch.toLong() *60*60*1000)).toString()
            }
            chart.visibleXRange > 4320 -> { // 6 months
                monthFormatter.format(Date(hoursSinceEpoch.toLong() *60*60*1000)).toString()
            }
            else -> {
                dayFormatter.format(Date(hoursSinceEpoch.toLong() *60*60*1000)).toString()
            }
        }
    }
}