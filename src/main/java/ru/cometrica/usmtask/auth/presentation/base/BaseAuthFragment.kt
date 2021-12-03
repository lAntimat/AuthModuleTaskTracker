package ru.cometrica.usmtask.auth.presentation.base

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.cometrica.commonui.BaseFragment
import ru.cometrica.usmtask.auth.presentation.customviews.BottomSheetHelp

open class BaseAuthFragment(@LayoutRes contentLayoutId: Int): BaseFragment(contentLayoutId) {

    protected val bsHelp = BottomSheetHelp.newInstance()

    fun BottomSheetDialogFragment.show(fragmentManager: FragmentManager) {
        show(fragmentManager, "")
    }

}