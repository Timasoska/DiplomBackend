package org.example.domain.repository

import org.example.domain.model.Discipline
import org.example.domain.model.Lecture
import org.example.domain.model.Topic

interface ContentRepository {
    suspend fun getAllDisciplines(): List<Discipline>
    // Новые методы
    suspend fun getTopicsByDisciplineId(disciplineId: Int): List<Topic>
    suspend fun getLectureByTopicId(topicId: Int): List<Lecture> // Или одна лекция на тему, зависит от логики. Сделаем список.
    suspend fun getLectureById(lectureId: Int): Lecture?
    suspend fun addFavorite(userId: Int, lectureId: Int)
    suspend fun removeFavorite(userId: Int, lectureId: Int)
    suspend fun getFavorites(userId: Int): List<Lecture>
}