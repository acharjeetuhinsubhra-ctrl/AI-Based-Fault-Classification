package com.example.faultclassifier1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AiAssistantActivity : AppCompatActivity() {

private val apiKey = BuildConfig.GEMINI_API_KEY
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        val etQuestion = findViewById<EditText>(R.id.etQuestion)
        val btnAsk = findViewById<Button>(R.id.btnAsk)
        val tvAnswer = findViewById<TextView>(R.id.tvAnswer)

        btnAsk.setOnClickListener {

            val question = etQuestion.text.toString().trim()

            if (question.isEmpty()) {
                tvAnswer.text = "Please enter a question."
                return@setOnClickListener
            }

            tvAnswer.text = "Thinking..."

            askGemini(question, tvAnswer)
        }
    }

    private fun askGemini(question: String, tvAnswer: TextView) {

        val client = OkHttpClient()

        val systemInstruction = """
You are an AI assistant specialized in power system protection, transmission line faults, COMTRADE CFG/DAT files, relay protection, fault classification, and fault location.

Answer clearly and technically, but in simple language for engineering students.

Focus on topics related to:
- Lightning fault
- Vegetation fault
- Jhoom or forest-fire fault
- Insulation flashover
- Solid jumper fault
- CFG and DAT files
- Positive sequence voltage and current
- Single-end and double-end fault location
- Transmission line fault analysis

If the question is unrelated, politely guide the user back to power system fault analysis.
""".trimIndent()

        val appContext = AppDataStore.getProjectContext()

        val fullPrompt = """
$systemInstruction

Current Application Data:
$appContext

User question:
$question

Use the current application data if the question asks about the uploaded file, classification result, fault type, fault phase, fault location, distance, tower number, or selected line.
""".trimIndent()

        val json = JSONObject()
        val contents = JSONArray()
        val content = JSONObject()
        val parts = JSONArray()
        val part = JSONObject()

        part.put("text", fullPrompt)
        parts.put(part)
        content.put("parts", parts)
        contents.put(content)
        json.put("contents", contents)

        val mediaType = "application/json".toMediaType()

        val requestBody =
            json.toString().toRequestBody(mediaType)

        val request =
            Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")                .post(requestBody)
                .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {

                runOnUiThread {
                    tvAnswer.text =
                        "Error connecting to AI Assistant.\n\n${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {

                val responseText =
                    response.body?.string() ?: ""

                try {

                    val jsonObject =
                        JSONObject(responseText)

                    val answer =
                        jsonObject
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")

                    runOnUiThread {
                        tvAnswer.text = answer
                    }

                } catch (e: Exception) {

                    runOnUiThread {
                        tvAnswer.text =
                            "AI response parsing error.\n\n$responseText"
                    }
                }
            }
        })
    }
}