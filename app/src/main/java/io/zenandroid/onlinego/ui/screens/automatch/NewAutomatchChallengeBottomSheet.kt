package io.zenandroid.onlinego.ui.screens.automatch

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.ui.views.ClickableBubbleChart
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

import android.graphics.Color
import android.util.Log
import android.view.ViewGroup.LayoutParams
import androidx.compose.material.Divider
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import io.zenandroid.onlinego.data.model.ogs.SeekGraphChallenge
import io.zenandroid.onlinego.R
import androidx.navigation.findNavController
import androidx.navigation.NavOptions
import androidx.core.os.bundleOf
import androidx.core.view.marginTop
import io.zenandroid.onlinego.ui.screens.stats.PLAYER_ID
import com.github.mikephil.charting.charts.BubbleChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.LimitLine.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BubbleData
import com.github.mikephil.charting.data.BubbleDataSet
import com.github.mikephil.charting.data.BubbleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBubbleDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import io.zenandroid.onlinego.ui.screens.newchallenge.ChallengeMarkerView
import io.zenandroid.onlinego.utils.setMargins
import io.zenandroid.onlinego.utils.setMarginsDP
import kotlin.math.abs
import kotlin.math.log10

private const val TAG = "NewAutomatchChallengeBS"

class NewAutomatchChallengeBottomSheet : BottomSheetDialogFragment(), OnChartValueSelectedListener {
  private val viewModel: NewAutomatchChallengeViewModel by viewModel()

