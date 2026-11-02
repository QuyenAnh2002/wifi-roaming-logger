/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 * Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung
 * Added in 2026 to this modified version of WiFiAnalyzer; see README.md
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

import android.content.Context
import com.vrem.wifianalyzer.MainContext
import com.vrem.wifianalyzer.vendor.model.VendorService
import com.vrem.wifianalyzer.wifi.band.WiFiChannel
import com.vrem.wifianalyzer.wifi.logger.LogLocation
import com.vrem.wifianalyzer.wifi.model.WiFiDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExport(
    private val vendorService: () -> VendorService = MainContext.INSTANCE::vendorService,
) {
    fun fileName(date: Date = Date()): String =
        "WiFiAnalyzer-" + SimpleDateFormat(FILE_NAME_FORMAT, Locale.US).format(date) + ".csv"

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
            val center = positiveOrNull(wiFiSignal.centerFrequency)
            val halfWidth = wiFiSignal.wiFiWidth.frequencyWidthHalf
            mapOf(
                CsvColumn.ROW_TYPE to ROW_TYPE_SCAN,
                CsvColumn.SSID to wiFiIdentifier.ssid,
                CsvColumn.BSSID to wiFiIdentifier.bssid,
                CsvColumn.CONNECTED to connected.toString(),
                CsvColumn.RSSI to validRssi(wiFiSignal.level).text(),
                CsvColumn.FREQUENCY to positiveOrNull(wiFiSignal.primaryFrequency).text(),
                CsvColumn.PRIMARY_CHANNEL to wiFiSignal.primaryWiFiChannel.text(),
                CsvColumn.CENTER_CHANNEL to wiFiSignal.centerWiFiChannel.text(),
                CsvColumn.CENTER_FREQUENCY to center.text(),
                CsvColumn.WIDTH to wiFiSignal.wiFiWidth.frequencyWidth.toString(),
                // The occupied band edges follow from the center and the width; a channel-table lookup would
                // turn edges outside the table (e.g. channel 14 at 2484 MHz) into 0.
                CsvColumn.RANGE_START to center?.minus(halfWidth).text(),
                CsvColumn.RANGE_END to center?.plus(halfWidth).text(),
                CsvColumn.STANDARD to wiFiSignal.extra.wiFiStandardDisplay(context),
                CsvColumn.SECURITY to wiFiSecurity.securities.joinToString(VALUE_SEPARATOR) { it.name },
                CsvColumn.CAPABILITIES to wiFiSecurity.capabilities,
                CsvColumn.FAST_ROAMING to wiFiSignal.extra.fastRoamingDisplay(context),
                CsvColumn.IS_80211MC to wiFiSignal.extra.is80211mc.toString(),
                CsvColumn.VENDOR to vendorService().findVendorName(wiFiIdentifier.bssid),
            )
        }

    private fun Int?.text(): String = this?.toString().orEmpty()

    private fun WiFiChannel.text(): String = takeIf { it != WiFiChannel.UNKNOWN }?.channel.text()

    companion object {
        private const val FILE_NAME_FORMAT = "yyyyMMdd-HHmmss"
        private const val VALUE_SEPARATOR = " "
    }
}
