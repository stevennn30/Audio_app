package com.serafimtech.serafimaudio

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.app.ActivityCompat
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.serafimtech.serafimaudio.Compose.*
import com.serafimtech.serafimaudio.FileData.DataReadWrite
import com.serafimtech.serafimaudio.FileData.FTP
import com.serafimtech.serafimaudio.FileData.UserSetting
import com.serafimtech.serafimaudio.Service.AudioSeriesService
import com.serafimtech.serafimaudio.Service.ServiceAttribute
import com.serafimtech.serafimaudio.Service.ServiceAttribute.EXTRA_DATA
import com.serafimtech.serafimaudio.ViewModel.Login.*
import com.serafimtech.serafimaudio.ui.theme.Black
import com.serafimtech.serafimaudio.ui.theme.SerafimAudioTheme
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val model: ViewModel by viewModels()
    private lateinit var settings: UserSetting
    private lateinit var navigator: Navigator
    private lateinit var DataReadWrite: DataReadWrite
    private var mAudioService: AudioSeriesService? = null

    //<editor-fold desc="<Broadcast>">
    private var FlagFirstConnect = false
    private var FlagBattery = false

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ServiceAttribute.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(ServiceAttribute.ACTION_DATA_NOTIFY)
        intentFilter.addAction(ServiceAttribute.ACTION_GATT_DISCONNECTED)
