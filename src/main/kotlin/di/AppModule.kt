package org.example.di

import org.example.data.repository.AuthRepositoryImpl
import org.example.data.repository.ContentRepositoryImpl
import org.example.domain.repository.AuthRepository
import org.example.domain.repository.ContentRepository
import org.example.domain.service.DocumentService
import org.example.domain.usecase.*
import org.example.features.auth.security.PasswordService
import org.example.features.auth.security.TokenService
import org.koin.dsl.module

val appModule = module {
    // Auth
    single<AuthRepository> { AuthRepositoryImpl() }
    single { PasswordService() }
    single { TokenService() }
    single<ContentRepository> { ContentRepositoryImpl() } // Если еще не было


    factory { RegisterUseCase(get(), get(), get()) }
    factory { LoginUseCase(get(), get(), get()) }

    // Content
    single<ContentRepository> { ContentRepositoryImpl() }
    factory { GetDisciplinesUseCase(get()) }
    factory { GetTopicsUseCase(get()) } // <--- Добавили
    factory { GetLectureUseCase(get()) } // <--- Добавили
    factory { FavoritesUseCase(get()) }
    factory { SearchUseCase(get()) }
    factory { GetTestUseCase(get()) }
    factory { SubmitTestUseCase(get()) }
    factory { GetRecommendationsUseCase(get()) }
    factory { GetProgressUseCase(get()) }
    factory { GetProgressUseCase(get()) } // <--- ВОТ ЭТО ВАЖНО
    factory { GetLeaderboardUseCase(get()) } // <--- Добавили
    // Добавляем этот UseCase
    factory { LectureProgressUseCase(get()) }
    factory { ImportContentUseCase(get()) }

    // --- ДЛЯ WORD ИМПОРТА ---
    single { DocumentService() } // Сервис как синглтон
    factory { UploadLectureUseCase(get()) }
    factory { UpdateLectureUseCase(get()) }
    factory { DeleteLectureUseCase(get()) }
    factory { SaveTestUseCase(get()) }
    factory { GetAdminTestUseCase(get()) } // <--- Добавить


}