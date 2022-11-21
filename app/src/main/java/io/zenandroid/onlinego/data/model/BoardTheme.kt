package io.zenandroid.onlinego.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.zenandroid.onlinego.R


/**
 * Created by alex on 1/9/2015.
 */
@Immutable
enum class BoardTheme(
    val backgroundImage: Int?,
    val backgroundColor: Int?,
    val whiteStone: Int,
    val blackStone: Int,
    val gridColor: Color
) {
    WOOD(
        R.drawable.wood,
        null,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Black
    ),
    HNG(
        null,
        R.color.bg_hng,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.DarkGray
    ),
    DARK_HNG(
        null,
        R.color.bg_dark_hng,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Cyan
    ),
    BOOK(
        null,
        R.color.bg_book,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Black
    ),
    NOCTURNE(
        null,
        R.color.bg_nocturne,
        R.drawable.ic_stone_white_svg,
        R.drawable.ic_stone_black_svg,
        Color.Gray
    )
    ;
}