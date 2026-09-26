package com.khanok.phonefriend

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class WakeWordService : Service(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())

    private var mode = "listening"

    private lateinit var wakeWord: String
    private lateinit var apiKey: String

    private val channelId = "phone_friend_channel"

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        startForeground(1, buildNotification("گوش می‌ده..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("phone_friend_prefs", MODE_PRIVATE)
        wakeWord = prefs.getString("wake_word", "دوست من") ?: "دوست من"
        apiKey = prefs.getString("api_key", "") ?: ""
        startListening()
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("fa", "IR")
        }
    }

    private fun startListening() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                handleHeardText(text)
            }

            override fun onError(error: Int) {
                handler.postDelayed({ startListening() }, 800)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(recognizerIntent)
    }

    private fun handleHeardText(text: String) {
        if (mode == "listening") {
            if (text.contains(wakeWord, ignoreCase = true)) {
                mode = "question"
                updateNotification("بله؟ بگو...")
                speak("جانم؟") { startListening() }
            } else {
                startListening()
            }
        } else {
            mode = "listening"
            updateNotification("گوش می‌ده...")
            if (text.isNotBlank()) {
                askAI(text)
            } else {
                startListening()
            }
        }
    }

    private fun askAI(question: String) {
        if (apiKey.isEmpty()) {
            speak("کلید ای پی آی تنظیم نشده") { startListening() }
            return
        }
        ClaudeApiClient.ask(apiKey, question) { answer ->
            handler.post {
                speak(answer) { startListening() }
            }
        }
    }

    private fun speak(text: String, onDone: () -> Unit) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt1")
        handler.postDelayed({ onDone() }, 1200 + (text.length * 60L))
    }

    private fun buildNotification(text: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "دوست من", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("دوست من")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, buildNotification(text))
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
