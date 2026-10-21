package io.zenandroid.onlinego.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.bg_preview_book
import io.zenandroid.onlinego.resources.bg_preview_cyan
import io.zenandroid.onlinego.resources.bg_preview_dark_blue
import io.zenandroid.onlinego.resources.bg_preview_dark_wood
import io.zenandroid.onlinego.resources.bg_preview_nocturne
import io.zenandroid.onlinego.resources.bg_preview_wood
import io.zenandroid.onlinego.resources.settings_board_style_book
import io.zenandroid.onlinego.resources.settings_board_style_cyan
import io.zenandroid.onlinego.resources.settings_board_style_dark_blue
import io.zenandroid.onlinego.resources.settings_board_style_dark_wood
import io.zenandroid.onlinego.resources.settings_board_style_light_wood
import io.zenandroid.onlinego.resources.settings_board_style_nocturne
import io.zenandroid.onlinego.resources.wood
import io.zenandroid.onlinego.resources.wood_dark
import io.zenandroid.onlinego.resources.wood_medium
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Immutable
data class BoardThemeStyle(
  val displayNameResId: StringResource,
  val backgroundImage: DrawableResource?,
  val backgroundImageDarkMode: DrawableResource?,
  val backgroundColor: Color?,
  val gridPreview: DrawableResource,
  val textAndGridColor: Color,
)

val BoardTheme.style: BoardThemeStyle
  get() = when (this) {
    BoardTheme.WOOD -> BoardThemeStyle(
      Res.string.settings_board_style_light_wood,
      Res.drawable.wood,
      Res.drawable.wood_medium,
      null,
      Res.drawable.bg_preview_wood,
      Color.Black,
    )

    BoardTheme.WOOD_DARK -> BoardThemeStyle(
      Res.string.settings_board_style_dark_wood,
      Res.drawable.wood_dark,
      Res.drawable.wood_dark,
      null,
      Res.drawable.bg_preview_dark_wood,
      Color.White,
    )

    BoardTheme.CYAN -> BoardThemeStyle(
      Res.string.settings_board_style_cyan,
      null,
      null,
      Color(0xFF00E7FB),
      Res.drawable.bg_preview_cyan,
      Color.DarkGray,
    )

    BoardTheme.DARK_BLUE -> BoardThemeStyle(
      Res.string.settings_board_style_dark_blue,
      null,
      null,
      Color(0xFF090C1F),
      Res.drawable.bg_preview_dark_blue,
      Color.Cyan,
    )

    BoardTheme.BOOK -> BoardThemeStyle(
      Res.string.settings_board_style_book,
      null,
      null,
      Color(0xFFFFFFFF),
      Res.drawable.bg_preview_book,
      Color.Gray,
    )

    BoardTheme.NOCTURNE -> BoardThemeStyle(
      Res.string.settings_board_style_nocturne,
      null,
      null,
      Color(0xFF444444),
      Res.drawable.bg_preview_nocturne,
      Color.Gray,
    )
  }
