package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateTopicRequest(
    val name: String
)