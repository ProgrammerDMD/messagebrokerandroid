package me.mihaidubceac.simplechatapplication

import android.util.Log
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.mihaidubceac.simplechatapplication.model.ApiConnectRequest
import me.mihaidubceac.simplechatapplication.model.MessageAcknowledgmentRequest
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    // Screen state: whether a connection attempt is currently in flight.
    // The LoginScreen collects this with collectAsStateWithLifecycle()
    // to disable its inputs/button while true.
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    // One-off error events. A Channel (rather than a StateFlow) is used because
    // each error should be delivered/consumed exactly once — the screen shows a
    // Toast for it, and it shouldn't be redelivered on recomposition or re-collection.
    private val _errorChannel = Channel<String>()

    // Exposed as a hot SharedFlow with replay = 0, scoped to viewModelScope, so the
    // screen can collect it with a plain `collect { }` inside a LaunchedEffect.
    // replay = 0 is intentional: a StateFlow (via stateIn) always replays its latest
    // value to new collectors, which would re-show a stale error after e.g. a
    // config change. shareIn(..., replay = 0) gives fresh, event-style delivery.
    val errorEvents: SharedFlow<String> = _errorChannel.receiveAsFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 0
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

    /**
     * Attempts to connect with the given [name] and [ipAddress].
     * Disables further attempts while one is in flight, and reports failures
     * through [errorEvents] instead of throwing.
     */
    fun connect(name: String, ipAddress: String, onConnected: (String) -> Unit) {
        if (_isConnecting.value) return

        viewModelScope.launch {
            _isConnecting.value = true
            try {
                val result = attemptConnection(name, ipAddress)
                if (result) {
                    onConnected(name.lowercase().trim().replace(" ", ""))
                } else {
                    _errorChannel.send("Could not connect to $ipAddress")
                }
            } catch (e: Exception) {
                _errorChannel.send(e.message ?: "Could not connect to $ipAddress")
            } finally {
                _isConnecting.value = false
            }
        }
    }

    /**
     * Placeholder for the real connection logic (socket handshake, auth call, etc.).
     * Replace this with whatever the app actually uses to open the chat connection
     * (e.g. a repository call), and have it throw on failure so [connect] can
     * surface the error message through [errorEvents].
     */
    private suspend fun attemptConnection(name: String, ipAddress: String): Boolean {
        try {
            val request = httpClient.post("http://${ipAddress}/connect") {
                contentType(ContentType.Application.Json)
                setBody(ApiConnectRequest(
                    id = name.lowercase().trim().replace(" ", ""),
                    name = name
                ))
            }

            if (request.status.isSuccess()) return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Failed to connect", e)
        }
        return false
    }
}
