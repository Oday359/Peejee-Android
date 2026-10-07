package com.peejee.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Shared comment model used by the Peejee comments dialog.
 *
 * Replies are stored as comments with parentCommentId
 * pointing to the original top-level comment.
 */
data class PeejeeCommentThreadItem(
    val id: String,
    val userId: String,
    val userName: String,
    val text: String,
    val timestamp: Long,
    val parentCommentId: String = "",
    val likeCount: Int = 0,
    val likedBy: Map<String, Boolean> = emptyMap(),
    val replyCount: Int = 0
)

@Composable
fun PeejeeCommentsDialog(
    postId: String,
    onDismiss: () -> Unit,
    onCountChanged: (Int) -> Unit = {}
) {
    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val currentUserId =
        auth.currentUser?.uid.orEmpty()

    var comments by remember(postId) {
        mutableStateOf<List<PeejeeCommentThreadItem>>(emptyList())
    }

    var loading by remember(postId) {
        mutableStateOf(true)
    }

    var sending by remember(postId) {
        mutableStateOf(false)
    }

    var errorMessage by remember(postId) {
        mutableStateOf("")
    }

    var newComment by remember(postId) {
        mutableStateOf("")
    }

    var replyingTo by remember(postId) {
        mutableStateOf<PeejeeCommentThreadItem?>(null)
    }

    /*
     * Listen to comments in real time.
     *
     * This means when a comment is liked, replied to,
     * or added, the UI receives the Firestore update.
     */
    DisposableEffect(postId) {

        if (postId.isBlank()) {

            loading = false
            comments = emptyList()
            onCountChanged(0)

            onDispose { }

        } else {

            val registration =
                firestore
                    .collection("posts")
                    .document(postId)
                    .collection("comments")
                    .orderBy(
                        "timestamp",
                        Query.Direction.ASCENDING
                    )
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {

                            loading = false

                            errorMessage =
                                error.message
                                    ?: "Could not load comments."

                            return@addSnapshotListener
                        }

                        comments =
                            snapshot
                                ?.documents
                                ?.mapNotNull { document ->

                                    val text =
                                        document.getString("text")
                                            ?: ""

                                    if (text.isBlank()) {

                                        null

                                    } else {

                                        PeejeeCommentThreadItem(
                                            id = document.id,

                                            userId =
                                                document.getString(
                                                    "userId"
                                                ) ?: "",

                                            userName =
                                                document.getString(
                                                    "userName"
                                                ) ?: "Peejee User",

                                            text = text,

                                            timestamp =
                                                document.getLong(
                                                    "timestamp"
                                                ) ?: 0L,

                                            parentCommentId =
                                                document.getString(
                                                    "parentCommentId"
                                                ) ?: "",

                                            likeCount =
                                                document.getLong(
                                                    "likeCount"
                                                )
                                                    ?.toInt()
                                                    ?: 0,

                                            likedBy =
                                                getCommentLikedBy(
                                                    document
                                                ),

                                            replyCount =
                                                document.getLong(
                                                    "replyCount"
                                                )
                                                    ?.toInt()
                                                    ?: 0
                                        )
                                    }
                                }
                                ?: emptyList()

                        onCountChanged(comments.size)

                        loading = false
                    }

            onDispose {
                registration.remove()
            }
        }
    }

    val topComments =
        comments.filter {
            it.parentCommentId.isBlank()
        }

    AlertDialog(

        onDismissRequest = {
            if (!sending) {
                onDismiss()
            }
        },

        title = {
            Text("Comments")
        },

        text = {

            Column(
                Modifier.fillMaxWidth()
            ) {

                if (loading) {

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                } else if (comments.isEmpty()) {

                    SelectionContainer {
                        Text(
                            "No comments yet. Be the first!"
                        )
                    }

                } else {

                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 370.dp)
                    ) {

                        items(
                            topComments,
                            key = { it.id }
                        ) { comment ->

                            PeejeeCommentRow(
                                comment = comment,
                                currentUserId = currentUserId,

                                onReply = {

                                    replyingTo = comment
                                    newComment = ""
                                    errorMessage = ""
                                },

                                onLike = {

                                    togglePeejeeCommentLike(
                                        firestore = firestore,
                                        postId = postId,
                                        commentId = comment.id,
                                        currentUserId = currentUserId
                                    )
                                }
                            )

                            /*
                             * Display replies directly under
                             * their parent comment.
                             */
                            comments
                                .filter {
                                    it.parentCommentId == comment.id
                                }
                                .forEach { reply ->

                                    PeejeeCommentRow(
                                        comment = reply,
                                        currentUserId = currentUserId,
                                        isReply = true,

                                        onReply = {

                                            /*
                                             * Keep replies one level deep.
                                             *
                                             * If somebody replies to a reply,
                                             * it still replies to the original
                                             * top-level comment.
                                             */
                                            replyingTo = comment
                                            newComment = ""
                                            errorMessage = ""
                                        },

                                        onLike = {

                                            togglePeejeeCommentLike(
                                                firestore = firestore,
                                                postId = postId,
                                                commentId = reply.id,
                                                currentUserId = currentUserId
                                            )
                                        }
                                    )
                                }
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    SelectionContainer {

                        Text(
                            errorMessage,
                            color =
                                MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }

                if (replyingTo != null) {

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        SelectionContainer {

                            Text(
                                "Replying to ${replyingTo!!.userName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        TextButton(
                            onClick = {

                                replyingTo = null
                                newComment = ""
                            },
                            enabled = !sending
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                OutlinedTextField(

                    value = newComment,

                    onValueChange = {

                        newComment = it
                        errorMessage = ""
                    },

                    label = {

                        Text(
                            if (replyingTo == null) {
                                "Write a comment"
                            } else {
                                "Write a reply"
                            }
                        )
                    },

                    enabled = !sending,

                    modifier = Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {

            TextButton(

                enabled =
                    !sending &&
                    newComment.isNotBlank(),

                onClick = {

                    sendPeejeeCommentOrReply(

                        firestore = firestore,
                        auth = auth,
                        postId = postId,
                        text = newComment.trim(),

                        parentCommentId =
                            replyingTo?.id.orEmpty(),

                        onSendingChanged = {
                            sending = it
                        },

                        onError = {
                            errorMessage = it
                        },

                        onSuccess = {

                            newComment = ""
                            replyingTo = null
                        }
                    )
                }
            ) {

                Text(
                    when {

                        sending ->
                            "Posting..."

                        replyingTo != null ->
                            "Reply"

                        else ->
                            "Comment"
                    }
                )
            }
        },

        dismissButton = {

            TextButton(

                enabled = !sending,

                onClick = onDismiss

            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PeejeeCommentRow(
    comment: PeejeeCommentThreadItem,
    currentUserId: String,
    isReply: Boolean = false,
    onReply: () -> Unit,
    onLike: () -> Unit
) {
    val liked =
        comment.likedBy[currentUserId] == true

    Row(

        Modifier
            .fillMaxWidth()
            .padding(
                start =
                    if (isReply) {
                        28.dp
                    } else {
                        0.dp
                    },
                top = 7.dp,
                bottom = 7.dp
            ),

        verticalAlignment =
            Alignment.Top
    ) {

        Box(

            Modifier
                .size(
                    if (isReply) {
                        32.dp
                    } else {
                        38.dp
                    }
                )
                .clip(CircleShape)
                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                "👤",
                fontSize =
                    if (isReply) {
                        16.sp
                    } else {
                        19.sp
                    }
            )
        }

        Spacer(
            Modifier.width(8.dp)
        )

        Column(
            Modifier.weight(1f)
        ) {

            /*
             * User name can be copied.
             */
            SelectionContainer {

                Text(
                    comment.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            /*
             * Comment/reply text can be long-pressed
             * and copied to the clipboard.
             */
            SelectionContainer {

                Text(
                    comment.text,
                    fontSize = 15.sp
                )
            }

            TextButton(

                onClick = onReply,

                contentPadding =
                    PaddingValues(
                        horizontal = 4.dp,
                        vertical = 0.dp
                    )

            ) {

                Text(
                    "Reply",
                    fontSize = 12.sp
                )
            }

            /*
             * Show reply count when there are replies.
             */
            if (
                !isReply &&
                comment.replyCount > 0
            ) {

                SelectionContainer {

                    Text(
                        "${comment.replyCount} repl" +
                            if (comment.replyCount == 1) {
                                "y"
                            } else {
                                "ies"
                            },

                        fontSize = 11.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }

        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally,

            modifier =
                Modifier
                    .clickable(
                        onClick = onLike
                    )
                    .padding(start = 5.dp)
        ) {

            Text(
                if (liked) {
                    "❤️"
                } else {
                    "♡"
                },
                fontSize = 22.sp
            )

            /*
             * Always show the number once there
             * is at least one like.
             */
            if (comment.likeCount > 0) {

                Text(
                    comment.likeCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getCommentLikedBy(
    document: DocumentSnapshot
): Map<String, Boolean> {

    val raw =
        document.get("likedBy")

    return if (raw is Map<*, *>) {

        raw.entries
            .mapNotNull { entry ->

                val key =
                    entry.key as? String

                val value =
                    entry.value as? Boolean

                if (
                    key != null &&
                    value != null
                ) {
                    key to value
                } else {
                    null
                }
            }
            .toMap()

    } else {

        emptyMap()
    }
}

private fun togglePeejeeCommentLike(
    firestore: FirebaseFirestore,
    postId: String,
    commentId: String,
    currentUserId: String
) {

    if (
        postId.isBlank() ||
        commentId.isBlank() ||
        currentUserId.isBlank()
    ) {
        return
    }

    val commentRef =
        firestore
            .collection("posts")
            .document(postId)
            .collection("comments")
            .document(commentId)

    firestore.runTransaction { transaction ->

        /*
         * IMPORTANT:
         *
         * ALL reads happen before ANY writes.
         *
         * This is required by Firestore transactions.
         */
        val snapshot =
            transaction.get(commentRef)

        if (!snapshot.exists()) {
            return@runTransaction null
        }

        val likes =
            snapshot
                .getLong("likeCount")
                ?.toInt()
                ?: 0

        val likedBy =
            getCommentLikedBy(snapshot)
                .toMutableMap()

        val alreadyLiked =
            likedBy[currentUserId] == true

        if (alreadyLiked) {

            likedBy.remove(currentUserId)

            transaction.update(
                commentRef,
                "likeCount",
                (likes - 1)
                    .coerceAtLeast(0)
            )

        } else {

            likedBy[currentUserId] = true

            transaction.update(
                commentRef,
                "likeCount",
                likes + 1
            )
        }

        transaction.update(
            commentRef,
            "likedBy",
            likedBy
        )

        null
    }
        .addOnFailureListener {
            // The real-time listener will keep
            // the displayed value unchanged if
            // the transaction fails.
        }
}

private fun sendPeejeeCommentOrReply(
    firestore: FirebaseFirestore,
    auth: FirebaseAuth,
    postId: String,
    text: String,
    parentCommentId: String,
    onSendingChanged: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {

    val currentUser =
        auth.currentUser

    if (currentUser == null) {

        onError(
            "Please log in again."
        )

        return
    }

    if (
        postId.isBlank() ||
        text.isBlank()
    ) {

        onError(
            "Please enter a comment."
        )

        return
    }

    onSendingChanged(true)
    onError("")

    /*
     * Get the current user's name first.
     */
    firestore
        .collection("users")
        .document(currentUser.uid)
        .get()

        .addOnSuccessListener { userDocument ->

            val userName =
                userDocument.getString("name")
                    ?: "Peejee User"

            val postRef =
                firestore
                    .collection("posts")
                    .document(postId)

            val commentRef =
                postRef
                    .collection("comments")
                    .document()

            val parentRef =
                parentCommentId
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        postRef
                            .collection("comments")
                            .document(it)
                    }

            val data =
                hashMapOf<String, Any>(

                    "commentId" to commentRef.id,

                    "postId" to postId,

                    "userId" to currentUser.uid,

                    "userName" to userName,

                    "text" to text,

                    "timestamp" to
                        System.currentTimeMillis(),

                    "parentCommentId" to
                        parentCommentId,

                    "likeCount" to 0,

                    "likedBy" to
                        emptyMap<String, Boolean>(),

                    "replyCount" to 0
                )

            var postOwnerId = ""
            var parentUserId = ""

            /*
             * IMPORTANT FIRESTORE TRANSACTION RULE:
             *
             * ALL reads must happen FIRST.
             * ONLY AFTER ALL READS are complete
             * do we perform writes.
             *
             * This fixes:
             *
             * "Firestore transactions require all
             * reads to be executed before all writes."
             */
            firestore
                .runTransaction { transaction ->

                    /*
                     * READ #1:
                     * Read the post before any writes.
                     */
                    val postSnapshot =
                        transaction.get(postRef)

                    if (!postSnapshot.exists()) {

                        throw IllegalStateException(
                            "This post no longer exists."
                        )
                    }

                    postOwnerId =
                        postSnapshot
                            .getString("userId")
                            ?: ""

                    /*
                     * READ #2:
                     * If this is a reply, read the
                     * parent comment BEFORE writing.
                     */
                    var parentSnapshot:
                            DocumentSnapshot? = null

                    if (parentRef != null) {

                        parentSnapshot =
                            transaction.get(parentRef)

                        if (!parentSnapshot.exists()) {

                            throw IllegalStateException(
                                "The comment you are replying to no longer exists."
                            )
                        }

                        parentUserId =
                            parentSnapshot
                                .getString("userId")
                                ?: ""
                    }

                    /*
                     * ALL READS ARE NOW COMPLETE.
                     *
                     * From this point onward we
                     * perform writes only.
                     */

                    val currentCommentCount =
                        postSnapshot
                            .getLong("commentCount")
                            ?.toInt()
                            ?: 0

                    /*
                     * WRITE #1:
                     * Create the new comment/reply.
                     */
                    transaction.set(
                        commentRef,
                        data
                    )

                    /*
                     * WRITE #2:
                     * Increase post comment count.
                     */
                    transaction.update(
                        postRef,
                        "commentCount",
                        currentCommentCount + 1
                    )

                    /*
                     * WRITE #3:
                     * Increase parent reply count.
                     */
                    if (parentRef != null) {

                        val parent =
                            parentSnapshot!!

                        val currentReplyCount =
                            parent
                                .getLong("replyCount")
                                ?.toInt()
                                ?: 0

                        transaction.update(
                            parentRef,
                            "replyCount",
                            currentReplyCount + 1
                        )
                    }

                    null
                }

                .addOnSuccessListener {

                    onSendingChanged(false)

                    /*
                     * Notify post owner about
                     * a normal comment.
                     */
                    if (
                        parentCommentId.isBlank() &&
                        postOwnerId.isNotBlank() &&
                        postOwnerId != currentUser.uid
                    ) {

                        createPeejeeNotification(

                            firestore = firestore,

                            recipientUserId =
                                postOwnerId,

                            type = "comment",

                            actorId =
                                currentUser.uid,

                            actorName =
                                userName,

                            text =
                                "commented on your post",

                            postId =
                                postId
                        )
                    }

                    /*
                     * Notify original commenter
                     * about a reply.
                     */
                    if (
                        parentCommentId.isNotBlank() &&
                        parentUserId.isNotBlank() &&
                        parentUserId != currentUser.uid
                    ) {

                        createPeejeeNotification(

                            firestore = firestore,

                            recipientUserId =
                                parentUserId,

                            type = "reply",

                            actorId =
                                currentUser.uid,

                            actorName =
                                userName,

                            text =
                                "replied to your comment",

                            postId =
                                postId
                        )
                    }

                    /*
                     * Notify users mentioned with @.
                     */
                    findMentionedUsers(

                        firestore = firestore,

                        text = text,

                        currentUserId =
                            currentUser.uid

                    ) { mentionedUsers ->

                        mentionedUsers.forEach {
                            mentionedUser ->

                            val mentionedUserId =
                                mentionedUser.uid

                            if (
                                mentionedUserId.isNotBlank() &&
                                mentionedUserId !=
                                currentUser.uid
                            ) {

                                createPeejeeNotification(

                                    firestore = firestore,

                                    recipientUserId =
                                        mentionedUserId,

                                    type = "mention",

                                    actorId =
                                        currentUser.uid,

                                    actorName =
                                        userName,

                                    text =
                                        "mentioned you in a comment",

                                    postId =
                                        postId
                                )
                            }
                        }
                    }

                    onSuccess()
                }

                .addOnFailureListener { exception ->

                    onSendingChanged(false)

                    onError(
                        exception.message
                            ?: "Could not post comment."
                    )
                }
        }

        .addOnFailureListener { exception ->

            onSendingChanged(false)

            onError(
                exception.message
                    ?: "Could not load your profile."
            )
        }
}
