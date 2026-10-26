package io.zenandroid.onlinego.ui.screens.onboarding

import androidx.compose.runtime.Composable

internal expect val notificationPermissionRequired: Boolean

@Composable
internal expect fun rememberNotificationPermissionRequest(onAnswered: () -> Unit): () -> Unit
