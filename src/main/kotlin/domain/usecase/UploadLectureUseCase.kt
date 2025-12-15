package org.example.domain.usecase

import org.example.data.db.Lectures
import org.example.data.db.dbQuery
import org.example.domain.service.DocumentService
import org.jetbrains.exposed.sql.insert
import java.io.InputStream

class UploadLectureUseCase(
    private val documentService: DocumentService
) {
    suspend operator fun invoke(topicId: Int, title: String, fileStream: InputStream) {
        // 1. Конвертируем
        val markdownContent = documentService.convertDocxToMarkdown(fileStream)

        // 2. Сохраняем в БД
        dbQuery {
            Lectures.insert {
                it[Lectures.title] = title
                it[Lectures.content] = markdownContent
                it[Lectures.topicId] = topicId
            }
        }
    }
}