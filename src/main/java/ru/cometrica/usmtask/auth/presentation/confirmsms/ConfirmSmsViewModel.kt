package ru.cometrica.usmtask.auth.presentation.confirmsms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
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
import ru.cometrica.usmtask.auth.models.AuthModel
import ru.cometrica.usmtask.auth.models.CheckPinMode
import javax.inject.Inject

@HiltViewModel
class ConfirmSmsViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private lateinit var pushToken: PushTokenModel

    private val _state = MutableStateFlow<State>(State.Default("", ""))
    val state: StateFlow<State>
        get() = _state

    private val _openPinCreation = MutableSharedFlow<AuthModel>()
    val openPinCreation: SharedFlow<AuthModel>
        get() = _openPinCreation

    private val _openPinCheck = MutableSharedFlow<Pair<AuthModel, CheckPinMode>>()
    val openPinCheck: SharedFlow<Pair<AuthModel, CheckPinMode>>
        get() = _openPinCheck

    private val _errorToShow = MutableSharedFlow<String>()
    val errorToShow: SharedFlow<String>
        get() = _errorToShow

    init {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            pushToken = PushTokenModel(FBS, task.result, UNDEFINED)
        }
    }

    var arguments: ConfirmSmsFragmentArgs? = null
    var authModel = AuthModel("")

    fun setArgs(args: ConfirmSmsFragmentArgs) {
        arguments = args
        authModel = args.authModel
        _state.value = State.Default(args.authModel.phone, args.authModel.nonce)
    }

    fun validateCode(code: String) {
        viewModelScope.launch {
            authModel.let {
                when (val result = repository.verifyUser(code, clearPhone(it.phone), it.nonce)) {
                    is Resource.Success -> {
                        val authModel = AuthModel(it.phone, it.nonce, code)
                        if (result.data.newPin) _openPinCreation.emit(authModel)
                        else _openPinCheck.emit(authModel to CheckPinMode.Login)
                    }
                    else -> _state.value = State.Error(result.toStringError())
                }
            }
        }
    }

    fun resentSms() {
        viewModelScope.launch {
            _state.emit(State.Loading)
            when (val result = repository.authenticate(clearPhone(authModel.phone))) {
                is Resource.Success -> {
                    with(result.data) {
                        authModel = AuthModel(phone = authModel.phone, nonce = nonce)
                        _state.emit(State.Default(authModel.phone, authModel.nonce))
                    }
                }
                else -> _state.value = State.Error(result.toStringError())
            }
        }
    }

    companion object {
        const val FBS = "FBS"
        const val UNDEFINED = "UNDEFINED"
    }
}