//        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        return intentFilter
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val data: ByteArray? = intent.getByteArrayExtra(EXTRA_DATA)
            try {
                when (action) {
                    ServiceAttribute.ACTION_GATT_SERVICES_DISCOVERED -> {
                        FlagFirstConnect = true
                        Thread {
                            try {
                                Thread.sleep(1000)
                                mAudioService?.ReadSetting(0x00.toByte())
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }.start()
                    }
                    ServiceAttribute.ACTION_DATA_NOTIFY -> {
                        if (data != null) {
                            StopTimer3()

                            when (data[1]) {
                                0x02.toByte() -> {
                                    if (data[4] == 0x00.toByte()) {
                                        model.setUIVolumeData(data[6].toInt())
                                        model.setUIEQEnabled(data[9].toInt() == 1)
                                        model.setUISurroundEnabled(data[10].toInt() == 1)
                                        model.setUIBassEnabled(data[11].toInt() == 1)
                                        model.setBassData(data[12].toInt())
                                        //1
                                        Thread {
                                            try {
                                                mAudioService?.ReadEQConfig(
                                                    AudioSeriesService.EQDataMode.Bank0
                                                )
                                            } catch (e: java.lang.Exception) {
                                                e.printStackTrace()
                                            }
                                        }.start()
                                    }
                                }
                                0x04.toByte() -> {
                                    when (data[4]) {
                                        0x02.toByte() -> when (data[5]) {
                                            0x04.toByte() -> {
                                                val EQBank0: MutableList<Float> =
                                                    model.UIEQLiveData.value as MutableList<Float>

                                                for (i in 6..15) {
                                                    EQBank0[i - 6] = data[i].toFloat() / 4
                                                }

                                                model.setUIEQData(EQBank0)

                                                //2
                                                Thread {
                                                    try {
                                                        mAudioService?.ReadEQConfig(
                                                            AudioSeriesService.EQDataMode.Bank1
                                                        )
                                                    } catch (e: java.lang.Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }.start()
                                            }
                                            0x05.toByte() -> {
                                                val EQBank1: MutableList<Float> =
                                                    model.UIEQLiveData.value as MutableList<Float>

                                                for (i in 6..10) {
                                                    EQBank1[i + 4] = data[i].toFloat() / 4
                                                }

                                                model.setUIEQData(EQBank1)
                                                //3
                                                if (FlagFirstConnect) {
                                                    Thread {
                                                        try {
                                                            mAudioService?.Read3DConfig()
                                                        } catch (e: java.lang.Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }.start()
                                                }
                                            }
                                        }
                                        0x01.toByte() -> Thread {
                                            try {
                                                mAudioService?.ReadEQConfig(
                                                    AudioSeriesService.EQDataMode.Bank0
                                                )
                                            } catch (e: java.lang.Exception) {
                                                e.printStackTrace()
                                            }
                                        }.start()
                                    }
                                }
                                0x05.toByte() -> {
                                    if (data[4] == 0x03.toByte()) {
                                        if (FlagFirstConnect) {
//                                            StopTimer3()

                                            model.setSurroundData(data[5] / 0x05)
                                            model.setConnectLiveData(true)
                                            FlagFirstConnect = false
                                            StartTimer()
                                            FlagBattery = true
                                        }
                                    }
                                }
                                0x06.toByte() -> {
                                    if (data[4] == 0x03.toByte()) {
//                                        bleViewModel.bassConfig.postValue(data[5].toInt())
                                    }
                                }
                                0x07.toByte() -> {
                                    Log.e("data[]", "0x07")
                                    if (data[2] == 0x00.toByte()) {
                                        if (data[3] == 0x03.toByte()) {
                                            val batt_level: Double =
                                                (data[6].toInt() shl 8 or (data[5].toInt() and 0xff) and 0xffff).toDouble() * 1.25 / 511 * 4
                                            val charge_state = "" + data[4]
                                            Log.e(charge_state, "")
                                            val battlevel2 = (batt_level - 3) / 1.2 * 100
                                            if (charge_state.contains("0")) {
                                                if (battlevel2 >= 80) {
                                                    model.setBatteryData(ViewModel.Battery.battery_full)
                                                } else if (battlevel2 < 80 && battlevel2 >= 60) {
                                                    model.setBatteryData(ViewModel.Battery.battery_reduce)
                                                } else if (battlevel2 < 60 && battlevel2 >= 30) {
                                                    model.setBatteryData(ViewModel.Battery.battery_half)
                                                } else if (battlevel2 < 30 && battlevel2 >= 15) {
                                                    model.setBatteryData(ViewModel.Battery.battery_low)
                                                } else if (battlevel2 < 15) {
                                                    model.setBatteryData(ViewModel.Battery.battery_out)
                                                }
                                            } else if (charge_state.contains("1")) {
                                                model.setBatteryData(ViewModel.Battery.battery_energy)
                                            }
                                        }
                                    }
                                }
                                0x09.toByte() -> {
                                    when (data[4]) {
                                        0x07.toByte() -> model.setUIVolumeData(data[5].toInt())
                                        0x05.toByte() -> model.setUISurroundEnabled(data[5].toInt() == 1)
                                        0x06.toByte() -> model.setUIBassEnabled(data[5].toInt() == 1)
                                    }
                                }
                            }
                        }
                    }
                    ServiceAttribute.ACTION_GATT_DISCONNECTED -> {
                        if (mAudioService!!.mConnectionState == BluetoothProfile.STATE_DISCONNECTED) {
                            model.setConnectLiveData(false)
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        Log.e(
                            "ACTION_STATE_CHANGED",
                            "" + (intent.getIntExtra(
                                BluetoothAdapter.EXTRA_STATE,
                                -1
                            ) == BluetoothAdapter.STATE_OFF)
                        )
                        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                            == BluetoothAdapter.STATE_OFF
                        ) {
                            model.setConnectLiveData(false)
                            if (!(getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled) {
                                if (ActivityCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    return
                                }
                                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            }
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="<Activity>">
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = UserSetting(baseContext)
        navigator = Navigator()
        DataReadWrite = DataReadWrite(this)

        DownloadVideo()
        DownloadPdf()
        DownloadPreset()
        VideoDay()

        Permissionsrequest()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter(), RECEIVER_EXPORTED)
        }else{
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        }

        Observe()
        FirebaseSignIn()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        }

        model.setAppTest(settings.test)

        bindService()

        setContent {
            val navController = rememberAnimatedNavController()
            val data by model.LoginLiveData.observeAsState()

            SerafimAudioTheme {
                //頂部狀態列與底部導航欄顏色控制
                rememberSystemUiController().run {
                    setStatusBarColor(Black, false)
//                    setSystemBarsColor(MaterialTheme.colors.primary, false)
                    setNavigationBarColor(Black, false)
                }

                Surface() {
                    AnimatedVisibility(
                        visible = data == Logged,
                        exit = exitAnim(Animation.anim.slide_fadein_and_fadeout_anim),
                        enter = enterAnim(Animation.anim.slide_fadein_and_fadeout_anim)
                    )
                    {
                        MenuFrame(navController, model, navigator)
                    }

                    AnimatedVisibility(
                        visible = data != Logged,
                        exit = exitAnim(Animation.anim.slide_fadein_and_fadeout_anim),
                        enter = enterAnim(Animation.anim.slide_fadein_and_fadeout_anim)
                    ) {
                        Loginnav(model)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser

        if (currentUser == null) {
            //OneTap()
        } else {
            model.setLoginData(Logged)
        }
    }

    //bluetooth
    override fun onResume() {
        super.onResume()
        if (!(getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        if (mAudioService != null) {
            if (mAudioService!!.mConnectionState == BluetoothProfile.STATE_CONNECTED
                && !mAudioService!!.mBusy
            ) {
                StartTimer()
                StartTimer3()
            }
        }

        if (model.LoginLiveData.value == Logged && model.PageLiveData.value == Navigator.NavTarget.Scan.label) {
            model.setReScanLiveData(false)
        }
    }

    //git test
    
    override fun onPause() {
        super.onPause()
//        model.setConnectLiveData(false)
        StopTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindservice()
        unregisterReceiver(mGattUpdateReceiver)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val username = credential.id
                    val password = credential.password
                    when {
                        idToken != null -> {
                            Log.d(TAG, "Got ID token.")

                            // Got an ID token from Google. Use it to authenticate
                            // with Firebase.
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d(TAG, "signInWithCredential:success")
                                        val user = auth.currentUser
                                        model.setLoginData(Logged)
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                                        model.setLoginData(Logout)
                                    }
                                }
                        }
                        password != null -> {
                            // Got a saved username and password. Use them to authenticate
                            // with your backend.
                            Log.d(TAG, "Got password.")
                        }
                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token or password!")
                        }
                    }
                } catch (e: ApiException) {
                    // ...
                }
            }
        }
    }

    private fun Observe() {
        //<editor-fold desc="<A1>">
        model.ReScanLiveData.observe(this) {
            if (it == false) {
//                StartTimer3()

                if (mAudioService != null) {
                    startscan()
                }
            }
        }

        model.BLEEQSingleLiveData.observe(this) {
            if (mAudioService != null) {
                if (!mAudioService?.mBusy!!) {
                    Thread {
                        val b = ByteArray(2)
                        b[0] = (it[0].toInt() + 1).toByte()
                        b[1] = (it[1].toInt() * 4).toByte()
                        mAudioService!!.WriteEQConfig(
                            AudioSeriesService.EQDataMode.Single,
                            b
                        )
                        mAudioService!!.WriteEQConfig(
                            AudioSeriesService.EQDataMode.Update,
                            byteArrayOf()
                        )
                    }.start()
                }
            }
        }

        model.BLEEQLiveData.observe(this) {
            val b1 = ByteArray(10)
            val b2 = ByteArray(5)
            for (i in 0..14) {
                val progress = it[i].toInt()
                if (i < 10) {
                    b1[i] = (progress * 4).toByte()
                } else {
                    b2[i - 10] = (progress * 4).toByte()
                }
            }
            if (mAudioService != null) {
                if (!mAudioService?.mBusy!!) {
                    Thread {
                        mAudioService?.WriteEQConfig(AudioSeriesService.EQDataMode.Bank0, b1)
                        mAudioService?.WriteEQConfig(AudioSeriesService.EQDataMode.Bank1, b2)
                        mAudioService?.WriteEQConfig(
                            AudioSeriesService.EQDataMode.Update,
                            byteArrayOf()
                        )
                    }.start()
                }
            }
        }

        model.EQCustomSingleLiveData.observe(this) {
            if (it[0] == "Add") {
                var PresetJSONObject = JSONObject()
                val jsonArray = JSONArray()

                for (integer in model.UIEQLiveData.value!!) {
                    jsonArray.put(integer)
                }

                if (!DataReadWrite.ReadFile(
                        DataReadWrite.DirCustomProgram,
                        DataReadWrite.FileCustom
                    ).equals("")
                ) {
                    PresetJSONObject = JSONObject(
                        DataReadWrite.ReadFile(
                            DataReadWrite.DirCustomProgram,
                            DataReadWrite.FileCustom
                        )
                    )
                }

                if (PresetJSONObject.has(it[1])) {
                    PresetJSONObject.remove(it[1])
                }
                PresetJSONObject.put(it[1], jsonArray)
                DataReadWrite.WriteFile(
                    PresetJSONObject.toString(),
                    DataReadWrite.DirCustomProgram,
                    DataReadWrite.FileCustom
                )

                model.setEQCustomListData(initEQmode(true) as List<String>)
                model.setEQCustomModeData(it[1])
                model.setEQDefaultModeData("")
            } else if (it[0] == "Remove") {
                try {
                    val PresetJSONObject = JSONObject(
                        DataReadWrite.ReadFile(
                            DataReadWrite.DirCustomProgram,
                            DataReadWrite.FileCustom
                        )
                    )
                    PresetJSONObject.remove(it[1])
                    DataReadWrite.WriteFile(
                        PresetJSONObject.toString(),
                        DataReadWrite.DirCustomProgram,
                        DataReadWrite.FileCustom
                    )
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

                model.setEQCustomListData(initEQmode(true) as List<String>)

                if (model.EQCustomModeLiveData.value == it[1]) {
                    model.setEQCustomModeData("")
                }
            }
        }

        model.EQCustomModeLiveData.observe(this) {
            if (it != "") {
                val jsonObject: JSONObject
                var jsonArray: JSONArray? = null

                try {
                    jsonObject =
                        JSONObject(
                            DataReadWrite.ReadFile(
                                DataReadWrite.DirCustomProgram,
                                DataReadWrite.FileCustom
                            )
                        )
                    jsonArray = jsonObject.getJSONArray(it)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                val EQmodedata = mutableListOf<Float>()
                for (i in 0..14) {
                    var progress = 0
                    try {
                        progress = jsonArray!!.getInt(i)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    EQmodedata.add(progress.toFloat())
                }
                model.setUIEQData(EQmodedata)
                model.setBLEEQData(EQmodedata)
            }
        }

        model.EQDefaultModeLiveData.observe(this) {
            if (it != "") {
                val jsonObject: JSONObject
                var jsonArray: JSONArray? = null

                try {
                    jsonObject = JSONObject(
                        DataReadWrite.ReadFile(
                            DataReadWrite.DirPresetProgram,
                            DataReadWrite.FilePreset
                        )
                    )
                    jsonArray = jsonObject.getJSONArray(it)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                val data = mutableListOf<Float>()
                for (i in 0..14) {
                    var progress = 0
                    try {
                        progress = jsonArray!!.getInt(i)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    data.add(progress.toFloat())
                }
                model.setUIEQData(data)
                model.setBLEEQData(data)
            }
        }

        model.BLEVolumeLiveData.observe(this) {
            if (mAudioService != null) {
                if (!mAudioService?.mBusy!!) {
                    Thread { mAudioService?.ControlIndependentVolume(it.toByte()) }.start()
                }
            }
        }

        model.BLEEQEnabledLiveData.observe(this) {
            if (mAudioService != null) {
                if (!mAudioService?.mBusy!!) {
                    Thread { mAudioService?.EnabledEQ(it) }.start()
                }
            }
        }

        model.BassLiveData.observe(this) {
            if (model.ConnectLiveData.value == true) {
                if (mAudioService != null) {
                    if (!mAudioService?.mBusy!!) {
                        Thread { mAudioService?.WriteBassBoostConfig(it.toByte()) }.start()
                    }
                }
            }
        }

        model.BLEBassEnabledLiveData.observe(this) {
            if (mAudioService != null) {
                if (!mAudioService?.mBusy!!) {
                    Thread { mAudioService?.EnabledBassBoost(it) }.start()
                }
            }
        }

        model.SurroundLiveData.observe(this) {
            if (model.ConnectLiveData.value == true) {
                if (mAudioService != null) {
                    if (!mAudioService?.mBusy!!) {
                        val b = ByteArray(3)
                        b[0] = (0x05 * it).toByte()
                        b[1] = (0x55 * it).toByte()
                        b[2] = (0x55 * it).toByte()
                        Thread { mAudioService?.Write3DConfig(b) }.start()
                    }
                }
            }
        }

        model.BLESurroundEnabledLiveData.observe(this) {
            if (mAudioService != null) {
                if (!mAudioService?.mBusy!!) {
                    Thread { mAudioService?.Enabled3DEffect(it) }.start()
                }
            }
        }

        model.ConnectLiveData.observe(this) {
            if (it) {
                if (model.QuestionnaireLiveData.value!!) {
                    model.setPageData(Navigator.NavTarget.Main.label)
                } else {
                    model.setPageData(Navigator.NavTarget.Questionnaire.label)
                    StartTimer2()
                }
            } else {
                FlagBattery = false
                StopTimer()
                BluetoothProfile.STATE_DISCONNECTED
                model.setPageData(Navigator.NavTarget.Scan.label)
//                model.setReScanLiveData(true)
                bindService()
            }
        }
        //</editor-fold>

        model.PageLiveData.observe(this) {
            if (it == Navigator.NavTarget.Main.label) {
                StopTimer2()
            }


            Navigator.NavTarget.values().forEachIndexed { _, navTarget ->
                if (navTarget.label == it) {
                    navigator.navigateTo(navTarget)
                }
            }

        }

        model.LoginLiveData.observe(this) {
            if (it == Logout) {
                Firebase.auth.signOut()
                unbindservice()
            }

            if (it == Register) {
                Log.e(TAG, "Observe: 點選註冊按紐")
                FirebaseCreateUserWithEmailAndPassword()
            }

            if (it == Login) {
                Log.e(TAG, "Observe: 點選登入按紐")
                FirebaseLoginWithEmailwithPassword()
            }

            if (it == Logged) {
                if (mAudioService != null) {
                    model.setReScanLiveData(false)
                }
            }
        }

        model.AppTestLiveData.observe(this) {
            Log.e(TAG, "Observe: APP測試$it")
            DataReadWrite.WriteFile(it.toString(), "Apptest", "Apptest")
        }

        model.GoogleLoginLiveData.observe(this) {
            if (it) {
                model.setGoogleLoginData(false)
                OneTap()
                //OneTapGoogleSignIn()
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="<Login>">
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var auth: FirebaseAuth
    private val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity
    private lateinit var signUpRequest: BeginSignInRequest

    private fun FirebaseSignIn() {
        auth = Firebase.auth
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build()
            )
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    //.setServerClientId(getString(R.string.default_web_client_id))
                    .setServerClientId("1023854033080-sk1l24162q3e340kvtl6v2qmtppq8g3q.apps.googleusercontent.com")
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(false)
            .build()
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    //.setServerClientId(getString(R.string.default_web_client_id))
                    .setServerClientId("1023854033080-sk1l24162q3e340kvtl6v2qmtppq8g3q.apps.googleusercontent.com")
                    // Show all accounts on the device.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
    }

    private fun OneTap() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                e.localizedMessage?.let { Log.d(TAG, it) }
            }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult(), ::handleSignInResult)

    private fun handleSignInResult(result: ActivityResult) {

        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        Log.d(TAG, "handleSignInResult() task = $task")
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            //val account = task.getResult(ApiException::class.java)
            //Log.d(TAG, "handleSignInResult() called with: result = $account")
            //val account = task.result
            Log.d(TAG, "handleSignInResult() called with: result")
        } catch (ex: ApiException) {
            Log.e(TAG, "handleSignInResult: ", ex)
        }

    }

    val intentSender: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->
            val data = activityResult.data
            val credential: SignInCredential =
                oneTapClient.getSignInCredentialFromIntent(data)
            Log.d("Credential", credential.googleIdToken.toString())
        }

    private val oneTapSignInResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "One-tap result_ok ")
                try {
                    /*
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val username = credential.id
                    val password = credential.password
                    when {
                        idToken != null -> {
                            // Got an ID token from Google. Use it to authenticate
                            // with your backend.
                            Log.d(TAG, "Got ID token.")
                        }
                        password != null -> {
                            // Got a saved username and password. Use them to authenticate
                            // with your backend.
                            Log.d(TAG, "Got password.")
                        }
                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token or password!")
                        }
                    }
                    */

                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                            // Don't re-prompt the user.
                            //showOneTapUI = false
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
                            // Try again or just ignore.
                        }
                        else -> {
                            Log.d(TAG, "Couldn't get credential from result." +
                                    " (${e.localizedMessage})")
                        }
                    }
                }
            }
        }

    private fun OneTapGoogleSignIn() {

        val request = GetSignInIntentRequest.builder()
            //.setServerClientId(getString(R.string.default_web_client_id))
            .setServerClientId("1023854033080-sk1l24162q3e340kvtl6v2qmtppq8g3q.apps.googleusercontent.com")
            .build()
/*
        oneTapClient.getSignInIntent(request)
            .addOnSuccessListener { result ->
                val intentSenderRequest = IntentSenderRequest.Builder(result).build()
                launcher.launch(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OneTapGoogleSignIn: ",e )
            }
*/

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        //launcher.launch(intentSenderRequest)
                        oneTapSignInResultLauncher.launch(intentSenderRequest)

                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                e.localizedMessage?.let { Log.d(TAG, it) }
            }

/*
        oneTapClient.getSignInIntent(request)
            .addOnSuccessListener { result ->
                val intentSenderRequest = IntentSenderRequest.Builder(result).build()
                launcher.launch(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OneTapGoogleSignIn: ",e )
            }
*/
/*
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    intentSender.launch(
                        IntentSenderRequest
                            .Builder(result.pendingIntent.intentSender)
                            .build()
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(
                        "SignUp UI",
                        "Couldn't start One Tap UI: ${e.localizedMessage}"
                    )
                }
            }
            .addOnFailureListener(this) { e ->
                // Maybe no Google Accounts found
                Log.d("SignUp UI", e.localizedMessage ?: "")
            }
*/
    }

    private fun FirebaseLoginWithEmailwithPassword() {
        val user = model.UserEmailLiveData.value
        val password = model.UserPasswordLiveData.value
        if (user != null && password != null) {
            if (user.isEmpty()) {
                Toast.makeText(
                    this,
                    resources.getString(R.string.no_email_input),
                    Toast.LENGTH_SHORT
                ).show()
            } else if (password.isEmpty()) {
                Toast.makeText(
                    this,
                    resources.getString(R.string.no_password_input),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                auth.signInWithEmailAndPassword(user, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (!auth.currentUser?.isEmailVerified!!) {
                            Toast.makeText(
                                this,
                                resources.getString(R.string.please_verify_email),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                resources.getString(R.string.email_login_successful),
                                Toast.LENGTH_SHORT
                            ).show()
                            model.setLoginData(Logged)
                        }
                    } else {
                        var error = it.exception.toString()
                        error = error.replaceBefore(": ", "")
                            .replaceFirst(": ", "")//替換無關字串，只顯示容易閱讀的部分

                        when (error) {
                            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                                Toast.makeText(
                                    this,
                                    resources.getString(R.string.no_user),
                                    Toast.LENGTH_SHORT
                                ).show()
                            "The password is invalid or the user does not have a password." ->
                                Toast.makeText(
                                    this,
                                    resources.getString(R.string.password_mistake),
                                    Toast.LENGTH_SHORT
                                ).show()
                            "The email address is badly formatted." ->
                                Toast.makeText(
                                    this,
                                    resources.getString(R.string.email_address_badly),
                                    Toast.LENGTH_SHORT
                                ).show()
                            else ->
                                Toast.makeText(
                                    this,
                                    resources.getString(R.string.email_login_fail),
                                    Toast.LENGTH_SHORT
                                ).show()
                        }
                    }
                }
            }
        }
    }

    private fun FirebaseCreateUserWithEmailAndPassword() {
        if (model.UserEmailLiveData.value!!.isEmpty()) {
            Toast.makeText(
                this, resources.getString(R.string.no_email_input),
                Toast.LENGTH_SHORT
            ).show()
        } else if (model.UserPasswordLiveData.value!!.isEmpty()) {
            Toast.makeText(
                this,
                resources.getString(R.string.no_password_input),
                Toast.LENGTH_SHORT
            ).show()
        } else if (model.UserConfirmPasswordLiveData.value!!.isEmpty()) {
            Toast.makeText(
                this, getString(R.string.password_reconfirm),
                Toast.LENGTH_SHORT
            ).show()
        } else if (model.UserPasswordLiveData.value!!.length < 6) {
            Toast.makeText(this, getString(R.string.password_length), Toast.LENGTH_SHORT).show()
        } else if (model.UserPasswordLiveData.value!! != model.UserConfirmPasswordLiveData.value!!) {
            Toast.makeText(this, getString(R.string.reconfirm_failed), Toast.LENGTH_SHORT).show()
//                }else if (rpManager.isConnected) {
//                    Toast.makeText(this,"no_internet_connected",Toast.LENGTH_SHORT).show()
        } else {
            auth.createUserWithEmailAndPassword(
                model.UserEmailLiveData.value!!,
                model.UserPasswordLiveData.value!!
            ).addOnCompleteListener {
                if (it.isSuccessful) {
                    var user = auth.currentUser
                    user!!.sendEmailVerification().addOnCompleteListener() {
                        user = if (it.isSuccessful) {
                            Log.d(TAG, "signInWithCredential:success")
                            model.setLoginData(Registered)
                            //                Text(text = "Verification email sent to :")
//                Spacer(modifier = Modifier.height(10.dp))
//                Text(model.UserEmailLiveData.value!!)
                            Toast.makeText(
                                this,
                                "Verification email sent to :" + model.UserEmailLiveData.value,
                                Toast.LENGTH_SHORT
                            ).show()
                            null
                        } else {
                            Toast.makeText(this, "verification_send_fail", Toast.LENGTH_SHORT)
                                .show()
                            null
                        }
                    }
                    // Sign in success, update UI with the signed-in user's information
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(
                        this,
                        "Failed to register. Please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                auth.signOut()
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="<DownloadData>">
    private fun DownloadPreset() {
        val PresetJSONObject = JSONObject()
        val DefaultPreset = JSONArray()
        val SerafimPreset = JSONArray()
        val RockPreset = JSONArray()
        val PopPreset = JSONArray()
        val JazzPreset = JSONArray()
        val BluesPreset = JSONArray()
        val ClassicalPreset = JSONArray()
        val ReggaePreset = JSONArray()
        val HipHopPreset = JSONArray()
        val RapPreset = JSONArray()
        try {
            for (i in 0..14) {
                DefaultPreset.put(i, 12)
            }

            //<editor-fold desc="<Serafim>">
            SerafimPreset.put(0, 15)
            SerafimPreset.put(1, 15)
            SerafimPreset.put(2, 17)
            SerafimPreset.put(3, 14)
            SerafimPreset.put(4, 13)
            SerafimPreset.put(5, 13)
            SerafimPreset.put(6, 12)
            SerafimPreset.put(7, 12)
            SerafimPreset.put(8, 12)
            SerafimPreset.put(9, 13)
            SerafimPreset.put(10, 13)
            SerafimPreset.put(11, 13)
            SerafimPreset.put(12, 15)
            SerafimPreset.put(13, 14)
            SerafimPreset.put(14, 14)
            //</editor-fold>

            //<editor-fold desc="<T1Rock>">
            RockPreset.put(0, 11)
            RockPreset.put(1, 12)
            RockPreset.put(2, 13)
            RockPreset.put(3, 14)
            RockPreset.put(4, 15)
            RockPreset.put(5, 15)
            RockPreset.put(6, 12)
            RockPreset.put(7, 11)
            RockPreset.put(8, 11)
            RockPreset.put(9, 11)
            RockPreset.put(10, 12)
            RockPreset.put(11, 12)
            RockPreset.put(12, 13)
            RockPreset.put(13, 16)
            RockPreset.put(14, 15)
            //</editor-fold>

            //<editor-fold desc="<T1Pop>">
            PopPreset.put(0, 14)
            PopPreset.put(1, 16)
            PopPreset.put(2, 15)
            PopPreset.put(3, 14)
            PopPreset.put(4, 13)
            PopPreset.put(5, 11)
            PopPreset.put(6, 10)
            PopPreset.put(7, 9)
            PopPreset.put(8, 12)
            PopPreset.put(9, 13)
            PopPreset.put(10, 13)
            PopPreset.put(11, 14)
            PopPreset.put(12, 14)
            PopPreset.put(13, 15)
            PopPreset.put(14, 15)
            //</editor-fold>

            //<editor-fold desc="<T1Jazz>">
            JazzPreset.put(0, 12)
            JazzPreset.put(1, 12)
            JazzPreset.put(2, 12)
            JazzPreset.put(3, 12)
            JazzPreset.put(4, 13)
            JazzPreset.put(5, 14)
            JazzPreset.put(6, 14)
            JazzPreset.put(7, 14)
            JazzPreset.put(8, 14)
            JazzPreset.put(9, 13)
            JazzPreset.put(10, 13)
            JazzPreset.put(11, 13)
            JazzPreset.put(12, 14)
            JazzPreset.put(13, 15)
            JazzPreset.put(14, 15)
            //</editor-fold>

            //<editor-fold desc="<T1Blues>">
            BluesPreset.put(0, 11)
            BluesPreset.put(1, 11)
            BluesPreset.put(2, 12)
            BluesPreset.put(3, 14)
            BluesPreset.put(4, 14)
            BluesPreset.put(5, 13)
            BluesPreset.put(6, 12)
            BluesPreset.put(7, 12)
            BluesPreset.put(8, 12)
            BluesPreset.put(9, 12)
            BluesPreset.put(10, 12)
            BluesPreset.put(11, 13)
            BluesPreset.put(12, 12)
            BluesPreset.put(13, 11)
            BluesPreset.put(14, 11)
            //</editor-fold>

            //<editor-fold desc="<T1Classical>">
            ClassicalPreset.put(0, 12)
            ClassicalPreset.put(1, 14)
            ClassicalPreset.put(2, 16)
            ClassicalPreset.put(3, 16)
            ClassicalPreset.put(4, 16)
            ClassicalPreset.put(5, 14)
            ClassicalPreset.put(6, 12)
            ClassicalPreset.put(7, 12)
            ClassicalPreset.put(8, 12)
            ClassicalPreset.put(9, 12)
            ClassicalPreset.put(10, 12)
            ClassicalPreset.put(11, 12)
            ClassicalPreset.put(12, 12)
            ClassicalPreset.put(13, 13)
            ClassicalPreset.put(14, 13)
            //</editor-fold>

            //<editor-fold desc="<T1Reggae>">
            ReggaePreset.put(0, 12)
            ReggaePreset.put(1, 12)
            ReggaePreset.put(2, 12)
            ReggaePreset.put(3, 12)
            ReggaePreset.put(4, 12)
            ReggaePreset.put(5, 10)
            ReggaePreset.put(6, 12)
            ReggaePreset.put(7, 13)
            ReggaePreset.put(8, 14)
            ReggaePreset.put(9, 15)
            ReggaePreset.put(10, 14)
            ReggaePreset.put(11, 12)
            ReggaePreset.put(12, 14)
            ReggaePreset.put(13, 14)
            ReggaePreset.put(14, 13)
            //</editor-fold>

            //<editor-fold desc="<T1HipHop>">
            HipHopPreset.put(0, 13)
            HipHopPreset.put(1, 14)
            HipHopPreset.put(2, 15)
            HipHopPreset.put(3, 15)
            HipHopPreset.put(4, 14)
            HipHopPreset.put(5, 12)
            HipHopPreset.put(6, 12)
            HipHopPreset.put(7, 11)
            HipHopPreset.put(8, 11)
            HipHopPreset.put(9, 11)
            HipHopPreset.put(10, 12)
            HipHopPreset.put(11, 13)
            HipHopPreset.put(12, 13)
            HipHopPreset.put(13, 13)
            HipHopPreset.put(14, 13)
            //</editor-fold>

            //<editor-fold desc="<T1Rap>">
            RapPreset.put(0, 11)
            RapPreset.put(1, 12)
            RapPreset.put(2, 12)
            RapPreset.put(3, 13)
            RapPreset.put(4, 14)
            RapPreset.put(5, 14)
            RapPreset.put(6, 12)
            RapPreset.put(7, 11)
            RapPreset.put(8, 11)
            RapPreset.put(9, 12)
            RapPreset.put(10, 12)
            RapPreset.put(11, 13)
            RapPreset.put(12, 15)
            RapPreset.put(13, 16)
            RapPreset.put(14, 17)
            //</editor-fold>
            PresetJSONObject.put(getString(R.string.Game), DefaultPreset)
            PresetJSONObject.put(getString(R.string.Serafim), SerafimPreset)
            PresetJSONObject.put(getString(R.string.Rock), RockPreset)
            PresetJSONObject.put(getString(R.string.Popular), PopPreset)
            PresetJSONObject.put(getString(R.string.Jazz), JazzPreset)
            PresetJSONObject.put(getString(R.string.Blues), BluesPreset)
            PresetJSONObject.put(getString(R.string.Classical), ClassicalPreset)
            PresetJSONObject.put(getString(R.string.Reggae), ReggaePreset)
            PresetJSONObject.put(getString(R.string.HipHop), HipHopPreset)
            PresetJSONObject.put(getString(R.string.Rap), RapPreset)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        DataReadWrite.WriteFile(
            PresetJSONObject.toString(),
            DataReadWrite.DirPresetProgram,
            DataReadWrite.FilePreset
        )
    }

    private fun DownloadVideo() {
        val ftp = FTP()
        Thread {
            for (i in 1..3) {
                Log.e(TAG, "DownloadVideo測試測試測試:$i")
                val remoteFile = File("/Serafim_Audio/Video/0$i.mp4")
                val file1 = File(DataReadWrite.WriteFiledir("video"), "0$i.mp4")
                try {
                    if (ftp.getFileSize(remoteFile.path) != DataReadWrite.getFileSize(file1) && !file1.isDirectory) {
                        ftp.downLoadFile(remoteFile, file1)
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun DownloadPdf() {
        val ftp = FTP()
        Thread {
            val remoteFile = File("/Serafim_Audio/manual.pdf")
            val file1 = File(DataReadWrite.WriteFiledir("manual"), "manual.pdf")
            try {
                if (ftp.getFileSize(remoteFile.path) != DataReadWrite.getFileSize(file1) && !file1.isDirectory) {
                    ftp.downLoadFile(remoteFile, file1)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun initEQmode(Custom: Boolean): Collection<String> {
        val EQModeName = mutableListOf<String>()

        try {
            val keys: Iterator<String?>

            if (Custom) {
                if (!DataReadWrite.ReadFile(
                        DataReadWrite.DirCustomProgram,
                        DataReadWrite.FileCustom
                    ).equals("")
                ) {
                    val CustomJSONObject =
                        JSONObject(
                            DataReadWrite.ReadFile(
                                DataReadWrite.DirCustomProgram,
                                DataReadWrite.FileCustom
                            )
                        )
                    keys = CustomJSONObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()!!
                        EQModeName.add(key)
                    }
                } else {
                    return EQModeName
                }
            } else {
                val PresetJSONObject =
                    JSONObject(
                        DataReadWrite.ReadFile(
                            DataReadWrite.DirPresetProgram,
                            DataReadWrite.FilePreset
                        )
                    )
                keys = PresetJSONObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key != null) {
                        EQModeName.add(key)
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return EQModeName
    }
    //</editor-fold>

    //<editor-fold desc="<Permission>">
    @RequiresApi(Build.VERSION_CODES.S)
    private fun Permissionsrequest() {
        val requestList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            requestList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            requestList.add(Manifest.permission.BLUETOOTH)
            requestList.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestList.add(Manifest.permission.BLUETOOTH_SCAN)
            requestList.add(Manifest.permission.BLUETOOTH_CONNECT)
//            requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        if (hasPermissions(this, *requestList.toTypedArray())) {
            Log.d("Permissionsrequest", "hasPermissions")
            model.setEQCustomListData(initEQmode(true) as List<String>)
            model.setEQDefaultListData(initEQmode(false) as List<String>)
        } else {
            //ActivityCompat.requestPermissions(this, requestList.toTypedArray(), 1)
            Log.d("Permissionsrequest", "Enter requestMultiplePermissions")
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissions ->
            permissions.entries.forEach {
                //Log.d("DEBUG", "${it.key} = ${it.value}")
                when (it.key) {
                    Manifest.permission.ACCESS_FINE_LOCATION -> {}
                    Manifest.permission.BLUETOOTH_SCAN -> {

                        if (it.value){
                            DownloadVideo()
                            DownloadPreset()
                            model.setEQCustomListData(initEQmode(true) as List<String>)
                            model.setEQDefaultListData(initEQmode(false) as List<String>)
                        } else {
                            Toast.makeText(this, "need your permission", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Manifest.permission.BLUETOOTH_CONNECT -> {}
                }
            }

    }
/*
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        Log.e("requestCode", "" + requestCode)
        Log.e("permissions", "" + permissions.toString())
        Log.e("grantResults", "" + grantResults.toString())

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DownloadVideo()
                DownloadPreset()
                model.setEQCustomListData(initEQmode(true) as List<String>)
                model.setEQDefaultListData(initEQmode(false) as List<String>)
            } else {
                Toast.makeText(this, "need your permission", Toast.LENGTH_SHORT).show()
            }
        }
    }
*/
    //</editor-fold>

    //<editor-fold desc="<Service>">
    private var FlagService = false

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, service: IBinder) {
            try {
                mAudioService = (service as AudioSeriesService.LocalBinder).service
                if (!mAudioService!!.initialize()) {
                    bindService()
                } else {
                    if (model.LoginLiveData.value == Logged)
                        model.setReScanLiveData(false)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            mAudioService = null
        }
    }

    private fun bindService() {
        try {
            unbindservice()
            Handler(Looper.getMainLooper()).postDelayed({
                FlagService = bindService(
                    Intent(this, AudioSeriesService::class.java),
                    mServiceConnection,
                    BIND_AUTO_CREATE
                )
            }, 1000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unbindservice() {
        if (FlagService) {
            unbindService(mServiceConnection)
            FlagService = false
        }
    }
    //</editor-fold>

    //<editor-fold desc="<Scan>">
    var scann = false
    private fun startscan() {
        if (!scann) {
            scann = true
            mAudioService!!.startLEScan()

            val ScanTime = 1500
            val mHandler = Handler(Looper.getMainLooper())
            mHandler.postDelayed({ stopscan() }, ScanTime.toLong())
        }
    }

    private fun stopscan() {
        scann = false
        mAudioService?.stopLEScan()
        if (mAudioService?.ScanAddress?.size == 0) {
            model.setReScanLiveData(true)
            return
        }
        var rssi = 0
        var ConnectAddress = ""

        for (key in mAudioService?.ScanAddress?.keys!!) {
            if (rssi == 0) {
                rssi = mAudioService!!.ScanAddress[key]!!
                ConnectAddress = key as String
            }
            if (mAudioService?.ScanAddress?.get(key)!! > rssi) {
                rssi = mAudioService!!.ScanAddress[key]!!
                ConnectAddress = key
            }
        }

        mAudioService!!.connect(ConnectAddress)
        //TODO
//        if (ConnectAddress == "80:F5:B5:60:07:B3") {
//            mAudioService!!.connect(ConnectAddress)
//        } else {
//            model.setReScanLiveData(true)
//            return
//        }

        model.setMacAddressLiveData(ConnectAddress)

        //檢查是否有註冊過保固
        val client = OkHttpClient().newBuilder().build()
        // 建立Request，設置連線資訊
        val request: Request = Request.Builder()
            .url("https://serafim-tech.com/test/test.php")
            .build()

        // 建立Call
        val call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val result = response.body!!.string()
                if (!result.contains(ConnectAddress)) {
                    model.setAddressLiveDataData(ConnectAddress)
                    model.setQuestionnaireData(false)
                } else {
                    model.setQuestionnaireData(true)
                }
            }
        })
    }

    //</editor-fold>

    //<editor-fold desc="<Questionnaire>">
    var Time2: Int = 100
    private var php_flag = ""
    private val handler = Handler(Looper.getMainLooper())
    var runnable2: Runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, Time2.toLong())
            //每隔一段时间要重复执行的代码
            /**建立連線 */
            val client = OkHttpClient().newBuilder().build()

            /**設置傳送需求 */
            val request: Request = Request.Builder()
                .url("https://serafim-tech.com/test/test.php")
                .build()

            /**設置回傳 */
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    /**如果傳送過程有發生錯誤 */
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    /**取得回傳 */
                    php_flag = response.body!!.string()
                    if (php_flag.contains(model.AddressLiveData.value!!)) {
                        model.setPageData(Navigator.NavTarget.Main.label)
                    }
                }
            })
        }
    }

    //啟動計時器
    fun StartTimer2() {
        handler.postDelayed(runnable2, Time2.toLong()) //启动计时器
    }

    //關閉計時器
    fun StopTimer2() {
        handler.removeCallbacks(runnable2)
    }
    //</editor-fold>

    //<editor-fold desc="<BatteryTimer>">
    private val handler1 = Handler(Looper.getMainLooper())
    var Time = 1000
    var runnable: Runnable = object : Runnable {
        override fun run() {
            handler1.postDelayed(this, Time.toLong())
            //每隔一段时间要重复执行的代码

            if (mAudioService != null) {
                if (mAudioService!!.mConnectionState == BluetoothProfile.STATE_CONNECTED && !mAudioService!!.mBusy) {
                    mAudioService!!.ReadBatteryState()
                }
            }
        }
    }

    //啟動計時器
    fun StartTimer() {
        handler1.postDelayed(runnable, Time.toLong()) //启动计时器
    }

    //關閉計時器
    fun StopTimer() {
        handler1.removeCallbacks(runnable)
    }
    //</editor-fold>

    //<editor-fold desc="<TEST>">
    private val handler3 = Handler(Looper.getMainLooper())
    var Time3 = 5000
    var runnable3: Runnable = Runnable {
        bindService()
    }

    //啟動計時器
    fun StartTimer3() {
        handler3.postDelayed(runnable3, Time3.toLong()) //启动计时器
    }

    //關閉計時器
    fun StopTimer3() {
        handler3.removeCallbacks(runnable3)
    }
    //</editor-fold>

    fun VideoDay() {
        //讀取目前系統時間
        val format = SimpleDateFormat("yyyy-MM-dd")
        val t = format.format(Date())
        if (settings.videoDay != t) {
            model.setDisplayVideoData(true)
            settings.videoDay = t
        }
    }
}
