package ru.cometrica.usmtask.auth.presentation.createpin

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import ru.cometrica.commonui.ext.showErrorSnackbar
import ru.cometrica.navigation.NavigationFlow
import ru.cometrica.navigation.ext.navigator
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.biometrichelper.BiometricHelper
import ru.cometrica.usmtask.auth.databinding.FragmentCreatePinBinding
import ru.cometrica.usmtask.auth.presentation.base.BaseAuthFragment
import ru.cometrica.usmtask.auth.presentation.createpin.models.CreatePinState
import ru.cometrica.usmtask.auth.presentation.createpin.models.PinCreateMode
import ru.cometrica.usmtask.auth.presentation.customviews.CustomKeyboardView
import javax.inject.Inject

@AndroidEntryPoint
class CreatePinFragment : BaseAuthFragment(R.layout.fragment_create_pin) {

    @Inject
    lateinit var biometricHelper: BiometricHelper

    private val viewModel: CreatePinViewModel by hiltNavGraphViewModels(R.id.auth_flow)
    private val binding: FragmentCreatePinBinding by viewBinding()

    private val args: CreatePinFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setArgs(args)

        bindViewModel()
        initKeyboard()

        binding.pinCodeView.setOnEnteredListener {
            viewModel.setPin(it)
        }

        binding.tvHelp.setOnClickListener { bsHelp.show(childFragmentManager) }
        binding.tvExit.setOnClickListener { viewModel.logout() }

        activity?.window?.statusBarColor = resources.getColor(R.color.secondary_light)
    }

    private fun initKeyboard() {
        binding.keyboard.setOnInteractionListener(object :
            CustomKeyboardView.OnFragmentInteractionListener {
            override fun onKeyboardClick(symbol: String) {
                binding.pinCodeView.appendKey(symbol)
            }

            override fun onBackspaceClick() {
                binding.pinCodeView.removeKey()
            }

            override fun onFingerPrintClick() {}

            override fun onFingerprintEnable(): Boolean {
                return false
            }
        })
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

    private fun bindViewModel() {
        viewModel.pinState.collectWhenStarted {
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
                    showErrorSnackbar(it.error)
                }
            }
        }

        viewModel.openReEnterPinScreen.collectWhenStarted {
            findNavController().navigate(R.id.createPinFragment2, Bundle().apply {
                putSerializable("mode", PinCreateMode.RepeatPin)
                putParcelable("authModel", args.authModel)
            })

        }

        viewModel.showBiometricSetupDialog.collectWhenStarted {
            showBiometricSetupDialog(it)
        }

        viewModel.openDashboard.collectWhenStarted {
            navigator().navigateToFlow(NavigationFlow.BottomViewFlow)
        }
    }
}