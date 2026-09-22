package me.mihaidubceac.simplechatapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import me.mihaidubceac.simplechatapplication.model.ChatMessage
import me.mihaidubceac.simplechatapplication.model.ChatScreenDetails
import me.mihaidubceac.simplechatapplication.model.MessageStatus
import me.mihaidubceac.simplechatapplication.model.Topic
import me.mihaidubceac.simplechatapplication.ui.theme.SimpleChatApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScreenState {
    LOGIN,
    CHAT
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleChatApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var userId by remember { mutableStateOf("") }
                    var name by remember { mutableStateOf("") }
                    var ipAddress by remember { mutableStateOf("") }
                    var screenState by remember { mutableStateOf(ScreenState.LOGIN) }
                    var message by remember { mutableStateOf("") }
                    when (screenState) {
                        ScreenState.LOGIN -> LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            name = name,
                            ipAddress = ipAddress,
                            onNameChange = { name = it },
                            onIpAddressChange = { ipAddress = it },
                            onConnect = {
                                screenState = ScreenState.CHAT
                                userId = it
                            }
                        )
                        ScreenState.CHAT -> {
                            val viewModel = hiltViewModel(
                                creationCallback = { factory: ChatScreenViewModelFactory ->
                                    factory.create(ChatScreenDetails(
                                        userId = userId,
                                        ipAddress = ipAddress
                                    ))
                                }
                            )

                            val messages by viewModel.messages.collectAsStateWithLifecycle()
                            val topics by viewModel.topics.collectAsStateWithLifecycle()
                            val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()

                            ChatScreen(
                                modifier = Modifier.padding(innerPadding),
                                currentUserId = userId,
                                message = message,
                                onValueChange = { message = it },
                                messages = messages[selectedTopic.id] ?: emptyList(),
                                topics = topics,
                                selectedTopic = selectedTopic,
                                onTopicSelected = viewModel::selectTopic,
                                onSubscribeTopic = viewModel::subscribeToTopic,
                                onSendMessage = {
                                    viewModel.sendMessage(message)
                                    message = ""
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    isOwnMessage: Boolean,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = message.userId,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard {
            Text(
                text = message.content,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sent at " + formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isOwnMessage) {
                Text(
                    text = when (message.status) {
                        MessageStatus.SENDING -> "Sending…"
                        MessageStatus.SENT -> "Sent"
                        MessageStatus.ERROR -> "Failed to send"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.status == MessageStatus.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    name: String,
    ipAddress: String,
    onIpAddressChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onConnect: (String) -> Unit,
) {
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(
            8.dp,
            Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please choose a name and server's IP to start chatting!",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isConnecting,
            placeholder = {
                Text("John Doe")
            }
        )
        TextField(
            value = ipAddress,
            onValueChange = onIpAddressChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isConnecting,
            placeholder = {
                Text("127.0.0.1:9001")
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            enabled = name.isNotBlank() && ipAddress.isNotBlank() && !isConnecting,
            modifier = Modifier.width(128.dp),
            onClick = {
                viewModel.connect(
                    name = name,
                    ipAddress = ipAddress,
                    onConnected = onConnect,
                )
            }
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Connect")
            }
        }
    }
}


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    currentUserId: String,
    message: String,
    onValueChange: (String) -> Unit,
    messages: List<ChatMessage>,
    topics: List<Topic>,
    selectedTopic: Topic,
    onTopicSelected: (Topic) -> Unit,
    onSubscribeTopic: (topicId: String) -> Unit,
    onSendMessage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .imePadding()
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(
            8.dp,
            Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopicSelector(
            topics = topics,
            selectedTopic = selectedTopic,
            onTopicSelected = onTopicSelected,
            onSubscribeTopic = onSubscribeTopic,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageItem(
                    message = msg,
                    isOwnMessage = msg.userId == currentUserId,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = message,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text("Type a message...")
                }
            )
            IconButton(
                enabled = message.isNotBlank(), onClick = onSendMessage) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}
