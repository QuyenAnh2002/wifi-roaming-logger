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

import android.content.Context
import com.vrem.wifianalyzer.MainContext
import com.vrem.wifianalyzer.vendor.model.VendorService
import com.vrem.wifianalyzer.wifi.logger.LogLocation
import com.vrem.wifianalyzer.wifi.model.WiFiData
import com.vrem.wifianalyzer.wifi.model.WiFiDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExport(
    private val vendorService: () -> VendorService = MainContext.INSTANCE::vendorService,
) {
    fun fileName(date: Date = Date()): String =
        "WiFiAnalyzer-" + SimpleDateFormat(FILE_NAME_FORMAT, Locale.US).format(date) + ".csv"

    fun data(
        context: Context,
        wiFiData: WiFiData,
        location: LogLocation?,
        date: Date = Date(),
    ): String =
        buildString {
            append(CSV_BOM)
            append(header())
            wiFiData.wiFiDetails.forEach {
                val connected = wiFiData.wiFiConnection.wiFiIdentifier.equals(it.wiFiIdentifier, true)
                append(row(context, date, it, connected, location))
            }
        }

    fun header(): String = CSV_HEADER

    fun row(
        context: Context,
        date: Date,
        wiFiDetail: WiFiDetail,
        connected: Boolean,
        location: LogLocation?,
    ): String =
        csvRow(
            timeValues(date.time) +
                scanValues(context, wiFiDetail, connected) +
                distanceValues(wiFiDetail.wiFiSignal.primaryFrequency, wiFiDetail.wiFiSignal.level) +
                locationValues(location),
        )

    private fun scanValues(
        context: Context,
        wiFiDetail: WiFiDetail,
        connected: Boolean,
    ): Map<CsvColumn, String> =
        with(wiFiDetail) {
            mapOf(
                CsvColumn.ROW_TYPE to ROW_TYPE_SCAN,
                CsvColumn.SSID to wiFiIdentifier.ssid,
                CsvColumn.BSSID to wiFiIdentifier.bssid,
                CsvColumn.CONNECTED to connected.toString(),
                CsvColumn.RSSI to wiFiSignal.level.toString(),
                CsvColumn.FREQUENCY to wiFiSignal.primaryFrequency.toString(),
                CsvColumn.PRIMARY_CHANNEL to wiFiSignal.primaryWiFiChannel.channel.toString(),
                CsvColumn.CENTER_CHANNEL to wiFiSignal.centerWiFiChannel.channel.toString(),
                CsvColumn.CENTER_FREQUENCY to wiFiSignal.centerFrequency.toString(),
                CsvColumn.WIDTH to wiFiSignal.wiFiWidth.frequencyWidth.toString(),
                CsvColumn.RANGE_START to wiFiSignal.wiFiChannelStart.frequency.toString(),
                CsvColumn.RANGE_END to wiFiSignal.wiFiChannelEnd.frequency.toString(),
                CsvColumn.STANDARD to wiFiSignal.extra.wiFiStandardDisplay(context),
                CsvColumn.SECURITY to wiFiSecurity.securities.joinToString(VALUE_SEPARATOR) { it.name },
                CsvColumn.CAPABILITIES to wiFiSecurity.capabilities,
                CsvColumn.FAST_ROAMING to wiFiSignal.extra.fastRoamingDisplay(context),
                CsvColumn.IS_80211MC to wiFiSignal.extra.is80211mc.toString(),
                CsvColumn.VENDOR to vendorService().findVendorName(wiFiIdentifier.bssid),
            )
        }

    companion object {
        private const val FILE_NAME_FORMAT = "yyyyMMdd-HHmmss"
        private const val VALUE_SEPARATOR = " "
    }
}
