package io.zenandroid.onlinego.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.BoardTheme

@Immutable
data class BoardThemeStyle(
  @StringRes val displayNameResId: Int,
  val backgroundImage: Int?,
  val backgroundImageDarkMode: Int?,
  val backgroundColor: Int?,
  val gridPreview: Int,
  val whiteStone: Int,
  val blackStone: Int,
  val textAndGridColor: Color,
)

val BoardTheme.style: BoardThemeStyle
  get() = when (this) {
    BoardTheme.WOOD -> BoardThemeStyle(
      R.string.settings_board_style_light_wood,
      R.drawable.wood,
      R.drawable.wood_medium,
      null,
      R.mipmap.bg_preview_wood,
      R.drawable.ic_stone_white_svg,
      R.drawable.ic_stone_black_svg,
      Color.Black,
    )

    BoardTheme.WOOD_DARK -> BoardThemeStyle(
      R.string.settings_board_style_dark_wood,
      R.drawable.wood_dark,
      R.drawable.wood_dark,
      null,
      R.mipmap.bg_preview_dark_wood,
      R.drawable.ic_stone_white_svg,
      R.drawable.ic_stone_black_svg,
      Color.White,
    )

    BoardTheme.CYAN -> BoardThemeStyle(
      R.string.settings_board_style_cyan,
      null,
      null,
      R.color.bg_cyan,
      R.mipmap.bg_preview_cyan,
      R.drawable.ic_stone_white_svg,
      R.drawable.ic_stone_black_svg,
      Color.DarkGray,
    )

    BoardTheme.DARK_BLUE -> BoardThemeStyle(
      R.string.settings_board_style_dark_blue,
      null,
      null,
      R.color.bg_dark_blue,
      R.mipmap.bg_preview_dark_blue,
      R.drawable.ic_stone_white_svg,
      R.drawable.ic_stone_black_svg,
      Color.Cyan,
    )

    BoardTheme.BOOK -> BoardThemeStyle(
      R.string.settings_board_style_book,
      null,
      null,
      R.color.bg_book,
      R.mipmap.bg_preview_book,
      R.drawable.ic_stone_white_svg,
      R.drawable.ic_stone_black_svg,
      Color.Gray,
    )

    BoardTheme.NOCTURNE -> BoardThemeStyle(
      R.string.settings_board_style_nocturne,
      null,
      null,
      R.color.bg_nocturne,
      R.mipmap.bg_preview_nocturne,
      R.drawable.ic_stone_white_svg,
      R.drawable.ic_stone_black_svg,
      Color.Gray,
    )
  }
