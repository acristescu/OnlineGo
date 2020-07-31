package io.zenandroid.onlinego.ui.screens.stats

import android.graphics.Typeface.BOLD
import android.graphics.Typeface.NORMAL
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL
import kotlinx.android.synthetic.main.view_player_profile.*
import kotlinx.android.synthetic.main.view_stats.*
import org.koin.android.ext.android.get
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment(), StatsContract.View {

    companion object {
        fun createFragment(id: Long): StatsFragment = StatsFragment().apply {
                playerID = id
        }
    }

    private lateinit var presenter: StatsContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics
    private var playerID = Util.getCurrentUserId()!!
    private var mostFacedOpponentGameRecords : List<Map<String, String>>? = null
    private var highestWinGameRecord : List<Map<String, String>>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = StatsPresenter(this, analytics, get(), playerID)
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(activity!!, javaClass.simpleName, javaClass.simpleName)
        presenter.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun fillPlayerDetails(playerDetails: OGSPlayer ) {
        nameView.text = playerDetails.username
        rankView.text = formatRank(egfToRank(playerDetails.ratings?.overall?.rating))
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

        var rankViewText = SpannableString("${formatRank(egfToRank(playerDetails.ratings?.overall?.rating))}\n" +
                "${playerDetails.ratings?.overall?.rating?.toInt()} ± ${playerDetails.ratings?.overall?.deviation?.toInt()}")
        rankViewText.apply {
            listOf(AbsoluteSizeSpan(20, true), StyleSpan(BOLD)).forEach { span ->
                setSpan(span,0, rankViewText.indexOf('\n'), SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        rankViewText.apply {
            listOf(AbsoluteSizeSpan(12, true), StyleSpan(NORMAL), ForegroundColorSpan(resources.getColor(R.color.colorTextSecondary))).forEach { span ->
                setSpan(span, rankViewText.indexOf('\n'), rankViewText.length, SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        rankView.text = rankViewText
    }

    override fun fillPlayerStats(playerStats: String): Pair<Long, Long> {
        var gameList = playerStats.split('\n').drop(1).dropLast(2) // drops header,initialRating,trailing endline
        var gameStats: MutableList<MutableMap<String, String>> = mutableListOf()
        var highestRank = 0f; var highestRankTimestamp : String? = null
        for (game in gameList) {
            val gameAttrList = game.split('\t')
            if (!gameAttrList.isEmpty() && gameAttrList.size == 14) {
                val gameMap = mutableMapOf(
                        "ended" to gameAttrList[0],
                        "game_id" to gameAttrList[1],
                        "played_black" to gameAttrList[2],
                        "handicap" to gameAttrList[3],
                        "rating" to gameAttrList[4],
                        "deviation" to gameAttrList[5],
                        "volatility" to gameAttrList[6],
                        "opponent_id" to gameAttrList[7],
                        "opponent_rating" to gameAttrList[8],
                        "opponent_deviation" to gameAttrList[9],
                        "outcome" to gameAttrList[10],
                        "extra" to gameAttrList[11],
                        "annulled" to gameAttrList[12],
                        "result" to gameAttrList[13]
                )
                val currentRank = (gameMap["rating"]?:"0").toFloat()
                val currentTimestamp = gameMap["ended"]?:"0"
                if (currentRank > highestRank) {
                    highestRank = currentRank
                    highestRankTimestamp = currentTimestamp
                }
                gameStats.add(gameMap)
            }
            else { continue }
        }

        fillHighestRank(highestRank, highestRankTimestamp)

        fillRankGraph(gameStats)

        fillOutcomePieChart(gameStats)

        fillWinningStreak(gameStats)

        fillCurrentForm(gameStats)


        //ID of the most faced opponent
        val mostFacedId = if (gameStats.isNotEmpty()) gameStats.groupingBy{ it["opponent_id"] }.eachCount().maxBy{ it.value }?.key else "0"

        //game record of the highest win
        var highestWin = if (gameStats.isNotEmpty()) gameStats.maxBy{ (it["opponent_rating"]?:"0").toFloat() }!! else mapOf(Pair("",""))

        //"most faced opponent" and "highest win" stats require a nested API call for opponent
        // details - returning the player IDs so that the Presenter can get those details
        //there may be cleaner ways to do it, but this Kotlin/Retrofit noob did not find them..
        mostFacedOpponentGameRecords = gameStats.filter{ it["opponent_id"] == mostFacedId }
        highestWinGameRecord = mutableListOf(highestWin)
        return Pair(mostFacedId!!.toLong(), (highestWin["opponent_id"]?:"0").toLong())
    }

    override fun mostFacedOpponent(playerDetails: OGSPlayer) {
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
        val outcomes = mostFacedOpponentGameRecords!!.groupingBy{ it["outcome"] }.eachCount()
        mostFacedOpponentDetailsView.text = "${mostFacedOpponentGameRecords!!.size} Games, ${outcomes["1"]?:"0"}-${outcomes["0"]?:"0"}"
    }

    override fun highestWin(playerDetails: OGSPlayer) {
        var dateFormat = SimpleDateFormat("MMM d, yyyy")
        highestWinOpponent.text = playerDetails.username
        highestWinRank.text = "(${formatRank(egfToRank(highestWinGameRecord!![0]["opponent_rating"]?.toDouble()))})"
        playerDetails.icon?.let {
            Glide.with(this)
                    .load(processGravatarURL(it, highestWinIconView.width))
                    .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                    .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                    .into(highestWinIconView)
        }
        highestWinDetails.text = "${dateFormat.format(Date((highestWinGameRecord!![0]["ended"]?:"0").toLong() * 1000))}"
    }

    private fun fillHighestRank(highestRank: Float, highestRankTimestamp: String?) {
        var highestRankText = SpannableString("${formatRank(egfToRank(highestRank.toDouble()))}\n${SimpleDateFormat("MMM d, yyyy").format(Date((highestRankTimestamp?:"0").toLong() * 1000))}")
        highestRankText.apply {
            listOf(AbsoluteSizeSpan(20, true), StyleSpan(BOLD)).forEach { span ->
                setSpan(span,0, highestRankText.indexOf('\n'), SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        highestRankText.apply {
            listOf(AbsoluteSizeSpan(12, true), StyleSpan(NORMAL), ForegroundColorSpan(resources.getColor(R.color.colorTextSecondary))).forEach { span ->
                setSpan(span, highestRankText.indexOf('\n'), highestRankText.length, SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        highestRankView.text = highestRankText
    }


    private fun fillRankGraph(gameStats: MutableList<MutableMap<String, String>>) {
        var lastAddedTimestamp = ""
        var rankGraphEntries: MutableList<Entry> = mutableListOf()
        for (game in gameStats) {
            val currentRank = (game["rating"]?:"0").toFloat()
            val currentTimestamp = game["ended"]?:"0"
            //jaggedness reduction - only chart one entry per 24h
            if(lastAddedTimestamp.isEmpty() || (lastAddedTimestamp.toInt() - currentTimestamp.toInt() > 60*60*24)) {
                //added in hours - we don't need more precision on the line chart points
                rankGraphEntries.add(Entry(currentTimestamp.toInt() / 60 / 60f, currentRank))
                lastAddedTimestamp = currentTimestamp
            }
        }

        if (rankGraphEntries.isNotEmpty()) {
            Collections.sort(rankGraphEntries, EntryXComparator())
            val rankDataSet = LineDataSet(rankGraphEntries, "Games")
            rankDataSet.setDrawCircles(false)
            rankDataSet.setDrawValues(false)
            rankDataSet.lineWidth = 1.5f
            rankDataSet.color = resources.getColor(R.color.rankGraphLine)
            rankDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER;
            val lineData = LineData(rankDataSet)
            rankGraph.data = lineData
        }
        rankGraph.xAxis.position = XAxis.XAxisPosition.BOTTOM;
        rankGraph.xAxis.valueFormatter = DayAxisValueFormatter(rankGraph)

        rankGraph.xAxis.setLabelCount(6, true)
        rankGraph.xAxis.textColor = resources.getColor(R.color.colorText)
        rankGraph.xAxis.setDrawGridLines(false)
        rankGraph.axisLeft.setDrawGridLines(false)
        rankGraph.axisLeft.textColor = resources.getColor(R.color.colorText)
        rankGraph.axisRight.setDrawGridLines(false)
        rankGraph.axisRight.valueFormatter = RankFormatter()
        rankGraph.axisRight.textColor = resources.getColor(R.color.colorText)
        rankGraph.description.isEnabled = false
        rankGraph.setDrawMarkers(false)
        rankGraph.setNoDataText("No ranked games on record")
        rankGraph.setNoDataTextColor(resources.getColor(R.color.colorActionableText))
        rankGraph.legend.isEnabled = false
        rankGraph.invalidate()
    }

    private fun fillOutcomePieChart(gameStats: MutableList<MutableMap<String, String>>) {
        val outcomes = gameStats.groupingBy{ it["outcome"] }.eachCount()
        if (outcomes.isNotEmpty()) {
            val lostEntry = PieEntry((outcomes["0"] ?: 0).toFloat(), "Lost")
            val wonEntry = PieEntry((outcomes["1"] ?: 0).toFloat(), "Won")
            val entries = mutableListOf(lostEntry, wonEntry)
            val dataSet = PieDataSet(entries, "Games")
            dataSet.setColors(intArrayOf(R.color.chartLost, R.color.chartWon), context)
            dataSet.setDrawValues(false)
            val pieData = PieData(dataSet)
            chart.data = pieData
            val gamesWon = wonEntry.value.toInt();
            val gamesLost = lostEntry.value.toInt()
            val gamesWonPercent = (gamesWon * 100 / (gamesWon + gamesLost).toFloat())
            val gamesLostPercent = 100 - gamesWonPercent

            val centerText = SpannableString("${gamesWon}  ${String.format("%.1f", gamesWonPercent)}%\n" +
                    "${gamesLost}  ${String.format("%.1f", gamesLostPercent)}%")
            centerText.setSpan(ForegroundColorSpan(resources.getColor(R.color.chartWon)), 0, centerText.indexOf('\n'), SPAN_EXCLUSIVE_EXCLUSIVE)
            centerText.apply {
                listOf(RelativeSizeSpan(1.8f), StyleSpan(BOLD)).forEach { span ->
                    setSpan(span, 0, centerText.indexOf(' '), SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            centerText.setSpan(ForegroundColorSpan(resources.getColor(R.color.chartLost)), centerText.indexOf('\n'), centerText.length, SPAN_EXCLUSIVE_EXCLUSIVE)
            centerText.apply {
                listOf(RelativeSizeSpan(1.8f), StyleSpan(BOLD)).forEach { span ->
                    setSpan(span, centerText.indexOf('\n'), centerText.indexOf(' ', centerText.indexOf(' ') + 2), SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            chart.centerText = centerText
        }
        chart.offsetLeftAndRight(0)
        chart.setTransparentCircleAlpha(0)
        chart.holeRadius = 60f
        chart.setHoleColor(resources.getColor(R.color.colorTextBackground))
        chart.setTouchEnabled(false)
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.setDrawEntryLabels(false)
        chart.setDrawMarkers(false)
        chart.setNoDataText("No ranked games on record")
        chart.setNoDataTextColor(resources.getColor(R.color.colorActionableText))

        chart.invalidate()
    }

    private fun fillWinningStreak(gameStats: MutableList<MutableMap<String, String>>) {
        var streakCount = 0; var streakStart = "" ; var streakEnd = ""
        var bestStreak = 0; var bestStreakStart : String? = null; var bestStreakEnd : String? = null
        for (game in gameStats) {
            if (game["outcome"] == "1") {
                if (streakCount == 0) streakEnd = game["ended"]!!
                streakStart = game["ended"]!!
                streakCount++
            }
            else {
                if (streakCount > bestStreak) {
                    bestStreak = streakCount
                    bestStreakStart = streakStart
                    bestStreakEnd = streakEnd
                }
                streakCount = 0
            }
        }

        winningStreak.text = "$bestStreak Games"
        val dateFormat = SimpleDateFormat("MMM d, yyyy")
        winningStreakDetails.text = "${dateFormat.format(Date((bestStreakStart?:"0").toLong() * 1000))} " +
                "to ${dateFormat.format(Date((bestStreakEnd?:"0").toLong() * 1000))}"
    }

    private fun fillCurrentForm(gameStats: MutableList<MutableMap<String, String>>) {
        var currentFormSpan = SpannableStringBuilder("")
        for ((idx, game) in gameStats.subList(0, 10.coerceAtMost(gameStats.size)).reversed().withIndex()) {
            when (game["outcome"]) {
                "1" -> {
                    currentFormSpan.append("W")
                    currentFormSpan.setSpan(ForegroundColorSpan(resources.getColor(R.color.chartWon)), idx, idx+1, SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "0" -> {
                    currentFormSpan.append("L")
                    currentFormSpan.setSpan(ForegroundColorSpan(resources.getColor(R.color.chartLost)), idx, idx+1, SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        currentForm.text = currentFormSpan
    }
}

class RankFormatter : ValueFormatter() {
    // override this for custom formatting of XAxis or YAxis labels
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return formatRank(egfToRank(value.toDouble()))
    }
    // ... override other methods for the other chart types
}

class DayAxisValueFormatter (chart: BarLineChartBase<*>?) : ValueFormatter() {
    private val chart: BarLineChartBase<*>? = chart

    override fun getFormattedValue(hoursSinceEpoch: Float): String? {
        //until 6 months - day+Month
        //more than 6 months - Month
        //more than 6 years - Year
        return when {
            chart!!.visibleXRange > 51840 -> { // 6 years
                SimpleDateFormat("yyyy").format(Date(hoursSinceEpoch.toLong() *60*60*1000)).toString()
            }
            chart!!.visibleXRange > 4320 -> { // 6 months
                SimpleDateFormat("MMM''yy").format(Date(hoursSinceEpoch.toLong() *60*60*1000)).toString()
            }
            else -> {
                SimpleDateFormat("dd MMM").format(Date(hoursSinceEpoch.toLong() *60*60*1000)).toString()
            }
        }
    }
}