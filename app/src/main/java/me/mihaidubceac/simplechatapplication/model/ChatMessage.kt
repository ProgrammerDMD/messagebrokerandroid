package me.mihaidubceac.simplechatapplication.model

enum class MessageStatus {
    SENDING,
    SENT,
    ERROR,
}

/**
 * UI-facing message model used by ChatScreen / ChatScreenViewModel. Unlike
 * [ApiMessage] (the network wire format), this carries a [MessageStatus] so
 * the sender's own message bubble can show whether it's still going out,
 * has been sent, or failed to send.
 */
data class ChatMessage(
    val id: String,
    val userId: String,
    val topic: String,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT,
)

fun ApiMessage.toChatMessage(status: MessageStatus = MessageStatus.SENT): ChatMessage =
    ChatMessage(
        id = id,
        userId = userId,
        topic = topic,
        content = content,
        timestamp = timestamp,
        status = status,
    )
