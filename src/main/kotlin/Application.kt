package org.example

// 1. Добавили правильный импорт для ContentNegotiation
import io.ktor.server.plugins.contentnegotiation.* // <--- ВАЖНО
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
// 2. Добавили импорт для launch
import kotlinx.coroutines.launch // <--- ВАЖНО

// Твои импорты (убедись, что пакеты совпадают с твоей структурой папок)
import org.example.data.db.Disciplines
import org.example.data.db.dbQuery
import org.example.di.appModule
import org.example.features.content.contentRouting
import org.example.plugins.configureDatabases
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // 1. Настройка JSON
    install(ContentNegotiation) { // <--- Теперь ошибка уйдет
        json()
    }

    // 2. Настройка Koin (DI)
    install(Koin) {
        modules(appModule)
    }

    // 3. Подключение БД
    configureDatabases()

    // 4. Заполнение базы (исправлен синтаксис запуска)
    launch {
        dbQuery {
            // Проверка: если таблица пустая, заполняем
            if (Disciplines.selectAll().empty()) {
                Disciplines.insert {
                    it[name] = "Уголовное право"
                    it[description] = "Изучение преступлений и наказаний"
                }
                Disciplines.insert {
                    it[name] = "Гражданское право"
                    it[description] = "Регулирование отношений между гражданами"
                }
                Disciplines.insert {
                    it[name] = "Административное право"
                    it[description] = "Управление и взаимодействие с государством"
                }
                // Можно добавить лог, чтобы видеть в консоли, что сработало
                println("База данных успешно инициализирована тестовыми данными!")
            }
        }
    }

    // 5. Роутинг
    routing {
        contentRouting()
    }
}