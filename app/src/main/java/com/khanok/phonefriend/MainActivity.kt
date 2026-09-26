package com.khanok.phonefriend

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.khanok.phonefriend.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefsName = "phone_friend_prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        binding.apiKeyInput.setText(prefs.getString("api_key", ""))
        binding.wakeWordInput.setText(prefs.getString("wake_word", "دوست من"))

        binding.startButton.setOnClickListener {
            val apiKey = binding.apiKeyInput.text.toString().trim()
            val wakeWord = binding.wakeWordInput.text.toString().trim()

            if (apiKey.isEmpty() || wakeWord.isEmpty()) {
                binding.statusText.text = "وضعیت: اول کلید API و کلمهٔ فعال‌ساز رو پر کن"
                return@setOnClickListener
            }

            prefs.edit()
                .putString("api_key", apiKey)
                .putString("wake_word", wakeWord)
                .apply()

            requestPermissionsThenStart()
        }

        binding.stopButton.setOnClickListener {
            stopService(Intent(this, WakeWordService::class.java))
            binding.statusText.text = "وضعیت: خاموش"
        }
    }

    private fun requestPermissionsThenStart() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.POST_NOTIFICATIONS)

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        } else {
            startFriendService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startFriendService()
        } else {
            binding.statusText.text = "وضعیت: بدون اجازهٔ میکروفون نمیشه شروع کرد"
        }
    }

    private fun startFriendService() {
        val intent = Intent(this, WakeWordService::class.java)
        ContextCompat.startForegroundService(this, intent)
        binding.statusText.text = "وضعیت: روشن — منتظر شنیدن کلمهٔ فعال‌ساز"
    }
}
