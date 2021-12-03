package ru.cometrica.usmtask.auth.presentation.createpin.models

import android.os.Parcel
import android.os.Parcelable
import android.view.View

internal class SavedState : View.BaseSavedState {

    var stateToSave: PinViewBundle? = null


    constructor(superState: Parcelable?) : super(superState)

    constructor(source: Parcel) : super(source) {
        stateToSave = source.readParcelable<PinViewBundle>(PinViewBundle::class.java.classLoader)
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeParcelable(stateToSave, flags)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}