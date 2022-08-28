package io.zenandroid.onlinego.ui.screens.stats

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener.*
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.EntryXComparator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.databinding.FragmentStatsBinding
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL
import org.koin.android.ext.android.get
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

const val PLAYER_ID = "PLAYER_ID"

@SuppressLint("SetTextI18n")
class StatsFragment : Fragment(), StatsContract.View {

    companion object {
        fun createFragment(id: Long): StatsFragment = StatsFragment().apply {
            arguments = bundleOf(PLAYER_ID to id)
        }
    }
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val shortFormat = DateTimeFormatter.ofPattern("d MMM uuuu")
    private lateinit var binding: FragmentStatsBinding

    private lateinit var presenter: StatsContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val playerId = arguments?.getLong(PLAYER_ID) ?: Util.getCurrentUserId()!!
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    presenter.currentFilter = StatsContract.Filter.values()[it.position]
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })
        presenter = StatsPresenter(this, analytics, get(), get(), playerId)
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
        binding.playerProfile.apply {
            nameView.text = playerDetails.username
            playerDetails.country?.let {
                flagView.text = convertCountryCodeToEmojiFlag(it)
            }
            playerDetails.icon?.let {
                Glide.with(this@StatsFragment)
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
    }

    override fun mostFacedOpponent(playerDetails: OGSPlayer, total: Int, won: Int) {
        binding.apply {
            mostFacedOpponentView.text = playerDetails.username
            opponentRankView.text = "(${formatRank(egfToRank(playerDetails.ratings?.overall?.rating))})"
            playerDetails.icon?.let {
                Glide.with(this@StatsFragment)
                        .load(processGravatarURL(it, opponentIconView.width))
                        .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                        .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                        .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                        .into(opponentIconView)
            }
            mostFacedOpponentDetailsView.text = "$total games ($won wins, ${total - won} losses)"
        }
    }

    override fun fillHighestWin(playerDetails: OGSPlayer, winningGame: Glicko2HistoryItem) {
        binding.apply {
            val rank = formatRank(egfToRank(playerDetails.ratings?.overall?.rating))

            highestWinOpponent.text = playerDetails.username
            highestWinRank.text = "($rank)"
            playerDetails.icon?.let {
                Glide.with(this@StatsFragment)
                        .load(processGravatarURL(it, highestWinIconView.width))
                        .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                        .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                        .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                        .into(highestWinIconView)
            }
            highestWinDetails.text = dateFormat.format(Date(winningGame.ended * 1000))
        }
    }

    override fun fillHighestRank(highestRank: Float, highestRankTimestamp: Long) {
        binding.playerProfile.highestRankView.text = formatRank(egfToRank(highestRank.toDouble()))
        binding.playerProfile.highestRankDateView.text = dateFormat.format(Date(highestRankTimestamp * 1000))
    }


    override fun fillRankGraph(entries: List<Entry>) {
        if (entries.isNotEmpty()) {
            Collections.sort(entries, EntryXComparator())
            val rankDataSet = LineDataSet(entries, "Games").apply {
                setDrawIcons(false)
                lineWidth = 1.3f
                highLightColor = Color.GRAY
                highlightLineWidth = 0.7f
                enableDashedHighlightLine(7f, 2f, 0f)
                setDrawCircles(false)
                setDrawValues(false)
                color = ResourcesCompat.getColor(resources, R.color.rankGraphLine, context?.theme)
                mode = LineDataSet.Mode.LINEAR
                setDrawFilled(true)
                fillDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, arrayOf(ResourcesCompat.getColor(resources, R.color.color_type_info, context?.theme), Color.TRANSPARENT).toIntArray())
            }
            binding.rankGraph.data = LineData(rankDataSet)
        }
        binding.rankGraph.apply {
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = DayAxisValueFormatter(binding.rankGraph)
                setDrawAxisLine(false)
                setDrawLabels(false)
                textColor = ResourcesCompat.getColor(resources, R.color.colorText, context?.theme)
                setDrawGridLines(false)
            }

            axisLeft.apply {
                setDrawGridLines(false)
                textColor = ResourcesCompat.getColor(resources, R.color.colorText, context?.theme)
                setDrawLabels(false)
                setDrawAxisLine(false)
            }

            axisRight.apply {
                setDrawGridLines(false)
                setDrawLabels(false)
                setDrawAxisLine(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return formatRank(egfToRank(value.toDouble()))
                    }
                }
            }

            val onChartValueSelectedListener = object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if(e == null) {
                        onNothingSelected()
                    } else {
                        binding.chartDetails.text = SpannableStringBuilder()
                            .bold { append("${e?.y?.toInt()} ELO (${formatRank(egfToRank(e.y.toDouble()), true)})") }
                            .append(" on ${formatDate(e.x.toLong())}")
                    }
                }

                override fun onNothingSelected() {
                    if(entries.isNotEmpty()) {
                        val delta = (entries.last().y - entries.first().y).toInt()
                        val color = ResourcesCompat.getColor(resources, if(delta < 0) R.color.chartLost else R.color.chartWon, context?.theme)
                        val arrow = if(delta < 0 ) "⬇" else "⬆"
                        binding.chartDetails.text = SpannableStringBuilder()
                            .color(color) {
                                bold { append("$arrow ${abs(entries.last().y - entries.first().y).toInt()} ELO")}
                            }
                            .append(" since ${formatDate(entries.first().x.toLong())}")
                    }
                }
            }
            onChartValueSelectedListener.onNothingSelected()

            setOnChartValueSelectedListener(onChartValueSelectedListener)

            onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartGesture) {
                    data?.setDrawValues(false)
                }

                override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartGesture) {
                    data?.setDrawValues(false)

                    onChartValueSelectedListener.onNothingSelected()
                    highlightValues(null)
                }

                override fun onChartLongPressed(me: MotionEvent) {}
                override fun onChartDoubleTapped(me: MotionEvent) {}
                override fun onChartSingleTapped(me: MotionEvent) {}
                override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) {}
                override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}
                override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {}
            }

            setViewPortOffsets(0f, 0f, 0f, 0f)

            if(entries.isNotEmpty()) {
                axisLeft.axisMinimum = entries.minOf { it.y } * .85f
                axisRight.axisMinimum = entries.minOf { it.y } * .85f
            }

            description.isEnabled = false
            setDrawMarkers(false)
            setNoDataText("No ranked games on record")
            setNoDataTextColor(ResourcesCompat.getColor(resources,R.color.colorActionableText, context?.theme))
            legend.isEnabled = false
            isDoubleTapToZoomEnabled = false

            animateY(250)
            invalidate()
            notifyDataSetChanged()
        }
    }

    private fun formatDate(secondsSinceEpoch: Long): String {
        return shortFormat.format(LocalDateTime.ofEpochSecond(secondsSinceEpoch, 0,  OffsetDateTime.now().offset))
    }

    override fun fillOutcomePieChart(lostCount: Int, wonCount: Int) {
        if (wonCount + lostCount != 0) {
            val lostEntry = PieEntry(lostCount.toFloat(), "Lost")
            val wonEntry = PieEntry(wonCount.toFloat(), "Won")
            val dataSet = PieDataSet(listOf(lostEntry, wonEntry), "Games").apply {
                setColors(intArrayOf(R.color.chartLost, R.color.chartWon), context)
                setDrawValues(false)
            }

            binding.chart.data = PieData(dataSet)

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

            binding.chart.centerText = centerText
        }
        binding.chart.apply {
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

            animateX(250)

            invalidate()
        }
    }

    override fun fillLongestStreak(length: Int, start: Long, end: Long) {
        val startDate = dateFormat.format(Date(start * 1000))
        val endDate = dateFormat.format(Date(end * 1000))
        binding.winningStreak.text = "$length game(s)"
        binding.winningStreakDetails.text = "$startDate to $endDate"
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
        binding.currentForm.text = currentFormSpan
    }
}

class DayAxisValueFormatter (private val chart: BarLineChartBase<*>) : ValueFormatter() {

    private val yearFormatter = SimpleDateFormat("yyyy", Locale.US)
    private val monthFormatter = SimpleDateFormat("MMM''yy", Locale.US)
    private val dayFormatter = SimpleDateFormat("dd MMM", Locale.US)

    override fun getFormattedValue(secondsSinceEpoch: Float): String? {
        return when {
            chart.visibleXRange > 189_216_000 -> { // 6 years
                yearFormatter.format(Date(secondsSinceEpoch.toLong() *1000)).toString()
            }
            chart.visibleXRange > 15_780_000 -> { // 6 months
                monthFormatter.format(Date(secondsSinceEpoch.toLong() *1000)).toString()
            }
            else -> {
                dayFormatter.format(Date(secondsSinceEpoch.toLong() *1000)).toString()
            }
        }
    }
}