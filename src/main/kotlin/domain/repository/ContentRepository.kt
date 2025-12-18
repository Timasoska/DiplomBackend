package org.example.domain.repository

import org.example.data.dto.*
import org.example.data.loader.SeedDiscipline
import org.example.domain.model.Discipline
import org.example.domain.model.Lecture
import org.example.domain.model.Test
import org.example.domain.model.Topic

interface ContentRepository {
    suspend fun getAllDisciplines(): List<Discipline>
    // Новые методы
    suspend fun getTopicsByDisciplineId(disciplineId: Int): List<Topic>
    suspend fun getLectureByTopicId(topicId: Int, userId: Int): List<Lecture>
    suspend fun getLectureById(lectureId: Int, userId: Int): LectureDto?
    suspend fun addFavorite(userId: Int, lectureId: Int)
    suspend fun removeFavorite(userId: Int, lectureId: Int)
    suspend fun getFavorites(userId: Int): List<Lecture>
    suspend fun searchLectures(query: String): List<Lecture>
    suspend fun getTestByTopicId(topicId: Int): Test?
    suspend fun saveTestAttempt(userId: Int, testId: Int, score: Int)
    suspend fun getCorrectAnswers(testId: Int): Map<Int, List<Int>>
    suspend fun getFullProgress(userId: Int): ProgressDto
    suspend fun getUserTestResults(userId: Int): List<Pair<Int, Int>>
    suspend fun getLeaderboard(): List<LeaderboardItemDto>
    suspend fun saveLectureProgress(userId: Int, lectureId: Int, index: Int, quote: String?)
    suspend fun getLectureProgress(userId: Int, lectureId: Int): LectureProgressDto?

    // --- ADMIN ---
    suspend fun importContent(data: List<SeedDiscipline>)

    suspend fun updateLecture(id: Int, title: String, content: String)
    suspend fun deleteLecture(id: Int)
    suspend fun saveTest(request: SaveTestRequest)
    suspend fun getFullTestByTopicId(topicId: Int): AdminTestResponse?
    suspend fun getTestByLectureId(lectureId: Int): Test?
    suspend fun getFullTestByLectureId(lectureId: Int): AdminTestResponse?

    suspend fun getReferenceMaterials(): List<ReferenceMaterialDto>
    suspend fun addReferenceMaterial(title: String, url: String)

    // Метод для прикрепления файла (админка)
    suspend fun attachFileToLecture(lectureId: Int, title: String, filePath: String)
    suspend fun saveTopic(disciplineId: Int, name: String)

}