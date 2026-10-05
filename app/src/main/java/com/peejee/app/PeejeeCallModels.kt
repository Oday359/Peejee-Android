package com.peejee.app

/**
 * The type of Peejee call.
 */
enum class PeejeeCallType {
    AUDIO,
    VIDEO
}

/**
 * The current state of a call.
 */
enum class PeejeeCallState {
    RINGING,
    CONNECTING,
    CONNECTED,
    ENDED,
    DECLINED,
    MISSED
}

/**
 * Information stored for a Peejee call.
 */
data class PeejeeCall(
    val callId: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val callerName: String = "",
    val receiverName: String = "",
    val type: String = PeejeeCallType.AUDIO.name,
    val state: String = PeejeeCallState.RINGING.name,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * Information about a WebRTC ICE candidate.
 */
data class PeejeeIceCandidate(
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0,
    val candidate: String = ""
)

/**
 * WebRTC session description.
 */
data class PeejeeSessionDescription(
    val type: String = "",
    val sdp: String = ""
)
