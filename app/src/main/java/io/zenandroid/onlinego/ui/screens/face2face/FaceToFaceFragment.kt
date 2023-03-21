package io.zenandroid.onlinego.ui.screens.face2face

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel


class FaceToFaceFragment: Fragment() {
  
  private val viewModel: FaceToFaceViewModel by viewModel()

  override fun onResume() {
    super.onResume()
    analyticsReportScreen("FaceToFace")
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        val state by rememberStateWithLifecycle(viewModel.state)

        FaceToFaceScreen(
          state = state,
          onUserAction = viewModel::onAction,
          onBackPressed = { requireActivity().onBackPressedDispatcher.onBackPressed() }
        )
      }
    }
  }
}