  private lateinit var chart: BubbleChart

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState)

    dialog.setOnShowListener {
      BottomSheetBehavior.from(dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!)
        .apply {
          state = BottomSheetBehavior.STATE_EXPANDED
          skipCollapsed = true
        }
    }

    return dialog
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        val state by rememberStateWithLifecycle(viewModel.state)

        OnlineGoTheme {
          NewAutomatchChallengeBottomSheetContent(
            state = state,
            chart = @Composable { modifier -> Chart(state, modifier) },
            onSmallCheckChanged = { viewModel.onSmallCheckChanged(it) },
            onMediumCheckChanged = { viewModel.onMediumCheckChanged(it) },
            onLargeCheckChanged = { viewModel.onLargeCheckChanged(it) },
            onSpeedChanged = { viewModel.onSpeedChanged(it) },
            onSearchClicked = {
              dismiss()
              val selectedSizes = mutableListOf<Size>()
              if (state.small) {
                selectedSizes.add(Size.SMALL)
              }
              if (state.medium) {
                selectedSizes.add(Size.MEDIUM)
              }
              if (state.large) {
                selectedSizes.add(Size.LARGE)
              }
              (activity as? MainActivity)?.onAutomatchSearchClicked(state.speed, selectedSizes)
            }
          )
        }
      }
    }
  }

  @Composable
  private fun Chart(state: AutomatchState, modifier: Modifier) {
    val challenges = state.challenges
    AndroidView(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(ratio = 5f/4f),
      factory = { context ->
        ClickableBubbleChart(context).apply {
          id = R.id.chart
          layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
          setMarginsDP(top = 4)
          description.isEnabled = false
          setOnChartValueSelectedListener(this@NewAutomatchChallengeBottomSheet)
          setDrawGridBackground(false)
          setTouchEnabled(true)

          // enable scaling and dragging
          isDragEnabled = true
          setScaleEnabled(true)

          setMaxVisibleValueCount(200)
          setPinchZoom(true)

          // create a dataset and give it a type
          val set1 = BubbleDataSet(ArrayList<BubbleEntry>(), "19x19")
          //set1.setDrawIcons(false)
          set1.setColor(ColorTemplate.COLORFUL_COLORS[0], 130)
          set1.setDrawValues(true)
          set1.isNormalizeSizeEnabled = false

          val set2 = BubbleDataSet(ArrayList<BubbleEntry>(), "13x13")
          //set2.setDrawIcons(false)
          //set2.setIconsOffset(MPPointF(0f, 15f))
          set2.setColor(ColorTemplate.COLORFUL_COLORS[1], 130)
          set2.setDrawValues(true)
          set2.isNormalizeSizeEnabled = false

          val set3 = BubbleDataSet(ArrayList<BubbleEntry>(), "9x9")
          set3.setColor(ColorTemplate.COLORFUL_COLORS[2], 130)
          set3.setDrawValues(true)
          set3.isNormalizeSizeEnabled = false

          val set4 = BubbleDataSet(ArrayList<BubbleEntry>(), "?x?")
          set4.setColor(ColorTemplate.COLORFUL_COLORS[3], 130)
          set4.setDrawValues(true)
          set4.isNormalizeSizeEnabled = false

          val set5 = BubbleDataSet(ArrayList<BubbleEntry>(), "Eligible")
          set5.setDrawIcons(false)
          set5.setColor(Color.BLUE, 130)
          set5.setDrawValues(true)
          set5.isNormalizeSizeEnabled = false

          val dataSets = ArrayList<IBubbleDataSet>()
          dataSets.add(set1) // add the data sets
          dataSets.add(set2)
          dataSets.add(set3)
          dataSets.add(set4)
          dataSets.add(set5)

          // create a data object with the data sets
          val data = BubbleData(dataSets)
          data.setDrawValues(false)
          data.setValueTextSize(8f)
          data.setValueTextColor(Color.WHITE)
          data.setHighlightCircleWidth(1.5f)

          this.data = data
          this.invalidate()

          legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            orientation = Legend.LegendOrientation.VERTICAL
            setDrawInside(false)
            textColor = ResourcesCompat.getColor(resources, R.color.colorText, context.theme)
          }

          axisLeft.apply {
            spaceTop = 30f
            spaceBottom = 30f
            setDrawZeroLine(false)
            setLabelCount(10, true)
            setAxisMinValue(-1f)
            setAxisMaxValue(38f)
            isGranularityEnabled = true
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
              override fun getFormattedValue(value: Float): String {
                val rank = value.toInt()
                return when {
                  rank < 30 -> "${30 - rank}k"
                  else -> "${rank - 29}d"
                }
              }
            }
            textColor = ResourcesCompat.getColor(resources, R.color.colorText, context.theme)
            state.rating.toFloat().let {
              addLimitLine(LimitLine(it, "").apply {
                lineWidth = .5f
                lineColor = Color.WHITE
                labelPosition = LimitLabelPosition.RIGHT_TOP
                textSize = 10f
              })
            }
          }

          axisRight.isEnabled = false

          xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = object : ValueFormatter() {
              override fun getFormattedValue(value: Float): String = when(value.toInt()) {
                0 -> "Blitz"
                3 -> "Live"
                6 -> "Correspondence"
                else -> ""
              }
            }
            //setLabelCount(4, true)
            setCenterAxisLabels(true)
            labelRotationAngle = 9f
            setAxisMinValue(0f)
            setAxisMaxValue(8f)
            isGranularityEnabled = true
            granularity = 1f
            textColor = ResourcesCompat.getColor(resources, R.color.colorText, context.theme)
            addLimitLine(LimitLine(2f, "").apply {
              lineWidth = 1.5f
              lineColor = Color.GRAY
              labelPosition = LimitLabelPosition.RIGHT_TOP
              textSize = 10f
            })
            addLimitLine(LimitLine(5f, "").apply {
              lineWidth = 1.5f
              lineColor = Color.GRAY
              labelPosition = LimitLabelPosition.RIGHT_TOP
              textSize = 10f
            })
          }

          setNoDataTextColor(ResourcesCompat.getColor(resources, R.color.colorActionableText, context.theme))

          let { chart ->
            // create a custom MarkerView (extend MarkerView) and specify the layout to use for it
            val mv = ChallengeMarkerView(context, {
              dismiss()
              (context as FragmentActivity).findNavController(R.id.fragment_container).navigate(
                R.id.stats,
                bundleOf(PLAYER_ID to it.id),
                NavOptions.Builder()
                  .setLaunchSingleTop(true)
                  .setPopUpTo(R.id.myGames, false, false)
                  .build())
            }, {
              dismiss()
            })
            mv.chartView = chart
            chart.marker = mv
          }
        }.also { this@NewAutomatchChallengeBottomSheet.chart = it }
      },
      update = { chart ->
        (chart as BubbleChart).apply {
          for(i in 0..4)
            data.getDataSetByIndex(i).clear()

          data.also {
            challenges.forEach { challenge: SeekGraphChallenge ->
              val rankDiff = (challenge.rank ?: 0.0) - state.rating.toDouble()
              val drawable = when {
                challenge.ranked && abs(rankDiff) > 9 -> null
                state.rating < challenge.min_rank -> null
                state.rating > challenge.max_rank -> null
                else -> resources.getDrawable(R.drawable.ic_star)
              }
              val dataset = when {
                drawable != null -> 4
                challenge.width == 19 -> 0
                challenge.width == 13 -> 1
                challenge.width == 9 -> 2
                else -> 3
              }
              val entry = BubbleEntry(
                log10((challenge.time_per_move ?: 0.0) + 1).toFloat(),
                challenge.rank?.toFloat() ?: 0f,
                .2f, drawable, challenge)
              data.addEntry(entry, dataset)
            }

            data.notifyDataChanged()
          }

          notifyDataSetChanged()
          invalidate()
        }
      }
    )
  }

  override fun onValueSelected(e: Entry, h: Highlight) {
    Log.d(TAG, "Val selected: " + chart.axisLeft.valueFormatter.getFormattedValue(e.y) + ", " + e.x + " - " + chart.data.getDataSetByIndex(h.dataSetIndex).label + " " + e.data)
  }

  override fun onNothingSelected() {
    Log.d(TAG, "Val unselected")
  }
}

