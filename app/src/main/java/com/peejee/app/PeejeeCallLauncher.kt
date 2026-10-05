package com.peejee.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
        PeejeeCallSignaling()
    }

    var activeCallId by remember {
        mutableStateOf<String?>(null)
    }

    var activeCallType by remember {
        mutableStateOf<PeejeeCallType?>(null)
    }

    var callError by remember {
        mutableStateOf("")
    }

    var startingCall by remember {
        mutableStateOf(false)
    }

    val currentUser = auth.currentUser

    fun startCall(
        callType: PeejeeCallType
    ) {

        if (!enabled || startingCall) {
            return
        }

        if (currentUser == null) {
            callError =
                "You are not logged in to Peejee."

            return
        }

        if (currentUser.uid.isBlank()) {
            callError =
                "Your Peejee account ID is missing."

            return
        }

        if (person.uid.isBlank()) {
            callError =
                "This user's account ID is missing."

            return
        }

        startingCall = true
        callError = ""

        val callerId =
            currentUser.uid

        val callerFallbackName =
            currentUser.displayName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Peejee User"

        /*
         * We try to get the caller's Peejee name.
         *
         * If the user document cannot be read,
         * we still continue with the fallback name.
         */
        firestore
            .collection("users")
            .document(callerId)
            .get()
            .addOnSuccessListener { document ->

                val callerName =
                    document
                        .getString("name")
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: callerFallbackName

                createTheCall(
                    signaling = signaling,
                    callerId = callerId,
                    receiverId = person.uid,
                    callerName = callerName,
                    receiverName = person.name,
                    callType = callType,
                    onSuccess = { callId ->

                        startingCall = false

                        activeCallId =
                            callId

                        activeCallType =
                            callType
                    },
                    onError = { exception ->

                        startingCall = false

                        callError =
                            "Call could not start:\n\n${exception.message ?: "Unknown Firebase error"}"
                    }
                )
            }
            .addOnFailureListener {

                /*
                 * Even if reading the caller profile fails,
                 * try creating the call with the fallback name.
                 */
                createTheCall(
                    signaling = signaling,
                    callerId = callerId,
                    receiverId = person.uid,
                    callerName = callerFallbackName,
                    receiverName = person.name,
                    callType = callType,
                    onSuccess = { callId ->

                        startingCall = false

                        activeCallId =
                            callId

                        activeCallType =
                            callType
                    },
                    onError = { exception ->

                        startingCall = false

                        callError =
                            "Call could not start:\n\n${exception.message ?: "Unknown Firebase error"}"
                    }
                )
            }
    }

    if (currentUser != null) {

        Row(
            horizontalArrangement =
                Arrangement.End,

            modifier =
                Modifier.padding(
                    horizontal = 2.dp
                )
        ) {

            IconButton(
                onClick = {
                    startCall(
                        PeejeeCallType.AUDIO
                    )
                },

                enabled =
                    enabled &&
                        !startingCall &&
                        activeCallId == null
            ) {

                Text("📞")
            }

            IconButton(
                onClick = {
                    startCall(
                        PeejeeCallType.VIDEO
                    )
                },

                enabled =
                    enabled &&
                        !startingCall &&
                        activeCallId == null
            ) {

                Text("📷")
            }
        }
    }

    if (startingCall) {

        AlertDialog(
            onDismissRequest = {
                // Do not cancel the Firestore request.
            },

            title = {
                Text("Peejee Call")
            },

            text = {
                Text(
                    "Starting ${if (activeCallType == PeejeeCallType.VIDEO) "video" else "audio"} call…"
                )
            },

            confirmButton = { }
        )
    }

    if (callError.isNotBlank()) {

        AlertDialog(
            onDismissRequest = {
                callError = ""
            },

            title = {
                Text("Call Error")
            },

            text = {
                Text(callError)
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        callError = ""
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    val callId =
        activeCallId

    val callType =
        activeCallType

    if (
        callId != null &&
        callType != null
    ) {

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
                startingCall = false
            }
        )
    }
}

private fun createTheCall(
    signaling: PeejeeCallSignaling,
    callerId: String,
    receiverId: String,
    callerName: String,
    receiverName: String,
    callType: PeejeeCallType,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {

    signaling.createCall(
        callerId = callerId,
        receiverId = receiverId,
        callerName = callerName,
        receiverName = receiverName,
        type = callType,

        onSuccess = { callId ->
            onSuccess(callId)
        },

        onError = { exception ->
            onError(exception)
        }
    )
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

        properties =
            DialogProperties(
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
