package ru.cometrica.usmtask.auth.presentation.customviews

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.ContextWrapper
import android.content.res.TypedArray
import android.os.Handler
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isInvisible
import ru.cometrica.commonui.clear
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.presentation.createpin.models.PinMarker
import ru.cometrica.usmtask.auth.presentation.createpin.models.PinViewBundle
import ru.cometrica.usmtask.auth.presentation.createpin.models.SavedState
import ru.cometrica.usmtask.auth.databinding.ViewPinCodeBinding
import ru.cometrica.commonui.color
import ru.cometrica.commonui.dpToPx


class PinCodeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private lateinit var onEnteredListener: (pin: String) -> Unit
    private lateinit var onClickListener: (pin: String) -> Unit
    var isSecret = false

    var isCodeFocusable: Boolean = false

    private var mPasscode: String = ""
    private var mLastPasscodeLength: Int = 0
    private var mPasscodeLength: Int = PASSCODE_LENGTH
    private var hasError: Boolean = false

    private val oldItemList = MutableList<PinMarker?>(PASSCODE_LENGTH) { null }
    private val pins = MutableList(PASSCODE_LENGTH) { PinMarker.Empty as PinMarker }
    private var dotsAnimation: AnimatorSet? = null

    var watcher: TextWatcher? = null

    var viewBinding = ViewPinCodeBinding.inflate(LayoutInflater.from(context),this, true)

    init {

        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.PinCodeView)
        isSecret = typedArray.getBoolean(R.styleable.PinCodeView_isSecret, false)
        isCodeFocusable = typedArray.getBoolean(R.styleable.PinCodeView_isCodeFocusable, false)
        typedArray.recycle()


        onCreateView()

    }

    private fun onCreateView() {
        initPinLayout()
        clearCodeInput()

        createTextWatcher()
        enableKeyboardListener()
    }


    fun setOnEnteredListener(callback: (pin: String) -> Unit) {
        onEnteredListener = callback
    }

    fun setOnSymbolAddListener(callback: (pin: String) -> Unit) {
        onClickListener = callback
    }

    fun showLoading(isShow: Boolean, delay: Long = 0) {

        if (isShow) {
            Handler().postDelayed({
                if (isSecret) {
                    startDotsAnim()
                } else {
                    setDotsCheckedState()
                    startDotsAnim()
                }
            }, delay)
        } else {
            setDotsDefaultState()
            stopAnimation()
        }
    }

    fun appendKey(pin: String, withCallback: Boolean = true) {
        onKeyboardClick(pin, withCallback)
    }

    fun removeKey() {
        stopAnimation()
        removePin()
    }

    fun getPinCode(): String =
        mPasscode

    fun clearCodeInput() {
        showLoading(false)
        hasError = false
        mPasscode = ""
        mLastPasscodeLength = 0
        viewBinding.hiddenEditText.clear()

        pins.clear()
        pins.addAll(MutableList(PASSCODE_LENGTH) { PinMarker.Empty })
        setPins(pins)

    }

    private fun initPinLayout() {
        for (index in pins.indices) {
            addPinView(PinMarker.Empty, index)
        }
    }

    private fun onKeyboardClick(symbol: String, withCallback: Boolean = false) {
        if (mPasscode.length < PASSCODE_LENGTH) {
            mPasscode += symbol
            mPasscodeLength = mPasscode.length

            if (mPasscodeLength > mLastPasscodeLength) {
                addPin(symbol)
            }
        } else if (hasError) {
            clearCodeInput()
            hasError = false
            onKeyboardClick(symbol)
        }

        if (mPasscode.length == PASSCODE_LENGTH) {
            if (::onEnteredListener.isInitialized)
                if(withCallback) onEnteredListener.invoke(getPinCode())
        }
    }

    private fun addPin(symbol: String) {
        val pinMarker = if (isSecret) PinMarker.Secret else PinMarker.Text(symbol)
        pins[mPasscodeLength - 1] = pinMarker
        setPins(pins)

        if (::onClickListener.isInitialized) {
            onClickListener.invoke(getPinCode())
        }
    }

    private fun removePin() {
        if (mPasscode.isEmpty()) return
        mPasscode = mPasscode.substring(0, mPasscode.length - 1)
        mPasscodeLength = mPasscode.length
        mLastPasscodeLength = mPasscodeLength
        changeTextColor()
        pins[mPasscodeLength] = PinMarker.Empty
        setPins(pins)
    }

    private fun setPins(pins: List<PinMarker>) {
        oldItemList.zip(pins).forEachIndexed { index, (oldPin, newPin) ->
            if (oldPin != newPin) {
                viewBinding.pinHolder.removeViewAt(index)
                addPinView(newPin, index)
            }
        }
    }

    private fun addPinView(pin: PinMarker, index: Int) {
        viewBinding.pinHolder.addView(createViewForPin(pin), index)
        oldItemList[index] = pin
    }

    private fun createViewForPin(pin: PinMarker): View = when (pin) {
        is PinMarker.Empty -> createImagePin()
        is PinMarker.Secret -> createCheckedImagePin()
        is PinMarker.Error -> createImagePin(color = R.color.red)
        is PinMarker.Success -> createImagePin(color = R.color.green)
        is PinMarker.Text -> createTextPin(pin.text, pin.color)
    }

    private fun createTextPin(text: String, color: Int): View {
        return LinearLayout(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LAYOUT_HEIGHT, LAYOUT_HEIGHT)
                .apply {
                    gravity = Gravity.CENTER
                    weight = 1.0f
                }

            addView(TextView(context).apply {
                //setPadding(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1))
                textSize = TEXT_SIZE
                setText(text)
                setTextColor(color(color))
                tag = TAG_PIN_CHECKED
                gravity = Gravity.CENTER

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                        .apply {
                            gravity = Gravity.CENTER
                            //Почему то на версиях ниже N, текст не выравнивается по центре
                            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.N) {
                                bottomMargin = dpToPx(4)
                            }
                        }
            })
        }

    }

    private fun createImagePin(color: Int? = null): View {
        return LinearLayout(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LAYOUT_HEIGHT,
                LAYOUT_WIDTH
            ).apply {
                gravity = Gravity.CENTER
                weight = 1.0f
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.pin_circle_pin_view)
                color?.let { setColorFilter(color(color)) }
                tag = TAG_PIN_UNCHECKED
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(PIN_SIZE, PIN_SIZE)
                        .apply {
                            gravity = Gravity.CENTER
                            weight = 1.0f
                        }
            }
            )
        }
    }

    private fun createCheckedImagePin(): View {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LAYOUT_HEIGHT, LAYOUT_WIDTH).apply {
                gravity = Gravity.CENTER
                weight = 1.0f
            }

            addView(ImageView(context).apply {
                setImageResource(R.drawable.pin_circle_checked_pin_view)
                tag = TAG_PIN_CHECKED
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(CHECKED_PIN_SIZE, CHECKED_PIN_SIZE)
                        .apply {
                            gravity = Gravity.CENTER
                            weight = 1.0f
                        }
            })

            getScaleAnimation(this).start()
        }
    }

    private fun changeViewColor(color: Int) {
        for (child in viewBinding.pinHolder.children) {
            if (child is LinearLayout) {
                if (child.children.first() is TextView) {
                    (child.children.first() as TextView).setTextColor(color)
                }
            }
        }
    }

    private fun startDotsAnim() {
        startDotsAnim(viewBinding.pinHolder.children)
    }

    private fun startDotsAnim(view: Sequence<View>) {

        val delay = ANIMATION_DURATION / 2

        var relativeStartTime = delay

        val animationsList = ArrayList<Animator>()
        view.forEach {
            animationsList.add(getScaleAnimation(it, relativeStartTime, repeatCount = 1000))
            relativeStartTime += delay
        }

        dotsAnimation = AnimatorSet().apply {
            playTogether(animationsList)
            duration = 1000
            start()
        }
    }

    private fun getScaleAnimation(
        view: View,
        relativeStartTime: Long = 0,
        repeatCount: Int = 0
    ): AnimatorSet {
        val xAnim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.25f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            startDelay = relativeStartTime
            this.repeatCount = repeatCount
        }

        val yAnim = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.25f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            startDelay = relativeStartTime
            this.repeatCount = repeatCount
        }

        return AnimatorSet().apply {
            playTogether(xAnim, yAnim)
        }
    }

    private fun stopAnimation() {
        dotsAnimation?.end()
    }

    fun shakeAnimation() {
        val animShake = AnimationUtils.loadAnimation(context, R.anim.shake)
        viewBinding.pinHolder.startAnimation(animShake)
    }

    fun setError() {
        hasError = true
        changeTextColor(R.color.red)
    }

    private fun changeTextColor(color: Int = R.color.black) {
        if (!isSecret) {
            pins.forEach {
                if (it is PinMarker.Text) it.color = color
            }

            pins.forEachIndexed { index, newPin ->
                viewBinding.pinHolder.removeViewAt(index)
                addPinView(newPin, index)
            }
        }
    }

    fun setDotsErrorState() {
        hasError = true
        pins.clear()
        pins.addAll(MutableList(PASSCODE_LENGTH) { PinMarker.Error })
        setPins(pins)
    }

    fun setDotsSuccessState() {
        pins.clear()
        pins.addAll(MutableList(PASSCODE_LENGTH) { PinMarker.Success })
        setPins(pins)
    }

    fun setDotsDefaultState() {
        pins.clear()
        pins.addAll(MutableList(PASSCODE_LENGTH) { PinMarker.Empty })
        setPins(pins)
    }

    fun setDotsCheckedState() {
        pins.clear()
        pins.addAll(MutableList(PASSCODE_LENGTH) { PinMarker.Secret })
        setPins(pins)
    }

    private fun enableKeyboardListener() {
        viewBinding.hiddenEditText.isFocusable = isCodeFocusable
        if (isCodeFocusable) {
            viewBinding.hiddenEditText.isInvisible = false
            watcher?.let {
                viewBinding.hiddenEditText.addTextChangedListener(it)
            }
        } else {
            viewBinding.hiddenEditText.isInvisible = false
        }
    }

    private fun createTextWatcher() {
        watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (before == 1) removeKey()
                else {
                    hasError = false
                    mPasscode = ""
                    mLastPasscodeLength = 0

                    pins.clear()
                    pins.addAll(MutableList(PASSCODE_LENGTH) { PinMarker.Empty as PinMarker })
                    setPins(pins)

                    s?.forEach {
                        appendKey(it.toString())
                    }
                }
            }
        }
    }


    override fun onSaveInstanceState(): Parcelable? {

        val superState = super.onSaveInstanceState()

        val ss = SavedState(superState)

        ss.stateToSave = PinViewBundle(
            passcode = mPasscode,
            passcodeLength = mPasscodeLength,
            lastPasscodeLength = mLastPasscodeLength
        )

        return ss
    }

