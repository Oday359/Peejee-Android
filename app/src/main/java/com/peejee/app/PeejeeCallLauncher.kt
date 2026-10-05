package com.peejee.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PeejeeChatCallControls(
    person: PeejeePerson,
    enabled: Boolean = true
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val signaling = remember {
        PeejeeCallSignaling(firestore)
    }

    var activeCallId by remember {
        mutableStateOf<String?>(null)
    }

    var activeCallType by remember {
        mutableStateOf<PeejeeCallType?>(null)
    }

    val currentUser = auth.currentUser

    if (currentUser != null) {

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {

            IconButton(
                onClick = {

                    if (!enabled) return@IconButton

                    val callerId = currentUser.uid

                    val callerFallbackName =
                        currentUser.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: "Peejee User"

                    firestore
                        .collection("users")
                        .document(callerId)
                        .get()
                        .addOnSuccessListener { document ->

                            val callerName =
                                document.getString("name")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: callerFallbackName

                            signaling.createCall(
                                callerId = callerId,
                                receiverId = person.uid,
                                callerName = callerName,
                                receiverName = person.name,
                                type = PeejeeCallType.AUDIO,
                                onSuccess = { callId ->

                                    activeCallId = callId
                                    activeCallType =
                                        PeejeeCallType.AUDIO
                                },
                                onError = {
                                    activeCallId = null
                                    activeCallType = null
                                }
                            )
                        }
                },
                enabled = enabled
            ) {
                Text("📞")
            }

            IconButton(
                onClick = {

                    if (!enabled) return@IconButton

                    val callerId = currentUser.uid

                    val callerFallbackName =
                        currentUser.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: "Peejee User"

                    firestore
                        .collection("users")
                        .document(callerId)
                        .get()
                        .addOnSuccessListener { document ->

                            val callerName =
                                document.getString("name")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: callerFallbackName

                            signaling.createCall(
                                callerId = callerId,
                                receiverId = person.uid,
                                callerName = callerName,
                                receiverName = person.name,
                                type = PeejeeCallType.VIDEO,
                                onSuccess = { callId ->

                                    activeCallId = callId
                                    activeCallType =
                                        PeejeeCallType.VIDEO
                                },
                                onError = {
                                    activeCallId = null
                                    activeCallType = null
                                }
                            )
                        }
                },
                enabled = enabled
            ) {
                Text("📹")
            }
        }
    }

    val callId = activeCallId
    val callType = activeCallType

    if (callId != null && callType != null) {

        PeejeeCallWindow(
            callId = callId,
            isCaller = true,
            callType = callType,
            onClose = {

                signaling.endCall(
                    callId = callId
                )

                activeCallId = null
                activeCallType = null
            }
        )
    }
}

@Composable
fun PeejeeCallWindow(
    callId: String,
    isCaller: Boolean,
    callType: PeejeeCallType,
    onClose: () -> Unit
) {

    Dialog(
        onDismissRequest = {
            onClose()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {

        PeejeeCallScreen(
            callId = callId,
            isCaller = isCaller,
            callType = callType,
            onEndCall = {
                onClose()
            }
        )
    }
}
