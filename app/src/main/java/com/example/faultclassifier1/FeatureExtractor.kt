package com.example.faultclassifier1

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

data class FeatureResult(
    val iaRms: Double,
    val ibRms: Double,
    val icRms: Double,

    val vaRms: Double,
    val vbRms: Double,
    val vcRms: Double,

    val maxDidt: Double,
    val maxDvdt: Double,

    val prefaultRms: Double,
    val s: Double,

    val faultIndex: Int,

    val b1: Int,
    val b2: Int,
    val b3: Int,
    val b4: Int,
    val b5: Int,
    val b6: Int,
    val b7: Int,
    val b8: Int,
    val b9: Int,
    val b10: Int,
    val b11: Int,
    val b12: Int,

    val signature: String
)

object FeatureExtractor {

    fun extract(data: ComtradeData): FeatureResult {

        val ia = data.ia
        val ib = data.ib
        val ic = data.ic
        val va = data.va
        val vb = data.vb
        val vc = data.vc
        val time = data.time

        val n = ia.size

        if (n < 20) {
            return emptyResult()
        }

        val fs = if (time.size > 2) {
            val dt = time[1] - time[0]
            if (dt != 0.0) 1.0 / dt else 1200.0
        } else {
            1200.0
        }

        val combined = DoubleArray(n)

        var i = 0
        while (i < n) {
            combined[i] = sqrt(
                ia[i] * ia[i] +
                        ib[i] * ib[i] +
                        ic[i] * ic[i]
            )
            i++
        }

        val didtCombined = derivativeAbs(combined, time)
        val maxDidt = arrayMax(didtCombined)

        val dvdtA = derivativeAbs(toArray(va), time)
        val maxDvdt = arrayMax(dvdtA)

        var faultIndex = 1
        val threshold = 0.20 * maxDidt

        i = 1
        while (i < didtCombined.size) {
            if (didtCombined[i] > threshold) {
                faultIndex = i
                break
            }
            i++
        }

        if (faultIndex < 20) {
            faultIndex = 20
        }

        if (faultIndex >= n) {
            faultIndex = n / 5
        }

        val iaRms = rmsList(ia, 0, n)
        val ibRms = rmsList(ib, 0, n)
        val icRms = rmsList(ic, 0, n)

        val vaRms = rmsList(va, 0, n)
        val vbRms = rmsList(vb, 0, n)
        val vcRms = rmsList(vc, 0, n)

        val prefaultRms = rmsList(ia, 0, faultIndex)

        val sValue = if (prefaultRms != 0.0) {
            maxDidt / prefaultRms
        } else {
            0.0
        }

        val peakA = maxAbsList(ia, faultIndex, n)
        val peakB = maxAbsList(ib, faultIndex, n)
        val peakC = maxAbsList(ic, faultIndex, n)

        var faultyPhase = "A"
        var faultCurrent = ia
        var faultVoltage = va

        if (peakB >= peakA && peakB >= peakC) {
            faultyPhase = "B"
            faultCurrent = ib
            faultVoltage = vb
        } else if (peakC >= peakA && peakC >= peakB) {
            faultyPhase = "C"
            faultCurrent = ic
            faultVoltage = vc
        }

        val activeStart = faultIndex
        var activeEnd = faultIndex + (0.25 * fs).toInt()

        if (activeEnd > n) {
            activeEnd = n
        }

        if (activeEnd <= activeStart + 10) {
            activeEnd = n
        }

        val absPeaks = halfCyclePeaks(faultCurrent, activeStart, activeEnd, fs)

        val b1 = bitSingleSharpImpulse(absPeaks)
        val b2 = bitMultipleRestrikes(absPeaks)

        val iaFaultRms = rmsList(ia, activeStart, activeEnd)
        val ibFaultRms = rmsList(ib, activeStart, activeEnd)
        val icFaultRms = rmsList(ic, activeStart, activeEnd)

        var sumSq = 0.0
        i = activeStart
        while (i < activeEnd) {
            val sum = ia[i] + ib[i] + ic[i]
            sumSq += sum * sum
            i++
        }

        val isumRms = sqrt(sumSq / (activeEnd - activeStart))

        val iref = maxOf(iaFaultRms, ibFaultRms, icFaultRms)

        val residualRatio = if (iref != 0.0) {
            isumRms / iref
        } else {
            999.0
        }

        val b3 = if (residualRatio < 0.20) 1 else 0

        val b4 = bitSustainedSteady(absPeaks)

        val b5 = if (residualRatio >= 0.20) 1 else 0

        val b6 = bitSlowGrowth(absPeaks)

        val meanCurrent = meanAbsSigned(faultCurrent, activeStart, activeEnd)
        val b7 = if (abs(meanCurrent) > 100.0) 1 else 0

        val b8 = bitVoltageCollapse(faultVoltage, activeStart, activeEnd, fs, b4)

        val fftResult = fftDominantBits(faultCurrent, faultIndex, n, fs, b2, b8)

        val b9 = fftResult[0]
        val b10 = fftResult[1]
        val b11 = fftResult[2]

        val b12 = bitCurrentFluctuation(absPeaks)

        val signature =
            "" + b1 + b2 + b3 + b4 + b5 + b6 +
                    b7 + b8 + b9 + b10 + b11 + b12

        return FeatureResult(
            iaRms,
            ibRms,
            icRms,
            vaRms,
            vbRms,
            vcRms,
            maxDidt,
            maxDvdt,
            prefaultRms,
            sValue,
            faultIndex,
            b1,
            b2,
            b3,
            b4,
            b5,
            b6,
            b7,
            b8,
            b9,
            b10,
            b11,
            b12,
            signature
        )
    }

