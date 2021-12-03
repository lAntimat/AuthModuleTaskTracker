package ru.cometrica.usmtask.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.cometrica.apimodels.Resource
import ru.cometrica.commonui.clearPhone
import ru.cometrica.data.data.AuthRepository
import ru.cometrica.data.data.Country
import ru.cometrica.data.ext.toStringError
import ru.cometrica.usmtask.auth.models.AuthModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _openSmsConfirm = MutableSharedFlow<AuthModel>()
    val openSmsConfirm: SharedFlow<AuthModel>
        get() = _openSmsConfirm

    private val _openBsCountries = MutableSharedFlow<ArrayList<Country>>()
    val openBsCountries: SharedFlow<ArrayList<Country>>
        get() = _openBsCountries

    private val _errorToShow = MutableSharedFlow<String>()
    val errorToShow: SharedFlow<String>
        get() = _errorToShow

    private val countries = arrayListOf<Country>()

    private val _currentCountry = MutableStateFlow(Country())
    val currentCountry: StateFlow<Country>
        get() = _currentCountry

    init {
        viewModelScope.launch {
            countries.addAll(repository.getCountryList())

            val currentCountry = countries.first()
            _currentCountry.value = currentCountry
        }
    }

    fun onLoginClick(phone: String) {
        viewModelScope.launch {
            when (val result = repository.authenticate(clearPhone(phone))) {
                is Resource.Success -> _openSmsConfirm.emit(AuthModel(phone, result.data.nonce))
                is Resource.Error -> _errorToShow.emit(result.toStringError())
            }
        }
    }

    fun onCountryClick(position: Int) {
        viewModelScope.launch {
            _currentCountry.value = countries[position]
        }
    }

    fun onOpenCountryBsClick() {
        viewModelScope.launch {
            _openBsCountries.emit(countries)
        }
    }
}