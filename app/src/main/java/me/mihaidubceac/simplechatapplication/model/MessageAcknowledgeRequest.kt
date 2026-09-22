package me.mihaidubceac.simplechatapplication.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageAcknowledgmentRequest(
    val userId: String,
    val messageIds: List<String>,
)
