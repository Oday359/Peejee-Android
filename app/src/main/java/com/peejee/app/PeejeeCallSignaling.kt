package com.peejee.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

/**
 * Peejee Firestore signaling manager.
 *
 * Firestore is used to exchange:
 * - Call information
 * - WebRTC offer
 * - WebRTC answer
 * - ICE candidates
 * - Call state
 *
 * The actual audio/video will continue to travel through WebRTC.
 */
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

    /**
     * Create a new call.
     */
    fun createCall(
        callerId: String,
        receiverId: String,
        callerName: String,
        receiverName: String,
        type: PeejeeCallType,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val callReference =
            firestore
                .collection(CALLS_COLLECTION)
                .document()

        val now =
            System.currentTimeMillis()

        val call =
            hashMapOf(
                "callId" to callReference.id,
                "callerId" to callerId,
                "receiverId" to receiverId,
                "callerName" to callerName,
                "receiverName" to receiverName,
                "type" to type.name,
                "state" to PeejeeCallState.RINGING.name,
                "createdAt" to now,
                "updatedAt" to now
            )

        callReference
            .set(call)
            .addOnSuccessListener {
                onSuccess(callReference.id)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    /**
     * Listen for changes to one call.
     */
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

    /**
     * Listen for incoming ringing calls for a user.
     */
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

    /**
     * Update the current call state.
     */
    fun updateCallState(
        callId: String,
        state: PeejeeCallState,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .update(
                mapOf(
                    "state" to state.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    /**
     * Save a WebRTC offer.
     */
    fun saveOffer(
        callId: String,
        description: PeejeeSessionDescription,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        val offer =
            hashMapOf(
                "type" to description.type,
                "sdp" to description.sdp
            )

        firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .set(
                mapOf(
                    OFFER_FIELD to offer,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    /**
     * Save a WebRTC answer.
     */
    fun saveAnswer(
        callId: String,
        description: PeejeeSessionDescription,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        val answer =
            hashMapOf(
                "type" to description.type,
                "sdp" to description.sdp
            )

        firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .set(
                mapOf(
                    ANSWER_FIELD to answer,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    /**
     * Read the WebRTC offer.
     */
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

    /**
     * Read the WebRTC answer.
     */
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

    /**
     * Add an ICE candidate from the caller.
     */
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

    /**
     * Add an ICE candidate from the receiver.
     */
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

    /**
     * Listen for caller ICE candidates.
     */
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

    /**
     * Listen for receiver ICE candidates.
     */
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

    /**
     * Stop a call by marking it ended.
     */
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

    /**
     * Remove a listener safely.
     */
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

                val data =
                    snapshot
                        ?.get(field) as? Map<*, *>

                if (data == null) {
                    onDescription(null)
                    return@addSnapshotListener
                }

                val type =
                    data["type"]
                        ?.toString()
                        ?: ""

                val sdp =
                    data["sdp"]
                        ?.toString()
                        ?: ""

                if (type.isBlank() || sdp.isBlank()) {
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

        val candidateData =
            hashMapOf(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "candidate" to candidate.candidate
            )

        firestore
            .collection(CALLS_COLLECTION)
            .document(callId)
            .collection(collectionName)
            .add(candidateData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
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
                            com.google.firebase.firestore.DocumentChange.Type.ADDED
                        ) {

                            val data =
                                change.document.data

                            val sdpMid =
                                data["sdpMid"]
                                    ?.toString()

                            val sdpMLineIndex =
                                when (
                                    val value =
                                        data["sdpMLineIndex"]
                                ) {
                                    is Number ->
                                        value.toInt()

                                    else ->
                                        0
                                }

                            val candidate =
                                data["candidate"]
                                    ?.toString()
                                    ?: ""

                            if (candidate.isNotBlank()) {

                                onCandidate(
                                    PeejeeIceCandidate(
                                        sdpMid = sdpMid,
                                        sdpMLineIndex =
                                            sdpMLineIndex,
                                        candidate =
                                            candidate
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
