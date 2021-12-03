package ru.cometrica.usmtask.auth.presentation.createpin.models

import ru.cometrica.usmtask.auth.R


sealed class PinMarker {
    object Empty : PinMarker()
    object Secret : PinMarker()
    object Error : PinMarker()
    object Success : PinMarker()
    data class Text(val text: String, var color: Int = R.color.black) : PinMarker()
}