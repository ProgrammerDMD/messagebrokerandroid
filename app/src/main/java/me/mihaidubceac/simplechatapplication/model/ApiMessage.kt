package me.mihaidubceac.simplechatapplication.model

import kotlinx.serialization.Serializable

/**
 * Wire format used when talking to the server (GET /messages, POST /message).
 * Kept separate from [ChatMessage], which is the UI-facing model that
 * ChatScreen actually renders and that carries local-only state like
 * [MessageStatus].
 */
@Serializable
data class ApiMessage(
    val id: String,
    val userId: String,
    val topic: String,
    val content: String,
    val timestamp: Long,
)
