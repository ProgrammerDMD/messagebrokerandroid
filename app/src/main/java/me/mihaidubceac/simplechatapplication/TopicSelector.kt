package me.mihaidubceac.simplechatapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.mihaidubceac.simplechatapplication.model.Topic

@Composable
fun TopicSelector(
    modifier: Modifier = Modifier,
    topics: List<Topic>,
    selectedTopic: Topic,
    onTopicSelected: (Topic) -> Unit,
    onSubscribeTopic: (topicId: String) -> Unit,
) {
    var showTopicDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showTopicDialog = true },
        modifier = modifier.fillMaxWidth(),
    ) {
        Text("# ${selectedTopic.name}")
    }

    if (showTopicDialog) {
        TopicListDialog(
            topics = topics,
            selectedTopic = selectedTopic,
            onDismiss = { showTopicDialog = false },
            onTopicSelected = { topic ->
                onTopicSelected(topic)
                showTopicDialog = false
            },
            onSubscribeTopic = onSubscribeTopic,
        )
    }
}

@Composable
private fun TopicListDialog(
    topics: List<Topic>,
    selectedTopic: Topic,
    onDismiss: () -> Unit,
    onTopicSelected: (Topic) -> Unit,
    onSubscribeTopic: (topicId: String) -> Unit,
) {
    var showSubscribeDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Topics", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(topics, key = { it.id }) { topic ->
                        TopicRow(
                            topic = topic,
                            selected = topic.id == selectedTopic.id,
                            onClick = { onTopicSelected(topic) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showSubscribeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Subscribe to topic")
                }
            }
        }
    }

    if (showSubscribeDialog) {
        SubscribeTopicDialog(
            onDismiss = { showSubscribeDialog = false },
            onSubscribe = { topicId ->
                onSubscribeTopic(topicId)
                showSubscribeDialog = false
            },
        )
    }
}

@Composable
private fun TopicRow(
    topic: Topic,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(topic.name, style = MaterialTheme.typography.bodyLarge)
        if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected")
        }
    }
}

@Composable
private fun SubscribeTopicDialog(
    onDismiss: () -> Unit,
    onSubscribe: (topicId: String) -> Unit,
) {
    var topicId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subscribe to a topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = topicId,
                    onValueChange = { topicId = it },
                    singleLine = true,
                    label = { Text("Topic ID") },
                    placeholder = { Text("general2") },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = topicId.isNotBlank(),
                onClick = { onSubscribe(topicId.trim()) },
            ) {
                Text("Subscribe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
