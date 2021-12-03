package ru.cometrica.usmtask.auth.biometrichelper

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.cometrica.usmtask.auth.R
import javax.inject.Inject

class BiometricHelperImpl @Inject constructor(
    @ApplicationContext val context: Context,
) : BiometricHelper {

    private val biometricManager = BiometricManager.from(context)
    private var onSuccessSetupCallback: (() -> Unit)? = null
    private var onSuccessCallback: ((String) -> Unit)? = null

    private lateinit var biometricPrompt: BiometricPrompt
    val cryptographyManager = CryptographyManager()

    private val ciphertextWrapper
        get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            context,
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )

    private val canAuthenticate =
        biometricManager.canAuthenticate()

    override fun showBiometricDialogSetupBiometry(
        fragment: Fragment,
        pinCode: String,
        onSuccess: () -> Unit,
        onCancel: (pin: String) -> Unit
    ) {
        this.onSuccessSetupCallback = onSuccess

        val secretKeyName = fragment.requireContext().getString(R.string.secret_pin_key)
        val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
        val biometricPrompt =
            BiometricPromptUtils.createBiometricPrompt(
                fragment,
                processCancel = {
                    onCancel.invoke(pinCode)
                },
                processSuccess = {
                    encryptAndStorePin(it, pinCode)
                    onSuccess.invoke()
                })
        val promptInfo = BiometricPromptUtils.createPromptInfo(fragment)
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun encryptAndStorePin(
        authResult: BiometricPrompt.AuthenticationResult,
        pin: String
    ) {
        authResult.cryptoObject?.cipher?.apply {
            val encryptedPinWrapper = cryptographyManager.encryptData(pin, this)
            cryptographyManager.persistCiphertextWrapperToSharedPrefs(
                encryptedPinWrapper,
                context,
                SHARED_PREFS_FILENAME,
                Context.MODE_PRIVATE,
                CIPHERTEXT_WRAPPER
            )
        }
    }

    override fun showBiometricDialog(
        fragment: Fragment,
        onSuccess: (pinCode: String) -> Unit
    ) {
        this.onSuccessCallback = onSuccess

        showBiometricPromptForDecryption(fragment)
    }

    private fun showBiometricPromptForDecryption(fragment: Fragment) {
        ciphertextWrapper?.let { textWrapper ->
            val canAuthenticate = BiometricManager.from(context).canAuthenticate()

            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val secretKeyName = context.getString(R.string.secret_pin_key)
                val cipher = cryptographyManager.getInitializedCipherForDecryption(
                    secretKeyName, textWrapper.initializationVector
                )
                biometricPrompt =
                    BiometricPromptUtils.createBiometricPrompt(
                        fragment, processCancel = {

                        }, processSuccess = {
                            onSuccessCallback?.invoke(
                                decryptServerPinFromStorage(it)
                            )
                        })
                val promptInfo = BiometricPromptUtils.createPromptInfo(fragment)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }
        }
    }

    private fun decryptServerPinFromStorage(authResult: BiometricPrompt.AuthenticationResult): String {
        ciphertextWrapper?.let { textWrapper ->
            authResult.cryptoObject?.cipher?.let {
                return cryptographyManager.decryptData(textWrapper.ciphertext, it)
            }
        } ?: return ""
    }

    override fun canAuthenticate(): Boolean {
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }
}