//    override fun onRestoreInstanceState(state: Parcelable?) {
//
//        if (state !is SavedState) {
//            super.onRestoreInstanceState(state)
//            return
//        }
//
//        val ss = state
//
//        val tempPasscode = ss.stateToSave?.passcode ?: ""
//
//
//        if (isCodeFocusable) {
//            viewBinding.hiddenEditText.removeTextChangedListener(watcher)
//            viewBinding.hiddenEditText.clear()
//
//            enableKeyboardListener()
//            viewBinding.hiddenEditText.setText(tempPasscode)
//        } else {
//            tempPasscode.forEach {
//                appendKey(it.toString(), withCallback = false)
//            }
//        }
//
//        super.onRestoreInstanceState(ss.superState)
//
//    }

    fun showKeyboard() {
        viewBinding.hiddenEditText.requestFocus()
        viewBinding.hiddenEditText.showKeyboard()
    }

    private fun EditText.showKeyboard() {
        if (requestFocus()) {
            (getActivity()?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(this, SHOW_IMPLICIT)
            setSelection(text.length)
        }
    }

    private fun View.getActivity(): AppCompatActivity? {
        var context = this.context
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    companion object {
        private const val PIN_VIEW_STATE = "PIN_VIEW_STATE"


        private const val TAG_PIN_UNCHECKED = "UNCHECKED"
        private const val TAG_PIN_CHECKED = "CHECKED"

        private val LAYOUT_HEIGHT: Int = dpToPx(40)
        private val LAYOUT_WIDTH: Int = dpToPx(40)
        private val PIN_SIZE: Int = dpToPx(12)
        private val CHECKED_PIN_SIZE: Int = dpToPx(12)
        private const val TEXT_SIZE = 32f

        private const val ANIMATION_DURATION = 400L
        private const val PASSCODE_LENGTH = 4
    }
}