package com.serafimtech.serafimaudio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ViewModel : ViewModel() {
    //<editor-fold desc="<A1_LiveData>">
    val ReScanLiveData: LiveData<Boolean> get() = _ReScanLiveData
    private val _ReScanLiveData = MutableLiveData<Boolean>()

    val ConnectLiveData: LiveData<Boolean> get() = _ConnectLiveData
    private val _ConnectLiveData = MutableLiveData<Boolean>()

    val MacAddressLiveData: LiveData<String> get() = _MacAddressLiveData
    private val _MacAddressLiveData = MutableLiveData<String>()

    val BLEEQEnabledLiveData: LiveData<Boolean> get() = _BLEEQEnabledLiveData
    private val _BLEEQEnabledLiveData = MutableLiveData<Boolean>()

    val UIEQEnabledLiveData: LiveData<Boolean> get() = _UIEQEnabledLiveData
    private val _UIEQEnabledLiveData = MutableLiveData<Boolean>()

    val UIEQLiveData: LiveData<List<Float>> get() = _UIEQLiveData
    private val _UIEQLiveData = MutableLiveData<List<Float>>()

    val BLEEQLiveData: LiveData<List<Float>> get() = _BLEEQLiveData
    private val _BLEEQLiveData = MutableLiveData<List<Float>>()

    val BLEEQSingleLiveData: LiveData<List<Float>> get() = _BLEEQSingleLiveData
    private val _BLEEQSingleLiveData = MutableLiveData<List<Float>>()

    val EQCustomSingleLiveData: LiveData<List<String>> get() = _EQCustomSingleLiveData
    private val _EQCustomSingleLiveData = MutableLiveData<List<String>>()

    val EQCustomListLiveData: LiveData<List<String>> get() = _EQCustomListLiveData
    private val _EQCustomListLiveData = MutableLiveData<List<String>>()

    val EQDefaultListLiveData: LiveData<List<String>> get() = _EQDefaultListLiveData
    private val _EQDefaultListLiveData = MutableLiveData<List<String>>()

    val EQCustomModeLiveData: LiveData<String> get() = _EQCustomModeLiveData
    private val _EQCustomModeLiveData = MutableLiveData<String>()

    val EQDefaultModeLiveData: LiveData<String> get() = _EQDefaultModeLiveData
    private val _EQDefaultModeLiveData = MutableLiveData<String>()

    val BassLiveData: LiveData<Int> get() = _BassLiveData
    private val _BassLiveData = MutableLiveData<Int>()

    val BLEBassEnabledLiveData: LiveData<Boolean> get() = _BLEBassEnabledLiveData
    private val _BLEBassEnabledLiveData = MutableLiveData<Boolean>()

    val UIBassEnabledLiveData: LiveData<Boolean> get() = _UIBassEnabledLiveData
    private val _UIBassEnabledLiveData = MutableLiveData<Boolean>()

    val SurroundLiveData: LiveData<Int> get() = _SurroundLiveData
    private val _SurroundLiveData = MutableLiveData<Int>()

    val BLESurroundEnabledLiveData: LiveData<Boolean> get() = _BLESurroundEnabledLiveData
    private val _BLESurroundEnabledLiveData = MutableLiveData<Boolean>()

    val UISurroundEnabledLiveData: LiveData<Boolean> get() = _UISurroundEnabledLiveData
    private val _UISurroundEnabledLiveData = MutableLiveData<Boolean>()

    val BLEVolumeLiveData: LiveData<Int> get() = _BLEVolumeLiveData
    private val _BLEVolumeLiveData = MutableLiveData<Int>()

    val UIVolumeLiveData: LiveData<Int> get() = _UIVolumeLiveData
    private val _UIVolumeLiveData = MutableLiveData<Int>()

    //</editor-fold>
    val AppTestLiveData: LiveData<Boolean> get() = _AppTestLiveData
    private val _AppTestLiveData = MutableLiveData<Boolean>()

    val PageLiveData: LiveData<String> get() = _PageLiveData
    private val _PageLiveData = MutableLiveData<String>()

    val LoginLiveData: LiveData<Login> get() = _LoginLiveData
    private val _LoginLiveData = MutableLiveData<Login>()

    enum class Login {
        Login,
        Logged,
        Logout,
        Register,
        Registered
    }

    val GoogleLoginLiveData: LiveData<Boolean> get() = _GoogleLoginLiveData
    private val _GoogleLoginLiveData = MutableLiveData<Boolean>()

    val DisplayVideoLiveData: LiveData<Boolean> get() = _DisplayVideoLiveData
    private val _DisplayVideoLiveData = MutableLiveData<Boolean>()

    val UserEmailLiveData: LiveData<String> get() = _UserEmailLiveData
    private val _UserEmailLiveData = MutableLiveData<String>()

    val UserPasswordLiveData: LiveData<String> get() = _UserPasswordLiveData
    private val _UserPasswordLiveData = MutableLiveData<String>()

    val UserConfirmPasswordLiveData: LiveData<String> get() = _UserConfirmPasswordLiveData
    private val _UserConfirmPasswordLiveData = MutableLiveData<String>()

    val BatteryLiveData: LiveData<Battery> get() = _BatteryLiveData
    private val _BatteryLiveData = MutableLiveData<Battery>()

    enum class Battery(val id: Int) {
        battery_energy(R.drawable.battery_energy),
        battery_full(R.drawable.battery_full),
        battery_reduce(R.drawable.battery_reduce),
        battery_half(R.drawable.battery_half),
        battery_low(R.drawable.battery_low),
        battery_out(R.drawable.battery_out)
    }

    val QuestionnaireLiveData: LiveData<Boolean> get() = _QuestionnaireLiveData
    private val _QuestionnaireLiveData = MutableLiveData<Boolean>()

    val AddressLiveData: LiveData<String> get() = _AddressLiveData
    private val _AddressLiveData = MutableLiveData<String>()

    init {
        _ReScanLiveData.value = false
        _UIEQLiveData.value = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        _ConnectLiveData.value = false
        _BassLiveData.value = 0
        _SurroundLiveData.value = 0
        _UIVolumeLiveData.value = 0
        _BLEVolumeLiveData.value = 0
        _BatteryLiveData.value = Battery.battery_energy
        _DisplayVideoLiveData.value = false
        _QuestionnaireLiveData.value = true
    }

    //<editor-fold desc="<A1_fun>">
    fun setReScanLiveData(enable: Boolean) {
        _ReScanLiveData.value = enable
    }

    fun setConnectLiveData(enable: Boolean) {
        _ConnectLiveData.value = enable
    }

    fun setMacAddressLiveData(address: String) {
        _MacAddressLiveData.value = address
    }

    fun setUIEQData(index: Int, param: Float) {
        val eq: List<Float> = _UIEQLiveData.value!!
        _UIEQLiveData.value = listOf()

        for (i in 0..14) {
            if (i != index) {
                _UIEQLiveData.value = _UIEQLiveData.value?.let { it + listOf(eq.get(i)) }
            } else {
                _UIEQLiveData.value = _UIEQLiveData.value?.let { it + listOf(param) }
            }
        }
    }

    fun setUIEQData(eq: List<Float>) {
        _UIEQLiveData.value = eq
    }

    fun setBLEEQSingleData(index: Int, param: Float) {
        _BLEEQSingleLiveData.value = listOf(index.toFloat(), param)
    }

    fun setBLEEQData(eq: List<Float>) {
        _BLEEQLiveData.value = eq
    }

    fun setBLEEQEnabled(enable: Boolean) {
        _BLEEQEnabledLiveData.value = enable
    }

    fun setUIEQEnabled(enable: Boolean) {
        _UIEQEnabledLiveData.value = enable
    }

    fun setEQCustomSingleData(mode: String, value: String) {
        _EQCustomSingleLiveData.value = listOf(mode, value)
    }

    fun setEQCustomListData(value: List<String>) {
        _EQCustomListLiveData.value = value
    }

    fun setEQDefaultListData(value: List<String>) {
        _EQDefaultListLiveData.value = value
    }

    fun setEQCustomModeData(value: String) {
        _EQCustomModeLiveData.value = value
    }

    fun setEQDefaultModeData(value: String) {
        _EQDefaultModeLiveData.value = value
    }

    fun setBassData(value: Int) {
        _BassLiveData.value = value
    }

    fun setBLEBassEnabled(enable: Boolean) {
        _BLEBassEnabledLiveData.value = enable
    }

    fun setUIBassEnabled(enable: Boolean) {
        _UIBassEnabledLiveData.value = enable
    }

    fun setSurroundData(value: Int) {
        _SurroundLiveData.value = value
    }

    fun setBLESurroundEnabled(enable: Boolean) {
        _BLESurroundEnabledLiveData.value = enable
    }

    fun setUISurroundEnabled(enable: Boolean) {
        _UISurroundEnabledLiveData.value = enable
    }

    fun setUIVolumeData(value: Int) {
        _UIVolumeLiveData.value = value
    }

    fun setAppTest(enable: Boolean) {
        _AppTestLiveData.value = enable
    }

    fun setBLEVolumeData(value: Int) {
        _BLEVolumeLiveData.value = value
    }

    fun setBatteryData(battery: Battery) {
        _BatteryLiveData.value = battery
    }
    //</editor-fold>

    fun setQuestionnaireData(enable: Boolean) {
        _QuestionnaireLiveData.postValue(enable)
    }

    fun setPageData(value: String) {
        if (_PageLiveData.value != value)
            _PageLiveData.postValue(value)
    }

    fun setLoginData(enable: Login) {
        _LoginLiveData.value = enable
    }

    fun setGoogleLoginData(enable: Boolean) {
        _GoogleLoginLiveData.value = enable
    }

    fun setDisplayVideoData(enable: Boolean) {
        _DisplayVideoLiveData.value = enable
    }

    fun setUserEmail(value: String) {
        _UserEmailLiveData.value = value
    }

    fun setUserPassword(value: String) {
        _UserPasswordLiveData.value = value
    }

    fun setUserConfirmPassword(value: String) {
        _UserConfirmPasswordLiveData.value = value
    }

    fun setAddressLiveDataData(value: String) {
        _AddressLiveData.postValue(value)
    }
}