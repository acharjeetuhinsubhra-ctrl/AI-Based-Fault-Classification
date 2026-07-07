package com.example.faultclassifier1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val btnFaultClassification =
            findViewById<Button>(R.id.btnFaultClassification)

        val btnFaultLocation =
            findViewById<Button>(R.id.btnFaultLocation)

        btnFaultClassification.setOnClickListener {

            val intent =
                Intent(this, ClassificationActivity::class.java)

            startActivity(intent)
        }

        btnFaultLocation.setOnClickListener {

            val intent =
                Intent(this, LocationActivity::class.java)

            startActivity(intent)
        }
        val btnAiAssistant =
            findViewById<Button>(R.id.btnAiAssistant)

        btnAiAssistant.setOnClickListener {
            val intent =
                Intent(this, AiAssistantActivity::class.java)

            startActivity(intent)
        }
    }
}