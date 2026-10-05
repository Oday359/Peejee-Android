package com.peejee.app

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private data class PeejeeConversationRow(
    val user: PeejeePerson,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int
)

@Composable
fun MessagesPageNew(
    onMessage: (PeejeePerson) -> Unit,
    paddingValues: PaddingValues
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid.orEmpty()

    var searchText by remember { mutableStateOf("") }

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
     * Listen to both messages sent by the current user
     * and messages received by the current user.
     *
     * We keep these as two separate snapshots and combine them
     * into one message list.
     */
    DisposableEffect(currentUserId) {

        if (currentUserId.isBlank()) {
            onDispose { }
        } else {

            var sentMessages = emptyList<PeejeeMessage>()
            var receivedMessages = emptyList<PeejeeMessage>()

            fun updateCombinedMessages() {
                val combined = (sentMessages + receivedMessages)
                    .associateBy { it.id }
                    .values
                    .sortedByDescending { it.timestamp }

                allMessages = combined
            }

            val sentListener =
                firestore
                    .collection("messages")
                    .whereEqualTo("senderId", currentUserId)
                    .addSnapshotListener { snapshot, error ->

                        if (error != null || snapshot == null) {
                            return@addSnapshotListener
                        }

                        sentMessages = snapshot.documents.mapNotNull {
                            messageFromDocument(it)
                        }

                        updateCombinedMessages()
                    }

            val receivedListener =
                firestore
                    .collection("messages")
                    .whereEqualTo("receiverId", currentUserId)
                    .addSnapshotListener { snapshot, error ->

                        if (error != null || snapshot == null) {
                            return@addSnapshotListener
                        }

                        receivedMessages = snapshot.documents.mapNotNull {
                            messageFromDocument(it)
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
     * Find the other user for every conversation.
     */
    LaunchedEffect(allMessages) {

        val otherUserIds = allMessages
            .map {
                if (it.senderId == currentUserId) {
                    it.receiverId
                } else {
                    it.senderId
                }
            }
            .filter { it.isNotBlank() }
            .distinct()

        otherUserIds.forEach { uid ->

            if (!userCache.containsKey(uid)) {

                firestore
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        if (document.exists()) {

                            val name =
                                document.getString("name")
                                    ?: document.getString("displayName")
                                    ?: "Peejee User"

                            val email =
                                document.getString("email")
                                    ?: ""

                            val isOnline =
                                document.getBoolean("isOnline")
                                    ?: false

                            userCache[uid] =
                                PeejeePerson(
                                    uid = uid,
                                    name = name,
                                    email = email,
                                    isOnline = isOnline
                                )
                        }
                    }
            }
        }
    }

    /*
     * Search Peejee users by name.
     */
    DisposableEffect(searchText.trim()) {

        val queryText = searchText.trim()

        if (queryText.isBlank()) {
            searchUsers = emptyList()
            onDispose { }
        } else {

            val listener =
                firestore
                    .collection("users")
                    .orderBy("name")
                    .startAt(queryText)
                    .endAt(queryText + "\uf8ff")
                    .limit(30)
                    .addSnapshotListener { snapshot, error ->

                        if (error != null || snapshot == null) {
                            return@addSnapshotListener
                        }

                        searchUsers =
                            snapshot.documents
                                .mapNotNull { document ->

                                    val uid = document.id

                                    if (uid == currentUserId) {
                                        return@mapNotNull null
                                    }

                                    val name =
                                        document.getString("name")
                                            ?: document.getString("displayName")
                                            ?: "Peejee User"

                                    val email =
                                        document.getString("email")
                                            ?: ""

                                    val isOnline =
                                        document.getBoolean("isOnline")
                                            ?: false

                                    PeejeePerson(
                                        uid = uid,
                                        name = name,
                                        email = email,
                                        isOnline = isOnline
                                    )
                                }
                    }

            onDispose {
                listener.remove()
            }
        }
    }

    /*
     * Build conversation rows from the message list.
     */
    val conversations = remember(
        allMessages,
        userCache,
        currentUserId
    ) {

        val grouped =
            allMessages
                .groupBy { message ->

                    if (message.senderId == currentUserId) {
                        message.receiverId
                    } else {
                        message.senderId
                    }
                }

        grouped.mapNotNull { entry ->

            val otherUserId = entry.key

            val person = userCache[otherUserId]
                ?: return@mapNotNull null

            val latestMessage =
                entry.value.maxByOrNull { it.timestamp }
                    ?: return@mapNotNull null

            val unreadCount =
                entry.value.count {
                    it.receiverId == currentUserId &&
                        !it.read
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
            onValueChange = {
                searchText = it
            },
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

        /*
         * SEARCH RESULTS
         */
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

            /*
             * PREVIOUS CONVERSATIONS
             */
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
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            MessagesPageProfileIcon(
                name = conversation.user.name
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
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.timestamp > 0L) {

                        Text(
                            text = formatPostTime(
                                conversation.timestamp
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.size(3.dp))

                Text(
                    text =
                        if (conversation.lastMessage.isBlank()) {
                            "Message"
                        } else {
                            conversation.lastMessage
                        },
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.size(3.dp))

                Text(
                    text =
                        if (conversation.user.isOnline) {
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
                        modifier = Modifier
                            .size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text =
                                if (conversation.unreadCount > 99) {
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
                name = person.name
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = person.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        if (person.isOnline) {
                            "Online"
                        } else {
                            "Offline"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onMessage
            ) {
                Text("Message")
            }
        }
    }
}

@Composable
private fun MessagesPageProfileIcon(
    name: String
) {

    val firstLetter =
        name.trim()
            .firstOrNull()
            ?.uppercase()
            ?: "P"

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = firstLetter,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
