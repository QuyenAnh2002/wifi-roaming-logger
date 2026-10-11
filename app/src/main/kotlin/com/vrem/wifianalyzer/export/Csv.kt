/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software:  you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.vrem.wifianalyzer.export

import com.vrem.wifianalyzer.wifi.model.calculateDistance
import com.vrem.wifianalyzer.wifi.model.calculateDistanceLogDistance
import java.util.Locale

private const val BYTE_ORDER_MARK = 0xFEFF
private const val CSV_SEPARATOR = ","
private const val DISTANCE_FORMAT = "%.1f"

/** [android.net.wifi.WifiInfo.INVALID_RSSI]: the framework has no RSSI measurement yet. */
const val INVALID_RSSI = -127

val CSV_BOM: String = Char(BYTE_ORDER_MARK).toString()

const val CSV_LINE_END = "\r\n"

fun csvEscape(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

fun csvLine(fields: List<String>): String = fields.joinToString(CSV_SEPARATOR, transform = ::csvEscape) + CSV_LINE_END

fun Number?.csvFormat(pattern: String): String = this?.let { pattern.format(Locale.US, it) }.orEmpty()

val LOG_DISTANCE_PATH_LOSS_EXPONENTS: List<Double> = listOf(2.0, 2.5, 3.0, 3.5)

val DISTANCE_HEADERS: List<String> =
    listOf("distance_fspl_m") + LOG_DISTANCE_PATH_LOSS_EXPONENTS.map(::logDistanceHeader)

private fun logDistanceHeader(exponent: Double): String =
    "distance_logdistance_" + exponent.toString().removeSuffix(".0").replace('.', '_')

/** A received signal strength is a negative dBm value above [INVALID_RSSI]; anything else is a sentinel. */
fun validRssi(rssi: Int?): Int? = rssi?.takeIf { it in (INVALID_RSSI + 1)..<0 }

/** Frequencies, link speeds and channel numbers are positive; Android reports -1 or 0 when unknown. */
fun positiveOrNull(value: Int?): Int? = value?.takeIf { it > 0 }

fun distanceFields(
    frequency: Int?,
    rssi: Int?,
): List<String> {
    val mhz = positiveOrNull(frequency)
    val dbm = validRssi(rssi)
    if (mhz == null || dbm == null) {
        return DISTANCE_HEADERS.map { "" }
    }
    return listOf(calculateDistance(mhz, dbm).csvFormat(DISTANCE_FORMAT)) +
        LOG_DISTANCE_PATH_LOSS_EXPONENTS.map {
            calculateDistanceLogDistance(mhz, dbm, it).csvFormat(DISTANCE_FORMAT)
        }
}