    private fun emptyResult(): FeatureResult {
        return FeatureResult(
            0.0, 0.0, 0.0,
            0.0, 0.0, 0.0,
            0.0, 0.0,
            0.0, 0.0,
            0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            "000000000000"
        )
    }

    private fun toArray(x: List<Double>): DoubleArray {
        val a = DoubleArray(x.size)
        var i = 0
        while (i < x.size) {
            a[i] = x[i]
            i++
        }
        return a
    }

    private fun derivativeAbs(x: DoubleArray, t: List<Double>): DoubleArray {
        val d = DoubleArray(x.size)

        var i = 1
        while (i < x.size) {
            val dt = t[i] - t[i - 1]

            d[i] = if (dt != 0.0) {
                abs((x[i] - x[i - 1]) / dt)
            } else {
                0.0
            }

            i++
        }

        return d
    }

    private fun rmsList(x: List<Double>, start: Int, end: Int): Double {
        var s = start
        var e = end

        if (s < 0) s = 0
        if (e > x.size) e = x.size
        if (e <= s) return 0.0

        var sum = 0.0
        var i = s

        while (i < e) {
            sum += x[i] * x[i]
            i++
        }

        return sqrt(sum / (e - s))
    }

    private fun maxAbsList(x: List<Double>, start: Int, end: Int): Double {
        var maxVal = 0.0
        var i = start

        while (i < end && i < x.size) {
            val v = abs(x[i])
            if (v > maxVal) maxVal = v
            i++
        }

        return maxVal
    }

    private fun arrayMax(x: DoubleArray): Double {
        var m = 0.0
        var i = 0

        while (i < x.size) {
            if (x[i] > m) m = x[i]
            i++
        }

        return m
    }

    private fun meanAbsSigned(x: List<Double>, start: Int, end: Int): Double {
        var s = start
        var e = end

        if (s < 0) s = 0
        if (e > x.size) e = x.size
        if (e <= s) return 0.0

        var sum = 0.0
        var i = s

        while (i < e) {
            sum += x[i]
            i++
        }

        return sum / (e - s)
    }

    private fun halfCyclePeaks(
        signal: List<Double>,
        start: Int,
        end: Int,
        fs: Double
    ): DoubleArray {

        val halfCycle = maxOf(3, (fs / 100.0).toInt())

        val temp = ArrayList<Double>()

        var pos = start

        while (pos + halfCycle < end && pos + halfCycle < signal.size) {

            var localMax = 0.0
            var j = pos

            while (j < pos + halfCycle) {
                val v = abs(signal[j])
                if (v > localMax) localMax = v
                j++
            }

            temp.add(localMax)

            pos += halfCycle
        }

        if (temp.size == 0) {
            return DoubleArray(0)
        }

        var globalMax = 0.0
        var i = 0

        while (i < temp.size) {
            if (temp[i] > globalMax) globalMax = temp[i]
            i++
        }

        val kept = ArrayList<Double>()

        i = 0
        while (i < temp.size) {
            if (temp[i] >= 0.005 * globalMax) {
                kept.add(temp[i])
            }
            i++
        }

        val out = DoubleArray(kept.size)

        i = 0
        while (i < kept.size) {
            out[i] = kept[i]
            i++
        }

        return out
    }

    private fun bitSingleSharpImpulse(peaks: DoubleArray): Int {
        if (peaks.size < 4) return 0

        var maxIndex = 0
        var maxVal = peaks[0]
        var minVal = peaks[0]
        var sum = 0.0

        var i = 0
        while (i < peaks.size) {
            if (peaks[i] > maxVal) {
                maxVal = peaks[i]
                maxIndex = i
            }
            if (peaks[i] < minVal) {
                minVal = peaks[i]
            }
            sum += peaks[i]
            i++
        }

        val mean = sum / peaks.size
        val variation = (maxVal - minVal) / (mean + 1e-12)

        var laterMax = 0.0
        i = 2
        while (i < peaks.size) {
            if (peaks[i] > laterMax) laterMax = peaks[i]
            i++
        }

        val firstTwoMax = maxOf(peaks[0], peaks[1])

        return if (maxIndex <= 1 && laterMax <= 1.05 * firstTwoMax && variation > 0.20) {
            1
        } else {
            0
        }
    }

    private fun bitMultipleRestrikes(peaks: DoubleArray): Int {
        if (peaks.size < 4) return 0

        val initial = maxOf(peaks[0], peaks[1])

        var later = 0.0
        var i = 2

        while (i < peaks.size) {
            if (peaks[i] > later) later = peaks[i]
            i++
        }

        return if (later > 1.05 * initial) 1 else 0
    }

