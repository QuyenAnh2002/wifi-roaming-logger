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

import com.vrem.wifianalyzer.wifi.logger.LogLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
private const val COORDINATE_FORMAT = "%.7f"
private const val METER_FORMAT = "%.1f"
private const val SPEED_FORMAT = "%.2f"

// SimpleDateFormat is not thread-safe and costly to build, so each writer thread reuses its own instance.
private val TIME_FORMATTER: ThreadLocal<SimpleDateFormat> =
    ThreadLocal.withInitial { SimpleDateFormat(TIME_FORMAT, Locale.US) }

const val ROW_TYPE_CONNECTION = "CONNECTION"
const val ROW_TYPE_SCAN = "SCAN"

enum class CsvColumn(
    val header: String,
) {
    EPOCH_MS("epoch_ms"),
    TIME("time"),
    ROW_TYPE("row_type"),
    EVENT("event"),
    SSID("ssid"),
    BSSID("bssid"),
    CONNECTED("connected"),
    RSSI("rssi_dbm"),
    FREQUENCY("frequency_mhz"),
    PRIMARY_CHANNEL("primary_channel"),
    CENTER_CHANNEL("center_channel"),
    CENTER_FREQUENCY("center_frequency_mhz"),
    WIDTH("width_mhz"),
    RANGE_START("range_start_mhz"),
    RANGE_END("range_end_mhz"),
    LINK_SPEED("link_speed_mbps"),
    SUPPLICANT_STATE("supplicant_state"),
    DISTANCE_FSPL("distance_fspl_m"),
    DISTANCE_LOG_2("distance_logdistance_2"),
    DISTANCE_LOG_2_5("distance_logdistance_2_5"),
    DISTANCE_LOG_3("distance_logdistance_3"),
    DISTANCE_LOG_3_5("distance_logdistance_3_5"),
    STANDARD("standard"),
    SECURITY("security"),
    CAPABILITIES("capabilities"),
    FAST_ROAMING("fast_roaming"),
    IS_80211MC("is_80211mc"),
    VENDOR("vendor"),
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    ACCURACY("accuracy_m"),
    ALTITUDE("altitude_m"),
    SPEED("speed_mps"),
    LOCATION_PROVIDER("location_provider"),
    LOCATION_AGE("location_age_ms"),
}

val DISTANCE_COLUMNS: List<CsvColumn> =
    listOf(
        CsvColumn.DISTANCE_FSPL,
        CsvColumn.DISTANCE_LOG_2,
        CsvColumn.DISTANCE_LOG_2_5,
        CsvColumn.DISTANCE_LOG_3,
        CsvColumn.DISTANCE_LOG_3_5,
    )

val CSV_HEADER: String = csvLine(CsvColumn.entries.map { it.header })

fun csvRow(values: Map<CsvColumn, String>): String = csvLine(CsvColumn.entries.map { values[it].orEmpty() })

fun timeValues(timeMs: Long): Map<CsvColumn, String> =
    mapOf(
        CsvColumn.EPOCH_MS to timeMs.toString(),
        CsvColumn.TIME to formatTime(timeMs),
    )

private fun formatTime(timeMs: Long): String =
    TIME_FORMATTER.get().run {
        timeZone = TimeZone.getDefault()
        format(Date(timeMs))
    }

fun distanceValues(
    frequency: Int?,
    rssi: Int?,
): Map<CsvColumn, String> = DISTANCE_COLUMNS.zip(distanceFields(frequency, rssi)).toMap()

fun locationValues(location: LogLocation?): Map<CsvColumn, String> =
    mapOf(
        CsvColumn.LATITUDE to location?.latitude.csvFormat(COORDINATE_FORMAT),
        CsvColumn.LONGITUDE to location?.longitude.csvFormat(COORDINATE_FORMAT),
        CsvColumn.ACCURACY to location?.accuracy.csvFormat(METER_FORMAT),
        CsvColumn.ALTITUDE to location?.altitude.csvFormat(METER_FORMAT),
        CsvColumn.SPEED to location?.speed.csvFormat(SPEED_FORMAT),
        CsvColumn.LOCATION_PROVIDER to location?.provider.orEmpty(),
        CsvColumn.LOCATION_AGE to location?.ageMs?.toString().orEmpty(),
    )
