package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import io.zenandroid.onlinego.utils.ReviewDiagnosticResult
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

fun MainViewController(versionCode: Int): UIViewController = OnlineGoViewController {
  OnlineGoRoot(
    versionCode = versionCode,
    reviewRequester = {},
    onForceReviewPrompt = {
      ReviewDiagnosticResult(false, listOf("In-app review is not available on iOS yet"), null)
    },
    onTestNotification = {},
    onLaunchBillingFlow = { _, _ -> },
    onFinish = {},
  )
}

internal class OnlineGoViewController(
  private val content: @Composable () -> Unit,
) : UIViewController(nibName = null, bundle = null) {
  private var statusBarStyle: UIStatusBarStyle = UIStatusBarStyleDarkContent

  fun setDarkTheme(darkTheme: Boolean) {
    val style = if (darkTheme) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent
    if (style == statusBarStyle) return
    statusBarStyle = style
    setNeedsStatusBarAppearanceUpdate()
  }

  override fun preferredStatusBarStyle(): UIStatusBarStyle = statusBarStyle

  @OptIn(ExperimentalForeignApi::class)
  override fun viewDidLoad() {
    super.viewDidLoad()
    val compose = ComposeUIViewController(content)
    addChildViewController(compose)
    compose.view.setFrame(view.bounds)
    compose.view.setAutoresizingMask(UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight)
    view.addSubview(compose.view)
    compose.didMoveToParentViewController(this)
  }
}
