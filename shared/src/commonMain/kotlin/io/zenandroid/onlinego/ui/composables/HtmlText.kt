package io.zenandroid.onlinego.ui.composables

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLinkStyles

internal expect fun htmlToAnnotatedString(html: String, linkStyles: TextLinkStyles): AnnotatedString
