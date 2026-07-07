package com.example.faultclassifier1

data class ClassificationResult(
    val faultType: String,
    val faultPhase: String,
    val za: Double,
    val zb: Double,
    val zc: Double,
    val s: Double,
    val signature: String,
    val matchScore: Int
)

object FaultClassifier {

    fun classify(features: FeatureResult): ClassificationResult {

        val za = if (features.iaRms != 0.0) features.vaRms / features.iaRms else 999999.0
        val zb = if (features.ibRms != 0.0) features.vbRms / features.ibRms else 999999.0
        val zc = if (features.icRms != 0.0) features.vcRms / features.icRms else 999999.0

        val zmin = minOf(za, zb, zc)

        val phase = if (zmin == za) {
            "PHASE A"
        } else if (zmin == zb) {
            "PHASE B"
        } else {
            "PHASE C"
        }

        val detected = features.signature

        val lightning = "100010111001"
        val flashover = "010011111111"
        val jhoom = "101000001011"
        val solid = "000110001000"
        val vegetation = "010011000011"

        val lightningScore = countMatch(detected, lightning)
        val flashoverScore = countMatch(detected, flashover)
        val jhoomScore = countMatch(detected, jhoom)
        val solidScore = countMatch(detected, solid)
        val vegetationScore = countMatch(detected, vegetation)

        var bestFault = "UNKNOWN FAULT"
        var bestScore = 0

        // 1. JHOOM: two phase involvement and no ground
        if (features.b1 == 1 && features.b3 == 1 && features.b5 == 0) {
            bestFault = "JHOOM FAULT"
            bestScore = jhoomScore
        }

        // 2. SOLID: sustained steady current is the strongest identity
        else if (
            features.b4 == 1 ||
            (
                    features.b1 == 0 &&
                            features.b2 == 0 &&
                            features.b3 == 0 &&
                            features.b5 == 1 &&
                            features.b9 == 1 &&
                            features.b11 == 0 &&
                            features.b12 == 0
                    )
        ) {
            bestFault = "SOLID JUMPER FAULT"
            bestScore = solidScore
        }

        // 3. LIGHTNING: sharp impulse but no restrike and fluctuating transient
        else if (
            features.b1 == 1 &&
            features.b2 == 0 &&
            features.b3 == 0 &&
            features.b4 == 0 &&
            features.b5 == 1 &&
            features.b12 == 1
        ) {
            bestFault = "LIGHTNING FAULT"
            bestScore = lightningScore
        }

        // 4. VEGETATION: restrike + slow growth + broadband/fluctuation,
        // but weak voltage collapse / weak harmonic cluster compared to flashover
        else if (
            features.b1 == 0 &&
            features.b2 == 1 &&
            features.b3 == 0 &&
            features.b4 == 0 &&
            features.b5 == 1 &&
            features.b6 == 1 &&
            features.b11 == 1 &&
            features.b12 == 1 &&
            features.b8 == 0
        ) {
            bestFault = "VEGETATION FAULT"
            bestScore = vegetationScore
        }

        // 5. INSULATION FLASHOVER: restrike + strong collapse/harmonic features
        else if (
            features.b2 == 1 &&
            features.b5 == 1 &&
            features.b6 == 1 &&
            features.b9 == 1 &&
            features.b10 == 1 &&
            features.b11 == 1 &&
            features.b12 == 1
        ) {
            bestFault = "INSULATION FLASHOVER"
            bestScore = flashoverScore
        }

        // 6. Fallback: nearest 12-bit match
        else {
            bestFault = "LIGHTNING FAULT"
            bestScore = lightningScore

            if (flashoverScore > bestScore) {
                bestScore = flashoverScore
                bestFault = "INSULATION FLASHOVER"
            }

            if (jhoomScore > bestScore) {
                bestScore = jhoomScore
                bestFault = "JHOOM FAULT"
            }

            if (solidScore > bestScore) {
                bestScore = solidScore
                bestFault = "SOLID JUMPER FAULT"
            }

            if (vegetationScore >= bestScore) {
                bestScore = vegetationScore
                bestFault = "VEGETATION FAULT"
            }
        }

        return ClassificationResult(
            faultType = bestFault,
            faultPhase = phase,
            za = za,
            zb = zb,
            zc = zc,
            s = features.s,
            signature = detected,
            matchScore = bestScore
        )
    }

    private fun countMatch(a: String, b: String): Int {
        var count = 0
        var i = 0

        while (i < a.length && i < b.length) {
            if (a[i] == b[i]) {
                count++
            }
            i++
        }

        return count
    }
}