package com.example.faultclassifier1

import android.content.Context
import android.net.Uri

data class ComtradeData(
    val ia: MutableList<Double>,
    val ib: MutableList<Double>,
    val ic: MutableList<Double>,
    val inCurrent: MutableList<Double>,
    val va: MutableList<Double>,
    val vb: MutableList<Double>,
    val vc: MutableList<Double>,
    val time: MutableList<Double>,
    val stationName: String
)

data class ChannelInfo(
    val index: Int,
    val name: String,
    val a: Double,
    val b: Double
)

object ComtradeReader {

    fun readComtrade(
        context: Context,
        cfgUri: Uri,
        datUri: Uri
    ): ComtradeData {

        val cfgLines = context.contentResolver
            .openInputStream(cfgUri)
            ?.bufferedReader()
            ?.readLines()
            ?: emptyList()

        val firstLine =
            if (cfgLines.isNotEmpty())
                cfgLines[0]
            else
                ""

        val stationName =
            SubstationDatabase.extractStationFromCfgFirstLine(firstLine)

        val analogCount =
            cfgLines[1].split(",")[1].replace("A", "").trim().toInt()

        val channels = ArrayList<ChannelInfo>()

        var i = 2
        while (i < 2 + analogCount) {

            val p = cfgLines[i].split(",")

            channels.add(
                ChannelInfo(
                    p[0].trim().toInt(),
                    p[1].trim().toUpperCase(),
                    p[5].trim().toDouble(),
                    p[6].trim().toDouble()
                )
            )

            i++
        }

        val chIA = findChannel(channels, "IA", "IL1")
        val chIB = findChannel(channels, "IB", "IL2")
        val chIC = findChannel(channels, "IC", "IL3")
        val chIN = findChannel(channels, "IN")

        val chVA = findChannel(channels, "VA", "UL1", "VL1")
        val chVB = findChannel(channels, "VB", "UL2", "VL2")
        val chVC = findChannel(channels, "VC", "UL3", "VL3")

        val ia = mutableListOf<Double>()
        val ib = mutableListOf<Double>()
        val ic = mutableListOf<Double>()
        val inc = mutableListOf<Double>()

        val va = mutableListOf<Double>()
        val vb = mutableListOf<Double>()
        val vc = mutableListOf<Double>()

        val time = mutableListOf<Double>()

        val datLines = context.contentResolver
            .openInputStream(datUri)
            ?.bufferedReader()
            ?.readLines()
            ?: emptyList()

        for (line in datLines) {

            val p = line.split(",")

            if (p.size < 5) {
                continue
            }

            try {

                val tRaw = p[1].trim().toDouble()

                val t =
                    if (tRaw > 10.0)
                        tRaw / 1000000.0
                    else
                        tRaw

                time.add(t)

                ia.add(readValue(p, chIA))
                ib.add(readValue(p, chIB))
                ic.add(readValue(p, chIC))
                inc.add(readValue(p, chIN))

                va.add(readValue(p, chVA))
                vb.add(readValue(p, chVB))
                vc.add(readValue(p, chVC))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return ComtradeData(
            ia,
            ib,
            ic,
            inc,
            va,
            vb,
            vc,
            time,
            stationName
        )
    }

    private fun findChannel(
        channels: ArrayList<ChannelInfo>,
        key1: String,
        key2: String = "",
        key3: String = ""
    ): ChannelInfo? {

        var i = 0

        while (i < channels.size) {

            val name = channels[i].name

            if (name == key1 || name.contains(key1)) {
                return channels[i]
            }

            if (key2 != "" && (name == key2 || name.contains(key2))) {
                return channels[i]
            }

            if (key3 != "" && (name == key3 || name.contains(key3))) {
                return channels[i]
            }

            i++
        }

        return null
    }

    private fun readValue(
        p: List<String>,
        ch: ChannelInfo?
    ): Double {

        if (ch == null) {
            return 0.0
        }

        val col = 2 + (ch.index - 1)

        if (col >= p.size) {
            return 0.0
        }

        val raw = p[col].trim().toDouble()

        return ch.a * raw + ch.b
    }
}