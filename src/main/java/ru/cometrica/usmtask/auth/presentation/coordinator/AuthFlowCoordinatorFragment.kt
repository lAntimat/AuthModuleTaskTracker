package ru.cometrica.usmtask.auth.presentation.coordinator

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.AndroidEntryPoint
import ru.cometrica.data.data.AuthRepository
import ru.cometrica.navigation.NavigationFlow
import ru.cometrica.navigation.ext.navigator
import ru.cometrica.usmtask.auth.R
import ru.cometrica.usmtask.auth.models.AuthModel
import ru.cometrica.usmtask.auth.models.CheckPinMode
import javax.inject.Inject


@AndroidEntryPoint
class AuthFlowCoordinatorFragment : Fragment(R.layout.fragment_auth_coordinator) {

    @Inject
    lateinit var authRepository: AuthRepository
    private var logoutDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (RootBeer(context).isRooted) {
            Toast.makeText(context, R.string.warning_unsafe_device, Toast.LENGTH_LONG).show()
        }

        lifecycleScope.launchWhenStarted {
            val token = authRepository.getToken()
            if (token.isNotEmpty()) {
                if (authRepository.isTimeToShowCheckPin()) {
                    val dest =
                        AuthFlowCoordinatorFragmentDirections.actionAuthFlowCoordinatorFragmentToCheckPinFragment(
                            AuthModel(""),
                            CheckPinMode.CheckPin
                        )
                    findNavController().navigate(dest)
                } else {
                    navigator().navigateToFlow(NavigationFlow.BottomViewFlow)
                }
            } else {
                val dest =
                    AuthFlowCoordinatorFragmentDirections.actionAuthFlowCoordinatorFragmentToLoginFragment()
                findNavController().navigate(dest)
            }
        }
    }
}