package com.serafimtech.serafimaudio.Compose

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serafimtech.serafimaudio.MainActivity
import com.serafimtech.serafimaudio.R
import com.serafimtech.serafimaudio.ViewModel
import kotlinx.coroutines.delay

@Composable
fun ScanScreen(model: ViewModel) {
    val context = LocalContext.current
    val rescan by model.ReScanLiveData.observeAsState()

    Column(
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        if (rescan!!) {
            StopLoadingAnimation()
        } else {
            LoadingAnimation()
        }

        val textStyleBody1 = MaterialTheme.typography.h3
        var textStyle by remember { mutableStateOf(textStyleBody1) }
        var readyToDraw by remember { mutableStateOf(false) }
        if (rescan!!) {
            Text(
                context.resources.getString(R.string.can_not_find_your_device),
                style = textStyle,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = Color.LightGray,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .drawWithContent {
                        if (readyToDraw) drawContent()
                    },
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.didOverflowWidth) {
                        textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.9)
                    } else {
                        readyToDraw = true
                    }
                }
            )
        } else {
            val textStyleBody2 = MaterialTheme.typography.h3
            var textStyle2 by remember { mutableStateOf(textStyleBody2) }
            var readyToDraw2 by remember { mutableStateOf(false) }
            Text(
                context.resources.getString(R.string.scanning_device),
                style = textStyle2,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = Color.LightGray,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .drawWithContent {
                        if (readyToDraw2) drawContent()
                    },
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.didOverflowWidth) {
                        textStyle2 = textStyle2.copy(fontSize = textStyle2.fontSize * 0.9)
                    } else {
                        readyToDraw2 = true
                    }
                }
            )
        }


        var lineColor by remember {
            mutableStateOf(Color.Red)
        }
        lineColor = if (rescan!!) {
            MaterialTheme.colors.primary.copy()
        } else {
            MaterialTheme.colors.onSurface.copy(0.32f)
        }

        OutlinedButton(
            enabled = rescan!!,
            onClick = { model.setReScanLiveData(false) },
            modifier = Modifier.fillMaxWidth(0.5f),
            border = BorderStroke(
                1.dp, lineColor
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Sensors,
                    contentDescription = "Localized description"
                )
                Text(text = context.resources.getString(R.string.Rescan))
            }
        }
        var isExit = false
        BackHandler(enabled = true) {
            val handler = Handler(Looper.getMainLooper())
            if ((!isExit)) {
                isExit = true
                handler.postDelayed({ isExit = false }, 1000 * 2) //x秒后没按就取消
            } else {
                MainActivity().finish()
                System.exit(0)
            }
        }
    }
}

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    circleSize: Dp = 25.dp,
    circleColor: Color = MaterialTheme.colors.primary,
    spaceBetween: Dp = 10.dp,
    travelDistance: Dp = 20.dp,
) {
    val circles = listOf(
        remember { Animatable(initialValue = 0f) },
        remember { Animatable(initialValue = 0f) },
        remember { Animatable(initialValue = 0f) },
    )

    circles.forEachIndexed { index, animatable ->
        LaunchedEffect(key1 = animatable) {
            delay(index * 200L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.0f at 0 with LinearOutSlowInEasing
                        1.0f at 300 with LinearOutSlowInEasing
                        0.0f at 600 with LinearOutSlowInEasing
                        0.0f at 1200 with LinearOutSlowInEasing
                    },
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    val circleValues = circles.map { it.value }
    val distance = with(LocalDensity.current) { travelDistance.toPx() }
    val lastCircle = circleValues.size - 1

    Row(modifier = modifier) {
        circleValues.forEachIndexed { index, value ->
            Box(modifier = Modifier
                .size(circleSize)
                .graphicsLayer { translationY = -value * distance }
                .background(color = circleColor, shape = CircleShape)
            )
            if (index != lastCircle) Spacer(modifier = Modifier.width(spaceBetween))
        }
    }
}

@Composable
fun StopLoadingAnimation(
    modifier: Modifier = Modifier,
    circleSize: Dp = 25.dp,
    circleColor: Color = MaterialTheme.colors.primary,
    spaceBetween: Dp = 10.dp,
    travelDistance: Dp = 20.dp,
) {
    val circles = listOf(
        remember { Animatable(initialValue = 0f) },
        remember { Animatable(initialValue = 1f) },
        remember { Animatable(initialValue = 0f) },
    )

    circles.forEachIndexed { index, animatable ->
        LaunchedEffect(key1 = animatable) {
            delay(index * 100L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.0f at 0 with LinearOutSlowInEasing
                        0.0f at 300 with LinearOutSlowInEasing
                        0.0f at 600 with LinearOutSlowInEasing
                        0.0f at 1200 with LinearOutSlowInEasing
                    },
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    val circleValues = circles.map { it.value }
    val distance = with(LocalDensity.current) { travelDistance.toPx() }
    val lastCircle = circleValues.size - 1

    Row(modifier = modifier) {
        circleValues.forEachIndexed { index, value ->
            Box(modifier = Modifier
                .size(circleSize)
                .graphicsLayer { translationY = -value * distance }
                .background(color = circleColor, shape = CircleShape)
            )
            if (index != lastCircle) Spacer(modifier = Modifier.width(spaceBetween))
        }
    }
}

