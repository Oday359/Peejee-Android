package com.peejee.app

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class PeejeeCallSignaling {

    companion object {
        private const val CALLS_COLLECTION = "calls"
        private const val OFFER_FIELD = "offer"
        private const val ANSWER_FIELD = "answer"
        private const val CALLER_CANDIDATES = "callerCandidates"
        private const val CALLEE_CANDIDATES = "calleeCandidates"
    }

    private val firestore =
        FirebaseFirestore.getInstance()

    fun createCall(
        callerId: String,
        receiverId: String,
        callerName: String,
        receiverName: String,
        type: PeejeeCallType,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val reference =
            firestore
                .collection(CALLS_COLLECTION)
                .document()

        val now =
            System.currentTimeMillis()

        val data =
            hashMapOf<String, Any>(
                "callId" to reference.id,
                "callerId" to callerId,
                "receiverId" to receiverId,
                "callerName" to callerName,
                "receiverName" to receiverName,
                "type" to type.name,
                "state" to PeejeeCallState.RINGING.name,
                "createdAt" to now,
                "updatedAt" to now
            )

        reference
            .set(data)
            .addOnSuccessListener {
                onSuccess(reference.id)
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun listenToCall(
        callId: String,
        onChanged: (PeejeeCall?) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onChanged(null)
                    return@addSnapshotListener
                }

                onChanged(
                    snapshot.toPeejeeCall()
                )
            }
    }

    fun listenForIncomingCalls(
        receiverId: String,
        onCall: (PeejeeCall) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return firestore
            .collection(CALLS_COLLECTION)
            .whereEqualTo(
                "receiverId",
                receiverId
            )
            .whereEqualTo(
                "state",
                PeejeeCallState.RINGING.name
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                snapshot
                    ?.documents
                    ?.forEach { document ->

                        val call =
                            document.toPeejeeCall()

                        if (call != null) {
                            onCall(call)
                        }
                    }
            }
    }

    fun updateCallState(
        callId: String,
        state: PeejeeCallState,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        val updates =
            hashMapOf<String, Any>(
                "state" to state.name,
                "updatedAt" to System.currentTimeMillis()
            )

        firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .update(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun saveOffer(
        callId: String,
        description: PeejeeSessionDescription,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        val offer =
            hashMapOf<String, Any>(
                "type" to description.type,
                "sdp" to description.sdp
            )

        val updates =
            hashMapOf<String, Any>(
                OFFER_FIELD to offer,
                "updatedAt" to System.currentTimeMillis()
            )

        firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .set(
                updates,
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun saveAnswer(
        callId: String,
        description: PeejeeSessionDescription,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        val answer =
            hashMapOf<String, Any>(
                "type" to description.type,
                "sdp" to description.sdp
            )

        val updates =
            hashMapOf<String, Any>(
                ANSWER_FIELD to answer,
                "updatedAt" to System.currentTimeMillis()
            )

        firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .set(
                updates,
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun listenForOffer(
        callId: String,
        onOffer: (PeejeeSessionDescription?) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return listenForSessionDescription(
            callId = callId,
            field = OFFER_FIELD,
            onDescription = onOffer,
            onError = onError
        )
    }

    fun listenForAnswer(
        callId: String,
        onAnswer: (PeejeeSessionDescription?) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return listenForSessionDescription(
            callId = callId,
            field = ANSWER_FIELD,
            onDescription = onAnswer,
            onError = onError
        )
    }

    fun addCallerCandidate(
        callId: String,
        candidate: PeejeeIceCandidate,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        addIceCandidate(
            callId = callId,
            collectionName = CALLER_CANDIDATES,
            candidate = candidate,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun addCalleeCandidate(
        callId: String,
        candidate: PeejeeIceCandidate,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        addIceCandidate(
            callId = callId,
            collectionName = CALLEE_CANDIDATES,
            candidate = candidate,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun listenForCallerCandidates(
        callId: String,
        onCandidate: (PeejeeIceCandidate) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return listenForCandidates(
            callId = callId,
            collectionName = CALLER_CANDIDATES,
            onCandidate = onCandidate,
            onError = onError
        )
    }

    fun listenForCalleeCandidates(
        callId: String,
        onCandidate: (PeejeeIceCandidate) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return listenForCandidates(
            callId = callId,
            collectionName = CALLEE_CANDIDATES,
            onCandidate = onCandidate,
            onError = onError
        )
    }

    fun endCall(
        callId: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        updateCallState(
            callId = callId,
            state = PeejeeCallState.ENDED,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun removeListener(
        registration: ListenerRegistration?
    ) {
        registration?.remove()
    }

    private fun listenForSessionDescription(
        callId: String,
        field: String,
        onDescription: (PeejeeSessionDescription?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val raw =
                    snapshot?.get(field)

                if (raw !is Map<*, *>) {
                    onDescription(null)
                    return@addSnapshotListener
                }

                val type =
                    raw["type"]?.toString() ?: ""

                val sdp =
                    raw["sdp"]?.toString() ?: ""

                if (
                    type.isBlank() ||
                    sdp.isBlank()
                ) {
                    onDescription(null)
                    return@addSnapshotListener
                }

                onDescription(
                    PeejeeSessionDescription(
                        type = type,
                        sdp = sdp
                    )
                )
            }
    }

    private fun addIceCandidate(
        callId: String,
        collectionName: String,
        candidate: PeejeeIceCandidate,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        val data =
            hashMapOf<String, Any?>(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "candidate" to candidate.candidate
            )

        firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .collection(collectionName)
            .add(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    private fun listenForCandidates(
        callId: String,
        collectionName: String,
        onCandidate: (PeejeeIceCandidate) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .collection(collectionName)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                snapshot
                    ?.documentChanges
                    ?.forEach { change ->

                        if (
                            change.type ==
                            DocumentChange.Type.ADDED
                        ) {

                            val data =
                                change.document.data

                            val sdpMid: String? =
                                data["sdpMid"]?.toString()

                            val sdpMLineIndex: Int =
                                when (
                                    val value =
                                        data["sdpMLineIndex"]
                                ) {
                                    is Number ->
                                        value.toInt()

                                    is String ->
                                        value.toIntOrNull()
                                            ?: 0

                                    else ->
                                        0
                                }

                            val candidateText: String =
                                data["candidate"]?.toString()
                                    ?: ""

                            if (
                                candidateText.isNotBlank()
                            ) {

                                onCandidate(
                                    PeejeeIceCandidate(
                                        sdpMid = sdpMid,
                                        sdpMLineIndex =
                                            sdpMLineIndex,
                                        candidate =
                                            candidateText
                                    )
                                )
                            }
                        }
                    }
            }
    }

    private fun DocumentSnapshot.toPeejeeCall():
        PeejeeCall? {

        if (!exists()) {
            return null
        }

        return PeejeeCall(
            callId =
                getString("callId")
                    ?: id,

            callerId =
                getString("callerId")
                    ?: "",

            receiverId =
                getString("receiverId")
                    ?: "",

            callerName =
                getString("callerName")
                    ?: "",

            receiverName =
                getString("receiverName")
                    ?: "",

            type =
                getString("type")
                    ?: PeejeeCallType.AUDIO.name,

            state =
                getString("state")
                    ?: PeejeeCallState.RINGING.name,

            createdAt =
                getLong("createdAt")
                    ?: 0L,

            updatedAt =
                getLong("updatedAt")
                    ?: 0L
        )
    }
}
