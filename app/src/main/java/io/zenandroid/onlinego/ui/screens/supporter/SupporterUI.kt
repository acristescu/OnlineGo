package io.zenandroid.onlinego.ui.screens.supporter

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.analytics.FirebaseAnalytics
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.R.drawable
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.recordException
import org.koin.androidx.compose.koinViewModel

@Composable
fun SupporterScreen(
  viewModel: SupporterViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }
  val context = LocalContext.current
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is SupporterEvent.ShowError -> Toast.makeText(
          context,
          event.throwable.message,
          Toast.LENGTH_LONG
        ).show()
      }
    }
  }

  val activity = LocalActivity.current
  SupporterContent(
    state = state,
    onBackClick = onNavigateBack,
    onSubscribeClick = {
      activity?.let {
        viewModel.onSubscribeClick(activity)
        FirebaseAnalytics.getInstance(activity)
          .logEvent("start_subscription_flow", null)
      } ?: run {
        recordException(Throwable("Activity is null, cannot start subscription flow"))
      }
    },
    onCancelSubscriptionClick = {
      activity?.let {
        it.startActivity(Intent(Intent.ACTION_VIEW).apply {
          data =
            "https://play.google.com/store/account/subscriptions?package=io.zenandroid.onlinego".toUri()
        })
        FirebaseAnalytics.getInstance(it).logEvent("cancel_subscription", null)
      } ?: run {
        recordException(Throwable("Activity is null, cannot cancel subscription"))
      }
    },
    onSliderChange = viewModel::onUserDragSlider
  )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupporterContent(
  state: SupporterState,
  onBackClick: () -> Unit,
  onSubscribeClick: () -> Unit,
  onCancelSubscriptionClick: () -> Unit,
  onSliderChange: (Float) -> Unit
) {
  Column {
    TopAppBar(
      title = {
        Text(
          text = stringResource(R.string.supporter_title),
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      navigationIcon = {
        IconButton(onClick = onBackClick) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      ),
    )

    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
      Text(
        text = stringResource(R.string.supporter_header),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = stringResource(R.string.supporter_intro),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
      )

      Spacer(modifier = Modifier.height(48.dp))

      SupportReason(
        icon = ImageVector.vectorResource(id = R.drawable.ic_branch),
        title = stringResource(R.string.supporter_reason_develop_title),
        description = stringResource(R.string.supporter_reason_develop_description)
      )

      SupportReason(
        icon = Icons.Default.Android,
        title = stringResource(R.string.supporter_reason_free_title),
        description = stringResource(R.string.supporter_reason_free_description)
      )

      SupportReason(
        icon = ImageVector.vectorResource(id = R.drawable.ic_github),
        title = stringResource(R.string.supporter_reason_open_source_title),
        description = stringResource(R.string.supporter_reason_open_source_description)
      )

      SupportReason(
        icon = ImageVector.vectorResource(id = R.drawable.ic_board_transparent),
        title = stringResource(R.string.supporter_reason_promote_title),
        description = stringResource(R.string.supporter_reason_promote_description)
      )

      Spacer(modifier = Modifier.height(36.dp))

      Text(
        text = stringResource(R.string.supporter_faqs),
        color = Color(0xFF9B9B9B),
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(16.dp))

      FAQItem(
        question = stringResource(R.string.supporter_faq_money_question),
        answer = stringResource(R.string.supporter_faq_money_answer)
      )

      FAQItem(
        question = stringResource(R.string.supporter_faq_features_question),
        answer = stringResource(R.string.supporter_faq_features_answer)
      )

      FAQItem(
        question = stringResource(R.string.supporter_faq_cancel_question),
        answer = stringResource(R.string.supporter_faq_cancel_answer)
      )

      FAQItem(
        question = stringResource(R.string.supporter_faq_payment_question),
        answer = stringResource(R.string.supporter_faq_payment_answer)
      )

      Spacer(modifier = Modifier.height(24.dp))
    }

    SupporterBottomBar(
      state = state,
      onSubscribeClick = onSubscribeClick,
      onCancelSubscriptionClick = onCancelSubscriptionClick,
      onSliderChange = onSliderChange
    )
  }
}

@Composable
fun SupportReason(
  icon: ImageVector,
  title: String,
  description: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth(),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier
        .padding(start = 12.dp, end = 20.dp, top = 24.dp)
        .size(24.dp)
        .align(Alignment.CenterVertically),
      tint = MaterialTheme.colorScheme.onSurface
    )

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = description,
        fontSize = 12.sp,
        lineHeight = 12.sp * 1.3f,
        modifier = Modifier.alpha(0.7f),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }

  Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun FAQItem(
  question: String,
  answer: String
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = question,
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = answer,
      fontSize = 12.sp,
      lineHeight = 12.sp * 1.3f,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.alpha(0.7f),
    )

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun SupporterBottomBar(
  state: SupporterState,
  onSubscribeClick: () -> Unit,
  onCancelSubscriptionClick: () -> Unit,
  onSliderChange: (Float) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.elevatedCardColors(),
    elevation = CardDefaults.cardElevation(
      defaultElevation = 16.dp,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
  ) {
    if (state.loading) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(128.dp),
        contentAlignment = Alignment.Center
      ) {
        LinearProgressIndicator(
          color = MaterialTheme.colorScheme.primary
        )
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // Subscribe title
        val thankYouTitle = stringResource(R.string.supporter_thank_you_title)
        val currentContribution = stringResource(R.string.supporter_current_contribution)
        val selectContribution = stringResource(R.string.supporter_select_contribution)
        Text(
          text = buildAnnotatedString {
            when {
              state.supporter -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                  append(thankYouTitle)
                  append("\n\n")
                }
                append(currentContribution)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                  append(state.currentContributionAmount)
                }
              }

              else -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                  append(selectContribution)
                }
              }
            }
          },
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.numberOfTiers != null && state.selectedTier != null && state.products != null) {
          Slider(
            value = state.displaySliderValue,
            onValueChange = onSliderChange,
            valueRange = 0f..(state.numberOfTiers - 1).toFloat(),
            steps = state.numberOfTiers - 2,
            modifier = Modifier.fillMaxWidth(1f)
          )

          Spacer(modifier = Modifier.width(16.dp))

          Text(
            text = state.selectedTierAmount,
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onSubscribeClick,
          enabled = state.subscribeButtonEnabled,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
        ) {
          Icon(
            imageVector = ImageVector.vectorResource(drawable.ic_star),
            contentDescription = stringResource(R.string.supporter_title),
            modifier = Modifier.padding(end = 12.dp, start = 8.dp)
          )
          Text(
            text = if (state.supporter) stringResource(R.string.supporter_update_amount)
            else stringResource(R.string.supporter_title),
            modifier = Modifier.padding(end = 8.dp),
            fontWeight = FontWeight.Medium,
          )
        }

        if (state.supporter) {
          Spacer(modifier = Modifier.height(8.dp))

          TextButton(
            onClick = onCancelSubscriptionClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
          ) {
            Text(
              text = stringResource(R.string.supporter_cancel_subscription),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun SupporterScreenPreview() {
  OnlineGoTheme {
    SupporterContent(
      state = SupporterState(
        loading = false,
        supporter = true,
        currentPurchaseDetails = null,
        products = null,
        numberOfTiers = 5,
        selectedTier = 2,
        subscribeButtonEnabled = true
      ),
      onBackClick = {},
      onSubscribeClick = {},
      onCancelSubscriptionClick = {},
      onSliderChange = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
fun SupportReasonPreviewLoading() {
  OnlineGoTheme {
    SupporterContent(
      state = SupporterState(
        loading = true,
        supporter = true,
        currentPurchaseDetails = null,
        products = null,
        numberOfTiers = 5,
        selectedTier = 2,
        subscribeButtonEnabled = true
      ),
      onBackClick = {},
      onSubscribeClick = {},
      onCancelSubscriptionClick = {},
      onSliderChange = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
fun SupportReasonPreviewNonSupporter() {
  OnlineGoTheme {
    SupporterContent(
      state = SupporterState(
        loading = false,
        supporter = false,
        currentPurchaseDetails = null,
        products = null,
        numberOfTiers = 5,
        selectedTier = 2,
        subscribeButtonEnabled = true
      ),
      onBackClick = {},
      onSubscribeClick = {},
      onCancelSubscriptionClick = {},
      onSliderChange = {}
    )
  }
}