package com.example.faultclassifier1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ClassificationActivity : AppCompatActivity() {

    private var cfgUri: Uri? = null
    private var datUri: Uri? = null

    private lateinit var tvCfgFile: TextView
    private lateinit var tvDatFile: TextView
    private lateinit var tvResult: TextView

    private var lastFaultName: String = "Fault Location"
    private var detectedLat: Double = 22.5726
    private var detectedLon: Double = 88.3639

    private val backendUrl = "http://10.145.93.119:5000/add_fault"

    private val cfgPicker =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    cfgUri = uri
                    val fileName = getFileName(uri)
                    tvCfgFile.text = "CFG : $fileName Selected"
                }
            }

    private val datPicker =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    datUri = uri
                    val fileName = getFileName(uri)
                    tvDatFile.text = "DAT : $fileName Selected"
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classification)

        val btnSelectCfg = findViewById<Button>(R.id.btnSelectCfg)
        val btnSelectDat = findViewById<Button>(R.id.btnSelectDat)
        val btnClassify = findViewById<Button>(R.id.btnClassify)
        val btnOpenMap = findViewById<Button>(R.id.btnOpenMap)
        val btnAiAssistant = findViewById<Button>(R.id.btnAiAssistant)

        tvCfgFile = findViewById(R.id.tvCfgFile)
        tvDatFile = findViewById(R.id.tvDatFile)
        tvResult = findViewById(R.id.tvResult)

        btnAiAssistant.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }

        btnSelectCfg.setOnClickListener {
            cfgPicker.launch(arrayOf("*/*"))
        }

        btnSelectDat.setOnClickListener {
            datPicker.launch(arrayOf("*/*"))
        }

        btnClassify.setOnClickListener {

            if (cfgUri == null || datUri == null) {
                tvResult.text = "Please select both CFG and DAT files."
                return@setOnClickListener
            }

            try {

                val data =
                        ComtradeReader.readComtrade(
                                this,
                                cfgUri!!,
                                datUri!!
                        )

                val point =
                        SubstationDatabase.findSubstation(data.stationName)

                val locationText =
                        if (point != null) {
                            detectedLat = point.latitude
                            detectedLon = point.longitude
                            "Detected Station Coordinate : ${point.latitude}, ${point.longitude}"
                        } else {
                            "Detected Station Coordinate : Not found in local database"
                        }

                val features =
                        FeatureExtractor.extract(data)

                val result =
                        FaultClassifier.classify(features)

                lastFaultName = result.faultType

                val description = when (result.faultType) {

                    "LIGHTNING FAULT" ->
                        "Transient high-energy fault caused by lightning stroke, producing a steep current surge and rapid voltage collapse."

                    "INSULATION FLASHOVER" ->
                        "Flashover across insulator surface caused by insulation breakdown, resulting in an arc between conductor and tower."

                    "JHOOM FAULT" ->
                        "Two-phase forest-fire fault caused by ionized hot air between conductors, generally without ground involvement."

                    "VEGETATION FAULT" ->
                        "Fault initiated by tree branches or vegetation contacting the transmission line, producing a relatively sustained fault current."

                    "SOLID JUMPER FAULT" ->
                        "Direct metallic short-circuit caused by jumper or conductor contact, producing very low fault impedance."

                    else ->
                        "Unknown fault."
                }

                AppDataStore.stationName = data.stationName
                AppDataStore.faultType = result.faultType
                AppDataStore.faultPhase = result.faultPhase
                AppDataStore.faultDescription = description

                sendFaultToBackend(
                        stationName = data.stationName,
                        lineName = "Classification Module",
                        faultType = result.faultType,
                        faultPhase = result.faultPhase,
                        faultDistanceLocal = 0.0,
                        faultDistanceRemote = 0.0,
                        towerLocal = 0,
                        towerRemote = 0
                )

                tvResult.text = """
DETECTED STATION : ${data.stationName}
$locationText

FAULT TYPE : ${result.faultType}

DESCRIPTION :
$description

FAULT PHASE : ${result.faultPhase}

DATABASE STATUS :
Result sent to PostgreSQL / Grafana backend.
""".trimIndent()

            } catch (e: Exception) {
                tvResult.text = "Error : ${e.message}"
                e.printStackTrace()
            }
        }

        btnOpenMap.setOnClickListener {

            val intent =
                    Intent(this, MapActivity::class.java)

            intent.putExtra("LATITUDE", detectedLat)
            intent.putExtra("LONGITUDE", detectedLon)
            intent.putExtra("FAULT_NAME", lastFaultName)

            startActivity(intent)
        }
    }

    private fun sendFaultToBackend(
            stationName: String,
            lineName: String,
            faultType: String,
            faultPhase: String,
            faultDistanceLocal: Double,
            faultDistanceRemote: Double,
            towerLocal: Int,
            towerRemote: Int
    ) {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("station_name", stationName)
        json.put("line_name", lineName)
        json.put("fault_type", faultType)
        json.put("fault_phase", faultPhase)
        json.put("fault_distance_local", faultDistanceLocal)
        json.put("fault_distance_remote", faultDistanceRemote)
        json.put("tower_local", towerLocal)
        json.put("tower_remote", towerRemote)

        val mediaType = "application/json".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request =
                Request.Builder()
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

    private fun getFileName(uri: Uri): String {

        var fileName = "File"

        val cursor =
                contentResolver.query(uri, null, null, null, null)

        if (cursor != null) {

            val nameIndex =
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }

            cursor.close()
        }

        return fileName
    }
}