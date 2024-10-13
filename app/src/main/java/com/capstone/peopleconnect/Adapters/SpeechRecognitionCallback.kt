package com.capstone.peopleconnect.Adapters

interface SpeechRecognitionCallback {
    fun onSpeechResult(result: String)
    fun onError(error: String)
}