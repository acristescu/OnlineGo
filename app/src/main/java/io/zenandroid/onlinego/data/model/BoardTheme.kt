package io.zenandroid.onlinego.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.zenandroid.onlinego.R


/**
 * Created by Shaggy on 21/11/2022.
 */
@Immutable
enum class BoardTheme(
    val displayName: String,
    val backgroundImage: Int?,
    val backgroundColor: Int?,
    val gridPreview: Int,
    val whiteStone: Int,
    val blackStone: Int,
    val textAndGridColor: Color
) {
    WOOD(
        "Light wood",
        R.drawable.wood,
        null,
        R.mipmap.bg_preview_wood,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Black
    ),
    WOOD_DARK(
        "Dark wood",
        R.drawable.wood_dark,
        null,
        R.mipmap.bg_preview_dark_wood,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.White
    ),
    CYAN(
        "Cyan",
        null,
        R.color.bg_cyan,
        R.mipmap.bg_preview_cyan,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.DarkGray
    ),
    DARK_BLUE(
        "Dark blue",
        null,
        R.color.bg_dark_blue,
        R.mipmap.bg_preview_dark_blue,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Cyan
    ),
    BOOK(
        "Book",
        null,
        R.color.bg_book,
        R.mipmap.bg_preview_book,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Gray
    ),
    NOCTURNE(
        "Nocturne",
        null,
        R.color.bg_nocturne,
        R.mipmap.bg_preview_nocturne,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Gray
    )
}