package ru.cometrica.usmtask.auth.presentation.confirmsms

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.cometrica.commonui.clear
import ru.cometrica.commonui.ext.showErrorSnackbar
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.databinding.FragmentConfirmSmsBinding
import ru.cometrica.usmtask.auth.presentation.base.BaseAuthFragment

@AndroidEntryPoint
class ConfirmSmsFragment : BaseAuthFragment(R.layout.fragment_confirm_sms) {

    private val viewModel: ConfirmSmsViewModel by viewModels()
    private val binding: FragmentConfirmSmsBinding by viewBinding()
    private val args by navArgs<ConfirmSmsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setArgs(args)

        bindViewModel()
        initListeners()
    }

    private fun initListeners() {
        binding.edPhone.doOnTextChanged { text, start, before, count ->
            if ((start == 3) and (text?.length == 4)) {
                viewModel.validateCode(text.toString())
            }
        }

        binding.tvResendSms.setOnClickListener { viewModel.resentSms() }
        binding.tvHelp.setOnClickListener { bsHelp.show(childFragmentManager) }
    }

    private fun bindViewModel() {
        viewModel.state.collectWhenStarted() {
            when (it) {
                is State.Default -> {
                    binding.progressBar.isVisible = false
                    binding.tvSubTitle.text = getString(R.string.confirm_sms_subtitle, it.phone)
                    binding.edPhone.clear()
                }
                is State.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is State.Error -> {
                    showErrorSnackbar(it.error)
                }
            }
        }

        viewModel.openPinCreation.collectWhenStarted { authModel ->

            val dest =
                ConfirmSmsFragmentDirections.actionConfirmSmsFragmentToCreatePinFragment(
                    authModel
                )
            findNavController().navigate(dest)
        }

        viewModel.openPinCheck.collectWhenStarted {
            val dest =
                ConfirmSmsFragmentDirections.actionConfirmSmsFragmentToCheckPinFragment(
                    it.first,
                    it.second
                )
            findNavController().navigate(dest)
        }

        viewModel.errorToShow.collectWhenStarted() {
            showErrorSnackbar(it)
        }
    }
}