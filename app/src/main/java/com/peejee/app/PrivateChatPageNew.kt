package com.peejee.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

private data class PeejeeReplyInfo(
    val messageId: String,
    val text: String,
    val senderName: String
)

private data class PeejeeVoiceInfo(
    val base64: String,
    val durationMs: Long
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
    if (currentUserId.isBlank() || otherUserId.isBlank()) return

    val data = hashMapOf<String, Any>(
        "userId" to currentUserId,
        "otherUserId" to otherUserId,
        "typing" to isTyping,
        "updatedAt" to System.currentTimeMillis()
    )

    firestore.collection("typing")
        .document(
            peejeeTypingDocumentId(
                currentUserId,
                otherUserId
            )
        )
        .set(data, SetOptions.merge())
}

private fun playPeejeeMessageAlert(context: Context) {
    try {
        val soundUri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

        RingtoneManager
            .getRingtone(context, soundUri)
            ?.play()
    } catch (_: Exception) {
    }

    try {
        val vibrator =
            context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as? Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    180L,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    } catch (_: Exception) {
    }
}

private fun formatPeejeeVoiceDuration(
    durationMs: Long
): String {
    val totalSeconds =
        (durationMs / 1000L).coerceAtLeast(0L)

    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    return String.format(
        "%d:%02d",
        minutes,
        seconds
    )
}

