package ru.cometrica.usmtask.auth.presentation.confirmsms

sealed class State {
    class Default(val phone: String, val nonce: String): State()
    object Loading: State()
    class Error(val error: String): State()
}