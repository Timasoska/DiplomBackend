package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReferenceMaterialDto(
    val id: Int,
    val title: String,
    val url: String
)