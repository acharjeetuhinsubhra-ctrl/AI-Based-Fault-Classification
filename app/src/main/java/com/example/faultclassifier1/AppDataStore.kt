package com.example.faultclassifier1

object AppDataStore {

    var stationName: String = "Not available"
    var faultType: String = "Not available"
    var faultPhase: String = "Not available"
    var faultDescription: String = "Not available"

    var selectedLine: String = "Not available"
    var lineLengthKm: Double = 0.0
    var faultDistanceLocalKm: Double = 0.0
    var faultDistanceRemoteKm: Double = 0.0
    var towersFromLocal: Long = 0
    var towersFromRemote: Long = 0

    fun getProjectContext(): String {
        return """
Latest Classification Result:
Station: $stationName
Fault Type: $faultType
Fault Phase: $faultPhase
Description: $faultDescription

Latest Fault Location Result:
Selected Line: $selectedLine
Line Length: $lineLengthKm km
Fault Distance from Local End: $faultDistanceLocalKm km
Fault Distance from Remote End: $faultDistanceRemoteKm km
Towers from Local End: $towersFromLocal
Towers from Remote End: $towersFromRemote
""".trimIndent()
    }
}


