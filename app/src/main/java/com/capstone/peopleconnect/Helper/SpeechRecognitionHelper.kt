package com.capstone.peopleconnect.Helper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.media.audiofx.NoiseSuppressor
import com.capstone.peopleconnect.Adapters.SpeechRecognitionCallback

class SpeechRecognitionHelper(
    private val context: Context,
    private val callback: SpeechRecognitionCallback
) {
    private lateinit var speechRecognizer: SpeechRecognizer
    private val handler = Handler()
    private var isListening: Boolean = false
    private var noiseSuppressor: NoiseSuppressor? = null // Make it nullable

    fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognizer", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Listening...")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "End of speech detected.")
                stopSpeechToText() // Stop the recognizer after speech is done
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    val recognizedText = it[0] // Get the first recognized text
                    Log.d("SpeechRecognizer", "Recognized text: $recognizedText")

                    // Pass the recognized text to the callback
                    callback.onSpeechResult(recognizedText)
                }
                stopSpeechToText() // Call stop after the result
            }

            override fun onError(error: Int) {
                // Handle errors and pass to callback
                callback.onError("Speech recognition error: $error")
                handleSpeechError(error)
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleSpeechError(error: Int) {
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                Log.e("SpeechRecognizer", "No match or timeout.")
                stopSpeechToText() // No need to restart immediately
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                Log.e("SpeechRecognizer", "Recognizer is busy. Trying again after a delay.")
                handler.postDelayed({ startSpeechToText() }, 1000)
            }
            else -> {
                Log.e("SpeechRecognizer", "Error code: $error")
                stopSpeechToText()
            }
        }
    }

    fun startSpeechToText() {
        if (!isListening) {
            isListening = true
            val audioSessionId = getAudioSessionId()

            // Check if the audio session ID is valid
            if (audioSessionId == -1) {
                Log.e("SpeechRecognitionHelper", "Invalid audio session ID. Cannot create NoiseSuppressor.")
                stopSpeechToText()
                return
            }

            // Create and enable NoiseSuppressor
            try {
                noiseSuppressor = NoiseSuppressor.create(audioSessionId)
                if (noiseSuppressor == null) {
                    Log.e("SpeechRecognitionHelper", "Failed to create NoiseSuppressor.")
                } else {
                    Log.d("SpeechRecognitionHelper", "NoiseSuppressor created successfully.")
                }
            } catch (e: Exception) {
                Log.e("SpeechRecognitionHelper", "Error creating NoiseSuppressor: ${e.message}")
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer.startListening(intent)
        }
    }


    fun stopSpeechToText() {
        if (isListening) {
            isListening = false
            speechRecognizer.stopListening()
            noiseSuppressor?.release() // Release NoiseSuppressor when done
            noiseSuppressor = null // Set to null after release
            Log.d("SpeechRecognizer", "Stopped listening")
        }
    }

    fun destroy() {
        speechRecognizer.destroy()
        noiseSuppressor?.release() // Ensure the NoiseSuppressor is released
    }

    fun requestMicrophonePermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            initSpeechRecognizer()
        }
    }

    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, initialize the speech recognizer
            initSpeechRecognizer()
            startSpeechToText()
            return true
        } else {
            // Permission denied
            Toast.makeText(context, "Microphone permission is required to use speech recognition", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun getAudioSessionId(): Int {
        // Check if microphone permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SpeechRecognitionHelper", "Microphone permission is not granted.")
            return -1 // Return an invalid session ID if permission is not granted
        }

        // Create an AudioRecord instance to obtain the audio session ID
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100, // Sample rate
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        )

        val sessionId = audioRecord.audioSessionId
        audioRecord.release() // Release the AudioRecord instance
        return sessionId
    }

}
