package com.example.faultclassifier1

data class LineInfo(
    val name: String,
    val lineLengthKm: Double,
    val fromLat: Double,
    val fromLon: Double,
    val toLat: Double,
    val toLon: Double
)

object LineDatabase {

    fun getLines(): ArrayList<LineInfo> {

        val lines = ArrayList<LineInfo>()

        lines.add(LineInfo("ALIPUR 2 → SALAKTI2", 100.56, 26.4919, 89.5271, 26.5150, 90.3650))
        lines.add(LineInfo("BINAGURI4 → BONGAIGAON4", 218.0, 26.7200, 89.0350, 26.4770, 90.5580))
        lines.add(LineInfo("ALIPUR 4 → BONGAIGAON4", 105.69, 26.4919, 89.5271, 26.4770, 90.5580))
        lines.add(LineInfo("ZIRO-PG1 → DAPORIJO1", 87.2, 27.5440, 93.8190, 27.9950, 94.2230))
        lines.add(LineInfo("ZIRO-PG1 → RNGNDI1", 44.5, 27.5440, 93.8190, 27.3430, 93.7590))
        lines.add(LineInfo("DAPORIJO1 → ALONG1", 81.7, 27.9950, 94.2230, 28.1690, 94.8000))
        lines.add(LineInfo("ALONG1 → PASIGHAT1", 77.0, 28.1690, 94.8000, 28.0660, 95.3260))
        lines.add(LineInfo("ROING1 → TEZU1", 24.0, 28.1440, 95.8430, 27.9270, 96.1660))
        lines.add(LineInfo("TEZU1 → NAMSAI1", 31.0, 27.9270, 96.1660, 27.6690, 95.8640))
        lines.add(LineInfo("NAHARLAGUN1 → ITANAGAR1", 20.0, 27.1040, 93.6950, 27.0840, 93.6050))

        return lines
    }
}