private fun copyPeejeeMessageToClipboard(
    context: Context,
    text: String
) {
    if (text.isBlank()) return

    try {
        val clipboard =
            context.getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as? ClipboardManager

        clipboard?.setPrimaryClip(
            ClipData.newPlainText(
                "Peejee message",
                text
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

    val firestore =
        remember {
            FirebaseFirestore.getInstance()
        }

    val auth =
        remember {
            FirebaseAuth.getInstance()
        }

    val currentUserId =
        auth.currentUser?.uid ?: ""

    var messages by remember {
        mutableStateOf<List<PeejeeMessage>>(emptyList())
    }

    var replyInfoByMessageId by remember {
        mutableStateOf<Map<String, PeejeeReplyInfo>>(emptyMap())
    }

    var voiceInfoByMessageId by remember {
        mutableStateOf<Map<String, PeejeeVoiceInfo>>(emptyMap())
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

    var isRecording by remember {
        mutableStateOf(false)
    }

    var recordingStartedAt by remember {
        mutableStateOf(0L)
    }

    var recordingFilePath by remember {
        mutableStateOf("")
    }
    var previewFilePath by remember {
        mutableStateOf("")
    }

    var previewDurationMs by remember {
        mutableStateOf(0L)
    }

    var isPreviewPlaying by remember {
        mutableStateOf(false)
    }

    var previewPlayer by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    var mediaRecorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    var currentlyPlayingMessageId by remember {
        mutableStateOf<String?>(null)
    }

    var currentPlayer by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    val chatBackgroundKey =
        remember(
            currentUserId,
            person.uid
        ) {
            peejeeChatBackgroundKey(
                currentUserId,
                person.uid
            )
        }

    /*
     * IMPORTANT:
     * The background is loaded from persistent storage using
     * the unique chat key.
     *
     * This means the selected background belongs only to
     * this private conversation and remains after the app
     * is closed and opened again.
     */
    var chatBackground by remember(
        chatBackgroundKey
    ) {
        mutableStateOf(
            loadPeejeeChatBackground(
                context,
                chatBackgroundKey
            )
        )
    }

    var chatPhotoPath by remember(
        chatBackgroundKey
    ) {
        mutableStateOf(
            loadPeejeeChatPhotoPath(
                context,
                chatBackgroundKey
            )
        )
    }

    var showBackgroundPicker by remember {
        mutableStateOf(false)
    }

    /*
     * Reload the saved private-chat background whenever
     * the conversation changes.
     */
    LaunchedEffect(chatBackgroundKey) {

        chatBackground =
            loadPeejeeChatBackground(
                context,
                chatBackgroundKey
            )

        chatPhotoPath =
            loadPeejeeChatPhotoPath(
                context,
                chatBackgroundKey
            )
    }

    val chatPhotoLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                val savedPath =
                    copyPeejeeChatPhoto(
                        context = context,
                        uri = uri,
                        chatKey = chatBackgroundKey
                    )

                if (savedPath != null) {

                    chatPhotoPath = savedPath

                    chatBackground =
                        PEEJEE_BG_PHOTO

                    savePeejeeChatPhotoPath(
                        context,
                        chatBackgroundKey,
                        savedPath
                    )

                    savePeejeeChatBackground(
                        context,
                        chatBackgroundKey,
                        PEEJEE_BG_PHOTO
                    )

                } else {

                    errorMessage =
                        "Could not use that photo. Please try another photo."
                }
            }
        }

    fun rebuildMessages(
        sentDocuments:
            List<com.google.firebase.firestore.DocumentSnapshot>,
        receivedDocuments:
            List<com.google.firebase.firestore.DocumentSnapshot>
    ) {

        val allDocuments =
            (
                sentDocuments +
                    receivedDocuments
                ).distinctBy {
                    it.id
                }

        messages =
            allDocuments
                .mapNotNull {
                    messageFromDocument(it)
                }
                .sortedBy {
                    it.timestamp
                }

        replyInfoByMessageId =
            allDocuments
                .mapNotNull { document ->

                    val replyText =
                        document.getString(
                            "replyToText"
                        )
                            ?: return@mapNotNull null

                    val replySenderName =
                        document.getString(
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

        voiceInfoByMessageId =
            allDocuments
                .mapNotNull { document ->

                    val base64 =
                        document.getString(
                            "voiceBase64"
                        )
                            ?: return@mapNotNull null

                    if (base64.isBlank()) {
                        return@mapNotNull null
                    }

                    PeejeeVoiceInfo(
                        base64 = base64,
                        durationMs =
                            document.getLong(
                                "voiceDurationMs"
                            ) ?: 0L
                    ).let {
                        document.id to it
                    }
                }
                .toMap()
    }

    fun stopCurrentPlayback() {

        try {
            currentPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            currentPlayer?.release()
        } catch (_: Exception) {
        }

        currentPlayer = null
        currentlyPlayingMessageId = null
    }

    fun playVoiceMessage(
        messageId: String
    ) {

        val voice =
            voiceInfoByMessageId[messageId]
                ?: return

        if (
            currentlyPlayingMessageId ==
                messageId
        ) {
            stopCurrentPlayback()
            return
        }

        stopCurrentPlayback()

        try {

            val bytes =
                Base64.decode(
                    voice.base64,
                    Base64.DEFAULT
                )

            val file =
                File(
                    context.cacheDir,
                    "peejee_voice_$messageId.3gp"
                )

            FileOutputStream(file).use {
                it.write(bytes)
            }

            val player =
                MediaPlayer()

            currentPlayer = player
            currentlyPlayingMessageId =
                messageId

            player.setDataSource(
                file.absolutePath
            )

            player.setOnCompletionListener {

                try {
                    player.release()
                } catch (_: Exception) {
                }

                if (currentPlayer === player) {
                    currentPlayer = null
                    currentlyPlayingMessageId =
                        null
                }
            }

            player.setOnErrorListener { mp, _, _ ->

                try {
                    mp.release()
                } catch (_: Exception) {
                }

                if (currentPlayer === mp) {
                    currentPlayer = null
                    currentlyPlayingMessageId =
                        null
                }

                true
            }

            player.prepare()
            player.start()

        } catch (exception: Exception) {

            stopCurrentPlayback()

            errorMessage =
                exception.message
                    ?: "Could not play voice note."
        }
    }

        fun sendVoiceNote(
        filePath: String,
        durationMs: Long,
        onComplete: (Boolean) -> Unit
    ) {
        if (
            currentUserId.isBlank() ||
            person.uid.isBlank() ||
            sending
        ) {
            onComplete(false)
            return
        }

        val file = File(filePath)

        if (!file.exists()) {
            errorMessage = "Voice recording was not found."
            onComplete(false)
            return
        }

        try {
            val bytes = file.readBytes()

            // Keep the existing 700 KB limit.
            if (bytes.size > 700_000) {
                errorMessage =
                    "Voice note is too large. Please record a shorter note."
                onComplete(false)
                return
            }

            sending = true
            errorMessage = ""

            val messageReference =
                firestore.collection("messages").document()

            val messageData = hashMapOf<String, Any>(
                "messageId" to messageReference.id,
                "senderId" to currentUserId,
                "receiverId" to person.uid,
                "text" to "",
                "messageType" to "voice",
                "voiceBase64" to Base64.encodeToString(
                    bytes,
                    Base64.NO_WRAP
                ),
                "voiceDurationMs" to durationMs,
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            )

            messageReference
                .set(messageData)
                .addOnSuccessListener {
                    firestore.collection("users")
                        .document(currentUserId)
                        .get()
                        .addOnSuccessListener { userDocument ->
                            createPeejeeNotification(
                                firestore = firestore,
                                recipientUserId = person.uid,
                                type = "message",
                                actorId = currentUserId,
                                actorName = userDocument.getString("name")
                                    ?: "Peejee User",
                                text = "sent you a voice note",
                                messageId = messageReference.id
                            )
                        }

                    sending = false
                    showEmojiPicker = false
                    errorMessage = ""

                    // The component can clear the preview after success.
                    onComplete(true)
                }
                .addOnFailureListener { exception ->
                    sending = false
                    errorMessage = exception.message
                        ?: "Could not send voice note."

                    // Keep the recording available for another attempt.
                    onComplete(false)
                }
        } catch (exception: Exception) {
            sending = false
            errorMessage = exception.message
                ?: "Could not send voice note."

            onComplete(false)
        }
    }

        if (
            isRecording ||
            sending ||
            currentUserId.isBlank() ||
            person.uid.isBlank()
        ) {
            return
        }

        try {

            val outputFile =
                File(
                    context.cacheDir,
                    "peejee_recording_${System.currentTimeMillis()}.3gp"
                )

            val recorder =
                MediaRecorder()

            recorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            recorder.setOutputFormat(
                MediaRecorder.OutputFormat.THREE_GPP
            )

            recorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AMR_NB
            )

            recorder.setOutputFile(
                outputFile.absolutePath
            )

            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            recordingFilePath =
                outputFile.absolutePath

            recordingStartedAt =
                System.currentTimeMillis()

            isRecording = true
            errorMessage = ""

        } catch (exception: Exception) {

            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }

            mediaRecorder = null
            isRecording = false

            errorMessage =
                exception.message
                    ?: "Could not start voice recording."
        }
    }

    /*
     * IMPORTANT:
     * There is intentionally NO LaunchedEffect here that
     * automatically stops recording after 20 seconds.
     */

    DisposableEffect(Unit) {

        onDispose {

            try {
                currentPlayer?.stop()
            } catch (_: Exception) {
            }

            try {
                currentPlayer?.release()
            } catch (_: Exception) {
            }
        }
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
                .addOnSuccessListener {
                    snapshot ->

                    if (
                        snapshot.isEmpty
                    ) {
                        return@addOnSuccessListener
                    }

                    val batch =
                        firestore.batch()

                    snapshot.documents
                        .forEach { document ->

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
                firestore,
                currentUserId,
                person.uid,
                false
            )

        } else {

            setPeejeeTypingState(
                firestore,
                currentUserId,
                person.uid,
                true
            )

            delay(2500L)

            setPeejeeTypingState(
                firestore,
                currentUserId,
                person.uid,
                false
            )
        }
    }

    DisposableEffect(
        currentUserId,
        person.uid
    ) {

        onDispose {

            setPeejeeTypingState(
                firestore,
                currentUserId,
                person.uid,
                false
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

                        if (
                            sentError != null
                        ) {

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

                                rebuildMessages(
                                    sentSnapshot?.documents
                                        ?: emptyList(),

                                    receivedSnapshot.documents
                                )

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

                        if (
                            receivedError != null
                        ) {
                            return@addSnapshotListener
                        }

                        val addedIncoming =
                            receivedSnapshot
                                ?.documentChanges
                                ?.any {
                                    it.type.name ==
                                        "ADDED"
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
                                        .map {
                                            it.id
                                        }
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

                            unreadDocuments
                                .forEach {
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

                                rebuildMessages(
                                    sentSnapshot.documents,
                                    receivedSnapshot?.documents
                                        ?: emptyList()
                                )
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
                    .addOnSuccessListener {
                        userDocument ->

                        createPeejeeNotification(
                            firestore =
                                firestore,

                            recipientUserId =
                                person.uid,

                            type =
                                "message",

                            actorId =
                                currentUserId,

                            actorName =
                                userDocument
                                    .getString(
                                        "name"
                                    )
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
                    firestore,
                    currentUserId,
                    person.uid,
                    false
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

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
    ) {

        PeejeeChatBackgroundLayer(
            backgroundId =
                chatBackground,

            photoPath =
                chatPhotoPath,

            modifier =
                Modifier.fillMaxSize()
        )

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Row(
                modifier =
                    Modifier
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

                PeejeeChatCallControls(
                    person = person,
                    enabled =
                        !isRecording &&
                            !sending
                )

                IconButton(
                    onClick = {
                        showBackgroundPicker = true
                    },

                    enabled =
                        !isRecording &&
                            !sending
                ) {

                    Text(
                        "🎨",
                        fontSize = 24.sp
                    )
                }
            }

            HorizontalDivider()

            if (selectedReply != null) {

                Card(
                    modifier =
                        Modifier
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

                            SelectionContainer {
                                Text(
                                    selectedReply!!.text,
                                    maxLines = 2,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                selectedReply =
                                    null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }

            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp
                        ),

                reverseLayout = false
            ) {

                itemsIndexed(
                    messages,
                    key = {
                        _, message ->
                        message.id
                    }
                ) {
                    _,
                    message ->

                    val mine =
                        message.senderId ==
                            currentUserId

                    val replyInfo =
                        replyInfoByMessageId[
                            message.id
                        ]

                    val voiceInfo =
                        voiceInfoByMessageId[
                            message.id
                        ]

                    Row(
                        modifier =
                            Modifier
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

                                    if (replyInfo != null) {

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
                                                    Modifier.padding(
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

                                                SelectionContainer {
                                                    Text(
                                                        replyInfo.text,

                                                        maxLines =
                                                            2,

                                                        fontSize =
                                                            11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (voiceInfo != null) {

                                        Button(
                                            onClick = {
                                                playVoiceMessage(
                                                    message.id
                                                )
                                            },

                                            enabled =
                                                !sending,

                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                if (
                                                    currentlyPlayingMessageId ==
                                                    message.id
                                                ) {

                                                    "⏹ Stop  ${
                                                        formatPeejeeVoiceDuration(
                                                            voiceInfo.durationMs
                                                        )
                                                    }"

                                                } else {

                                                    "▶ Voice note  ${
                                                        formatPeejeeVoiceDuration(
                                                            voiceInfo.durationMs
                                                        )
                                                    }"
                                                }
                                            )
                                        }

                                    } else {

                                        SelectionContainer {
                                            Text(
                                                message.text,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    Spacer(
                                        Modifier.height(3.dp)
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
                                                Modifier.width(5.dp)
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

                                /*
                                 * COPY IS ONLY AVAILABLE FOR
                                 * WRITTEN MESSAGES.
                                 */
                                if (voiceInfo == null) {

                                    TextButton(
                                        onClick = {

                                            copyPeejeeMessageToClipboard(
                                                context,
                                                message.text
                                            )

                                            selectedMessage =
                                                null
                                        }
                                    ) {
                                        Text("📋 Copy")
                                    }

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
                                }

                                /*
                                 * ONLY THE SENDER CAN DELETE
                                 * THEIR OWN MESSAGE OR VOICE NOTE.
                                 */
                                if (mine) {

                                    TextButton(
                                        onClick = {

                                            val messageId =
                                                message.id

                                            firestore
                                                .collection(
                                                    "messages"
                                                )
                                                .document(
                                                    messageId
                                                )
                                                .delete()
                                                .addOnSuccessListener {

                                                    if (
                                                        currentlyPlayingMessageId ==
                                                        messageId
                                                    ) {
                                                        stopCurrentPlayback()
                                                    }

                                                    selectedMessage =
                                                        null
                                                }
                                                .addOnFailureListener {

                                                    errorMessage =
                                                        "Could not delete message."
                                                }
                                        }
                                    ) {

                                        Text(
                                            if (
                                                voiceInfo != null
                                            ) {
                                                "🗑 Delete voice note"
                                            } else {
                                                "🗑 Delete"
                                            }
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
                    modifier =
                        Modifier
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
                                "😀",
                                "😂",
                                "🤣",
                                "😊",
                                "😍",
                                "🥰",
                                "😘",
                                "😎",
                                "🤔",
                                "😢",
                                "😭",
                                "😡",
                                "👍",
                                "👎",
                                "👏",
                                "🙏",
                                "❤️",
                                "🔥",
                                "🎉",
                                "💯",
                                "😇",
                                "😉",
                                "😮",
                                "😴"
                            )
                        ) {
                            _,
                            emoji ->

                            TextButton(
                                onClick = {

                                    newMessage +=
                                        emoji

                                    showEmojiPicker =
                                        false
                                },

                                enabled =
                                    !sending &&
                                        !isRecording
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

            if (isRecording) {

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            "🔴 Recording voice note…",

                            fontWeight =
                                FontWeight.Bold,

                            modifier =
                                Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                stopRecordingAndSend()
                            }
                        ) {
                            Text("Stop & Send")
                        }
                    }
                }
            }

            Row(
                modifier =
                    Modifier
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

                    enabled =
                        !sending &&
                            !isRecording
                ) {

                    Text(
                        "😊",
                        fontSize = 25.sp
                    )
                }

                OutlinedTextField(
                    value =
                        newMessage,

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

                    enabled =
                        !sending &&
                            !isRecording
                )

                Spacer(
                    Modifier.width(4.dp)
                )

                IconButton(
                    onClick = {

                        if (isRecording) {
                            stopRecordingAndSend()
                        } else {
                            requestOrStartRecording()
                        }
                    },

                    enabled =
                        !sending &&
                            newMessage.isBlank()
                ) {

                    Text(
                        if (isRecording) {
                            "⏹"
                        } else {
                            "🎙️"
                        },

                        fontSize = 25.sp
                    )
                }

                Spacer(
                    Modifier.width(2.dp)
                )

                Button(
                    onClick = {
                        sendMessage()
                    },

                    enabled =
                        !sending &&
                            !isRecording &&
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

    if (showBackgroundPicker) {

        PeejeeChatBackgroundPicker(
            selectedBackground =
                chatBackground,

            onBackgroundSelected = {
                selected ->

                /*
                 * Save immediately when the user chooses
                 * a background.
                 *
                 * This is for THIS private chat only.
                 */
                chatBackground =
                    selected

                savePeejeeChatBackground(
                    context,
                    chatBackgroundKey,
                    selected
                )

                showBackgroundPicker =
                    false
            },

            onChoosePhoto = {
                chatPhotoLauncher.launch(
                    arrayOf("image/*")
                )
            },

            onDismiss = {
                showBackgroundPicker =
                    false
            }
        )
    }

    if (false) {

        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },

            title = {
                Text("")
            },

            text = {
                Text("")
            }
        )
    }
}
