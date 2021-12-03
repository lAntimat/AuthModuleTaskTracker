package ru.cometrica.usmtask.auth.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthModel(
    val phone: String,
    val nonce: String = "",
    val otpCode: String = ""
) : Parcelable