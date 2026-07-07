package com.example.faultclassifier1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnProceed =
            findViewById<Button>(R.id.btnProceed)

        btnProceed.setOnClickListener {

            val intent =
                Intent(this, MenuActivity::class.java)

            startActivity(intent)
        }
    }
}