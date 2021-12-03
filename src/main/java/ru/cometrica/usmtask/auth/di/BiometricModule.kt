package ru.cometrica.usmtask.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.cometrica.usmtask.auth.biometrichelper.BiometricHelper
import ru.cometrica.usmtask.auth.biometrichelper.BiometricHelperImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class BiometricModule {

    @Binds
    abstract fun bindBiometricHelper(biometricHelper: BiometricHelperImpl): BiometricHelper
}