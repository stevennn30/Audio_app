package com.serafimtech.serafimaudio

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.rounded.List
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.serafimtech.serafimaudio.Compose.Animation
import com.serafimtech.serafimaudio.Compose.PlayVideo
import com.serafimtech.serafimaudio.Compose.enterAnim
import com.serafimtech.serafimaudio.Compose.exitAnim

val EQsliderRange: Float = 24.0f

@Composable
fun MainScreen(model: ViewModel, function: (Boolean) -> Unit) {
    val context = LocalContext.current
    val playvideo by model.DisplayVideoLiveData.observeAsState()

    function.invoke(!playvideo!!)

    AnimatedVisibility(
        visible = !playvideo!!,
        exit = exitAnim(Animation.anim.slide_fadein_and_fadeout_anim),
        enter = enterAnim(Animation.anim.slide_fadein_and_fadeout_anim)
    ) {
        Column() {
            EQSliderLineChart(
                model,
                Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
            )

            Divider()

            var state by remember { mutableStateOf(0) }

            Column(modifier = Modifier.weight(1f)) {
                TabRow(selectedTabIndex = state) {
                    Tab(
                        selected = state == 0,
                        onClick = { state = 0 },
                        icon = {
                            Icon(
                                Icons.Rounded.List,
                                contentDescription = "EQ"
                            )
                        },
                        text = { Text(text = context.resources.getString(R.string.equalizer)) }
                    )
                    Tab(
                        selected = state == 1,
                        onClick = { state = 1 },
                        icon = {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Setting"
                            )
                        },
                        text = { Text(text = context.resources.getString(R.string.setting)) }
                    )
                }

                if (state == 0) {
                    Box(Modifier.padding(bottom = 10.dp, start = 10.dp, end = 10.dp)) {
                        EQModePage(model)
                    }
                } else {
                    Box(Modifier.padding(30.dp)) {
                        SettingPage(model)
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = playvideo!!,
        exit = exitAnim(Animation.anim.slide_fadein_and_fadeout_anim),
        enter = enterAnim(Animation.anim.slide_fadein_and_fadeout_anim)
    ) {
        PlayVideo {
            model.setDisplayVideoData(it)
        }
    }

    BackHandler(enabled = true) {
        model.setConnectLiveData(false)
    }
}

@Composable
fun EQSliderLineChart(model: ViewModel, modifier: Modifier) {
    val data by model.UIEQLiveData.observeAsState()
    val EQEnabledData by model.UIEQEnabledLiveData.observeAsState()

    LazyRow(modifier = modifier) {
        items(15) { index ->
            var sliderPosition by remember { mutableStateOf(0f) }
            var sliding by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillParentMaxHeight()
            ) {
                Box(modifier = Modifier.fillParentMaxWidth(0.2f)) {
                    Linechart(model, index)
                }

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillParentMaxWidth(0.2f)
                        .fillParentMaxHeight()
                ) {
                    VerticalSlider(
                        value = if (!sliding) {
                            (data?.get(index) ?: 0F)
                        } else {
                            sliderPosition
                        },
                        onValueChange = {
                            sliding = true
                            sliderPosition = it

                            model.setUIEQData(index, it)
                            model.setEQCustomModeData("")
                            model.setEQDefaultModeData("")
                        },
                        onValueChangeFinished = {
                            sliding = false
                            model.setUIEQData(index, sliderPosition.toInt().toFloat())
                            model.setBLEEQSingleData(index, sliderPosition.toInt().toFloat())
                        },
                        valueRange = 0f..EQsliderRange,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            disabledActiveTrackColor = Color.Transparent,
                            disabledActiveTickColor = Color.Transparent,
                            disabledInactiveTickColor = Color.Transparent,
                            disabledInactiveTrackColor = Color.Transparent,
                        ),
                        modifier = Modifier.weight(15f),
                        enabled = EQEnabledData == true
                    )

                    val a = if (!sliding) {
                        (data?.get(index) ?: 0F)
                    } else {
                        sliderPosition
                    }.toInt()-12
                    Text(
                        text =a.toString()+"db",
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text =
                        when (index) {
                            0 -> "90Hz"
                            1 -> "130Hz"
                            2 -> "180Hz"
                            3 -> "270Hz"
                            4 -> "390Hz"
                            5 -> "570Hz"
                            6 -> "825Hz"
                            7 -> "1.2kHz"
                            8 -> "1.7kHz"
                            9 -> "2.5kHz"
                            10 -> "3.7kHz"
                            11 -> "5.4kHz"
                            12 -> "7.8kHz"
                            13 -> "11kHz"
                            14 -> "16kHz"
                            else -> "EQ"
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EQModePage(model: ViewModel) {
    val EQEnabledData by model.UIEQEnabledLiveData.observeAsState()
    val EQCustomListData by model.EQCustomListLiveData.observeAsState()
    val EQDefaultListData by model.EQDefaultListLiveData.observeAsState()
    val EQCustomModeData by model.EQCustomModeLiveData.observeAsState()
    val EQDefaultModeData by model.EQDefaultModeLiveData.observeAsState()

    var showPresetDialog by remember { mutableStateOf(false) }
    var showDeletePresetDialog by remember { mutableStateOf(false) }
    var DeleteTitle by remember { mutableStateOf("") }
    val context = LocalContext.current

    //<editor-fold desc="新增方案">
    AnimatedVisibility(showPresetDialog) {
        Dialog(
            onDismissRequest = {
                showPresetDialog = false
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                securePolicy = SecureFlagPolicy.SecureOff
            )
        ) {
            val focuREequester = LocalFocusManager.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        MaterialTheme.colors.background, shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        focuREequester.clearFocus()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                var presetname by rememberSaveable { mutableStateOf("") }
                var result by remember { mutableStateOf(0) }

                TextField(
                    value = presetname,
                    onValueChange = {
                        presetname = it
                        result = 0
                    },
                    placeholder = {
                        if (result == 0) {
                            Text(context.resources.getString(R.string.project_name))
                        } else if (result == 1) {
                            Text(context.resources.getString(R.string.NonNull))
                        } else if (result == 2) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row() {
                    Button(
                        onClick = {
                            showPresetDialog = false
                        },
                        modifier = Modifier.weight(0.9f),
                        border = BorderStroke(1.dp, Color.DarkGray),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xB0D60E0E)),
                    ) {
                        Text(text = context.resources.getString(R.string.cancel))
                    }

                    Button(
                        onClick = {
                            if (presetname != "") {
                                model.setEQCustomSingleData("Add", presetname)
                                showPresetDialog = false
                            } else {
                                result = 1
                            }
                        },
                        modifier = Modifier.weight(0.9f),
                        border = BorderStroke(1.dp, Color.DarkGray),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xB0D60E0E)),
                    ) {
                        Text(text = context.resources.getString(R.string.ok))
                    }
                }
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="刪除方案">
    AnimatedVisibility(showDeletePresetDialog) {
        Dialog(
            onDismissRequest = {
                showPresetDialog = false
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                securePolicy = SecureFlagPolicy.SecureOff
            )
        ) {
            val focuREequester = LocalFocusManager.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        MaterialTheme.colors.background, shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        focuREequester.clearFocus()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
//                Spacer(modifier = Modifier.height(50.dp))

                val textStyleBody1 = MaterialTheme.typography.h3
                var textStyle by remember { mutableStateOf(textStyleBody1) }
                var readyToDraw by remember { mutableStateOf(false) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight(0.3f)
                ) {
                    Text(

                        text = context.resources.getString(R.string.Delete),
                        style = textStyle,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,

                        color = Color.LightGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                if (readyToDraw) drawContent()
                            },
                        onTextLayout = { textLayoutResult ->
                            if (textLayoutResult.didOverflowHeight || textLayoutResult.didOverflowWidth) {
                                textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.9)
                            } else {
                                readyToDraw = true
                            }
                        }
                    )
                }

//                Spacer(modifier = Modifier.height(50.dp))
                Row() {
                    Button(
                        onClick = {
                            showDeletePresetDialog = false
                        },
                        modifier = Modifier.weight(0.9f),
                        border = BorderStroke(1.dp, Color.DarkGray),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xB0D60E0E)),
                    ) {
                        Text(text = context.resources.getString(R.string.cancel))
                    }

                    Button(
                        onClick = {
                            showDeletePresetDialog = false
                            model.setEQCustomSingleData("Remove", DeleteTitle)
                        },
                        modifier = Modifier.weight(0.9f),
                        border = BorderStroke(1.dp, Color.DarkGray),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xB0D60E0E)),
                    ) {
                        Text(text = context.resources.getString(R.string.ok))
                    }
                }
            }
        }
    }
    //</editor-fold>

    Column() {
        Row(modifier = Modifier.clickable {
            model.setBLEEQEnabled(!EQEnabledData!!)
            model.setUIEQEnabled(!EQEnabledData!!)
        }) {
            Text(
                text = "EQ",
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                textAlign = TextAlign.Start
            )
            Switch(
                checked = EQEnabledData == true,
                onCheckedChange = {
                    model.setBLEEQEnabled(it)
                    model.setUIEQEnabled(it)
                    if (!it) {
                        model.setEQCustomModeData("")
                        model.setEQDefaultModeData("")
                    }
                },
            )
        }

        Divider()

        Row(modifier = Modifier.clickable { showPresetDialog = true }) {
            Text(
                text = context.getString(R.string.add_project),
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                textAlign = TextAlign.Start
            )
            IconButton(onClick = { showPresetDialog = true }) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Localized description")
            }
        }

        Divider()

        LazyColumn(
            Modifier.fillMaxSize()
        ) {
            EQCustomListData?.forEachIndexed { index, title ->
                item {
                    Row(modifier = Modifier.clickable {
                        if (EQEnabledData == true) {
                            Log.e("EQEnabledData", "" + EQEnabledData)
                            model.setEQCustomModeData(title)
                            model.setEQDefaultModeData("")
                        }
                    }) {
                        Text(
                            text = title,
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            textAlign = TextAlign.Start
                        )
                        RadioButton(
                            selected = EQCustomModeData == title,
                            onClick = {
                                if (EQEnabledData == true) {
                                    model.setEQCustomModeData(title)
                                    model.setEQDefaultModeData("")
                                }
                            },
                        )
                        IconButton(onClick = {
                            DeleteTitle = title
                            showDeletePresetDialog = true
                        }) {
                            Icon(
                                Icons.Outlined.RemoveCircleOutline,
                                contentDescription = "Localized description"
                            )
                        }
                    }

                    Divider()
                }
            }

            EQDefaultListData?.forEachIndexed { index, title ->
                item {
                    Row(modifier = Modifier.clickable {
                        if (EQEnabledData == true) {
                            model.setEQDefaultModeData(title)
                            model.setEQCustomModeData("")
                        }
                    }) {
                        Text(
                            text = title,
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            textAlign = TextAlign.Start
                        )
                        RadioButton(
                            selected = EQDefaultModeData == title,
                            onClick = {
                                if (EQEnabledData == true) {
                                    model.setEQDefaultModeData(title)
                                    model.setEQCustomModeData("")
                                }
                            }
                        )
                    }

                    if (index < EQDefaultListData?.size!! - 1)
                        Divider()
                }
            }
        }
    }
}

