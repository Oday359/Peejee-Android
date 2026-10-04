package com.peejee.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PrivateChatPageNew(
    person: PeejeePerson,
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val currentUserId =
        auth.currentUser?.uid ?: ""

    var messages by remember {
        mutableStateOf<List<PeejeeMessage>>(emptyList())
    }

    var newMessage by remember {
        mutableStateOf("")
    }

    var sending by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var showEmojiRow by remember {
        mutableStateOf(false)
    }

    /*
     * Mark received messages as read when the chat is opened.
     */
    LaunchedEffect(
        currentUserId,
        person.uid
    ) {
        if (
            currentUserId.isNotBlank() &&
            person.uid.isNotBlank()
        ) {
            firestore
                .collection("messages")
                .whereEqualTo(
                    "senderId",
                    person.uid
                )
                .whereEqualTo(
                    "receiverId",
                    currentUserId
                )
                .whereEqualTo(
                    "read",
                    false
                )
                .get()
                .addOnSuccessListener { snapshot ->

                    if (!snapshot.isEmpty) {
                        val batch =
                            firestore.batch()

                        snapshot.documents.forEach { document ->
                            batch.update(
                                document.reference,
                                "read",
                                true
                            )
                        }

                        batch.commit()
                    }
                }
        }
    }

    /*
     * Listen for messages between both users.
     */
    DisposableEffect(
        currentUserId,
        person.uid
    ) {
        if (
            currentUserId.isBlank() ||
            person.uid.isBlank()
        ) {
            onDispose { }
        } else {

            val sentRegistration =
                firestore
                    .collection("messages")
                    .whereEqualTo(
                        "senderId",
                        currentUserId
                    )
                    .whereEqualTo(
                        "receiverId",
                        person.uid
                    )
                    .addSnapshotListener { sentSnapshot, error ->

                        if (error != null) {
                            return@addSnapshotListener
                        }

                        firestore
                            .collection("messages")
                            .whereEqualTo(
                                "senderId",
                                person.uid
                            )
                            .whereEqualTo(
                                "receiverId",
                                currentUserId
                            )
                            .get()
                            .addOnSuccessListener { receivedSnapshot ->

                                val sentMessages =
                                    sentSnapshot
                                        ?.documents
                                        ?.mapNotNull {
                                            messageFromDocument(it)
                                        }
                                        ?: emptyList()

                                val receivedMessages =
                                    receivedSnapshot
                                        .documents
                                        .mapNotNull {
                                            messageFromDocument(it)
                                        }

                                messages =
                                    (
                                        sentMessages +
                                            receivedMessages
                                        )
                                        .distinctBy {
                                            it.id
                                        }
                                        .sortedBy {
                                            it.timestamp
                                        }
                            }
                    }

            val receivedRegistration =
                firestore
                    .collection("messages")
                    .whereEqualTo(
                        "senderId",
                        person.uid
                    )
                    .whereEqualTo(
                        "receiverId",
                        currentUserId
                    )
                    .addSnapshotListener { receivedSnapshot, error ->

                        if (error != null) {
                            return@addSnapshotListener
                        }

                        firestore
                            .collection("messages")
                            .whereEqualTo(
                                "senderId",
                                currentUserId
                            )
                            .whereEqualTo(
                                "receiverId",
                                person.uid
                            )
                            .get()
                            .addOnSuccessListener { sentSnapshot ->

                                val sentMessages =
                                    sentSnapshot
                                        .documents
                                        .mapNotNull {
                                            messageFromDocument(it)
                                        }

                                val receivedMessages =
                                    receivedSnapshot
                                        ?.documents
                                        ?.mapNotNull {
                                            messageFromDocument(it)
                                        }
                                        ?: emptyList()

                                messages =
                                    (
                                        sentMessages +
                                            receivedMessages
                                        )
                                        .distinctBy {
                                            it.id
                                        }
                                        .sortedBy {
                                            it.timestamp
                                        }
                            }
                    }

            onDispose {
                sentRegistration.remove()
                receivedRegistration.remove()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        /*
         * CHAT HEADER
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {
                Text("← Back")
            }

            DefaultProfileIcon(
                size = 45
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = person.name,
                    fontSize = 18.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        if (person.isOnline) {
                            "🟢 Online"
                        } else {
                            "⚫ Offline"
                        },
                    fontSize = 12.sp
                )
            }

            /*
             * Voice call button.
             * The actual WebRTC call system
             * will be connected in the next step.
             */
            IconButton(
                onClick = {
                    errorMessage =
                        "Voice call will be connected next."
                }
            ) {
                Text(
                    "📞",
                    fontSize = 22.sp
                )
            }

            /*
             * Video call button.
             * The actual WebRTC call system
             * will be connected in the next step.
             */
            IconButton(
                onClick = {
                    errorMessage =
                        "Video call will be connected next."
                }
            ) {
                Text(
                    "🎥",
                    fontSize = 22.sp
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
            color =
                MaterialTheme
                    .colorScheme
                    .outlineVariant
        ) {}

        /*
         * MESSAGE LIST
         */
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            itemsIndexed(
                messages,
                key = { _, message ->
                    message.id
                }
            ) { _, message ->

                val mine =
                    message.senderId ==
                        currentUserId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 3.dp
                        ),
                    horizontalArrangement =
                        if (mine) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                ) {

                    Surface(
                        modifier = Modifier
                            .widthIn(
                                max = 300.dp
                            ),
                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),
                        color =
                            if (mine) {
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                            }
                    ) {

                        Column(
                            modifier = Modifier
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 9.dp
                                )
                        ) {

                            Text(
                                text =
                                    message.text,
                                fontSize = 16.sp
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text =
                                        formatPostTime(
                                            message.timestamp
                                        ),
                                    fontSize = 9.sp
                                )

                                if (mine) {

                                    Spacer(
                                        Modifier.width(5.dp)
                                    )

                                    Text(
                                        text =
                                            if (message.read) {
                                                "✓✓ Seen"
                                            } else {
                                                "✓ Sent"
                                            },
                                        fontSize = 9.sp,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (errorMessage.isNotBlank()) {

            Text(
                text = errorMessage,
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                fontSize = 12.sp,
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 3.dp
                    )
            )
        }

        /*
         * SIMPLE EMOJI ROW
         */
        if (showEmojiRow) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),
                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                listOf(
                    "😀",
                    "😂",
                    "😍",
                    "❤️",
                    "👍",
                    "🙏",
                    "🔥",
                    "🎉"
                ).forEach { emoji ->

                    TextButton(
                        onClick = {
                            newMessage += emoji
                        }
                    ) {
                        Text(
                            emoji,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }

        /*
         * MESSAGE INPUT
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    showEmojiRow =
                        !showEmojiRow
                }
            ) {
                Text(
                    "😊",
                    fontSize = 25.sp
                )
            }

            OutlinedTextField(
                value = newMessage,
                onValueChange = {
                    newMessage = it
                    errorMessage = ""
                },
                modifier =
                    Modifier.weight(1f),
                placeholder = {
                    Text("Message...")
                },
                enabled = !sending,
                maxLines = 4
            )

            Spacer(
                Modifier.width(6.dp)
            )

            Button(
                onClick = {

                    val text =
                        newMessage.trim()

                    if (
                        currentUserId.isBlank() ||
                        person.uid.isBlank()
                    ) {

                        errorMessage =
                            "Please log in again."

                        return@Button
                    }

                    if (text.isBlank()) {
                        return@Button
                    }

                    sending = true
                    errorMessage = ""

                    val messageReference =
                        firestore
                            .collection("messages")
                            .document()

                    val messageData =
                        hashMapOf<String, Any>(
                            "messageId" to
                                messageReference.id,
                            "senderId" to
                                currentUserId,
                            "receiverId" to
                                person.uid,
                            "text" to text,
                            "timestamp" to
                                System.currentTimeMillis(),
                            "read" to false
                        )

                    messageReference
                        .set(messageData)
                        .addOnSuccessListener {

                            sending = false
                            newMessage = ""
                            showEmojiRow = false

                            createPeejeeNotification(
                                firestore =
                                    firestore,
                                recipientUserId =
                                    person.uid,
                                type = "message",
                                actorId =
                                    currentUserId,
                                actorName =
                                    "Peejee User",
                                text =
                                    "sent you a message",
                                messageId =
                                    messageReference.id
                            )
                        }
                        .addOnFailureListener { exception ->

                            sending = false

                            errorMessage =
                                exception.message
                                    ?: "Could not send message."
                        }
                },
                enabled =
                    !sending &&
                        newMessage.isNotBlank()
            ) {

                Text(
                    if (sending) {
                        "..."
                    } else {
                        "Send"
                    }
                )
            }
        }
    }
}
