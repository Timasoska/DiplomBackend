package org.example.domain.repository

import org.example.data.dto.LeaderboardItemDto
import org.example.data.dto.LectureDto
import org.example.data.dto.ProgressDto
import org.example.domain.model.Discipline
import org.example.domain.model.Lecture
import org.example.domain.model.Test
import org.example.domain.model.Topic

interface ContentRepository {
    suspend fun getAllDisciplines(): List<Discipline>
    // Новые методы
    suspend fun getTopicsByDisciplineId(disciplineId: Int): List<Topic>
    suspend fun getLectureByTopicId(topicId: Int): List<Lecture> // Или одна лекция на тему, зависит от логики. Сделаем список.
    suspend fun getLectureById(lectureId: Int, userId: Int): LectureDto?
    suspend fun addFavorite(userId: Int, lectureId: Int)
    suspend fun removeFavorite(userId: Int, lectureId: Int)
    suspend fun getFavorites(userId: Int): List<Lecture>
    suspend fun searchLectures(query: String): List<Lecture>
    suspend fun getTestByTopicId(topicId: Int): Test?
    suspend fun saveTestAttempt(userId: Int, testId: Int, score: Int)
    suspend fun getCorrectAnswers(testId: Int): Map<Int, List<Int>>
    // Возвращаем пару: (ID Темы, Балл за попытку)
    // Этот метод теперь возвращает готовую статистику
    suspend fun getFullProgress(userId: Int): ProgressDto
    suspend fun getUserTestResults(userId: Int): List<Pair<Int, Int>>
    suspend fun getLeaderboard(): List<LeaderboardItemDto>
}