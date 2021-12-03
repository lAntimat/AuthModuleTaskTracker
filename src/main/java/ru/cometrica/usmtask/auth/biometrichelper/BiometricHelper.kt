package ru.cometrica.usmtask.auth.biometrichelper

import androidx.fragment.app.Fragment


interface BiometricHelper {
    fun showBiometricDialogSetupBiometry(
        fragment: Fragment,
        pinCode: String,
        onSuccess: () -> Unit,
        onCancel: (pin: String) -> Unit
    )

    fun showBiometricDialog(
        fragment: Fragment,
        onSuccess: (pin: String) -> Unit
    )

    fun canAuthenticate(): Boolean

}