@Composable
fun SettingPage(model: ViewModel) {
    val BassEnabled by model.UIBassEnabledLiveData.observeAsState()
    val BassData by model.BassLiveData.observeAsState()
    val SurroundEnabled by model.UISurroundEnabledLiveData.observeAsState()
    val SurroundData by model.SurroundLiveData.observeAsState()
    val VolumeData by model.UIVolumeLiveData.observeAsState()
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround) {
        Row() {
            var sliderPosition by remember { mutableStateOf(0f) }
            var sliding by remember { mutableStateOf(false) }

            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = context.resources.getString(R.string.surround3d))
                Switch(
                    checked = SurroundEnabled == true,
                    onCheckedChange = {
                        model.setBLESurroundEnabled(it)
                        model.setUISurroundEnabled(it)
                    }
                )
            }

            Slider(
                value = if (!sliding) {
                    SurroundData?.toFloat() ?: 0f
                } else {
                    sliderPosition
                },
                onValueChange = {
                    sliding = true
                    sliderPosition = it

                    model.setSurroundData(it.toInt())
                },
                onValueChangeFinished = {
                    sliding = false
                },
                valueRange = 0f..24f,
                modifier = Modifier.weight(3f),
                enabled = SurroundEnabled == true
            )
        }

        Row() {
            var sliderPosition by remember { mutableStateOf(0f) }
            var sliding by remember { mutableStateOf(false) }

            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = context.resources.getString(R.string.bass))
                Switch(
                    checked = BassEnabled == true,
                    onCheckedChange = {
                        model.setBLEBassEnabled(it)
                        model.setUIBassEnabled(it)
                    }
                )
            }

            Slider(
                value = if (!sliding) {
                    BassData?.toFloat() ?: 0f
                } else {
                    sliderPosition
                },
                onValueChange = {
                    sliding = true
                    sliderPosition = it

                    model.setBassData(it.toInt())
                },
                onValueChangeFinished = {
                    sliding = false
                },
                valueRange = 1f..17f,
                modifier = Modifier.weight(3f),
                enabled = BassEnabled == true
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            var sliderPosition by remember { mutableStateOf(0f) }
            var sliding by remember { mutableStateOf(false) }

            Text(
                text = context.resources.getString(R.string.volume),
                modifier = Modifier.weight(1f)
            )

            Slider(
                value = if (!sliding) {
                    VolumeData?.toFloat() ?: 0f
                } else {
                    sliderPosition
                },
                onValueChange = {
                    sliding = true
                    sliderPosition = it

                    model.setUIVolumeData(it.toInt())
                },
                onValueChangeFinished = {
                    sliding = false
                    model.setBLEVolumeData(sliderPosition.toInt())
                },
                valueRange = 0f..63f,
                modifier = Modifier.weight(3f)
            )
        }
    }
}

