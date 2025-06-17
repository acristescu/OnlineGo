package io.zenandroid.onlinego.ui.screens.supporter

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
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.io.Files.append
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.R.drawable
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupporterScreen(
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
          text = "Become a supporter",
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colors.onSurface
        )
      },
      navigationIcon = {
        IconButton(onClick = onBackClick) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colors.onSurface
          )
        }
      },
      elevation = 0.dp,
      backgroundColor = MaterialTheme.colors.surface
    )

    Column(
      modifier = Modifier
        .background(MaterialTheme.colors.surface)
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
      Text(
        text = "Support the Android app",
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onSurface,
        fontSize = 18.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "The project was started back in 2015 as a hobby. While excellent browser-based services existed, there were no similar quality mobile apps. The goal was to create a free and open source (FOSS), professionally made Android app that allows GO players to interact and discover this amazing game.\n\nWe are not associated with OGS, although they graciously allowed us to use their online services for free.\n\nWe rely on support from people like you to make it possible. If you enjoy using the app, please consider supporting us with a monthly contribution and becoming a Supporter!",
        color = MaterialTheme.colors.onSurface,
        fontSize = 16.sp,
      )

      Spacer(modifier = Modifier.height(48.dp))

      SupportReason(
        icon = ImageVector.vectorResource(id = R.drawable.ic_branch),
        title = "Help us develop the app faster",
        description = "Supporters make it feasible to allocate more time for development of new features such as KataGo AI integration, AI assisted analysis, SGF editor etc."
      )

      SupportReason(
        icon = Icons.Default.Android,
        title = "Keep the app free",
        description = "We want the app to be free for everyone. Forever. Free from ads, free to download, free from enforced subscriptions."
      )

      SupportReason(
        icon = ImageVector.vectorResource(id = R.drawable.ic_github),
        title = "Support open-source",
        description = "The app is and always will be open source. This means you can browse its code, contribute fixes and features or study how it works."
      )

      SupportReason(
        icon = ImageVector.vectorResource(id = R.drawable.ic_board_transparent),
        title = "Promote GO",
        description = "Having a professionally developed Android app allows more people to discover this amazing game."
      )

      Spacer(modifier = Modifier.height(36.dp))

      Text(
        text = "FAQs",
        color = Color(0xFF9B9B9B),
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(16.dp))

      FAQItem(
        question = "What is the money used for?",
        answer = "The money enables our main developer, who is a professional Android contractor, to take time off between contracts to work on this project. In the future, he could dedicate full-time to improving the app at a much faster pace."
      )

      FAQItem(
        question = "Are there any supporter-only features?",
        answer = "No, because we believe that everyone should have access to a professionally made Android app for GO. Support is entirely optional."
      )

      FAQItem(
        question = "Can I change or cancel my subscription?",
        answer = "Yes, at any time, either from this page or from the 'Subscriptions' page in Google Play."
      )

      FAQItem(
        question = "Are there any other methods of payment?",
        answer = "Apps distributed through Google Play are required to use Google Pay, as Google charges 15% of the proceeds. We are not allowed to offer any alternative means of payment in-app."
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
      tint = MaterialTheme.colors.onSurface
    )

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = description,
        fontSize = 12.sp,
        lineHeight = 12.sp * 1.3f,
        modifier = Modifier.alpha(0.7f),
        color = MaterialTheme.colors.onSurface,
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
      color = MaterialTheme.colors.onSurface,
      fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = answer,
      fontSize = 12.sp,
      lineHeight = 12.sp * 1.3f,
      color = MaterialTheme.colors.onSurface,
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
    backgroundColor = MaterialTheme.colors.surface,
    elevation = 32.dp
  ) {
    if (state.loading) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(128.dp),
        contentAlignment = Alignment.Center
      ) {
        LinearProgressIndicator(
          color = MaterialTheme.colors.primary
        )
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // Subscribe title
        Text(
          text = buildAnnotatedString {
            when {
              state.supporter -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                  append("Thank you for your support!\n\n")
                }
                append("Your current contribution is ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                  append(state.currentContributionAmount)
                }
              }
              else -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                  append("Select your monthly contribution")
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
            contentDescription = "Become a supporter",
            modifier = Modifier.padding(end = 12.dp, start = 8.dp)
          )
          Text(
            text = state.subscribeButtonText ?: "Become a Supporter",
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
              text = "Cancel subscription",
              color = MaterialTheme.colors.primary
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
    SupporterScreen(
      state = SupporterState(
        loading = false,
        supporter = true,
        currentPurchaseDetails = null,
        products = null,
        numberOfTiers = 5,
        selectedTier = 2,
        subscribeTitleText = "Thank you for your support!",
        subscribeButtonText = "Update amount",
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
    SupporterScreen(
      state = SupporterState(
        loading = true,
        supporter = true,
        currentPurchaseDetails = null,
        products = null,
        numberOfTiers = 5,
        selectedTier = 2,
        subscribeTitleText = "Thank you for your support!",
        subscribeButtonText = "Update amount",
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
    SupporterScreen(
      state = SupporterState(
        loading = false,
        supporter = false,
        currentPurchaseDetails = null,
        products = null,
        numberOfTiers = 5,
        selectedTier = 2,
        subscribeTitleText = "Select your monthly contribution",
        subscribeButtonText = "Become a supporter",
        subscribeButtonEnabled = true
      ),
      onBackClick = {},
      onSubscribeClick = {},
      onCancelSubscriptionClick = {},
      onSliderChange = {}
    )
  }
}