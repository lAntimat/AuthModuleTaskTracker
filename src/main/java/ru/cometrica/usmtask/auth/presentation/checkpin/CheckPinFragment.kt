package ru.cometrica.usmtask.auth.presentation.checkpin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import ru.cometrica.commonui.ext.showErrorSnackbar
import ru.cometrica.navigation.NavigationFlow
import ru.cometrica.navigation.ext.navigator
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.biometrichelper.BiometricHelper
import ru.cometrica.usmtask.auth.databinding.FragmentCheckPinBinding
import ru.cometrica.usmtask.auth.presentation.base.BaseAuthFragment
import ru.cometrica.usmtask.auth.presentation.createpin.models.CreatePinState
import ru.cometrica.usmtask.auth.presentation.customviews.CustomKeyboardView
import javax.inject.Inject

@AndroidEntryPoint
class CheckPinFragment : BaseAuthFragment(R.layout.fragment_check_pin) {


    private val viewModel: CheckPinViewModel by viewModels()
    private val baseCheckPinViewModel: BaseCheckPinViewModel by viewModels()
    private val binding: FragmentCheckPinBinding by viewBinding()

    private val args by navArgs<CheckPinFragmentArgs>()

    @Inject
    lateinit var biometricHelper: BiometricHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setFragmentArgs(args)
        bindViewModel()
        initListeners()

        activity?.window?.statusBarColor = resources.getColor(R.color.secondary_light)
    }

    private fun showBiometricDialog() {
        biometricHelper.showBiometricDialog(this) {
            viewModel.successAuth(it)
        }
    }

    private fun showBiometricSetupDialog(pin: String) {
        biometricHelper.showBiometricDialogSetupBiometry(
            this, pin, onSuccess = {
                viewModel.successFingerprintSetup()
            }, onCancel = {
                viewModel.cancelFingerprintAdding(it)
            }
        )
    }

    private fun initListeners() {
        binding.pinCodeView.setOnEnteredListener {
            viewModel.setPin(it)
        }

        binding.tvHelp.setOnClickListener {
            bsHelp.show(childFragmentManager)
        }

        binding.tvExit.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun initKeyboard(isShowFingerprintBtn: Boolean) {
        binding.keyboard.setOnInteractionListener(object :
            CustomKeyboardView.OnFragmentInteractionListener {
            override fun onKeyboardClick(symbol: String) {
                binding.pinCodeView.appendKey(symbol)
            }

            override fun onBackspaceClick() {
                binding.pinCodeView.removeKey()
            }

            override fun onFingerPrintClick() {
                lifecycleScope.launchWhenStarted { showBiometricDialog() }
            }

            override fun onFingerprintEnable(): Boolean {
                return isShowFingerprintBtn
            }
        })
    }

    private fun bindViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.pinState.collect {
                when (it) {
                    is CreatePinState.Initial -> {
                        binding.tvTitle.text = getString(it.title)
                        binding.tvSubtitle.text = getString(it.subTitle)

                        binding.pinCodeView.setDotsDefaultState()
                        binding.pinCodeView.showLoading(false)
                        binding.keyboard.setIsClickable(true)
                    }
                    is CreatePinState.Loading -> {
                        binding.keyboard.setIsClickable(false)
                        binding.pinCodeView.showLoading(true)
                    }
                    is CreatePinState.Success -> {
                        binding.pinCodeView.setDotsSuccessState()
                        binding.keyboard.setIsClickable(true)
                    }
                    is CreatePinState.Error -> {
                        binding.pinCodeView.setDotsErrorState()
                        binding.keyboard.setIsClickable(true)
                    }
                }
            }
        }

        viewModel.openDashboard.collectWhenStarted {
            navigator().navigateToFlow(NavigationFlow.BottomViewFlow)
        }

        viewModel.openLoginScreen.collectWhenStarted {
            val dest = CheckPinFragmentDirections.actionCheckPinFragmentToLoginFragment()
            findNavController().navigate(dest)
        }

        viewModel.showBiometricDialog.collectWhenCreated {
            showBiometricDialog()
        }

        viewModel.showBiometricSetupDialog.collectWhenStarted() {
            showBiometricSetupDialog(it)
        }

        viewModel.errors.collectWhenStarted() {
            showErrorSnackbar(it)
        }

        viewModel.isShowFingerprintBtnFlow.collectWhenResumed {
            initKeyboard(it)
        }
    }
}