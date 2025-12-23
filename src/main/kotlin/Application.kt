package org.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.example.data.db.*
import org.example.di.appModule
import org.example.features.admin.adminRouting
import org.example.features.auth.authRouting
import org.example.features.content.contentRouting
import org.example.features.testing.testingRouting
import org.example.features.analytics.analyticsRouting
import org.example.features.content.groupRouting
import org.example.features.engagment.engagementRouting
import org.example.features.flashcards.flashcardRouting
import org.example.plugins.configureDatabases
import org.example.plugins.configureSecurity
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(Koin) {
        modules(appModule)
    }

    configureDatabases()
    configureSecurity()

    // Заполнение базы данными
    launch {
        org.example.data.loader.ContentLoader.loadAllContent()
    }

    routing {
        contentRouting()
        authRouting()
        testingRouting()
        analyticsRouting()
        adminRouting()
        groupRouting()
        flashcardRouting()
        engagementRouting() // <--- ДОБАВЛЕНО
    }
}