@Composable
private fun NewAutomatchChallengeBottomSheetContent(
  state: AutomatchState,
  chart: @Composable (modifier: Modifier) -> Unit,
  onSmallCheckChanged: (Boolean) -> Unit,
  onMediumCheckChanged: (Boolean) -> Unit,
  onLargeCheckChanged: (Boolean) -> Unit,
  onSpeedChanged: (Speed) -> Unit,
  onSearchClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface {
    Column(
      modifier
        .padding(16.dp)
    ) {
      Text(text = "Try your hand at a game against a human opponent of similar rating to you.")
      Text(
        text = "Game size",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
      )
      Row {
        SizeCheckbox(checked = state.small, text = "9×9", onClick = onSmallCheckChanged)
        Spacer(modifier = Modifier.weight(1f))
        SizeCheckbox(checked = state.medium, text = "13×13", onClick = onMediumCheckChanged)
        Spacer(modifier = Modifier.weight(1f))
        SizeCheckbox(checked = state.large, text = "19×19", onClick = onLargeCheckChanged)
      }
      Text(
        text = "Time Controls",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
      )
      Box {
        var expanded by remember { mutableStateOf(false) }
        Text(
          text = state.speed.getText()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() },
          color = MaterialTheme.colors.primary,
          modifier = Modifier
            .clickable {
              expanded = true
            }
            .padding(top = 4.dp)
            .fillMaxWidth()
        )
        DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
          modifier = Modifier.fillMaxWidth()
        ) {
          Speed.entries.forEach {
            DropdownMenuItem(onClick = {
              expanded = false
              onSpeedChanged(it)
            }) {
              Text(text = it.getText()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() })
            }
          }
        }
      }
      Divider(
        //color = ResourcesCompat.getColor(this.context, android.R.color.black),
        thickness = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(0.dp, 10.dp)
      )
      Column(
        modifier = Modifier.padding(0.dp, 4.dp)
      ) {
        Text(
          text = "Challenges",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colors.primary,
          modifier = Modifier.padding(top = 16.dp)
        )
        chart(modifier = Modifier.fillMaxWidth())
      }
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        enabled = state.isAnySizeSelected,
        onClick = onSearchClicked
      ) {
        Text("Search")
      }
    }
  }
}

@Composable
private fun RowScope.SizeCheckbox(checked: Boolean, text: String, onClick: (Boolean) -> Unit) {
  Checkbox(
    checked = checked,
    colors = CheckboxDefaults.colors(
      checkedColor = MaterialTheme.colors.primary
    ),
    onCheckedChange = onClick
  )
  Text(
    text = text,
    modifier = Modifier
      .align(Alignment.CenterVertically)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onClick(!checked) }
  )
}

@Preview(showBackground = true)
@Composable
private fun NewAutomatchChallengeBottomSheetPreview() {
  OnlineGoTheme {
    Box(modifier = Modifier.fillMaxSize())
    NewAutomatchChallengeBottomSheetContent(AutomatchState(), {}, {}, {}, {}, {}, {})
  }
}
