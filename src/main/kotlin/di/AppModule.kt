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
    // Infrastructure & Services
    single<AuthRepository> { AuthRepositoryImpl() }
    single<ContentRepository> { ContentRepositoryImpl() }
    single { PasswordService() }
    single { TokenService() }
    single { DocumentService() }

    // Auth UseCases
    factory { RegisterUseCase(get(), get(), get()) }
    factory { LoginUseCase(get(), get(), get()) }

    // Content & Learning UseCases
    factory { GetDisciplinesUseCase(get()) }
    factory { GetTopicsUseCase(get()) }
    factory { GetLectureUseCase(get()) }
    factory { FavoritesUseCase(get()) }
    factory { SearchUseCase(get()) }
    factory { LectureProgressUseCase(get()) }
    factory { ImportContentUseCase(get()) }

    // Testing UseCases
    factory { GetTestUseCase(get()) }
    factory { GetTestByLectureUseCase(get()) }
    factory { SubmitTestUseCase(get()) }
    factory { SaveTestUseCase(get()) }
    factory { GetAdminTestUseCase(get()) }

    // Management UseCases (Teacher)
    factory { UploadLectureUseCase(get()) }
    factory { UpdateLectureUseCase(get()) }
    factory { DeleteLectureUseCase(get()) }
    factory { SaveTopicUseCase(get()) }
    factory { UpdateTopicUseCase(get()) }
    factory { DeleteTopicUseCase(get()) }

    // Groups & Analytics UseCases
    factory { CreateGroupUseCase(get()) }
    factory { JoinGroupUseCase(get()) }
    factory { GetTeacherGroupsUseCase(get()) }
    factory { GetAnalyticsUseCase(get()) }
    factory { UpdateGroupUseCase(get()) }
    factory { DeleteGroupUseCase(get()) }
    factory { RemoveStudentUseCase(get()) }

    // Analytics (Student)
    factory { GetRecommendationsUseCase(get()) }
    factory { GetProgressUseCase(get()) }
    factory { GetLeaderboardUseCase(get()) }
}