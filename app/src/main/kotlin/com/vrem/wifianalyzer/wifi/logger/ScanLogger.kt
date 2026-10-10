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
package com.vrem.wifianalyzer.wifi.logger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.vrem.annotation.OpenClass
import com.vrem.util.nullToEmpty
import com.vrem.wifianalyzer.export.CsvExport
import com.vrem.wifianalyzer.wifi.manager.WiFiManagerWrapper
import com.vrem.wifianalyzer.wifi.scanner.toWiFiDetail
import java.util.Date

@OpenClass
class ScanLogger(
    private val context: Context,
    private val wiFiManagerWrapper: WiFiManagerWrapper,
    private val location: () -> LogLocation?,
    private val logFile: LogFile,
    private val csvExport: CsvExport = CsvExport(),
) {
    private val lastSeen = mutableMapOf<String, Long>()
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ): Unit = log()
        }
    private var registered = false

    fun start(handler: Handler) {
        if (registered) {
            return
        }
        registered =
            runCatching {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                    null,
                    handler,
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
            }.isSuccess
        handler.post(::log)
    }

    fun stop() {
        if (registered) {
            runCatching { context.unregisterReceiver(receiver) }
            registered = false
        }
    }

    fun reset() {
        lastSeen.clear()
    }

    internal fun log() {
        val connectedBssid = wiFiManagerWrapper.wiFiInfo()?.bssid
        val nowMs = System.currentTimeMillis()
        val nowMicros = SystemClock.elapsedRealtimeNanos() / NANOS_PER_MICRO
        wiFiManagerWrapper
            .scanResults()
            .filter { lastSeen[String.nullToEmpty(it.BSSID)] != it.timestamp }
            .sortedWith(compareBy({ it.timestamp }, { it.BSSID }))
            .forEach { scanResult ->
                val bssid = String.nullToEmpty(scanResult.BSSID)
                lastSeen[bssid] = scanResult.timestamp
                val observed = Date(nowMs - (nowMicros - scanResult.timestamp) / MICROS_PER_MILLI)
                val connected = bssid.equals(connectedBssid, ignoreCase = true)
                logFile.write(csvExport.row(context, observed, scanResult.toWiFiDetail(), connected, location()))
            }
    }

    private companion object {
        const val NANOS_PER_MICRO = 1_000L
        const val MICROS_PER_MILLI = 1_000L
    }
}
