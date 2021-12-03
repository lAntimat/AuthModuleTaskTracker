package ru.cometrica.usmtask.auth.presentation.createpin.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
internal class PinViewBundle(
    var passcode: String? = "",
    var passcodeLength: Int? = 0,
    var lastPasscodeLength: Int? = 0
) : Parcelable