package io.zenandroid.onlinego.ui.composables

import androidx.core.content.res.ResourcesCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R


// TODO: these need to stop existing!!!
internal val whiteStone = VectorDrawableCompat.create(OnlineGoApplication.instance.resources, R.drawable.ic_stone_white_svg, null)!!
internal val blackStone = VectorDrawableCompat.create(OnlineGoApplication.instance.resources, R.drawable.ic_stone_black_svg, null)!!
internal val shadowDrawable = ResourcesCompat.getDrawable(OnlineGoApplication.instance.resources, R.drawable.gradient_shadow, null)!!