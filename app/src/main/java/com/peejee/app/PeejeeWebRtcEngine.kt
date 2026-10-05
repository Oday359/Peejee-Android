package com.peejee.app

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Peejee WebRTC engine.
 *
 * This class prepares the microphone, camera and WebRTC media tracks.
 *
 * It does NOT handle Firebase signaling or call screens yet.
 * Those will be added separately.
 */
class PeejeeWebRtcEngine(
    private val context: Context
) {

    private var initialized = false

    private var peerConnectionFactory: PeerConnectionFactory? = null

    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null

    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null

    private var cameraCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var eglBase: EglBase? = null

    /**
     * Initialize the WebRTC engine.
     */
    fun initialize() {

        if (initialized) {
            return
        }

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        eglBase = EglBase.create()

        val audioDeviceModule =
            JavaAudioDeviceModule
                .builder(context.applicationContext)
                .createAudioDeviceModule()

        peerConnectionFactory =
            PeerConnectionFactory
                .builder()
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(
                    org.webrtc.DefaultVideoEncoderFactory(
                        eglBase!!.eglBaseContext,
                        true,
                        true
                    )
                )
                .setVideoDecoderFactory(
                    org.webrtc.DefaultVideoDecoderFactory(
                        eglBase!!.eglBaseContext
                    )
                )
                .createPeerConnectionFactory()

        audioDeviceModule.release()

        createAudioTrack()

        initialized = true
    }

    /**
     * Create the microphone audio track.
     */
    private fun createAudioTrack() {

        val factory =
            peerConnectionFactory
                ?: return

        val constraints =
            MediaConstraints()

        audioSource =
            factory.createAudioSource(
                constraints
            )

        audioTrack =
            factory.createAudioTrack(
                "PEEJEE_AUDIO_TRACK",
                audioSource
            )
    }

    /**
     * Start the front camera and create
     * the local video track.
     */
    fun startCamera(): VideoTrack? {

        val factory =
            peerConnectionFactory
                ?: return null

        if (videoTrack != null) {
            return videoTrack
        }

        val enumerator =
            Camera2Enumerator(
                context.applicationContext
            )

        val cameraName =
            enumerator
                .deviceNames
                .firstOrNull {
                    enumerator.isFrontFacing(it)
                }
                ?: enumerator
                    .deviceNames
                    .firstOrNull()
                ?: return null

        val capturer =
            enumerator.createCapturer(
                cameraName,
                null
            )
                ?: return null

        cameraCapturer =
            capturer

        surfaceTextureHelper =
            SurfaceTextureHelper.create(
                "PeejeeCameraThread",
                eglBase!!.eglBaseContext
            )

        videoSource =
            factory.createVideoSource(
                false
            )

        capturer.initialize(
            surfaceTextureHelper,
            context.applicationContext,
            videoSource!!.capturerObserver
        )

        capturer.startCapture(
            1280,
            720,
            30
        )

        videoTrack =
            factory.createVideoTrack(
                "PEEJEE_VIDEO_TRACK",
                videoSource
            )

        return videoTrack
    }

    /**
     * Return the local microphone track.
     */
    fun getAudioTrack(): AudioTrack? {
        return audioTrack
    }

    /**
     * Return the local camera track.
     */
    fun getVideoTrack(): VideoTrack? {
        return videoTrack
    }

    /**
     * Turn microphone on or off.
     */
    fun setMicrophoneEnabled(
        enabled: Boolean
    ) {
        audioTrack?.setEnabled(enabled)
    }

    /**
     * Turn camera video on or off.
     */
    fun setCameraEnabled(
        enabled: Boolean
    ) {
        videoTrack?.setEnabled(enabled)
    }

    /**
     * Switch between front and rear camera.
     */
    fun switchCamera() {

        val capturer =
            cameraCapturer
                ?: return

        val enumerator =
            Camera2Enumerator(
                context.applicationContext
            )

        val currentCamera =
            enumerator
                .deviceNames
                .firstOrNull {
                    enumerator.isFrontFacing(it)
                }

        if (currentCamera == null) {
            return
        }

        capturer.switchCamera(
            object : CameraVideoCapturer.CameraSwitchHandler {

                override fun onCameraSwitchDone(
                    isFrontCamera: Boolean
                ) {
                    // Camera switch completed.
                }

                override fun onCameraSwitchError(
                    errorDescription: String
                ) {
                    // Camera switch failed.
                }
            }
        )
    }

    /**
     * Stop the camera.
     */
    private fun stopCamera() {

        val capturer =
            cameraCapturer

        if (capturer != null) {

            try {
                capturer.stopCapture()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            capturer.dispose()
        }

        cameraCapturer = null
    }

    /**
     * Release all WebRTC resources.
     */
    fun release() {

        stopCamera()

        videoTrack?.dispose()
        videoTrack = null

        videoSource?.dispose()
        videoSource = null

        audioTrack?.dispose()
        audioTrack = null

        audioSource?.dispose()
        audioSource = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        eglBase?.release()
        eglBase = null

        initialized = false
    }
}
