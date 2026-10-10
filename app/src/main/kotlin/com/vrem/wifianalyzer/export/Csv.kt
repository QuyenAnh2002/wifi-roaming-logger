/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
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
private const val INVALID_RSSI = -127

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

fun distanceFields(
    frequency: Int?,
    rssi: Int?,
): List<String> =
    if (frequency == null || frequency <= 0 || rssi == null || rssi <= INVALID_RSSI) {
        DISTANCE_HEADERS.map { "" }
    } else {
        listOf(calculateDistance(frequency, rssi).csvFormat(DISTANCE_FORMAT)) +
            LOG_DISTANCE_PATH_LOSS_EXPONENTS.map {
                calculateDistanceLogDistance(frequency, rssi, it).csvFormat(DISTANCE_FORMAT)
            }
    }
