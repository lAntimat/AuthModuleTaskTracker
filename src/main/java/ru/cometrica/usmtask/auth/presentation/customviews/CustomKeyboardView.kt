package ru.cometrica.usmtask.auth.presentation.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import ru.cometrica.usmtask.auth.databinding.ViewCustomKeyboardBinding


class CustomKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var binding = ViewCustomKeyboardBinding.inflate(LayoutInflater.from(context),this, true)

    private var mListener: OnFragmentInteractionListener? = null

    fun setIsClickable(isEnabled: Boolean) {
        listOf(
            binding.fingerprintButton,
            binding.backspaceButton,
            binding.s1,
            binding.s2,
            binding.s3,
            binding.s4,
            binding.s5,
            binding.s6,
            binding.s7,
            binding.s8,
            binding.s9,
            binding.s0
        ).forEach {
            it.isClickable = isEnabled
        }
    }

    fun setFingerprintVisibility(isVisible: Boolean) {
        binding.fingerprintButton.isVisible = isVisible
    }

    init {
        setListeners()
    }



    fun setOnInteractionListener(listener: OnFragmentInteractionListener) {
        this.mListener = listener

        binding.fingerprintButton.isInvisible = mListener?.onFingerprintEnable()?.not() ?: true
    }

    private fun setListeners() {

        binding.s0.setOnClickListener { mListener?.onKeyboardClick(binding.s0.tag.toString()) }
        binding.s1.setOnClickListener { mListener?.onKeyboardClick(binding.s1.tag.toString()) }
        binding.s2.setOnClickListener { mListener?.onKeyboardClick(binding.s2.tag.toString()) }
        binding.s3.setOnClickListener { mListener?.onKeyboardClick(binding.s3.tag.toString()) }
        binding.s4.setOnClickListener { mListener?.onKeyboardClick(binding.s4.tag.toString()) }
        binding.s5.setOnClickListener { mListener?.onKeyboardClick(binding.s5.tag.toString()) }
        binding.s6.setOnClickListener { mListener?.onKeyboardClick(binding.s6.tag.toString()) }
        binding.s7.setOnClickListener { mListener?.onKeyboardClick(binding.s7.tag.toString()) }
        binding.s8.setOnClickListener { mListener?.onKeyboardClick(binding.s8.tag.toString()) }
        binding.s9.setOnClickListener { mListener?.onKeyboardClick(binding.s9.tag.toString()) }

        binding.backspaceButton.setOnClickListener { mListener?.onBackspaceClick() }

        binding.fingerprintButton.setOnClickListener { mListener?.onFingerPrintClick() }
    }

    interface OnFragmentInteractionListener {

        fun onKeyboardClick(symbol: String)

        fun onBackspaceClick()

        fun onFingerPrintClick()

        fun onFingerprintEnable(): Boolean
    }
}
