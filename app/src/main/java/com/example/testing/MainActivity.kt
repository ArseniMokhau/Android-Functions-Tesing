package com.example.testing

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import android.speech.RecognitionListener
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        val helpStatusTextView = findViewById<TextView>(R.id.helpStatusTextView)
        val startVoiceButton: Button = findViewById(R.id.startVoiceButton)
        resultTextView = findViewById(R.id.resultTextView)

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // Set up onClickListener for the voice recognition button
        startVoiceButton.setOnClickListener {
            startVoiceRecognition()
        }

        // Set up a RecognitionListener to handle speech recognition events
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // Handle speech recognition errors
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> resultTextView.text = "No match found"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> resultTextView.text = "Speech recognition timed out"
                    // Add handling for other error cases as needed
                    else -> resultTextView.text = "Error: $error"
                }
            }

            override fun onResults(results: Bundle?) {
                // Process the final speech recognition results
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    val spokenText = it[0].toLowerCase()
                    resultTextView.text = spokenText

                    // Check if the spoken text matches predefined phrases
                    if (spokenText == "stop") {
                        // Perform actions when "stop" is recognized
                        // Example: stopContinuousVoiceRecognition()
                    } else if (spokenText == "help") {
                        // Perform actions when "help" is recognized
                        helpStatusTextView.text = "Help is on the way!"
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial speech recognition results
                // Not used in this example but can be extended for continuous processing
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle additional recognition events (if needed)
            }
        })
    }

    private fun startVoiceRecognition() {
        // Start the voice recognition process
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak 'stop' or 'help'")

        // Add a custom list of phrases to be recognized
        val phrases = arrayListOf("stop", "help")
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Optional: use offline recognition if available
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US") // Optional: specify language preference

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)

        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        intent.putExtra(RecognizerIntent.EXTRA_RESULTS, phrases.toTypedArray())

        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        // Release resources when the activity is destroyed
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
