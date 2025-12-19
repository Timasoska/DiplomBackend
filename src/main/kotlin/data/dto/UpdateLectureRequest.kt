package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateLectureRequest(
    val title: String,
    val content: String
)