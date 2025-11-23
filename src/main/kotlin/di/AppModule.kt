package org.example.di

import org.example.data.repository.ContentRepositoryImpl
import org.example.domain.repository.ContentRepository
import org.example.domain.usecase.GetDisciplinesUseCase
import org.koin.dsl.module

val appModule = module {
    // Singleton: один экземпляр репозитория на все приложение
    single<ContentRepository> { ContentRepositoryImpl() }

    // Factory: создается каждый раз, когда нужен
    factory { GetDisciplinesUseCase(get()) }
}