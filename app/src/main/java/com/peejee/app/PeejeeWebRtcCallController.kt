package com.peejee.app

import android.content.Context
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Peejee WebRTC call controller.
 *
 * Handles:
 * - microphone
 * - camera
 * - PeerConnection
 * - SDP offer
 * - SDP answer
 * - ICE candidates
 * - remote video
 * - microphone mute
 * - camera enable/disable
 * - camera switching
 * - cleanup
 *
 * Firebase signaling is handled by PeejeeCallSignaling.
 */
class PeejeeWebRtcCallController(
    private val context: Context,
    private val callId: String,
    private val isCaller: Boolean,
    private val callType: PeejeeCallType,
    private val signaling: PeejeeCallSignaling,
    private val onStateChanged: (PeejeeCallState) -> Unit,
    private val onError: (String) -> Unit,
    private val onRemoteVideoTrack: (VideoTrack) -> Unit,
    private val onLocalVideoTrack: (VideoTrack) -> Unit
) {

    companion object {

        private const val STREAM_ID = "peejee_stream"

        private const val STUN_SERVER =
            "stun:stun.l.google.com:19302"

        @Volatile
        private var webRtcInitialized = false
    }

    private val appContext =
        context.applicationContext

    private var factory:
        PeerConnectionFactory? = null

    private var peerConnection:
        PeerConnection? = null

    private var audioDeviceModule:
        JavaAudioDeviceModule? = null

    private var audioSource:
        org.webrtc.AudioSource? = null

    private var audioTrack:
        org.webrtc.AudioTrack? = null

    private var videoSource:
        VideoSource? = null

    private var videoTrack:
        VideoTrack? = null

    private var cameraCapturer:
        CameraVideoCapturer? = null

    private var surfaceTextureHelper:
        SurfaceTextureHelper? = null

    private var eglBase:
        EglBase? = null

    private var answerListener:
        com.google.firebase.firestore.ListenerRegistration? = null

    private var offerListener:
        com.google.firebase.firestore.ListenerRegistration? = null

    private var remoteCandidateListener:
        com.google.firebase.firestore.ListenerRegistration? = null

    private var remoteDescriptionSet =
        false

    private val pendingRemoteCandidates =
        mutableListOf<IceCandidate>()

    private var released = false

    private fun ensureWebRtcInitialized() {

        if (webRtcInitialized) {
            return
        }

        synchronized(
            PeejeeWebRtcCallController::class.java
        ) {

            if (webRtcInitialized) {
                return
            }

            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions
                    .builder(appContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )

            webRtcInitialized = true
        }
    }

    fun start() {

        if (released) {
            return
        }

        try {

            ensureWebRtcInitialized()

            eglBase =
                EglBase.create()

            audioDeviceModule =
                JavaAudioDeviceModule
                    .builder(appContext)
                    .createAudioDeviceModule()

            factory =
                PeerConnectionFactory
                    .builder()
                    .setAudioDeviceModule(
                        audioDeviceModule
                    )
                    .setVideoEncoderFactory(
                        DefaultVideoEncoderFactory(
                            eglBase!!.eglBaseContext,
                            true,
                            true
                        )
                    )
                    .setVideoDecoderFactory(
                        DefaultVideoDecoderFactory(
                            eglBase!!.eglBaseContext
                        )
                    )
                    .createPeerConnectionFactory()

            audioDeviceModule?.release()
            audioDeviceModule = null

            createLocalAudio()

            if (
                callType ==
                PeejeeCallType.VIDEO
            ) {
                createLocalVideo()
            }

            createPeerConnection()

            listenForSignaling()

            if (isCaller) {
                createOffer()
            } else {
                onStateChanged(
                    PeejeeCallState.CONNECTING
                )
            }

        } catch (exception: Exception) {

            onError(
                exception.message
                    ?: "Could not start the call."
            )

            release()
        }
    }

    private fun createLocalAudio() {

        val localFactory =
            factory
                ?: throw IllegalStateException(
                    "WebRTC factory is not ready."
                )

        audioSource =
            localFactory.createAudioSource(
                MediaConstraints()
            )

        audioTrack =
            localFactory.createAudioTrack(
                "PEEJEE_AUDIO",
                audioSource
            )
    }

    private fun createLocalVideo() {

        val localFactory =
            factory
                ?: throw IllegalStateException(
                    "WebRTC factory is not ready."
                )

        val egl =
            eglBase
                ?: throw IllegalStateException(
                    "EGL is not ready."
                )

        val enumerator =
            Camera2Enumerator(appContext)

        val cameraName =
            enumerator.deviceNames
                .firstOrNull {
                    enumerator.isFrontFacing(it)
                }
                ?: enumerator.deviceNames
                    .firstOrNull()
                ?: throw IllegalStateException(
                    "No camera was found."
                )

        val capturer =
            enumerator.createCapturer(
                cameraName,
                null
            )
                ?: throw IllegalStateException(
                    "Could not open the camera."
                )

        cameraCapturer =
            capturer

        surfaceTextureHelper =
            SurfaceTextureHelper.create(
                "PeejeeCallCamera",
                egl.eglBaseContext
            )

        videoSource =
            localFactory.createVideoSource(
                false
            )

        capturer.initialize(
            surfaceTextureHelper,
            appContext,
            videoSource!!.capturerObserver
        )

        capturer.startCapture(
            1280,
            720,
            30
        )

        videoTrack =
            localFactory.createVideoTrack(
                "PEEJEE_VIDEO",
                videoSource
            )

        videoTrack?.setEnabled(true)

        videoTrack?.let {
            onLocalVideoTrack(it)
        }
    }

    private fun createPeerConnection() {

        val localFactory =
            factory
                ?: throw IllegalStateException(
                    "WebRTC factory is not ready."
                )

        val iceServer =
            PeerConnection.IceServer
                .builder(STUN_SERVER)
                .createIceServer()

        val configuration =
            PeerConnection.RTCConfiguration(
                listOf(iceServer)
            ).apply {

                sdpSemantics =
                    PeerConnection.SdpSemantics.UNIFIED_PLAN
            }

        val observer =
            object : PeerConnection.Observer {

                override fun onSignalingChange(
                    newState:
                    PeerConnection.SignalingState
                ) {
                }

                override fun onIceConnectionChange(
                    newState:
                    PeerConnection.IceConnectionState
                ) {

                    when (newState) {

                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> {

                            onStateChanged(
                                PeejeeCallState.CONNECTED
                            )
                        }

                        PeerConnection.IceConnectionState.FAILED -> {

                            onError(
                                "The call connection failed."
                            )
                        }

                        PeerConnection.IceConnectionState.DISCONNECTED -> {

                            onStateChanged(
                                PeejeeCallState.ENDED
                            )
                        }

                        else -> {
                        }
                    }
                }

                override fun onIceConnectionReceivingChange(
                    receiving: Boolean
                ) {
                }

                override fun onIceGatheringChange(
                    newState:
                    PeerConnection.IceGatheringState
                ) {
                }

                override fun onIceCandidate(
                    candidate: IceCandidate
                ) {

                    val peejeeCandidate =
                        PeejeeIceCandidate(
                            sdpMid =
                                candidate.sdpMid,
                            sdpMLineIndex =
                                candidate.sdpMLineIndex,
                            candidate =
                                candidate.sdp
                        )

                    if (isCaller) {

                        signaling.addCallerCandidate(
                            callId = callId,
                            candidate = peejeeCandidate
                        )

                    } else {

                        signaling.addCalleeCandidate(
                            callId = callId,
                            candidate = peejeeCandidate
                        )
                    }
                }

                override fun onIceCandidatesRemoved(
                    candidates:
                    Array<IceCandidate>
                ) {
                }

                override fun onAddStream(
                    stream: MediaStream
                ) {

                    val remoteVideo =
                        stream.videoTracks
                            .firstOrNull()

                    if (remoteVideo != null) {
                        onRemoteVideoTrack(
                            remoteVideo
                        )
                    }
                }

                override fun onRemoveStream(
                    stream: MediaStream
                ) {
                }

                override fun onDataChannel(
                    dataChannel:
                    org.webrtc.DataChannel
                ) {
                }

                override fun onRenegotiationNeeded() {
                }

                override fun onAddTrack(
                    receiver: RtpReceiver,
                    mediaStreams:
                    Array<MediaStream>
                ) {

                    val track =
                        receiver.track()

                    if (
                        track is VideoTrack
                    ) {
                        onRemoteVideoTrack(
                            track
                        )
                    }
                }

                override fun onTrack(
                    transceiver:
                    RtpTransceiver
                ) {

                    val track =
                        transceiver
                            .receiver
                            .track()

                    if (
                        track is VideoTrack
                    ) {
                        onRemoteVideoTrack(
                            track
                        )
                    }
                }

                override fun onConnectionChange(
                    newState:
                    PeerConnection.PeerConnectionState
                ) {

                    when (newState) {

                        PeerConnection.PeerConnectionState.CONNECTED -> {

                            onStateChanged(
                                PeejeeCallState.CONNECTED
                            )
                        }

                        PeerConnection.PeerConnectionState.FAILED -> {

                            onError(
                                "The call connection failed."
                            )
                        }

                        PeerConnection.PeerConnectionState.DISCONNECTED,
                        PeerConnection.PeerConnectionState.CLOSED -> {

                            onStateChanged(
                                PeejeeCallState.ENDED
                            )
                        }

                        else -> {
                        }
                    }
                }
            }

        peerConnection =
            localFactory.createPeerConnection(
                configuration,
                observer
            )

        if (peerConnection == null) {
            throw IllegalStateException(
                "Could not create the WebRTC connection."
            )
        }

        audioTrack?.let { track ->

            peerConnection?.addTrack(
                track,
                listOf(STREAM_ID)
            )
        }

        if (
            callType ==
            PeejeeCallType.VIDEO
        ) {

            videoTrack?.let { track ->

                peerConnection?.addTrack(
                    track,
                    listOf(STREAM_ID)
                )
            }
        }
    }

    private fun listenForSignaling() {

        if (isCaller) {

            answerListener =
                signaling.listenForAnswer(
                    callId = callId,
                    onAnswer = { description ->

                        if (description == null) {
                            return@listenForAnswer
                        }

                        if (
                            peerConnection
                                ?.remoteDescription != null
                        ) {
                            return@listenForAnswer
                        }

                        val remoteDescription =
                            SessionDescription(
                                SessionDescription.Type
                                    .fromCanonicalForm(
                                        description.type
                                    ),
                                description.sdp
                            )

                        setRemoteDescription(
                            remoteDescription
                        )
                    }
                )

            remoteCandidateListener =
                signaling.listenForCalleeCandidates(
                    callId = callId,
                    onCandidate = {
                        handleRemoteCandidate(it)
                    }
                )

        } else {

            offerListener =
                signaling.listenForOffer(
                    callId = callId,
                    onOffer = { description ->

                        if (description == null) {
                            return@listenForOffer
                        }

                        if (
                            peerConnection
                                ?.remoteDescription != null
                        ) {
                            return@listenForOffer
                        }

                        val remoteDescription =
                            SessionDescription(
                                SessionDescription.Type
                                    .fromCanonicalForm(
                                        description.type
                                    ),
                                description.sdp
                            )

                        setRemoteDescription(
                            remoteDescription
                        )
                    }
                )

            remoteCandidateListener =
                signaling.listenForCallerCandidates(
                    callId = callId,
                    onCandidate = {
                        handleRemoteCandidate(it)
                    }
                )
        }
    }

    private fun createOffer() {

        onStateChanged(
            PeejeeCallState.CONNECTING
        )

        peerConnection?.createOffer(
            object : SimpleSdpObserver() {

                override fun onCreateSuccess(
                    description:
                    SessionDescription
                ) {

                    peerConnection?.setLocalDescription(
                        object : SimpleSdpObserver() {

                            override fun onSetSuccess() {

                                signaling.saveOffer(
                                    callId = callId,
                                    description =
                                        PeejeeSessionDescription(
                                            type =
                                                description
                                                    .type
                                                    .canonicalForm(),
                                            sdp =
                                                description.description
                                        )
                                )
                            }

                            override fun onSetFailure(
                                error: String
                            ) {

                                onError(
                                    "Could not set the call offer: $error"
                                )
                            }
                        },
                        description
                    )
                }

                override fun onCreateFailure(
                    error: String
                ) {

                    onError(
                        "Could not create the call offer: $error"
                    )
                }
            },
            MediaConstraints()
        )
    }

    private fun setRemoteDescription(
        description:
        SessionDescription
    ) {

        peerConnection?.setRemoteDescription(
            object : SimpleSdpObserver() {

                override fun onSetSuccess() {

                    remoteDescriptionSet =
                        true

                    flushPendingRemoteCandidates()

                    if (!isCaller) {
                        createAnswer()
                    }
                }

                override fun onSetFailure(
                    error: String
                ) {

                    onError(
                        "Could not set the remote call description: $error"
                    )
                }
            },
            description
        )
    }

    private fun createAnswer() {

        onStateChanged(
            PeejeeCallState.CONNECTING
        )

        peerConnection?.createAnswer(
            object : SimpleSdpObserver() {

                override fun onCreateSuccess(
                    description:
                    SessionDescription
                ) {

                    peerConnection?.setLocalDescription(
                        object : SimpleSdpObserver() {

                            override fun onSetSuccess() {

                                signaling.saveAnswer(
                                    callId = callId,
                                    description =
                                        PeejeeSessionDescription(
                                            type =
                                                description
                                                    .type
                                                    .canonicalForm(),
                                            sdp =
                                                description.description
                                        )
                                )
                            }

                            override fun onSetFailure(
                                error: String
                            ) {

                                onError(
                                    "Could not set the call answer: $error"
                                )
                            }
                        },
                        description
                    )
                }

                override fun onCreateFailure(
                    error: String
                ) {

                    onError(
                        "Could not create the call answer: $error"
                    )
                }
            },
            MediaConstraints()
        )
    }

    private fun handleRemoteCandidate(
        candidate:
        PeejeeIceCandidate
    ) {

        val iceCandidate =
            IceCandidate(
                candidate.sdpMid,
                candidate.sdpMLineIndex,
                candidate.candidate
            )

        if (!remoteDescriptionSet) {

            synchronized(
                pendingRemoteCandidates
            ) {
                pendingRemoteCandidates.add(
                    iceCandidate
                )
            }

            return
        }

        peerConnection?.addIceCandidate(
            iceCandidate
        )
    }

    private fun flushPendingRemoteCandidates() {

        val candidates =
            synchronized(
                pendingRemoteCandidates
            ) {

                val copy =
                    pendingRemoteCandidates.toList()

                pendingRemoteCandidates.clear()

                copy
            }

        candidates.forEach { candidate ->

            peerConnection?.addIceCandidate(
                candidate
            )
        }
    }

    fun setMicrophoneEnabled(
        enabled: Boolean
    ) {
        audioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(
        enabled: Boolean
    ) {
        videoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {

        cameraCapturer?.switchCamera(
            object :
                CameraVideoCapturer.CameraSwitchHandler {

                override fun onCameraSwitchDone(
                    isFrontCamera: Boolean
                ) {
                }

                override fun onCameraSwitchError(
                    errorDescription: String
                ) {

                    onError(
                        "Could not switch camera."
                    )
                }
            }
        )
    }

    fun getLocalVideoTrack():
        VideoTrack? {
        return videoTrack
    }

    fun getEglBaseContext():
        EglBase.Context? {
        return eglBase?.eglBaseContext
    }

    fun end() {

        if (released) {
            return
        }

        onStateChanged(
            PeejeeCallState.ENDED
        )

        release()
    }

    fun release() {

        if (released) {
            return
        }

        released = true

        try {
            answerListener?.remove()
        } catch (_: Exception) {
        }

        try {
            offerListener?.remove()
        } catch (_: Exception) {
        }

        try {
            remoteCandidateListener?.remove()
        } catch (_: Exception) {
        }

        answerListener = null
        offerListener = null
        remoteCandidateListener = null

        try {
            cameraCapturer?.stopCapture()
        } catch (_: Exception) {
        }

        try {
            cameraCapturer?.dispose()
        } catch (_: Exception) {
        }

        cameraCapturer = null

        try {
            surfaceTextureHelper?.dispose()
        } catch (_: Exception) {
        }

        surfaceTextureHelper = null

        try {
            peerConnection?.close()
        } catch (_: Exception) {
        }

        try {
            peerConnection?.dispose()
        } catch (_: Exception) {
        }

        peerConnection = null

        try {
            videoTrack?.dispose()
        } catch (_: Exception) {
        }

        videoTrack = null

        try {
            videoSource?.dispose()
        } catch (_: Exception) {
        }

        videoSource = null

        try {
            audioTrack?.dispose()
        } catch (_: Exception) {
        }

        audioTrack = null

        try {
            audioSource?.dispose()
        } catch (_: Exception) {
        }

        audioSource = null

        try {
            factory?.dispose()
        } catch (_: Exception) {
        }

        factory = null

        try {
            eglBase?.release()
        } catch (_: Exception) {
        }

        eglBase = null

        synchronized(
            pendingRemoteCandidates
        ) {
            pendingRemoteCandidates.clear()
        }
    }

    private open class SimpleSdpObserver :
        SdpObserver {

        override fun onCreateSuccess(
            description:
            SessionDescription
        ) {
        }

        override fun onSetSuccess() {
        }

        override fun onCreateFailure(
            error: String
        ) {
        }

        override fun onSetFailure(
            error: String
        ) {
        }
    }
}
