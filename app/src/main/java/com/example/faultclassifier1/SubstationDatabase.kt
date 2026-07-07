package com.example.faultclassifier1

data class SubstationPoint(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object SubstationDatabase {

    fun extractStationFromCfgFirstLine(firstLine: String): String {

        var text = firstLine.toUpperCase()

        val commaIndex = text.indexOf(",")

        if (commaIndex >= 0) {
            text = text.substring(0, commaIndex)
        }

        text = text.replace("400/132KV", "")
        text = text.replace("400/220KV", "")
        text = text.replace("220/132KV", "")
        text = text.replace("132 KV", "")
        text = text.replace("400KV", "")
        text = text.replace("220KV", "")
        text = text.replace("132KV", "")
        text = text.replace("KV", "")
        text = text.replace("/", " ")
        text = text.replace("_", " ")
        text = text.replace("-", " ")

        text = text.trim()

        while (text.contains("  ")) {
            text = text.replace("  ", " ")
        }

        if (text.length == 0) {
            return "UNKNOWN"
        }

        return text
    }

    fun findSubstation(nameInput: String): SubstationPoint? {

        val name = nameInput.toUpperCase()

        if (name.contains("SILCHAR")) {
            return SubstationPoint("SILCHAR", 24.8330, 92.7780)
        }

        if (name.contains("IMPHAL")) {
            return SubstationPoint("IMPHAL", 24.8170, 93.9368)
        }

        if (name.contains("AIZAWL")) {
            return SubstationPoint("AIZAWL SS", 23.7271, 92.7176)
        }

        if (name.contains("AGRA")) {
            return SubstationPoint("PGCIL NE-AGRA", 27.1767, 78.0081)
        }

        if (name.contains("PGCIL")) {
            return SubstationPoint("PGCIL", 27.1767, 78.0081)
        }

        if (name.contains("BADARPUR")) {
            return SubstationPoint("BADARPUR", 24.8680, 92.5960)
        }

        if (name.contains("AGARTALA")) {
            return SubstationPoint("AGARTALA", 23.8315, 91.2868)
        }

        if (name.contains("KUMARGHAT")) {
            return SubstationPoint("KUMARGHAT", 24.1580, 92.0300)
        }

        if (name.contains("TINSUKHIA")) {
            return SubstationPoint("TINSUKHIA", 27.4890, 95.3590)
        }

        if (name.contains("KATHALGURI")) {
            return SubstationPoint("KATHALGURI", 27.2500, 95.1600)
        }

        return null
    }
}