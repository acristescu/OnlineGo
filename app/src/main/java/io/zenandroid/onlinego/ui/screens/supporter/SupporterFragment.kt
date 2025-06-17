package io.zenandroid.onlinego.ui.screens.supporter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SupporterFragment : Fragment() {

  private val viewModel: SupporterViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        val state by viewModel.state.collectAsState()

        OnlineGoTheme {
          SupporterScreen(
            state = state,
            onBackClick = { activity?.onBackPressed() },
            onSubscribeClick = {
              viewModel.onSubscribeClick(requireActivity())
              FirebaseAnalytics.getInstance(requireContext())
                .logEvent("start_subscription_flow", null)
            },
            onCancelSubscriptionClick = {
              requireActivity().startActivity(Intent(Intent.ACTION_VIEW).apply {
                data =
                  "https://play.google.com/store/account/subscriptions?package=io.zenandroid.onlinego".toUri()
              })
              FirebaseAnalytics.getInstance(requireContext()).logEvent("cancel_subscription", null)
            },
            onSliderChange = { value ->
              viewModel.onUserDragSlider(value)
            }
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    analyticsReportScreen("Supporter")
    viewModel.onViewResumed()
  }

  override fun onPause() {
    super.onPause()
    viewModel.onViewPaused()
  }

  private fun observeEvents() {
    lifecycleScope.launch {
      viewModel.events.collect { event ->
        when (event) {
          is SupporterEvent.ShowError -> showError(event.throwable)
        }
      }
    }
  }

  private fun showError(t: Throwable) {
    recordException(t)
    Toast.makeText(requireContext(), t.message, Toast.LENGTH_LONG).show()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    observeEvents()
  }
}

