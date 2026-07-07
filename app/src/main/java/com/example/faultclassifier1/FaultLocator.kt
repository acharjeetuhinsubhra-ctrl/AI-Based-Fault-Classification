package com.example.faultclassifier1

import kotlin.math.abs

data class FaultLocatorResult(
        val m: Double,
        val faultDistanceKm: Double
)

object FaultLocator {

    fun calculateFaultLocation(
            vsMag: Double,
            vsAngle: Double,
            isMag: Double,
            isAngle: Double,
            vrMag: Double,
            vrAngle: Double,
            irMag: Double,
            irAngle: Double,
            lineLengthKm: Double
    ): FaultLocatorResult {

        val vs = PhasorUtil.fromMagnitudeAngle(vsMag, vsAngle)
        val isCurrent = PhasorUtil.fromMagnitudeAngle(isMag, isAngle)
        val vr = PhasorUtil.fromMagnitudeAngle(vrMag, vrAngle)
        val ir = PhasorUtil.fromMagnitudeAngle(irMag, irAngle)

        val zSend =
                if (isCurrent.absValue() != 0.0) vs.divide(isCurrent).absValue() else 0.0

        val zReceive =
                if (ir.absValue() != 0.0) vr.divide(ir).absValue() else 0.0

        var m =
                if (zSend + zReceive != 0.0) zSend / (zSend + zReceive) else 0.5

        if (m < 0.0) m = 0.0
        if (m > 1.0) m = 1.0

        return FaultLocatorResult(m, m * lineLengthKm)
    }

    fun calculateFaultLocationFromPaper(
            preVsMag: Double,
            preVsAngle: Double,
            preIsMag: Double,
            preIsAngle: Double,
            preVrMag: Double,
            preVrAngle: Double,
            preIrMag: Double,
            preIrAngle: Double,
            faultVsMag: Double,
            faultVsAngle: Double,
            faultIsMag: Double,
            faultIsAngle: Double,
            faultVrMag: Double,
            faultVrAngle: Double,
            faultIrMag: Double,
            faultIrAngle: Double,
            lineLengthKm: Double
    ): FaultLocatorResult {

        val preVs = PhasorUtil.fromMagnitudeAngle(preVsMag, preVsAngle)
        val preIs = PhasorUtil.fromMagnitudeAngle(preIsMag, preIsAngle)
        val preVr = PhasorUtil.fromMagnitudeAngle(preVrMag, preVrAngle)
        val preIr = PhasorUtil.fromMagnitudeAngle(preIrMag, preIrAngle)

        val faultVs = PhasorUtil.fromMagnitudeAngle(faultVsMag, faultVsAngle)
        val faultIs = PhasorUtil.fromMagnitudeAngle(faultIsMag, faultIsAngle)
        val faultVr = PhasorUtil.fromMagnitudeAngle(faultVrMag, faultVrAngle)
        val faultIr = PhasorUtil.fromMagnitudeAngle(faultIrMag, faultIrAngle)

        try {

            val denominator =
                    preVr.multiply(preIs).subtract(preVs.multiply(preIr))

            if (denominator.absValue() == 0.0) {
                return fallbackFaultLocation(
                        faultVs,
                        faultIs,
                        faultVr,
                        faultIr,
                        lineLengthKm
                )
            }

            val a1 =
                    preVs.multiply(preIs).divide(denominator)

            val a2 =
                    preVr.multiply(preIr).scale(-1.0).divide(denominator)

            val numeratorDelta =
                    a2.subtract(a1.conjugate())

            val denominatorDelta =
                    a2.conjugate().subtract(a1)

            if (denominatorDelta.absValue() == 0.0) {
                return fallbackFaultLocation(
                        faultVs,
                        faultIs,
                        faultVr,
                        faultIr,
                        lineLengthKm
                )
            }

            val rootBase =
                    numeratorDelta.divide(denominatorDelta).sqrtC()

            val deltaCandidates = arrayListOf(
                    rootBase,
                    rootBase.scale(-1.0)
            )

            var bestDelta = deltaCandidates[0]
            var bestGammaL = ComplexNumber(0.0, 0.0)
            var bestZc = ComplexNumber(0.0, 0.0)
            var bestError = Double.MAX_VALUE

            for (delta in deltaCandidates) {

                val invDelta =
                        ComplexNumber(1.0, 0.0).divide(delta)

                val coshGammaL =
                        a1.multiply(delta).add(a2.multiply(invDelta))

                val gammaL =
                        coshGammaL.acoshC()

                val sinhGammaL =
                        gammaL.sinhC()

                if (sinhGammaL.absValue() == 0.0) {
                    continue
                }

                val zc =
                        preVr.multiply(gammaL.coshC())
                                .subtract(preVs.multiply(delta))
                                .divide(preIr.multiply(sinhGammaL))

                val estimatedVs =
                        preVr.multiply(gammaL.coshC())
                                .subtract(preIr.multiply(zc).multiply(sinhGammaL))
                                .divide(delta)

                val error =
                        estimatedVs.subtract(preVs).absValue()

                if (error < bestError) {
                    bestError = error
                    bestDelta = delta
                    bestGammaL = gammaL
                    bestZc = zc
                }
            }

            if (bestGammaL.absValue() == 0.0 || bestZc.absValue() == 0.0) {
                return fallbackFaultLocation(
                        faultVs,
                        faultIs,
                        faultVr,
                        faultIr,
                        lineLengthKm
                )
            }

            val coshGammaL = bestGammaL.coshC()
            val sinhGammaL = bestGammaL.sinhC()

            val n1 =
                    faultIs.multiply(bestZc).multiply(coshGammaL)
                            .subtract(faultVs.multiply(sinhGammaL))

            val o1 =
                    faultIr.multiply(bestZc)

            val l1 =
                    faultVs.multiply(coshGammaL)
                            .subtract(faultIs.multiply(bestZc).multiply(sinhGammaL))

            val m1 =
                    faultVr

            val qa =
                    n1.multiply(m1.conjugate())
                            .add(l1.conjugate().multiply(o1))

            val qb =
                    m1.conjugate().multiply(o1)
                            .add(m1.multiply(o1.conjugate()))
                            .add(l1.conjugate().multiply(n1))
                            .add(l1.multiply(n1.conjugate()))

            val qc =
                    l1.conjugate().multiply(o1)
                            .add(l1.multiply(o1.conjugate()))

            val rootsDelta2 =
                    solveQuadraticComplex(qa, qb, qc)

            var bestM = -1.0
            var bestQuality = Double.MAX_VALUE

            for (delta2 in rootsDelta2) {

                val top =
                        n1.multiply(delta2).add(o1)

                val bottom =
                        l1.multiply(delta2).add(m1)

                if (bottom.absValue() == 0.0) {
                    continue
                }

                val ratio =
                        top.divide(bottom)

                val mComplex =
                        ratio.atanhC().divide(bestGammaL)

                val mReal =
                        mComplex.real

                val quality =
                        abs(delta2.absValue() - 1.0) + abs(mComplex.imag)

                if (mReal >= 0.0 && mReal <= 1.0 && quality < bestQuality) {
                    bestQuality = quality
                    bestM = mReal
                }
            }

            if (bestM < 0.0 || bestM > 1.0) {
                val top =
                        n1.multiply(bestDelta).add(o1)

                val bottom =
                        l1.multiply(bestDelta).add(m1)

                if (bottom.absValue() != 0.0) {
                    val ratio =
                            top.divide(bottom)

                    val mComplex =
                            ratio.atanhC().divide(bestGammaL)

                    bestM = mComplex.real
                }
            }

            if (bestM < 0.0) bestM = 0.0
            if (bestM > 1.0) bestM = 1.0

            return FaultLocatorResult(
                    bestM,
                    bestM * lineLengthKm
            )

        } catch (e: Exception) {
            e.printStackTrace()

            return fallbackFaultLocation(
                    faultVs,
                    faultIs,
                    faultVr,
                    faultIr,
                    lineLengthKm
            )
        }
    }

