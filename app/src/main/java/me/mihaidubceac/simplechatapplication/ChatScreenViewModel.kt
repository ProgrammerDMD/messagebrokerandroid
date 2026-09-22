package me.mihaidubceac.simplechatapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.mihaidubceac.simplechatapplication.model.ApiMessage
import me.mihaidubceac.simplechatapplication.model.ChatMessage
import me.mihaidubceac.simplechatapplication.model.ChatScreenDetails
import me.mihaidubceac.simplechatapplication.model.DefaultTopic
import me.mihaidubceac.simplechatapplication.model.MessageAcknowledgmentRequest
import me.mihaidubceac.simplechatapplication.model.MessageStatus
import me.mihaidubceac.simplechatapplication.model.Topic
import me.mihaidubceac.simplechatapplication.model.toChatMessage
import java.util.UUID
import kotlin.collections.emptyMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel(assistedFactory = ChatScreenViewModelFactory::class)
class ChatScreenViewModel @AssistedInject constructor(
    @Assisted private val details: ChatScreenDetails
) : ViewModel() {

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(
        mutableMapOf()
    )

    val messages = _messages.stateIn(
        scope = viewModelScope,
        initialValue = emptyMap(),
        started = SharingStarted.WhileSubscribed(5000L)
    )

    private val _topics = MutableStateFlow(listOf(DefaultTopic))

    val topics = _topics.stateIn(
        scope = viewModelScope,
        initialValue = listOf(DefaultTopic),
        started = SharingStarted.WhileSubscribed(5000L)
    )

    private val _selectedTopic = MutableStateFlow(DefaultTopic)

    val selectedTopic = _selectedTopic.stateIn(
        scope = viewModelScope,
        initialValue = DefaultTopic,
        started = SharingStarted.WhileSubscribed(5000L)
    )

    val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1000.milliseconds)
                Log.d("ChatScreenViewModel", "Checking messages from API")

                try {
                    val newMessages: List<ApiMessage> = httpClient
                        .get("http://${details.ipAddress}/messages/${details.userId}")
                        .body()

                    if (newMessages.isEmpty()) continue

                    httpClient.post("http://${details.ipAddress}/acknowledge") {
                        contentType(ContentType.Application.Json)
                        setBody(MessageAcknowledgmentRequest(
                            userId = details.userId,
                            messageIds = newMessages.map { it.id }
                        ))
                    }

                    _messages.update { current ->
                        val incomingByTopic = newMessages
                            .sortedBy { it.timestamp }
                            .map { it.toChatMessage(status = MessageStatus.SENT) }
                            .groupBy { it.topic }

                        if (incomingByTopic.isEmpty()) return@update current

                        val updated = current.toMutableMap()
                        incomingByTopic.forEach { (topic, incoming) ->
                            val incomingIds = incoming.mapTo(HashSet()) { it.id }
                            val existing = updated[topic].orEmpty().filterNot { it.id in incomingIds }
                            updated[topic] = existing + incoming
                        }
                        updated
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("ChatScreenViewModel", "Failed to fetch messages", e)
                }
            }
        }
    }

    fun selectTopic(topic: Topic) {
        _selectedTopic.value = topic
    }

    fun subscribeToTopic(topicId: String) {
        if (topicId.isBlank()) return

        val newTopic = Topic(
            id = topicId,
            name = topicId.replaceFirstChar { it.uppercase() },
        )

        _topics.update { current ->
            if (current.any { it.id == newTopic.id }) current else current + newTopic
        }

        selectTopic(newTopic)
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val topic = selectedTopic.value
        val localId = UUID.randomUUID().toString()
        val chatMessage = ChatMessage(
            id = localId,
            userId = details.userId,
            topic = topic.id,
            content = content,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
        )

        _messages.update { current ->
            val updated = current.toMutableMap()
            updated[topic.id] = updated[topic.id].orEmpty() + chatMessage
            updated
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                httpClient
                    .post("http://${details.ipAddress}/message") {
                        contentType(ContentType.Application.Json)
                        setBody(ApiMessage(
                            id = localId,
                            userId = details.userId,
                            topic = topic.id,
                            content = chatMessage.content,
                            timestamp = chatMessage.timestamp
                        ))
                    }
            } catch (e: CancellationException) {
                updateMessageStatus(topic.id, localId, MessageStatus.ERROR)
                throw e
            } catch (e: Exception) {
                Log.e("ChatScreenViewModel", "Failed to send message", e)
                updateMessageStatus(topic.id, localId, MessageStatus.ERROR)
            }
        }
    }

    private fun updateMessageStatus(topicId: String, messageId: String, status: MessageStatus) {
        _messages.update { current ->
            val updated = current.toMutableMap()
            updated[topicId] = updated[topicId].orEmpty().map { msg ->
                if (msg.id == messageId) msg.copy(status = status) else msg
            }
            updated
        }
    }
}

@AssistedFactory
interface ChatScreenViewModelFactory {
    fun create(details: ChatScreenDetails): ChatScreenViewModel
}
