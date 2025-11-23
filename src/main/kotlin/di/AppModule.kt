package org.example.di

import org.example.data.repository.AuthRepositoryImpl
import org.example.data.repository.ContentRepositoryImpl
import org.example.domain.repository.AuthRepository
import org.example.domain.repository.ContentRepository
import org.example.domain.usecase.GetDisciplinesUseCase
import org.example.domain.usecase.LoginUseCase
import org.example.domain.usecase.RegisterUseCase
import org.example.features.auth.security.PasswordService
import org.example.features.auth.security.TokenService
import org.koin.dsl.module

val appModule = module {
    // Auth
    single<AuthRepository> { AuthRepositoryImpl() }
    single { PasswordService() }
    single { TokenService() }

    factory { RegisterUseCase(get(), get(), get()) }
    factory { LoginUseCase(get(), get(), get()) }

    // Content
    single<ContentRepository> { ContentRepositoryImpl() }
    factory { GetDisciplinesUseCase(get()) }
}