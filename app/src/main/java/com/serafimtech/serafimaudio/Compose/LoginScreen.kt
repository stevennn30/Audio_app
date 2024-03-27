package com.serafimtech.serafimaudio.Compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.serafimtech.serafimaudio.FileData.UserSetting
import com.serafimtech.serafimaudio.R
import com.serafimtech.serafimaudio.ViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Loginnav(model: ViewModel) {
    val navController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController = navController,
        startDestination = "Login",
        enterTransition = {//加号可以拼接两个动画
            enterAnim(Animation.anim.slide_fadein_and_fadeout_anim)
        },
        exitTransition = {
            exitAnim(Animation.anim.slide_fadein_and_fadeout_anim)
        }
    ) {
        composable("Login") {
            LoginScreen(model, navController)
        }

        composable("Register") {
            RegisterScreen(model, navController)
        }
    }
}

@OptIn(ExperimentalCoilApi::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(model: ViewModel, navController: NavHostController) {
    val focuREequester = LocalFocusManager.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var count = 0
    val AppTest by model.AppTestLiveData.observeAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clickable(indication = null,
            interactionSource = remember { MutableInteractionSource() }) {
            focuREequester.clearFocus()
        }) {

//        Image(
//            painterResource(R.drawable.sign_background),
//            contentDescription = "",
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.fillMaxSize()
//        )

        ImageOnlyBlur(
            modifier = Modifier
//                .clip(RoundedCornerShape(14.dp))
                .fillMaxSize(),
            blurhash = "|OHkCc7JJ7#mEgSgoebHS2}ExGn%WBNbjuW;j[S21c,q\$jSgs.WVayayfQ5lNakCxZxGayafWVo1NaNabGofs.j[WVjZo1OWo2R+afo1bHoLbHj@xZofR*aee:oLbHW;oLX8oLjZafnjjZfkbHbHR+WVsooLWojZn%a|Wp",
            contentDescription = "Image Blurhash Used"
        )

        Column(
            modifier = Modifier.fillMaxSize(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        )
        {
            Image(
                painterResource(R.drawable.sign_logo),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .weight(1f)
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        count++
                        if (count >= 7) {
                            UserSetting(context).test = !AppTest!!
                            model.setAppTest(!AppTest!!)
                            count = 0
                        }
                    }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    var user by rememberSaveable { mutableStateOf("") }
                    var password by rememberSaveable { mutableStateOf("") }
                    model.setUserEmail(user)
                    model.setUserPassword(password)
                    TextField(
                        value = user,
                        onValueChange = { user = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Localized description"
                            )
                        },
                        placeholder = { Text(context.resources.getString(R.string.email_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Email
                        )
                    )

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Localized description"
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text(context.resources.getString(R.string.password_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            },
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = { navController.navigate("Register") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
                        Text(text = context.resources.getString(R.string.Register))
                    }

                    OutlinedButton(onClick = { model.setLoginData(ViewModel.Login.Login) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                        border= BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text(text = context.resources.getString(R.string.Login))
                    }

                    //test
//                    Button(onClick = {
//                        model.setLoginData(ViewModel.Login.Logged);model.setReScanLiveData(false)
//                    },
//                        enabled = AppTest!!,
//                        modifier =
//                        if (AppTest == true) {
//                            Modifier.weight(0.3f)
//                        } else {
//                            Modifier.width(0.dp)
//                        },
//                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
//                        Text(text = " ")
//                    }
                }

                //guest mode
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            model.setLoginData(ViewModel.Login.Logged);
                            model.setReScanLiveData(false)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text(text = context.resources.getString(R.string.Guest_Mode))
                    }
                }
            }

            IconButton(onClick = { model.setGoogleLoginData(true) }, Modifier.weight(0.15f)) {
                Icon(painter = painterResource(R.drawable.sign_google), contentDescription = "")
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RegisterScreen(model: ViewModel, navController: NavHostController) {
    val focusRequester = LocalFocusManager.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val data by model.LoginLiveData.observeAsState()

    if (data == ViewModel.Login.Registered) {
        LaunchedEffect(data) {
            navController.navigate("Login") {
                popUpTo("Login") {
                    inclusive = true
                }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clickable(indication = null,
            interactionSource = remember { MutableInteractionSource() }) {
            focusRequester.clearFocus()
        }) {
//        Image(
//            painterResource(R.drawable.sign_background),
//            contentDescription = "",
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.fillMaxSize()
//        )

        ImageOnlyBlur(
            modifier = Modifier
//                .clip(RoundedCornerShape(14.dp))
                .fillMaxSize(),
            blurhash = "|OHkCc7JJ7#mEgSgoebHS2}ExGn%WBNbjuW;j[S21c,q\$jSgs.WVayayfQ5lNakCxZxGayafWVo1NaNabGofs.j[WVjZo1OWo2R+afo1bHoLbHj@xZofR*aee:oLbHW;oLX8oLjZafnjjZfkbHbHR+WVsooLWojZn%a|Wp",
            contentDescription = "Image Blurhash Used"
        )

        Column(
            modifier = Modifier.fillMaxSize(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painterResource(R.drawable.sign_logo),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .weight(1f)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                    var user by rememberSaveable { mutableStateOf("") }
                    var password by rememberSaveable { mutableStateOf("") }
                    var confirmPassword by rememberSaveable { mutableStateOf("") }
                    model.setUserEmail(user)
                    model.setUserPassword(password)
                    model.setUserConfirmPassword(confirmPassword)
                    TextField(
                        value = user,
                        onValueChange = { user = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Localized description"
                            )
                        },
                        placeholder = { Text(context.resources.getString(R.string.email_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                    )

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Localized description"
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text(context.resources.getString(R.string.password_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )

                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Localized description"
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text(context.resources.getString(R.string.reconfirm_password_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            },
                        )
                    )
                }

                Button(onClick = { model.setLoginData(ViewModel.Login.Register) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
                modifier = Modifier.fillMaxWidth(0.4f)) {
                    Text(text = context.resources.getString(R.string.confirm))
                }
            }
        }
    }
}

@ExperimentalCoilApi
@Composable
fun ImageOnlyBlur(
    modifier: Modifier = Modifier,
    blurhash: String,
    contentDescription: String? = null,
) {
    val bitmap = BlurhashDecoder.decode(blurhash, 4, 3)
    if (bitmap != null)
        Image(
            bitmap = bitmap.asImageBitmap(),
            modifier = modifier,
            contentScale = ContentScale.Crop,
            contentDescription = contentDescription
        )
}
