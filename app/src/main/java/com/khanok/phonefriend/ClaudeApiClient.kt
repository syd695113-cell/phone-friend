package com.khanok.phonefriend

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object ClaudeApiClient {

    private val client = OkHttpClient()
    private const val URL = "https://api.anthropic.com/v1/messages"

    fun ask(apiKey: String, question: String, callback: (String) -> Unit) {
        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 300)
            put("system", "تو یه دوست صمیمی و کوتاه‌جواب هستی که به فارسی جواب می‌دی. جواب‌هاتو کوتاه نگه دار چون با صدا خونده میشن.")
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", question)
                }
            ))
        }

        val request = Request.Builder()
            .url(URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(RequestBody.create(
                MediaType.parse("application/json"), body.toString()
            ))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("نتونستم وصل بشم، دوباره امتحان کن")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body()?.string() ?: "{}")
                    val content = json.optJSONArray("content")
                    val text = content?.optJSONObject(0)?.optString("text") ?: "جواب نگرفتم"
                    callback(text)
                } catch (e: Exception) {
                    callback("یه مشکلی تو جواب پیش اومد")
                }
            }
        })
    }
}
