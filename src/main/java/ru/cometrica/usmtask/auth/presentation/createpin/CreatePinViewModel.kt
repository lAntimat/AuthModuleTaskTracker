package ru.cometrica.usmtask.auth.presentation.createpin

import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.cometrica.apimodels.Resource
import ru.cometrica.commonui.clearPhone
import ru.cometrica.data.PushTokenModel
import ru.cometrica.data.data.AuthRepository
import ru.cometrica.data.ext.toStringError
import ru.cometrica.data.resource.ResourceProvider
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.biometrichelper.BiometricHelper
import ru.cometrica.usmtask.auth.models.AuthModel
import ru.cometrica.usmtask.auth.presentation.checkpin.BaseCheckPinViewModel
import ru.cometrica.usmtask.auth.presentation.createpin.models.CreatePinState
import ru.cometrica.usmtask.auth.presentation.createpin.models.PinCreateMode
import javax.inject.Inject

@HiltViewModel
class CreatePinViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val biometricHelper: BiometricHelper,
    private val resourceProvider: ResourceProvider
) : BaseCheckPinViewModel(repository, biometricHelper) {

    private var pinCreateMode = PinCreateMode.EnterPin
    private var pin = ""
    private var authModel = AuthModel("")

    val pinState: MutableStateFlow<CreatePinState> = MutableStateFlow(CreatePinState.Initial())
    val openReEnterPinScreen = MutableSharedFlow<Unit>(replay = 0)
    private lateinit var pushToken: PushTokenModel

    init {
        getPushToken()
    }

    private fun getPushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            pushToken = PushTokenModel("FBS", task.result, "UNDEFINED")
        }
    }

    fun setArgs(args: CreatePinFragmentArgs) {
        pinCreateMode = args.mode
        authModel = args.authModel

        isShowFingerPrintBtn = false

        val initialState = when (pinCreateMode) {
            PinCreateMode.EnterPin -> {
                CreatePinState.Initial(
                    title = R.string.auth_enter_pin_title,
                    subTitle = R.string.auth_enter_pin_subtitle,
                )
            }
            PinCreateMode.RepeatPin -> {
                CreatePinState.Initial(
                    title = R.string.auth_re_enter_pin_title,
                    subTitle = R.string.auth_re_enter_pin_subtitle,
                )
            }
        }
        viewModelScope.launch {
            pinState.value = initialState
        }
    }

    fun setPin(pin: String) {
        when (pinCreateMode) {
            PinCreateMode.EnterPin -> {
                this.pin = pin
                viewModelScope.launch {
                    openReEnterPinScreen.emit(Unit)
                }
            }
            PinCreateMode.RepeatPin -> {
                validatePins(pin)
            }
        }
    }

    private fun validatePins(
        reEnterPin: String
    ) {
        when {
            this.pin != reEnterPin -> {
                viewModelScope.launch {
                    pinState.value = CreatePinState.Loading
                    delay(500)
                    pinState.value =
                        CreatePinState.Error(resourceProvider.getString(R.string.error_pins_not_match))
                }
            }
            this.pin == reEnterPin -> {
                viewModelScope.launch {
                    pinState.value = CreatePinState.Loading
                    val result = repository.authorize(
                        pin,
                        authModel.otpCode,
                        clearPhone(authModel.phone),
                        authModel.nonce
                    )
                    if (result is Resource.Success) {
                        repository.saveToken(result.data.sessionId)
                        repository.authSuccess()

                        pinState.value = CreatePinState.Success

                        if (biometricHelper.canAuthenticate()) {
                            _showBiometricSetupDialog.emit(pin)
                        } else _openDashboard.emit(Unit)
                    } else {
                        pinState.value = CreatePinState.Error(
                            result.toStringError()
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {

            when (val result = repository.logout()) {
                is Resource.Success -> {
                    repository.clearAll()
                    repository.removePushToken(pushToken)
                }
                else -> result.toStringError()
            }
        }
    }
}