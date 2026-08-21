package io.zenandroid.onlinego.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.zenandroid.onlinego.R


/**
 * Created by Shaggy on 21/11/2022.
 */
@Immutable
enum class BoardTheme(
  @StringRes val displayNameResId: Int,
    val backgroundImage: Int?,
    val backgroundImageDarkMode: Int?,
    val backgroundColor: Int?,
    val gridPreview: Int,
    val whiteStone: Int,
    val blackStone: Int,
    val textAndGridColor: Color
) {
    WOOD(
      R.string.settings_board_style_light_wood,
        R.drawable.wood,
        R.drawable.wood_medium,
        null,
        R.mipmap.bg_preview_wood,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Black
    ),
    WOOD_DARK(
      R.string.settings_board_style_dark_wood,
        R.drawable.wood_dark,
        R.drawable.wood_dark,
        null,
        R.mipmap.bg_preview_dark_wood,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.White
    ),
    CYAN(
      R.string.settings_board_style_cyan,
        null,
        null,
        R.color.bg_cyan,
        R.mipmap.bg_preview_cyan,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.DarkGray
    ),
    DARK_BLUE(
      R.string.settings_board_style_dark_blue,
        null,
        null,
        R.color.bg_dark_blue,
        R.mipmap.bg_preview_dark_blue,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Cyan
    ),
    BOOK(
      R.string.settings_board_style_book,
        null,
        null,
        R.color.bg_book,
        R.mipmap.bg_preview_book,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Gray
    ),
    NOCTURNE(
      R.string.settings_board_style_nocturne,
        null,
        null,
        R.color.bg_nocturne,
        R.mipmap.bg_preview_nocturne,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Gray
    );

}