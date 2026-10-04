package com.peejee.app

import android.content.Context
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay

private data class PeejeeReplyInfo(
val messageId: String,
val text: String,
val senderName: String
)

private fun peejeeTypingDocumentId(
firstUserId: String,
secondUserId: String
): String {
return "${firstUserId}_${secondUserId}"
}

private fun setPeejeeTypingState(
firestore: FirebaseFirestore,
currentUserId: String,
otherUserId: String,
isTyping: Boolean
) {
if (
currentUserId.isBlank() ||
otherUserId.isBlank()
) {
return
}

val data = hashMapOf<String, Any>(  
    "userId" to currentUserId,  
    "otherUserId" to otherUserId,  
    "typing" to isTyping,  
    "updatedAt" to System.currentTimeMillis()  
)  

firestore  
    .collection("typing")  
    .document(  
        peejeeTypingDocumentId(  
            currentUserId,  
            otherUserId  
        )  
    )  
    .set(  
        data,  
        SetOptions.merge()  
    )

}

private fun playPeejeeMessageAlert(
context: Context
) {
try {
val soundUri =
RingtoneManager.getDefaultUri(
RingtoneManager.TYPE_NOTIFICATION
)

RingtoneManager  
        .getRingtone(  
            context,  
            soundUri  
        )  
        ?.play()  
} catch (_: Exception) {  
}  

try {  
    val vibrator =  
        context.getSystemService(  
            Context.VIBRATOR_SERVICE  
        ) as? Vibrator  

    vibrator?.vibrate(  
        VibrationEffect.createOneShot(  
            180L,  
            VibrationEffect.DEFAULT_AMPLITUDE  
        )  
    )  
} catch (_: Exception) {  
}

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PrivateChatPageNew(
person: PeejeePerson,
paddingValues: PaddingValues,
onBack: () -> Unit
) {
val context = LocalContext.current

val firestore = remember {  
    FirebaseFirestore.getInstance()  
}  

val auth = remember {  
    FirebaseAuth.getInstance()  
}  

val currentUserId =  
    auth.currentUser?.uid ?: ""  

var messages by remember {  
    mutableStateOf<List<PeejeeMessage>>(  
        emptyList()  
    )  
}  

var replyInfoByMessageId by remember {  
    mutableStateOf<Map<String, PeejeeReplyInfo>>(  
        emptyMap()  
    )  
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

var personIsOnline by remember {  
    mutableStateOf(person.isOnline)  
}  

var otherPersonIsTyping by remember {  
    mutableStateOf(false)  
}  

var showEmojiPicker by remember {  
    mutableStateOf(false)  
}  

var selectedMessage by remember {  
    mutableStateOf<PeejeeMessage?>(null)  
}  

var selectedReply by remember {  
    mutableStateOf<PeejeeMessage?>(null)  
}  

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

                if (snapshot.isEmpty) {  
                    return@addOnSuccessListener  
                }  

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

DisposableEffect(person.uid) {  
    val registration =  
        firestore  
            .collection("users")  
            .document(person.uid)  
            .addSnapshotListener {  
                    document,  
                    error ->  
                if (  
                    error == null &&  
                    document != null  
                ) {  
                    personIsOnline =  
                        document.getBoolean(  
                            "isOnline"  
                        ) ?: false  
                }  
            }  

    onDispose {  
        registration.remove()  
    }  
}  

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
        val registration =  
            firestore  
                .collection("typing")  
                .document(  
                    peejeeTypingDocumentId(  
                        person.uid,  
                        currentUserId  
                    )  
                )  
                .addSnapshotListener {  
                        document,  
                        error ->  
                    if (  
                        error == null &&  
                        document != null  
                    ) {  
                        otherPersonIsTyping =  
                            document.getBoolean(  
                                "typing"  
                            ) ?: false  
                    }  
                }  

        onDispose {  
            registration.remove()  
        }  
    }  
}  

LaunchedEffect(  
    newMessage,  
    currentUserId,  
    person.uid  
) {  
    if (  
        currentUserId.isBlank() ||  
        person.uid.isBlank()  
    ) {  
        return@LaunchedEffect  
    }  

    if (newMessage.isBlank()) {  
        setPeejeeTypingState(  
            firestore = firestore,  
            currentUserId = currentUserId,  
            otherUserId = person.uid,  
            isTyping = false  
        )  
    } else {  
        setPeejeeTypingState(  
            firestore = firestore,  
            currentUserId = currentUserId,  
            otherUserId = person.uid,  
            isTyping = true  
        )  

        delay(2500L)  

        setPeejeeTypingState(  
            firestore = firestore,  
            currentUserId = currentUserId,  
            otherUserId = person.uid,  
            isTyping = false  
        )  
    }  
}  

DisposableEffect(  
    currentUserId,  
    person.uid  
) {  
    onDispose {  
        setPeejeeTypingState(  
            firestore = firestore,  
            currentUserId = currentUserId,  
            otherUserId = person.uid,  
            isTyping = false  
        )  
    }  
}  

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
                .addSnapshotListener {  
                        sentSnapshot,  
                        sentError ->  

                    if (sentError != null) {  
                        errorMessage =  
                            sentError.message  
                                ?: "Could not load messages."  

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
                        .addOnSuccessListener {  
                                receivedSnapshot ->  

                            val sentDocuments =  
                                sentSnapshot  
                                    ?.documents  
                                    ?: emptyList()  

                            val receivedDocuments =  
                                receivedSnapshot  
                                    .documents  

                            val allDocuments =  
                                (  
                                    sentDocuments +  
                                        receivedDocuments  
                                    )  
                                    .distinctBy {  
                                        it.id  
                                    }  

                            messages =  
                                allDocuments  
                                    .mapNotNull {  
                                        messageFromDocument(  
                                            it  
                                        )  
                                    }  
                                    .sortedBy {  
                                        it.timestamp  
                                    }  

                            replyInfoByMessageId =  
                                allDocuments  
                                    .mapNotNull {  
                                        document ->  

                                        val replyText =  
                                            document  
                                                .getString(  
                                                    "replyToText"  
                                                )  
                                                ?: return@mapNotNull null  

                                        val replySenderName =  
                                            document  
                                                .getString(  
                                                    "replyToSenderName"  
                                                )  
                                                ?: "Peejee User"  

                                        PeejeeReplyInfo(  
                                            messageId =  
                                                document.id,  
                                            text =  
                                                replyText,  
                                            senderName =  
                                                replySenderName  
                                        )  
                                    }  
                                    .associateBy {  
                                        it.messageId  
                                    }  

                            errorMessage = ""  
                        }  
                        .addOnFailureListener {  
                                exception ->  

                            errorMessage =  
                                exception.message  
                                    ?: "Could not load messages."  
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
                .addSnapshotListener {  
                        receivedSnapshot,  
                        receivedError ->  

                    if (receivedError != null) {  
                        return@addSnapshotListener  
                    }  

                    /*  
                     * The initial Firestore snapshot also reports  
                     * existing documents as ADDED. We only play  
                     * the alert when there were already messages  
                     * on screen and a new message appears.  
                     */  
                    val addedIncoming =  
                        receivedSnapshot  
                            ?.documentChanges  
                            ?.any { change ->  
                                change.type.name == "ADDED"  
                            } == true  

                    if (  
                        addedIncoming &&  
                        receivedSnapshot != null  
                    ) {  
                        val newestIncoming =  
                            receivedSnapshot  
                                .documents  
                                .mapNotNull {  
                                    messageFromDocument(  
                                        it  
                                    )  
                                }  
                                .maxByOrNull {  
                                    it.timestamp  
                                }  

                        if (  
                            newestIncoming != null &&  
                            newestIncoming.senderId ==  
                            person.uid  
                        ) {  
                            val previousIds =  
                                messages  
                                    .map { it.id }  
                                    .toSet()  

                            if (  
                                previousIds.isNotEmpty() &&  
                                !previousIds.contains(  
                                    newestIncoming.id  
                                )  
                            ) {  
                                playPeejeeMessageAlert(  
                                    context  
                                )  
                            }  
                        }  
                    }  

                    /*  
                     * This chat is open, so incoming messages  
                     * become Seen.  
                     */  
                    val unreadDocuments =  
                        receivedSnapshot  
                            ?.documents  
                            ?.filter {  
                                it.getBoolean(  
                                    "read"  
                                ) != true  
                            }  
                            ?: emptyList()  

                    if (  
                        unreadDocuments.isNotEmpty()  
                    ) {  
                        val batch =  
                            firestore.batch()  

                        unreadDocuments.forEach {  
                                document ->  
                            batch.update(  
                                document.reference,  
                                "read",  
                                true  
                            )  
                        }  

                        batch.commit()  
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
                        .addOnSuccessListener {  
                                sentSnapshot ->  

                            val sentDocuments =  
                                sentSnapshot  
                                    .documents  

                            val receivedDocuments =  
                                receivedSnapshot  
                                    ?.documents  
                                    ?: emptyList()  

                            val allDocuments =  
                                (  
                                    sentDocuments +  
                                        receivedDocuments  
                                    )  
                                    .distinctBy {  
                                        it.id  
                                    }  

                            messages =  
                                allDocuments  
                                    .mapNotNull {  
                                        messageFromDocument(  
                                            it  
                                        )  
                                    }  
                                    .sortedBy {  
                                        it.timestamp  
                                    }  

                            replyInfoByMessageId =  
                                allDocuments  
                                    .mapNotNull {  
                                        document ->  

                                        val replyText =  
                                            document  
                                                .getString(  
                                                    "replyToText"  
                                                )  
                                                ?: return@mapNotNull null  

                                        val replySenderName =  
                                            document  
                                                .getString(  
                                                    "replyToSenderName"  
                                                )  
                                                ?: "Peejee User"  

                                        PeejeeReplyInfo(  
                                            messageId =  
                                                document.id,  
                                            text =  
                                                replyText,  
                                            senderName =  
                                                replySenderName  
                                        )  
                                    }  
                                    .associateBy {  
                                        it.messageId  
                                    }  
                        }  
                }  

        onDispose {  
            sentRegistration.remove()  
            receivedRegistration.remove()  
        }  
    }  
}  

fun sendMessage() {  
    val text =  
        newMessage.trim()  

    if (  
        text.isBlank() ||  
        sending ||  
        currentUserId.isBlank() ||  
        person.uid.isBlank()  
    ) {  
        return  
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
            "text" to  
                text,  
            "timestamp" to  
                System.currentTimeMillis(),  
            "read" to  
                false  
        )  

    val reply =  
        selectedReply  

    if (reply != null) {  
        messageData[  
            "replyToMessageId"  
        ] = reply.id  

        messageData[  
            "replyToText"  
        ] = reply.text  

        messageData[  
            "replyToSenderName"  
        ] =  
            if (  
                reply.senderId ==  
                currentUserId  
            ) {  
                "You"  
            } else {  
                person.name  
            }  
    }  

    messageReference  
        .set(messageData)  
        .addOnSuccessListener {  

            firestore  
                .collection("users")  
                .document(currentUserId)  
                .get()  
                .addOnSuccessListener { userDocument ->  
                    createPeejeeNotification(  
                        firestore = firestore,  
                        recipientUserId = person.uid,  
                        type = "message",  
                        actorId = currentUserId,  
                        actorName =  
                            userDocument.getString("name")  
                                ?: "Peejee User",  
                        text =  
                            "sent you a message",  
                        messageId =  
                            messageReference.id  
                    )  
                }  

            newMessage = ""  
            selectedReply = null  
            showEmojiPicker = false  

            setPeejeeTypingState(  
                firestore = firestore,  
                currentUserId = currentUserId,  
                otherUserId = person.uid,  
                isTyping = false  
            )  

            sending = false  
        }  
        .addOnFailureListener {  
                exception ->  

            sending = false  

            errorMessage =  
                exception.message  
                    ?: "Could not send message."  
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
                vertical = 10.dp  
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
            modifier =  
                Modifier.weight(1f)  
        ) {  
            Text(  
                person.name,  
                fontSize = 19.sp,  
                fontWeight =  
                    FontWeight.Bold  
            )  

            Text(  
                when {  
                    otherPersonIsTyping ->  
                        "${person.name} is typing…"  

                    personIsOnline ->  
                        "🟢 Online"  

                    else ->  
                        "⚪ Offline"  
                },  
                fontSize = 12.sp  
            )  
        }  
    }  

    HorizontalDivider()  

    if (selectedReply != null) {  
        Card(  
            modifier = Modifier  
                .fillMaxWidth()  
                .padding(8.dp)  
        ) {  
            Row(  
                modifier =  
                    Modifier  
                        .fillMaxWidth()  
                        .padding(10.dp),  
                verticalAlignment =  
                    Alignment.CenterVertically  
            ) {  
                Column(  
                    modifier =  
                        Modifier.weight(1f)  
                ) {  
                    Text(  
                        "Replying to ${  
                            if (  
                                selectedReply!!  
                                    .senderId ==  
                                currentUserId  
                            ) {  
                                "your message"  
                            } else {  
                                person.name  
                            }  
                        }",  
                        fontSize = 12.sp,  
                        fontWeight =  
                            FontWeight.Bold  
                    )  

                    Text(  
                        selectedReply!!.text,  
                        maxLines = 2,  
                        fontSize = 13.sp  
                    )  
                }  

                TextButton(  
                    onClick = {  
                        selectedReply = null  
                    }  
                ) {  
                    Text("Cancel")  
                }  
            }  
        }  
    }  

    LazyColumn(  
        modifier = Modifier  
            .weight(1f)  
            .fillMaxWidth()  
            .padding(  
                horizontal = 10.dp  
            ),  
        reverseLayout = false  
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

            val replyInfo =  
                replyInfoByMessageId[  
                    message.id  
                ]  

            Row(  
                modifier = Modifier  
                    .fillMaxWidth()  
                    .padding(  
                        vertical = 4.dp  
                    ),  
                horizontalArrangement =  
                    if (mine) {  
                        Arrangement.End  
                    } else {  
                        Arrangement.Start  
                    }  
            ) {  

                Box {  

                    Surface(  
                        modifier =  
                            Modifier  
                                .widthIn(  
                                    max = 300.dp  
                                )  
                                .combinedClickable(  
                                    onClick = { },  
                                    onLongClick = {  
                                        selectedMessage =  
                                            message  
                                    }  
                                ),  
                        shape =  
                            RoundedCornerShape(  
                                topStart = 18.dp,  
                                topEnd = 18.dp,  
                                bottomStart =  
                                    if (mine) {  
                                        18.dp  
                                    } else {  
                                        4.dp  
                                    },  
                                bottomEnd =  
                                    if (mine) {  
                                        4.dp  
                                    } else {  
                                        18.dp  
                                    }  
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
                            modifier =  
                                Modifier.padding(  
                                    horizontal = 14.dp,  
                                    vertical = 9.dp  
                                )  
                        ) {  

                            if (  
                                replyInfo != null  
                            ) {  
                                Card(  
                                    modifier =  
                                        Modifier  
                                            .fillMaxWidth()  
                                            .padding(  
                                                bottom = 7.dp  
                                            )  
                                ) {  
                                    Column(  
                                        modifier =  
                                            Modifier  
                                                .padding(  
                                                    7.dp  
                                                )  
                                    ) {  
                                        Text(  
                                            "↩ ${replyInfo.senderName}",  
                                            fontSize =  
                                                10.sp,  
                                            fontWeight =  
                                                FontWeight.Bold  
                                        )  

                                        Text(  
                                            replyInfo.text,  
                                            maxLines = 2,  
                                            fontSize =  
                                                11.sp  
                                        )  
                                    }  
                                }  
                            }  

                            Text(  
                                message.text,  
                                fontSize = 16.sp  
                            )  

                            Spacer(  
                                Modifier.height(  
                                    3.dp  
                                )  
                            )  

                            Row(  
                                verticalAlignment =  
                                    Alignment.CenterVertically  
                            ) {  

                                Text(  
                                    formatPostTime(  
                                        message.timestamp  
                                    ),  
                                    fontSize = 9.sp  
                                )  

                                if (mine) {  
                                    Spacer(  
                                        Modifier.width(  
                                            5.dp  
                                        )  
                                    )  

                                    Text(  
                                        if (  
                                            message.read  
                                        ) {  
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

                    DropdownMenu(  
                        expanded =  
                            selectedMessage?.id ==  
                                message.id,  
                        onDismissRequest = {  
                            selectedMessage =  
                                null  
                        }  
                    ) {  

                        TextButton(  
                            onClick = {  
                                selectedReply =  
                                    message  

                                selectedMessage =  
                                    null  
                            }  
                        ) {  
                            Text("↩ Reply")  
                        }  

                        if (mine) {  
                            TextButton(  
                                onClick = {  

                                    firestore  
                                        .collection(  
                                            "messages"  
                                        )  
                                        .document(  
                                            message.id  
                                        )  
                                        .delete()  
                                        .addOnFailureListener {  
                                            errorMessage =  
                                                "Could not delete message."  
                                        }  

                                    selectedMessage =  
                                        null  
                                }  
                            ) {  
                                Text("🗑 Delete")  
                            }  
                        }  
                    }  
                }  
            }  
        }  
    }  

    if (  
        errorMessage.isNotBlank()  
    ) {  
        Text(  
            errorMessage,  
            color =  
                MaterialTheme  
                    .colorScheme  
                    .error,  
            modifier =  
                Modifier.padding(  
                    horizontal = 12.dp  
                )  
        )  
    }  

    if (showEmojiPicker) {  
        Card(  
            modifier = Modifier  
                .fillMaxWidth()  
                .padding(  
                    horizontal = 8.dp,  
                    vertical = 4.dp  
                )  
        ) {  
            LazyRow(  
                modifier =  
                    Modifier  
                        .fillMaxWidth()  
                        .padding(6.dp),  
                horizontalArrangement =  
                    Arrangement.spacedBy(2.dp)  
            ) {  
                itemsIndexed(  
                    listOf(  
                        "😀", "😂", "🤣", "😊", "😍",  
                        "🥰", "😘", "😎", "🤔", "😢",  
                        "😭", "😡", "👍", "👎", "👏",  
                        "🙏", "❤️", "🔥", "🎉", "💯",  
                        "😇", "😉", "😮", "😴"  
                    )  
                ) { _, emoji ->  

                    TextButton(  
                        onClick = {  
                            newMessage +=  
                                emoji  

                            showEmojiPicker =  
                                false  
                        },  
                        enabled = !sending  
                    ) {  
                        Text(  
                            emoji,  
                            fontSize = 23.sp  
                        )  
                    }  
                }  
            }  
        }  
    }  

    Row(  
        modifier = Modifier  
            .fillMaxWidth()  
            .padding(8.dp),  
        verticalAlignment =  
            Alignment.CenterVertically  
    ) {  

        IconButton(  
            onClick = {  
                showEmojiPicker =  
                    !showEmojiPicker  
            },  
            enabled = !sending  
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
            },  
            modifier =  
                Modifier.weight(1f),  
            placeholder = {  
                Text(  
                    "Write a message…"  
                )  
            },  
            maxLines = 4,  
            enabled = !sending  
        )  

        Spacer(  
            Modifier.width(6.dp)  
        )  

        Button(  
            onClick = {  
                sendMessage()  
            },  
            enabled =  
                !sending &&  
                    newMessage  
                        .trim()  
                        .isNotBlank()  
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
