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

import android.content.Context
import android.net.wifi.ScanResult
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.vrem.annotation.OpenClass
import com.vrem.util.nullToEmpty
import com.vrem.util.ssid
import com.vrem.wifianalyzer.export.CSV_BOM
import com.vrem.wifianalyzer.export.CSV_HEADER
import com.vrem.wifianalyzer.export.CsvExport
import com.vrem.wifianalyzer.wifi.scanner.toWiFiDetail
import java.io.File
import java.util.Date
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * The single place where scan results become `SCAN` rows.
 *
 * Every scan the app receives is passed to [record]. Each BSSID observation that was not recorded before
 * (a new `ScanResult.timestamp` for that BSSID and SSID) becomes one row, stamped with the moment the radio
 * actually saw the BSSID and the device position nearest to that moment. The RSSI is the raw value of that
 * scan, never the smoothed value shown on screen.
 *
 * Rows are appended to [history], which holds the whole session since the app process started and is what
 * Export shares, and to [runLog] while a measurement run is active. All file work happens on [executor],
 * which keeps rows in order and off the main thread.
 */
@OpenClass
class ScanRecorder(
    private val context: Context,
    private val location: (Long) -> LogLocation?,
    private val history: File,
    private val executor: Executor = Executors.newSingleThreadExecutor(),
    private val callbackExecutor: Executor = ContextCompat.getMainExecutor(context),
    private val csvExport: CsvExport = CsvExport(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val elapsedRealtimeNanos: () -> Long = SystemClock::elapsedRealtimeNanos,
) {
    // Accessed on the executor thread only.
    private val lastSeen = mutableMapOf<Pair<String, String>, Long>()

    @Volatile
    private var recorded = false

    @Volatile
    var runLog: LogFile? = null

    init {
        executor.execute { history.delete() }
    }

    val empty: Boolean get() = !recorded

    fun record(
        scanResults: List<ScanResult>,
        connectedBssid: String?,
    ) {
        val nowMs = currentTimeMillis()
        val nowNanos = elapsedRealtimeNanos()
        executor.execute { append(rows(scanResults, connectedBssid, nowMs, nowNanos)) }
    }

    /**
     * Appends rows built elsewhere (RTT rows) to the session history and the run log, on the same executor
     * as the SCAN rows so the file order is kept.
     */
    fun appendRows(rows: String) {
        executor.execute { append(rows) }
    }

    /** Writes the session history as a complete CSV file to [target], then reports success on the main thread. */
    fun export(
        target: File,
        onDone: (Boolean) -> Unit,
    ) {
        executor.execute {
            val success =
                runCatching {
                    target.bufferedWriter(Charsets.UTF_8).use { output ->
                        output.write(CSV_BOM + CSV_HEADER)
                        if (history.exists()) {
                            history.bufferedReader(Charsets.UTF_8).use { it.copyTo(output) }
                        }
                    }
                }.isSuccess
            callbackExecutor.execute { onDone(success) }
        }
    }

    internal fun rows(
        scanResults: List<ScanResult>,
        connectedBssid: String?,
        nowMs: Long,
        nowNanos: Long,
    ): String =
        buildString {
            scanResults
                .filter { lastSeen[it.key()] != it.timestamp }
                .sortedWith(compareBy({ it.timestamp }, { it.BSSID }))
                .forEach { scanResult ->
                    lastSeen[scanResult.key()] = scanResult.timestamp
                    val seenNanos = seenNanos(scanResult.timestamp, nowNanos)
                    val seen = Date(nowMs - (nowNanos - seenNanos) / NANOS_PER_MILLI)
                    val connected = String.nullToEmpty(scanResult.BSSID).equals(connectedBssid, ignoreCase = true)
                    append(csvExport.row(context, seen, scanResult.toWiFiDetail(), connected, location(seenNanos)))
                }
        }

    private fun append(rows: String) {
        if (rows.isEmpty()) {
            return
        }
        runCatching { history.appendText(rows, Charsets.UTF_8) }.onSuccess { recorded = true }
        runLog?.write(rows)
    }

    private fun ScanResult.key(): Pair<String, String> = String.nullToEmpty(BSSID) to ssid()

    private companion object {
        const val NANOS_PER_MICRO = 1_000L
        const val NANOS_PER_MILLI = 1_000_000L

        /**
         * `ScanResult.timestamp` is the time since boot, in microseconds, when the BSSID was last seen.
         * A value that is missing or ahead of now cannot be trusted, so the row falls back to now.
         */
        fun seenNanos(
            timestampMicros: Long,
            nowNanos: Long,
        ): Long {
            val nanos = timestampMicros * NANOS_PER_MICRO
            return if (timestampMicros > 0 && nanos <= nowNanos) nanos else nowNanos
        }
    }
}
