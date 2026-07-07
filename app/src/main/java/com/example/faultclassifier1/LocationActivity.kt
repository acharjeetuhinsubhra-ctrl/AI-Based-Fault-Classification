package com.example.faultclassifier1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToLong
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class LocationCalculationResult(
        val selectedLine: LineInfo,
        val lineLength: Double,
        val ratio: Double,
        val faultDistanceLocal: Double,
        val faultDistanceRemote: Double,
        val towersLocal: Long,
        val towersRemote: Long,
        val faultLat: Double,
        val faultLon: Double
)

class LocationActivity : AppCompatActivity() {
    private val backendUrl = "http://10.145.93.119:5000/add_fault"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        val radioSingle = findViewById<RadioButton>(R.id.radioSingle)
        val radioDouble = findViewById<RadioButton>(R.id.radioDouble)

        val tvPreReceivingTitle = findViewById<TextView>(R.id.tvPreReceivingTitle)
        val tvFaultReceivingTitle = findViewById<TextView>(R.id.tvFaultReceivingTitle)

        val spinnerLine = findViewById<Spinner>(R.id.spinnerLine)

        val etPreVsMag = findViewById<EditText>(R.id.etPreVsMag)
        val etPreVsAngle = findViewById<EditText>(R.id.etPreVsAngle)
        val etPreIsMag = findViewById<EditText>(R.id.etPreIsMag)
        val etPreIsAngle = findViewById<EditText>(R.id.etPreIsAngle)

        val etPreVrMag = findViewById<EditText>(R.id.etPreVrMag)
        val etPreVrAngle = findViewById<EditText>(R.id.etPreVrAngle)
        val etPreIrMag = findViewById<EditText>(R.id.etPreIrMag)
        val etPreIrAngle = findViewById<EditText>(R.id.etPreIrAngle)

        val etFaultVsMag = findViewById<EditText>(R.id.etFaultVsMag)
        val etFaultVsAngle = findViewById<EditText>(R.id.etFaultVsAngle)
        val etFaultIsMag = findViewById<EditText>(R.id.etFaultIsMag)
        val etFaultIsAngle = findViewById<EditText>(R.id.etFaultIsAngle)

        val etFaultVrMag = findViewById<EditText>(R.id.etFaultVrMag)
        val etFaultVrAngle = findViewById<EditText>(R.id.etFaultVrAngle)
        val etFaultIrMag = findViewById<EditText>(R.id.etFaultIrMag)
        val etFaultIrAngle = findViewById<EditText>(R.id.etFaultIrAngle)

        val etLineLength = findViewById<EditText>(R.id.etLineLength)
        val btnShowResults = findViewById<Button>(R.id.btnShowResults)
        val tvLocationResult = findViewById<TextView>(R.id.tvLocationResult)
        val btnLocateFault = findViewById<Button>(R.id.btnLocateFault)
        val btnAiAssistant = findViewById<Button>(R.id.btnAiAssistant)

