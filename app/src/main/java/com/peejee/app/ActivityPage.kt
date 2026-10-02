package com.peejee.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class PeejeeNotification(
    val id: String,
    val type: String,
    val fromUserName: String,
    val message: String,
    val timestamp: Long,
    val read: Boolean
)

@Composable
fun ActivityPage(
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

    var notifications by remember {
        mutableStateOf<List<PeejeeNotification>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var markingAllRead by remember {
        mutableStateOf(false)
    }

    DisposableEffect(currentUserId) {

        if (currentUserId.isBlank()) {

            loading = false

            onDispose { }

        } else {

            val registration =
                firestore
                    .collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                    )
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {

                            loading = false

                            errorMessage =
                                error.message
                                    ?: "Could not load notifications."

                            return@addSnapshotListener
                        }

                        if (snapshot != null) {

                            notifications =
                                snapshot.documents.mapNotNull { document ->

                                    PeejeeNotification(
                                        id = document.id,

                                        type =
                                            document.getString("type")
                                                ?: "activity",

                                        fromUserName =
                                            document.getString("fromUserName")
                                                ?: "Peejee User",

                                        message =
                                            document.getString("message")
                                                ?: "You have a new activity.",

                                        timestamp =
                                            document.getLong("timestamp")
                                                ?: 0L,

                                        read =
                                            document.getBoolean("read")
                                                ?: false
                                    )
                                }
                        }

                        loading = false
                    }

            onDispose {
                registration.remove()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {
                Text("← Back")
            }

            Text(
                "Activity",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (notifications.any { !it.read }) {

                TextButton(
                    enabled = !markingAllRead,
                    onClick = {

                        val unread =
                            notifications.filter {
                                !it.read
                            }

                        if (unread.isEmpty()) {
                            return@TextButton
                        }

                        markingAllRead = true

                        val batch =
                            firestore.batch()

                        unread.forEach { notification ->

                            val reference =
                                firestore
                                    .collection("users")
                                    .document(currentUserId)
                                    .collection("notifications")
                                    .document(notification.id)

                            batch.update(
                                reference,
                                "read",
                                true
                            )
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                markingAllRead = false
                            }
                            .addOnFailureListener {
                                markingAllRead = false
                            }
                    }
                ) {

                    Text(
                        if (markingAllRead) {
                            "Saving..."
                        } else {
                            "Mark all read"
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }

        HorizontalDivider()

        when {

            loading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            errorMessage.isNotBlank() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        errorMessage,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }

            notifications.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            "🔔",
                            fontSize = 55.sp
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            "No activity yet",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(6.dp)
                        )

                        Text(
                            "Your likes, comments, followers and other activity will appear here."
                        )
                    }
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 12.dp
                        )
                ) {

                    itemsIndexed(
                        notifications,
                        key = {
                                _, notification ->
                            notification.id
                        }
                    ) { _, notification ->

                        NotificationCard(
                            notification =
                                notification
                        )
                    }

                    item {
                        Spacer(
                            Modifier.height(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: PeejeeNotification
) {

    val background =
        if (notification.read) {
            MaterialTheme
                .colorScheme
                .surface
        } else {
            MaterialTheme
                .colorScheme
                .primaryContainer
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 5.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant,
                        shape =
                            MaterialTheme
                                .shapes
                                .medium
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    notificationIcon(
                        notification.type
                    ),
                    fontSize = 25.sp
                )
            }

            Spacer(
                Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    notification.message,
                    fontSize = 16.sp,
                    fontWeight =
                        if (!notification.read) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                )

                Spacer(
                    Modifier.height(4.dp)
                )

                if (
                    notification.fromUserName
                        .isNotBlank()
                ) {

                    Text(
                        notification.fromUserName,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                if (
                    notification.timestamp > 0
                ) {

                    Text(
                        formatPostTime(
                            notification.timestamp
                        ),
                        fontSize = 10.sp
                    )
                }
            }

            if (!notification.read) {

                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary,
                            shape =
                                androidx.compose.foundation
                                    .shape
                                    .CircleShape
                        )
                )
            }
        }
    }
}

fun notificationIcon(
    type: String
): String {

    return when (
        type.lowercase()
    ) {

        "like" -> "❤️"

        "comment" -> "💬"

        "follow" -> "👤"

        "message" -> "✉️"

        "mention" -> "@️"

        else -> "🔔"
    }
}