    private fun fallbackFaultLocation(
            faultVs: ComplexNumber,
            faultIs: ComplexNumber,
            faultVr: ComplexNumber,
            faultIr: ComplexNumber,
            lineLengthKm: Double
    ): FaultLocatorResult {

        val zSend =
                if (faultIs.absValue() != 0.0) faultVs.divide(faultIs).absValue() else 0.0

        val zReceive =
                if (faultIr.absValue() != 0.0) faultVr.divide(faultIr).absValue() else 0.0

        var m =
                if (zSend + zReceive != 0.0) zSend / (zSend + zReceive) else 0.5

        if (m < 0.0) m = 0.0
        if (m > 1.0) m = 1.0

        return FaultLocatorResult(m, m * lineLengthKm)
    }

    private fun solveQuadraticComplex(
            a: ComplexNumber,
            b: ComplexNumber,
            c: ComplexNumber
    ): ArrayList<ComplexNumber> {

        val roots = ArrayList<ComplexNumber>()

        if (a.absValue() == 0.0) {
            if (b.absValue() != 0.0) {
                roots.add(c.scale(-1.0).divide(b))
            }
            return roots
        }

        val fourAC =
                a.multiply(c).scale(4.0)

        val discriminant =
                b.multiply(b).subtract(fourAC)

        val sqrtD =
                discriminant.sqrtC()

        val twoA =
                a.scale(2.0)

        val minusB =
                b.scale(-1.0)

        roots.add(minusB.add(sqrtD).divide(twoA))
        roots.add(minusB.subtract(sqrtD).divide(twoA))

        return roots
    }
}