        btnAiAssistant.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }

        val lines = LineDatabase.getLines()
        val lineNames = ArrayList<String>()

        for (line in lines) {
            lineNames.add(line.name)
        }

        val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                lineNames
        )

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        )

        spinnerLine.adapter = adapter

        fun showReceivingEnd(show: Boolean) {

            val visibility =
                    if (show) View.VISIBLE else View.GONE

            tvPreReceivingTitle.visibility = visibility

            etPreVrMag.visibility = visibility
            etPreVrAngle.visibility = visibility
            etPreIrMag.visibility = visibility
            etPreIrAngle.visibility = visibility

            tvFaultReceivingTitle.visibility = visibility

            etFaultVrMag.visibility = visibility
            etFaultVrAngle.visibility = visibility
            etFaultIrMag.visibility = visibility
            etFaultIrAngle.visibility = visibility
        }

        fun calculateLocation(): LocationCalculationResult {

            val selectedLine =
                    lines[spinnerLine.selectedItemPosition]

            val preVsMag =
                    etPreVsMag.text.toString().toDouble()

            val preVsAngle =
                    etPreVsAngle.text.toString().toDouble()

            val preIsMag =
                    etPreIsMag.text.toString().toDouble()

            val preIsAngle =
                    etPreIsAngle.text.toString().toDouble()

            val faultVsMag =
                    etFaultVsMag.text.toString().toDouble()

            val faultVsAngle =
                    etFaultVsAngle.text.toString().toDouble()

            val faultIsMag =
                    etFaultIsMag.text.toString().toDouble()

            val faultIsAngle =
                    etFaultIsAngle.text.toString().toDouble()

            val preVrMag =
                    if (radioSingle.isChecked)
                        0.0
                    else
                        etPreVrMag.text.toString().toDouble()

            val preVrAngle =
                    if (radioSingle.isChecked)
                        0.0
                    else
                        etPreVrAngle.text.toString().toDouble()

            val preIrMag =
                    if (radioSingle.isChecked)
                        0.0
                    else
                        etPreIrMag.text.toString().toDouble()

            val preIrAngle =
                    if (radioSingle.isChecked)
                        0.0
                    else
                        etPreIrAngle.text.toString().toDouble()

            val faultVrMag =
                    if (radioSingle.isChecked)
                        0.0
                    else
                        etFaultVrMag.text.toString().toDouble()

            val faultVrAngle =
                    if (radioSingle.isChecked)
                        0.0
                    else
                        etFaultVrAngle.text.toString().toDouble()

            val faultIrMag =
                    if (radioSingle.isChecked)
                        0.0
                    else
                        etFaultIrMag.text.toString().toDouble()

            val faultIrAngle =
                    if (radioSingle.isChecked)
                        0.0
                    else
                        etFaultIrAngle.text.toString().toDouble()

            val lineLengthText =
                    etLineLength.text.toString().trim()

            val lineLength =
                    if (lineLengthText.isEmpty()) {
                        selectedLine.lineLengthKm
                    } else {
                        lineLengthText.toDouble()
                    }

            val result =
                    if (radioSingle.isChecked) {

                        FaultLocator.calculateFaultLocation(
                                faultVsMag,
                                faultVsAngle,
                                faultIsMag,
                                faultIsAngle,
                                faultVsMag * 0.95,
                                faultVsAngle,
                                faultIsMag * 0.50,
                                faultIsAngle,
                                lineLength
                        )

                    } else {

                        FaultLocator.calculateFaultLocationFromPaper(
                                preVsMag,
                                preVsAngle,
                                preIsMag,
                                preIsAngle,
                                preVrMag,
                                preVrAngle,
                                preIrMag,
                                preIrAngle,
                                faultVsMag,
                                faultVsAngle,
                                faultIsMag,
                                faultIsAngle,
                                faultVrMag,
                                faultVrAngle,
                                faultIrMag,
                                faultIrAngle,
                                lineLength
                        )
                    }

            var ratio = result.m

            if (ratio < 0.0) {
                ratio = 0.0
            }

            if (ratio > 1.0) {
                ratio = 1.0
            }

            val faultDistanceLocal =
                    ratio * lineLength

            val faultDistanceRemote =
                    lineLength - faultDistanceLocal

            val towersLocal =
                    (faultDistanceLocal / 0.3).roundToLong()

            val towersRemote =
                    (faultDistanceRemote / 0.3).roundToLong()

            val faultLat =
                    selectedLine.fromLat +
                            ratio * (selectedLine.toLat - selectedLine.fromLat)

            val faultLon =
                    selectedLine.fromLon +
                            ratio * (selectedLine.toLon - selectedLine.fromLon)

            return LocationCalculationResult(
                    selectedLine,
                    lineLength,
                    ratio,
                    faultDistanceLocal,
                    faultDistanceRemote,
                    towersLocal,
                    towersRemote,
                    faultLat,
                    faultLon
            )
        }

        radioDouble.isChecked = true
        showReceivingEnd(true)

        radioSingle.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                showReceivingEnd(false)
            }
        }

        radioDouble.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                showReceivingEnd(true)
            }
        }

        btnShowResults.setOnClickListener {

            try {

                val result =
                        calculateLocation()
                AppDataStore.selectedLine = result.selectedLine.name
                AppDataStore.lineLengthKm = result.lineLength
                AppDataStore.faultDistanceLocalKm = result.faultDistanceLocal
                AppDataStore.faultDistanceRemoteKm = result.faultDistanceRemote
                AppDataStore.towersFromLocal = result.towersLocal
                AppDataStore.towersFromRemote = result.towersRemote

                if (radioSingle.isChecked) {

                    tvLocationResult.text = """
Selected Line :
${result.selectedLine.name}

Total Line Length :
${"%.2f".format(result.lineLength)} km

Fault Distance from Local End :
${"%.2f".format(result.faultDistanceLocal)} km

Approximate Tower Distance from Local End :
${result.towersLocal} towers

Fault Location Factor :
${"%.4f".format(result.ratio)}
""".trimIndent()

                } else {

                    tvLocationResult.text = """
Selected Line :
${result.selectedLine.name}

Total Line Length :
${"%.2f".format(result.lineLength)} km

Fault Distance from Local End :
${"%.2f".format(result.faultDistanceLocal)} km

Fault Distance from Remote End :
${"%.2f".format(result.faultDistanceRemote)} km

Approximate Tower Distance from Local End :
${result.towersLocal} towers

Approximate Tower Distance from Remote End :
${result.towersRemote} towers

Fault Location Factor :
${"%.4f".format(result.ratio)}
""".trimIndent()
                }

            } catch (e: Exception) {
                tvLocationResult.text =
                        "Please enter all required values correctly."
                e.printStackTrace()
            }
        }

        btnLocateFault.setOnClickListener {

            try {

                val result =
                        calculateLocation()
                AppDataStore.selectedLine = result.selectedLine.name
                AppDataStore.lineLengthKm = result.lineLength
                AppDataStore.faultDistanceLocalKm = result.faultDistanceLocal
                AppDataStore.faultDistanceRemoteKm = result.faultDistanceRemote
                AppDataStore.towersFromLocal = result.towersLocal
                AppDataStore.towersFromRemote = result.towersRemote
                sendFaultToBackend(
                        stationName = AppDataStore.stationName,
                        lineName = result.selectedLine.name,
                        faultType = AppDataStore.faultType,
                        faultPhase = AppDataStore.faultPhase,
                        localDistance = result.faultDistanceLocal,
                        remoteDistance = result.faultDistanceRemote,
                        localTower = result.towersLocal,
                        remoteTower = result.towersRemote
                )

                val intent = Intent(
                        this@LocationActivity,
                        MapActivity::class.java
                )

                intent.putExtra("LATITUDE", result.faultLat)
                intent.putExtra("LONGITUDE", result.faultLon)
                intent.putExtra("FAULT_NAME", result.selectedLine.name)

                startActivity(intent)

            } catch (e: Exception) {
                tvLocationResult.text =
                        "Please enter all required values correctly before locating on map."
                e.printStackTrace()
            }
        }
    }
    private fun sendFaultToBackend(
            stationName: String,
            lineName: String,
            faultType: String,
            faultPhase: String,
            localDistance: Double,
            remoteDistance: Double,
            localTower: Long,
            remoteTower: Long
    ) {

        val client = OkHttpClient()

        val json = JSONObject()

        json.put("station_name", stationName)
        json.put("line_name", lineName)
        json.put("fault_type", faultType)
        json.put("fault_phase", faultPhase)
        json.put("fault_distance_local", localDistance)
        json.put("fault_distance_remote", remoteDistance)
        json.put("tower_local", localTower)
        json.put("tower_remote", remoteTower)

        val body = json.toString().toRequestBody(
                "application/json".toMediaType()
        )

        val request = Request.Builder()
                .url(backendUrl)
                .post(body)
                .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }
}