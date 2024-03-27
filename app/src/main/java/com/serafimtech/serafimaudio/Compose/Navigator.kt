package com.serafimtech.serafimaudio.Compose

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.serafimtech.serafimaudio.MainScreen
import com.serafimtech.serafimaudio.ManualScreen
import com.serafimtech.serafimaudio.R
import com.serafimtech.serafimaudio.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class Navigator {
    private val _sharedFlow =
        MutableSharedFlow<NavTarget>(extraBufferCapacity = 1)
    val sharedFlow = _sharedFlow.asSharedFlow()

    fun navigateTo(navTarget: NavTarget) {
        _sharedFlow.tryEmit(navTarget)
    }

    enum class NavTarget(val label: String) {
        Scan("scan"),
        Questionnaire("questionnaire"),
        Main("Main"),
        Manual("Manual")
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationComponent(
    navController: NavHostController,
    model: ViewModel,
    navigator: Navigator,
    showTopAppBar: (Boolean) -> Unit,
    showNavigationIcon: (Boolean) -> Unit,
    TopAppBarTittle: (String) -> Unit,
) {
    LaunchedEffect("navigation") {
        navigator.sharedFlow.onEach {
            navController.navigate(it.label)
            {
                popUpTo(it.label) {
                    inclusive = true
                }
            }
        }.launchIn(this)
    }
    val context = LocalContext.current

    AnimatedNavHost(
        navController = navController,
        startDestination = Navigator.NavTarget.Scan.label,
        enterTransition = {//加号可以拼接两个动画
            enterAnim(Animation.anim.slide_fadein_and_fadeout_anim)
        },
        exitTransition = {
            exitAnim(Animation.anim.slide_fadein_and_fadeout_anim)
        }
    ) {
        composable(Navigator.NavTarget.Scan.label) {
            showTopAppBar.invoke(false)
            ScanScreen(model)
        }

        composable(Navigator.NavTarget.Questionnaire.label) {
            showTopAppBar.invoke(false)
            QuestionnaireScreen(model)
        }

        composable(Navigator.NavTarget.Main.label) {
            showNavigationIcon.invoke(false)
            TopAppBarTittle.invoke(context.resources.getString(R.string.app_name))
            MainScreen(model) { showTopAppBar.invoke(it) }
        }

        composable(Navigator.NavTarget.Manual.label) {
            showTopAppBar.invoke(true)
            showNavigationIcon.invoke(true)
            TopAppBarTittle.invoke(context.resources.getString(R.string.a1_Manual))
            ManualScreen(model)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MenuFrame(
    navController: NavHostController,
    model: ViewModel,
    navigator: Navigator,
) {
    val context = LocalContext.current
    val MacAddress by model.MacAddressLiveData.observeAsState()
    val battery by model.BatteryLiveData.observeAsState()

    val SheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    var showInformationDialog by remember {
        mutableStateOf(false)
    }
    var showTopAppBar by remember { mutableStateOf(false) }
    var TopAppBarTittle by remember { mutableStateOf("") }
    var showNavigationIcon by remember { mutableStateOf(false) }

    val IntentNotifications = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(context.resources.getString(R.string.NotificationsUrl))
    )
    val IntentContact = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(context.resources.getString(R.string.ContactUsUrl))
    )

    //<editor-fold desc="資訊欄">
    AnimatedVisibility(showInformationDialog) {
        Dialog(
            onDismissRequest = {
                showInformationDialog = false
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                securePolicy = SecureFlagPolicy.SecureOff
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 15.dp)
                    .background(
                        MaterialTheme.colors.background, shape = RoundedCornerShape(8.dp)
                    )
                    .border(0.5.dp, Color.White, shape = RoundedCornerShape(8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                var appvertion = ""
                try {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        appvertion = context.packageManager.getPackageInfo(
                            context.packageName,
                            PackageManager.PackageInfoFlags.of(0)
                        ).versionName.replace("[a-zA-Z] | =".toRegex(), "")
                    } else {
                        appvertion = context.packageManager.getPackageInfo(
                            context.packageName,
                            0
                        ).versionName.replace("[a-zA-Z] | =".toRegex(), "")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = context.resources.getString(R.string.app_version) + appvertion,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    lineHeight = 20.sp,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = context.resources.getString(R.string.Device_ID) + MacAddress,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    lineHeight = 20.sp,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Divider()

                Button(
                    onClick = {
                        showInformationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
                ) {
                    Text(text = context.resources.getString(R.string.ok))
                }
            }
        }
    }
    //</editor-fold>

    ModalBottomSheetLayout(
        sheetState = SheetState,
        sheetContent = {
            Column() {
                ListItem(
                    text = { Text(context.resources.getString(R.string.Logout)) },
                    icon = {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Localized description"
                        )
                    },
                    modifier = Modifier.clickable {
                        scope.launch { SheetState.hide() }
                        model.setLoginData(ViewModel.Login.Logout)
                        model.setConnectLiveData(false)
                    }
                )

                Divider(color = Color.DarkGray)

                ListItem(
                    text = { Text(context.resources.getString(R.string.Notifications)) },
                    icon = {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Localized description"
                        )
                    },
                    modifier = Modifier.clickable {
                        scope.launch { SheetState.hide() }
                        startActivity(context, IntentNotifications, null)
                    }
                )

                Divider(color = Color.DarkGray)

                ListItem(
                    text = { Text(context.resources.getString(R.string.Contact_us)) },
                    icon = {
                        Icon(
                            Icons.Default.ContactSupport,
                            contentDescription = "Localized description"
                        )
                    },
                    modifier = Modifier.clickable {
                        scope.launch { SheetState.hide() }
                        startActivity(context, IntentContact, null)
                    }
                )

                Divider(color = Color.DarkGray)

                ListItem(
                    text = { Text(context.resources.getString(R.string.a1_Manual)) },
                    icon = {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Localized description"
                        )
                    },
                    modifier = Modifier.clickable {
                        scope.launch { SheetState.hide() }
                        model.setPageData(Navigator.NavTarget.Manual.label)
                    }
                )

                Divider(color = Color.DarkGray)

                ListItem(
                    text = { Text(context.resources.getString(R.string.information)) },
                    icon = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Localized description"
                        )
                    },
                    modifier = Modifier.clickable {
                        scope.launch { SheetState.hide() }
                        showInformationDialog = true
                    }
                )

                Divider(color = Color.DarkGray)
            }
        },
        sheetShape = RoundedCornerShape(
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
            topStart = 12.dp,
            topEnd = 12.dp
        ),
    ) {
        Column {
            AnimatedVisibility(showTopAppBar) {
                TopAppBar(
                    title = {
                        Text(text = TopAppBarTittle)
                    },
                    navigationIcon = {
                        if (showNavigationIcon) {
                            IconButton(onClick = {
                                navController.navigateUp()
                            }) {
                                Icon(
                                    Icons.Default.KeyboardBackspace,
                                    contentDescription = "Localized description"
                                )
                            }
                        }
                    },
                    actions = {
                        Image(
                            painterResource(battery!!.id),
                            contentDescription = "",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth(0.1f)
                        )

                        IconButton(
                            onClick = {
                                scope.launch { SheetState.show() }
                            },
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }

            NavigationComponent(
                navController,
                model,
                navigator,
                { showTopAppBar = it },
                { showNavigationIcon = it },
                { TopAppBarTittle = it }
            )
        }
    }
}
