package io.zenandroid.onlinego.di

import coil3.PlatformContext

fun prewarmApp(density: Float) = prewarmApp(PlatformContext.INSTANCE, density)
