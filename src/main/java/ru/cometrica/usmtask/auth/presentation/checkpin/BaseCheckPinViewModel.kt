package ru.cometrica.usmtask.auth.presentation.checkpin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import ru.cometrica.apimodels.Resource
import ru.cometrica.data.PushTokenModel
import ru.cometrica.data.data.AuthRepository
import ru.cometrica.data.ext.toStringError
import ru.cometrica.usmtask.auth.biometrichelper.BiometricHelper
import ru.cometrica.usmtask.core.emit

open class BaseCheckPinViewModel constructor(
    private val repository: AuthRepository,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private lateinit var pushToken: PushTokenModel

    protected val _openDashboard = MutableSharedFlow<Unit>()
    val openDashboard: SharedFlow<Unit> = _openDashboard

    protected val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String>
        get() = _errors

    protected val _showBiometricDialog = MutableSharedFlow<Unit>()
    val showBiometricDialog: SharedFlow<Unit>
        get() = _showBiometricDialog

    protected val _showBiometricSetupDialog = MutableSharedFlow<String>()
    val showBiometricSetupDialog: SharedFlow<String>
        get() = _showBiometricSetupDialog

    var isShowFingerPrintBtn = false
    protected val _openLoginScreen = MutableSharedFlow<Unit>()
    val openLoginScreen: SharedFlow<Unit>
        get() = _openLoginScreen

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

    open fun successAuth(pin: String, onError: (error: String) -> Unit) {
        viewModelScope.launch {
            val response = repository.refreshAccessToken(pin)
            if (response is Resource.Success) {
                repository.saveToken(response.data.sessionId)
                repository.authSuccess()
                repository.sendPushToken(pushToken)
                _openDashboard.emit(Unit)
            } else {
                val error = (response as Resource.Error).failure.toStringError()
                _errors.emit(error)
                onError.invoke(error)
            }
        }
    }

    fun successFingerprintSetup() {
        viewModelScope.launch {
            repository.setFingerPrintSetup(true)
            _openDashboard.emit(Unit)
        }
    }

    fun cancelFingerprintAdding(pin: String) {
        viewModelScope.launch {
            repository.setFingerPrintSetup(false)
            _openDashboard.emit()
        }
    }
}