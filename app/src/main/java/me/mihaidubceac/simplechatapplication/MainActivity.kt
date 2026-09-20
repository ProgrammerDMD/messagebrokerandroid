package me.mihaidubceac.simplechatapplication

import android.os.Bundle
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.mihaidubceac.simplechatapplication.ui.theme.SimpleChatApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScreenState {
    LOGIN,
    CHAT
}

data class ChatMessage(
    val id: String,
    val name: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleChatApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var name by remember { mutableStateOf("") }
                    var screenState by remember { mutableStateOf(ScreenState.LOGIN) }

                    when (screenState) {
                        ScreenState.LOGIN -> LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            value = name,
                            onValueChange = { name = it },
                            onConnect = {
                                screenState = ScreenState.CHAT
                            }
                        )
                        ScreenState.CHAT -> ChatScreen(
                            modifier = Modifier.padding(innerPadding),
                            name = name,
                        )
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
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = message.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard {
            Text(
                text = message.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "Sent at " + formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
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
            text = "Please choose a name to start chatting!",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text("John Doe")
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            enabled = value.isNotBlank(),
            modifier = Modifier.width(128.dp),
            onClick = onConnect
        ) {
            Text("Connect")
        }
    }
}


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    message: String,
    onValueChange: (String) -> Unit,
    messages: List<ChatMessage>,
    onSendMessage: () -> Unit,
) {
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
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageItem(message = msg)
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
                enabled = message.isNotBlank(), onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SimpleChatApplicationTheme {
        Greeting("Android")
    }
}