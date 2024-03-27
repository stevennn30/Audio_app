package com.serafimtech.serafimaudio.Compose

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntSize

class Animation {
    enum class anim {
        slide_horizonal_anim,
        slide_fadein_and_fadeout_anim,
        slide_expandin_an_shinkout_anim,
        scalein_and_scaleout
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun enterAnim(flag: Animation.anim): EnterTransition {
    return when (flag) {
        Animation.anim.slide_horizonal_anim -> {
            slideInHorizontally(animationSpec = tween(1000), initialOffsetX = {
                it
            })
        }
        Animation.anim.slide_fadein_and_fadeout_anim -> {
            fadeIn(animationSpec = tween(1000), initialAlpha = 0f)
        }
        Animation.anim.slide_expandin_an_shinkout_anim -> {
            expandIn(animationSpec = tween(1000), expandFrom = Alignment.TopStart) {
                IntSize(0, 0)
            }
        }
        Animation.anim.scalein_and_scaleout -> {
            scaleIn(
                animationSpec = tween(1000),
                initialScale = 0f,
                transformOrigin = TransformOrigin(0f, 0f)
            )
        }
        else -> {
            slideInHorizontally(animationSpec = tween(1000), initialOffsetX = {
                -it
            })
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun exitAnim(flag: Animation.anim): ExitTransition {
    return when (flag) {
        Animation.anim.slide_horizonal_anim -> {
            slideOutHorizontally(animationSpec = tween(1000), targetOffsetX = {
                -it
            })
        }
        Animation.anim.slide_fadein_and_fadeout_anim -> {
            fadeOut(animationSpec = tween(1000), targetAlpha = 0f)
        }
        Animation.anim.slide_expandin_an_shinkout_anim -> {
            shrinkOut(animationSpec = tween(1000), shrinkTowards = Alignment.BottomEnd) {//缩小80%
                it * 4 / 5
            }
        }
        Animation.anim.scalein_and_scaleout -> {
            scaleOut(
                animationSpec = tween(1000),
                targetScale = 0f,
                transformOrigin = TransformOrigin(1f, 1f)
            )
        }
        else -> {
            slideOutHorizontally(animationSpec = tween(1000), targetOffsetX = {
                it
            })
        }
    }
}