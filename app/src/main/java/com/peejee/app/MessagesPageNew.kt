package com.peejee.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

private data class PeejeeConversationRow(
    val user: PeejeePerson,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int
)

private fun readPeejeePerson(
    document: DocumentSnapshot,
    fallbackUid: String = document.id
): PeejeePerson {
    val name =
        document.getString("name")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: document.getString("displayName")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: document.getString("fullName")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: document.getString("username")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: "Peejee User"

    return PeejeePerson(
        uid = fallbackUid,
        name = name,
        email = document.getString("email").orEmpty(),
        isOnline = document.getBoolean("isOnline") ?: false,
        profilePhoto = document.getString("profilePhoto").orEmpty()
    )
}

@Composable
fun MessagesPageNew(
    onMessage: (PeejeePerson) -> Unit,
    paddingValues: PaddingValues
) {
    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUserId =
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    var searchText by remember {
        mutableStateOf("")
    }

    var allMessages by remember {
        mutableStateOf<List<PeejeeMessage>>(emptyList())
    }

    var searchUsers by remember {
        mutableStateOf<List<PeejeePerson>>(emptyList())
    }

    val userCache = remember {
        mutableStateMapOf<String, PeejeePerson>()
    }

    /*
     * Listen for sent and received messages separately.
     * Combine them and remove duplicate message IDs.
     */
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            allMessages = emptyList()

            onDispose {
                allMessages = emptyList()
            }
        } else {
            var sentMessages = emptyList<PeejeeMessage>()
            var receivedMessages = emptyList<PeejeeMessage>()

            fun updateCombinedMessages() {
                allMessages =
                    (sentMessages + receivedMessages)
                        .associateBy { it.id }
                        .values
                        .sortedByDescending { it.timestamp }
            }

            val sentListener =
                firestore.collection("messages")
                    .whereEqualTo("senderId", currentUserId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) {
                            return@addSnapshotListener
                        }

                        sentMessages = snapshot.documents.mapNotNull {
                            peejeeMessageFromDocument(it)
                        }

                        updateCombinedMessages()
                    }

            val receivedListener =
                firestore.collection("messages")
                    .whereEqualTo("receiverId", currentUserId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) {
                            return@addSnapshotListener
                        }

                        receivedMessages = snapshot.documents.mapNotNull {
                            peejeeMessageFromDocument(it)
                        }

                        updateCombinedMessages()
                    }

            onDispose {
                sentListener.remove()
                receivedListener.remove()
            }
        }
    }

    /*
     * Load profiles for people in existing conversations.
     * Profile photos are Base64 strings stored in users/{uid}.
     */
    LaunchedEffect(allMessages, currentUserId) {
        val otherUserIds =
            allMessages.mapNotNull { message ->
                when {
                    message.senderId == currentUserId ->
                        message.receiverId.takeIf { it.isNotBlank() }

                    message.receiverId == currentUserId ->
                        message.senderId.takeIf { it.isNotBlank() }

                    else -> null
                }
            }.distinct()

        otherUserIds.forEach { uid ->
            if (!userCache.containsKey(uid)) {
                userCache[uid] = PeejeePerson(
                    uid = uid,
                    name = "Peejee User"
                )

                firestore.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            userCache[uid] = readPeejeePerson(
                                document = document,
                                fallbackUid = uid
                            )
                        }
                    }
            }
        }

        /*
         * Remove profiles that no longer belong to a conversation.
         */
        userCache.keys
            .filter { it !in otherUserIds }
            .forEach { uid ->
                userCache.remove(uid)
            }
    }

    /*
     * Search for users by their saved name.
     */
    DisposableEffect(searchText.trim(), currentUserId) {
        val queryText = searchText.trim()

        if (queryText.isBlank()) {
            searchUsers = emptyList()

            onDispose {
                searchUsers = emptyList()
            }
        } else {
            val listener =
                firestore.collection("users")
                    .orderBy("name")
                    .startAt(queryText)
                    .endAt(queryText + "\uf8ff")
                    .limit(30)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) {
                            return@addSnapshotListener
                        }

                        searchUsers =
                            snapshot.documents.mapNotNull { document ->
                                if (document.id == currentUserId) {
                                    return@mapNotNull null
                                }

                                readPeejeePerson(
                                    document = document,
                                    fallbackUid = document.id
                                )
                            }
                    }

            onDispose {
                listener.remove()
            }
        }
    }

    /*
     * Group messages by the other participant.
     * Count unread messages received by the current user.
     */
    val conversations = remember(
        allMessages,
        userCache.toMap(),
        currentUserId
    ) {
        allMessages
            .filter { message ->
                message.senderId == currentUserId ||
                    message.receiverId == currentUserId
            }
            .groupBy { message ->
                if (message.senderId == currentUserId) {
                    message.receiverId
                } else {
                    message.senderId
                }
            }
            .mapNotNull { entry ->
                val otherUserId = entry.key

                if (otherUserId.isBlank()) {
                    return@mapNotNull null
                }

                val latestMessage =
                    entry.value.maxByOrNull { it.timestamp }
                        ?: return@mapNotNull null

                val person =
                    userCache[otherUserId]
                        ?: PeejeePerson(
                            uid = otherUserId,
                            name = "Peejee User"
                        )

                val unreadCount =
                    entry.value.count { message ->
                        message.receiverId == currentUserId &&
                            !message.read
                    }

                PeejeeConversationRow(
                    user = person,
                    lastMessage = latestMessage.text,
                    timestamp = latestMessage.timestamp,
                    unreadCount = unreadCount
                )
            }
            .sortedByDescending { it.timestamp }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "Private Messages",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                top = 12.dp,
                bottom = 8.dp
            )
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = {
                Text("Search Peejee user")
            },
            placeholder = {
                Text("Type a name to start a chat")
            }
        )

        Spacer(modifier = Modifier.size(10.dp))

        if (searchText.trim().isNotBlank()) {
            if (searchUsers.isEmpty()) {
                Text(
                    text = "No Peejee users found.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = searchUsers,
                        key = { it.uid }
                    ) { person ->
                        SearchUserRow(
                            person = person,
                            onMessage = {
                                onMessage(person)
                            }
                        )
                    }
                }
            }
        } else {
            if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No conversations yet.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.size(6.dp))

                    Text(
                        text = "Search for a Peejee user above to start a private chat.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = conversations,
                        key = { it.user.uid }
                    ) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = {
                                onMessage(conversation.user)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: PeejeeConversationRow,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MessagesPageProfileIcon(
                name = conversation.user.name,
                profilePhoto = conversation.user.profilePhoto
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.user.name,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                    if (conversation.timestamp > 0L) {
                        Text(
                            text = formatPostTime(conversation.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.size(3.dp))

                Text(
                    text = conversation.lastMessage
                        .ifBlank { "Message" },
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.size(3.dp))

                Text(
                    text = if (conversation.user.isOnline) {
                        "Online"
                    } else {
                        "Offline"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (conversation.unreadCount > 99) {
                                "99+"
                            } else {
                                conversation.unreadCount.toString()
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchUserRow(
    person: PeejeePerson,
    onMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MessagesPageProfileIcon(
                name = person.name,
                profilePhoto = person.profilePhoto
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = person.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    text = if (person.isOnline) {
                        "Online"
                    } else {
                        "Offline"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(onClick = onMessage) {
                Text("Message")
            }
        }
    }
}

@Composable
private fun MessagesPageProfileIcon(
    name: String,
    profilePhoto: String
) {
    /*
     * Decode the saved Base64 image.
     * Remember the result so it isn't decoded on every recomposition.
     */
    val profileBitmap = remember(profilePhoto) {
        if (profilePhoto.isBlank()) {
            null
        } else {
            base64ToBitmap(profilePhoto)
        }
    }

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center
    ) {
        if (profileBitmap != null) {
            Image(
                bitmap = profileBitmap.asImageBitmap(),
                contentDescription = "$name profile picture",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val firstLetter =
                name.trim()
                    .firstOrNull()
                    ?.uppercase()
                    ?: "P"

            Text(
                text = firstLetter,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/*
 * Read message fields from Firestore.
 * Keep the read-status handling used by this page.
 */
private fun peejeeMessageFromDocument(
    document: DocumentSnapshot
): PeejeeMessage? {
    val senderId =
        document.getString("senderId") ?: return null

    val receiverId =
        document.getString("receiverId") ?: return null

    val text =
        document.getString("text") ?: return null

    return PeejeeMessage(
        id = document.id,
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        timestamp = document.getLong("timestamp") ?: 0L,
        read = document.getBoolean("read") ?: false
    )
}
