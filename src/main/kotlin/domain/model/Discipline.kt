package org.example.domain.model


import kotlinx.serialization.Serializable

@Serializable // Нужно для автоматического превращения в JSON
data class Discipline(
    val id: Int,
    val name: String,
    val description: String
)