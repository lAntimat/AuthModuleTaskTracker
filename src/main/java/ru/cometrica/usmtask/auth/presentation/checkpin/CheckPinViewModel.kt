package ru.cometrica.usmtask.auth.presentation.checkpin

import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.cometrica.apimodels.Resource
import ru.cometrica.commonui.clearPhone
import ru.cometrica.data.PushTokenModel
import ru.cometrica.data.data.AuthRepository
import ru.cometrica.data.ext.toStringError
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.biometrichelper.BiometricHelper
import ru.cometrica.usmtask.auth.models.AuthModel
import ru.cometrica.usmtask.auth.models.CheckPinMode
import ru.cometrica.usmtask.auth.presentation.createpin.models.CreatePinState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CheckPinViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val biometricHelper: BiometricHelper
) : BaseCheckPinViewModel(repository, biometricHelper) {

    private var pin = ""
    private lateinit var pushToken: PushTokenModel

    private val _pinState: MutableStateFlow<CreatePinState> =
        MutableStateFlow(CreatePinState.Initial())
    val pinState: StateFlow<CreatePinState> = _pinState

    private val _isShowFingerprintBtnFlow = MutableSharedFlow<Boolean>(replay = 1)
    val isShowFingerprintBtnFlow: SharedFlow<Boolean>
        get() = _isShowFingerprintBtnFlow

    var authModel = AuthModel("")
    var args: CheckPinFragmentArgs? = null


    init {
        _pinState.value = CreatePinState.Initial(
            title = R.string.auth_check_pin_title,
            subTitle = R.string.auth_check_pin_subtitle,
        )

        getPushToken()
    }

    fun setFragmentArgs(args: CheckPinFragmentArgs) {
        this.authModel = args.authModel
        this.args = args

        //Кнопка вызова отпечатка пальца
        isShowFingerPrintBtn = when (args.mode) {
            CheckPinMode.Login -> false
            CheckPinMode.CheckPin -> true
        }

        //Показываем когда юзер открыл экран проверки пина
        viewModelScope.launch {
            if (!biometricHelper.canAuthenticate() || !repository.isFingerprintSetup()) isShowFingerPrintBtn =
                false

            _isShowFingerprintBtnFlow.emit(isShowFingerPrintBtn)

            if ((args.mode == CheckPinMode.CheckPin) and biometricHelper.canAuthenticate() and repository.isFingerprintSetup()) {
                delay(200)
                _showBiometricDialog.emit(Unit)
            }
        }
    }

    fun setPin(pin: String) {
        this.pin = pin
        args?.mode.let { mode ->
            when (mode) {
                CheckPinMode.Login -> {
                    viewModelScope.launch {
                        _pinState.value = CreatePinState.Loading
                        val response = repository.authorize(
                            pin,
                            authModel.otpCode,
                            clearPhone(authModel.phone),
                            authModel.nonce
                        )
                        if (response is Resource.Success) {
                            _pinState.value = CreatePinState.Success
                            repository.saveToken(response.data.sessionId)
                            Timber.d("Token ${response.data.sessionId}")
                            if (biometricHelper.canAuthenticate()) _showBiometricSetupDialog.emit(
                                pin
                            )
                            else {
                                repository.sendPushToken(pushToken)
                                _openDashboard.emit(Unit)
                            }
                        } else {
                            _pinState.value = CreatePinState.Error(response.toStringError())
                        }
                    }
                }
                CheckPinMode.CheckPin -> {
                    viewModelScope.launch {
                        _pinState.value = CreatePinState.Loading
                        successAuth(pin) {
                            _pinState.value = CreatePinState.Error(it)
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    fun successAuth(pin: String) {
        super.successAuth(pin) {
            _pinState.value = CreatePinState.Error(it)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            repository.clearAll()
            repository.removePushToken(pushToken)
            _openLoginScreen.emit(Unit)

        }
    }

    private fun getPushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            pushToken = PushTokenModel("FBS", task.result, "UNDEFINED")
        }
    }
}