    private fun bitSlowGrowth(peaks: DoubleArray): Int {
        if (peaks.size < 4) return 0

        val initial = maxOf(peaks[0], peaks[1])

        var later = 0.0
        var i = 2

        while (i < peaks.size) {
            if (peaks[i] > later) later = peaks[i]
            i++
        }

        return if (later > 1.15 * initial) 1 else 0
    }

    private fun bitSustainedSteady(peaks: DoubleArray): Int {
        if (peaks.size < 4) return 0

        var maxVal = peaks[0]
        var minVal = peaks[0]
        var sum = 0.0

        var i = 0
        while (i < peaks.size) {
            if (peaks[i] > maxVal) maxVal = peaks[i]
            if (peaks[i] < minVal) minVal = peaks[i]
            sum += peaks[i]
            i++
        }

        val mean = sum / peaks.size
        val variation = (maxVal - minVal) / (mean + 1e-12)

        return if (variation <= 0.15) 1 else 0
    }

    private fun bitCurrentFluctuation(peaks: DoubleArray): Int {
        if (peaks.size < 4) return 0

        var maxVal = peaks[0]
        var minVal = peaks[0]
        var sum = 0.0

        var i = 0
        while (i < peaks.size) {
            if (peaks[i] > maxVal) maxVal = peaks[i]
            if (peaks[i] < minVal) minVal = peaks[i]
            sum += peaks[i]
            i++
        }

        val mean = sum / peaks.size
        val variation = (maxVal - minVal) / (mean + 1e-12)

        return if (variation > 0.20) 1 else 0
    }

    private fun bitVoltageCollapse(
        voltage: List<Double>,
        start: Int,
        end: Int,
        fs: Double,
        b4: Int
    ): Int {

        val peaks = halfCyclePeaks(voltage, start, end, fs)

        if (peaks.size < 4) return 0

        val first = peaks[0]

        var sum = 0.0
        var count = 0
        var i = 1

        while (i < peaks.size && i < 5) {
            sum += peaks[i]
            count++
            i++
        }

        if (count == 0) return 0

        val nextAvg = sum / count
        val dropRatio = (first - nextAvg) / (first + 1e-12)

        return if (dropRatio >= 0.25 && b4 == 0) 1 else 0
    }

    private fun fftDominantBits(
        signal: List<Double>,
        start: Int,
        n: Int,
        fs: Double,
        b2: Int,
        b8: Int
    ): IntArray {

        val result = IntArray(3)
        result[0] = 0
        result[1] = 0
        result[2] = 0

        var end = start + (0.25 * fs).toInt()

        if (end > n) end = n

        val len = end - start

        if (len < 16) {
            return result
        }

        val real = DoubleArray(len)

        var mean = 0.0
        var i = 0

        while (i < len) {
            mean += signal[start + i]
            i++
        }

        mean /= len

        i = 0
        while (i < len) {
            val window = 0.5 - 0.5 * cos(2.0 * PI * i / (len - 1))
            real[i] = (signal[start + i] - mean) * window
            i++
        }

        val maxFreq = fs / 2.0
        val mags = ArrayList<Double>()
        val freqs = ArrayList<Double>()

        var k = 1
        while (k < len / 2) {

            val freq = k * fs / len

            var re = 0.0
            var im = 0.0

            var m = 0
            while (m < len) {
                val angle = 2.0 * PI * k * m / len
                re += real[m] * cos(angle)
                im -= real[m] * kotlin.math.sin(angle)
                m++
            }

            val mag = sqrt(re * re + im * im)

            freqs.add(freq)
            mags.add(mag)

            k++
        }

        var domFreq = 0.0
        var domMag = 0.0

        i = 0
        while (i < freqs.size) {
            if (freqs[i] >= 40.0 && mags[i] > domMag) {
                domMag = mags[i]
                domFreq = freqs[i]
            }
            i++
        }

        if (domFreq >= 45.0 && domFreq <= 55.0) {
            result[0] = 1
        }

        var clusterCount = 0

        i = 0
        while (i < freqs.size) {
            if (freqs[i] >= 40.0 && freqs[i] <= 60.0 && mags[i] > 0.30 * domMag) {
                clusterCount++
            }
            i++
        }

        if (clusterCount >= 4 && b2 == 1) {
            result[1] = 1
        }

        var totalPower = 0.0
        var highPower = 0.0

        i = 0
        while (i < freqs.size) {
            val p = mags[i] * mags[i]
            totalPower += p

            if (freqs[i] >= 150.0 && freqs[i] <= maxFreq) {
                highPower += p
            }

            i++
        }

        val highRatio = if (totalPower != 0.0) highPower / totalPower else 0.0

        if (highRatio >= 0.005 || result[1] == 1) {
            result[2] = 1
        }

        if (result[2] == 1 && result[0] == 1 && b2 == 1 && b8 == 0) {
            if (highRatio > 0.10) {
                result[0] = 0
            }
        }

        return result
    }
}