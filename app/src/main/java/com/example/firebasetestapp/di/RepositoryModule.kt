package com.example.firebasetestapp.di

import com.example.firebasetestapp.repository.auth.AuthRepository
import com.example.firebasetestapp.repository.auth.AuthRepositoryImpl
import com.example.firebasetestapp.repository.auth.PhoneAuthRepository
import com.example.firebasetestapp.repository.auth.PhoneAuthRepositoryImpl
import com.example.firebasetestapp.repository.pref.UserSettingsPrefRepository
import com.example.firebasetestapp.repository.pref.UserSettingsPrefRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ) : AuthRepository

    @Binds
    @Singleton
    abstract fun bindPhoneAuthRepository(
        phoneAuthRepositoryImpl: PhoneAuthRepositoryImpl
    ) : PhoneAuthRepository

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(
        userSettingsPrefRepositoryImpl: UserSettingsPrefRepositoryImpl
    ) : UserSettingsPrefRepository

}