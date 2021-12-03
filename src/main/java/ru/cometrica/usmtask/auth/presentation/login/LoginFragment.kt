package ru.cometrica.usmtask.auth.presentation.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.redmadrobot.inputmask.MaskedTextChangedListener
import com.redmadrobot.inputmask.helper.AffinityCalculationStrategy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.cometrica.commonui.ext.showErrorSnackbar
import ru.cometrica.commonui.showKeyboard
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.databinding.FragmentLoginBinding
import ru.cometrica.usmtask.auth.presentation.base.BaseAuthFragment
import ru.cometrica.usmtask.auth.presentation.customviews.BottomSheetCountries


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class LoginFragment : BaseAuthFragment(R.layout.fragment_login) {

    private val viewModel by viewModels<LoginViewModel>()
    private val binding: FragmentLoginBinding by viewBinding()

    var listener: MaskedTextChangedListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewModel()
        initListeners()
        initTextMask()

        activity?.window?.statusBarColor = resources.getColor(R.color.secondary_light)
    }

    override fun onResume() {
        super.onResume()
        binding.edPhone.showKeyboard()
    }

    private fun initListeners() {
        binding.btnLogin.setOnClickListener {
            viewModel.onLoginClick(binding.edPhone.text.toString())
        }

        binding.tvHelp.setOnClickListener {
            bsHelp.show(childFragmentManager)
        }

        binding.llCountryContainer.setOnClickListener {
            viewModel.onOpenCountryBsClick()
        }
    }

    private fun initTextMask() {
        listener = MaskedTextChangedListener.installOn(
            binding.edPhone,
            "",
            listOf(),
            AffinityCalculationStrategy.PREFIX
        )
    }

    private fun bindViewModel() {
        viewModel.openSmsConfirm.collectWhenResumed { authModel ->
            val dest =
                LoginFragmentDirections.actionLoginFragmentToConfirmSmsFragment(authModel)
            findNavController().navigate(dest)
        }

        viewModel.errorToShow.collectWhenStarted {
            showErrorSnackbar(it)
        }

        viewModel.currentCountry.collectWhenStarted {
            binding.edPhone.text?.clear()

            binding.tvCountryIcon.text = it.flag
            binding.tvCountryName.text = it.name
            binding.edPhone.hint = it.phoneCode

            listener?.primaryFormat = it.phoneCode + it.phoneMask
        }

        viewModel.openBsCountries.collectWhenStarted {
            val bsCountries = BottomSheetCountries.newInstance(it)
            bsCountries.show(childFragmentManager)
            bsCountries.setOnClickListener { position ->
                viewModel.onCountryClick(position)
                bsCountries.dismiss()
            }
        }
    }
}