@Composable
fun Linechart(model: ViewModel, index: Int) {
    val data by model.UIEQLiveData.observeAsState()
    val EQEnabledData by model.UIEQEnabledLiveData.observeAsState()

    var lineColor by remember {
        mutableStateOf(Color.Red)
    }

    if (EQEnabledData == true) {
        lineColor = MaterialTheme.colors.primary.copy()
    } else {
        lineColor = MaterialTheme.colors.onSurface.copy(0.32f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxHeight(15F/17F)
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val myPath = Path()

        if (index == 0) {
            val x0 = canvasWidth / 2
            val y0 = canvasHeight * (EQsliderRange - data?.get(index)!!) / EQsliderRange
            val x1 = canvasWidth
            val y1 =
                canvasHeight * (EQsliderRange - (data?.get(index + 1)!! + data?.get(index)!!) / 2) / EQsliderRange

            drawLine(
                //左上角x=0,y=0
                start = Offset(x = x0, y = y0),
                end = Offset(x = x1, y = y1),
                color = lineColor,
                strokeWidth = 5F
            )

            myPath.moveTo(canvasWidth / 2, canvasHeight)
            myPath.lineTo(x0, y0)
            myPath.lineTo(x1, y1)
            myPath.lineTo(canvasWidth, canvasHeight)
        } else if (index == 14) {
            val x0 = 0f
            val y0 =
                canvasHeight * (EQsliderRange - (data?.get(index - 1)!! + data?.get(index)!!) / 2) / EQsliderRange
            val x1 = canvasWidth / 2
            val y1 = canvasHeight * (EQsliderRange - data?.get(index)!!) / EQsliderRange

            drawLine(
                //左上角x=0,y=0
                start = Offset(x = x0, y = y0),
                end = Offset(x = x1, y = y1),
                color = lineColor,
                strokeWidth = 5F
            )

            myPath.moveTo(0f, canvasHeight)
            myPath.lineTo(x0, y0)
            myPath.lineTo(x1, y1)
            myPath.lineTo(canvasWidth / 2, canvasHeight)
        } else {
            val x0 = 0f
            val y0 =
                canvasHeight * (EQsliderRange - (data?.get(index - 1)!! + data?.get(index)!!) / 2) / EQsliderRange
            val x1 = canvasWidth / 2
            val y1 = canvasHeight * (EQsliderRange - data?.get(index)!!) / EQsliderRange
            val x2 = canvasWidth
            val y2 =
                canvasHeight * (EQsliderRange - (data?.get(index + 1)!! + data?.get(index)!!) / 2) / EQsliderRange

            drawLine(
                //左上角x=0,y=0
                start = Offset(x = x0, y = y0),
                end = Offset(x = x1, y = y1),
                color = lineColor,
                strokeWidth = 5F
            )

            drawLine(
                //左上角x=0,y=0
                start = Offset(x = x1, y = y1),
                end = Offset(x = x2, y = y2),
                color = lineColor,
                strokeWidth = 5F
            )

            myPath.moveTo(0f, canvasHeight)
            myPath.lineTo(x0, y0)
            myPath.lineTo(x1, y1)
            myPath.lineTo(x2, y2)
            myPath.lineTo(canvasWidth, canvasHeight)
        }

        myPath.close()

        drawPath(
            path = myPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor,
                    Color.Transparent
                )
            )
        )
    }
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors(),
) {
    Slider(
        colors = colors,
        interactionSource = interactionSource,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        valueRange = valueRange,
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight,
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .then(modifier)
    )
}