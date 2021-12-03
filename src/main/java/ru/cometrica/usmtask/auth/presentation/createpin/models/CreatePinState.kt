package ru.cometrica.usmtask.auth.presentation.createpin.models

import androidx.annotation.StringRes

sealed class CreatePinState {
    data class Initial(@StringRes val title: Int = 0, @StringRes val subTitle: Int = 0): CreatePinState()
    object Loading: CreatePinState()
    object Success: CreatePinState()
    data class Error(val error: String): CreatePinState()
}