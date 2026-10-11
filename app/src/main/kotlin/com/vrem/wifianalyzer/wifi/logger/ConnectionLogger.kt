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
package com.vrem.wifianalyzer.wifi.logger

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.annotation.RequiresApi
import com.vrem.annotation.OpenClass
import com.vrem.util.nullToEmpty
import com.vrem.wifianalyzer.MainContext
import com.vrem.wifianalyzer.export.CSV_HEADER
import com.vrem.wifianalyzer.export.CsvColumn
import com.vrem.wifianalyzer.export.ROW_TYPE_CONNECTION
import com.vrem.wifianalyzer.export.csvRow
import com.vrem.wifianalyzer.export.distanceValues
import com.vrem.wifianalyzer.export.locationValues
import com.vrem.wifianalyzer.export.positiveOrNull
import com.vrem.wifianalyzer.export.timeValues
import com.vrem.wifianalyzer.export.validRssi
import com.vrem.wifianalyzer.wifi.manager.WiFiManagerWrapper
import com.vrem.wifianalyzer.wifi.model.convertSSID

enum class LogEvent {
    START,
    RESUME,
    PAUSE,
    CONNECTED,
    DISCONNECTED,
    SSID,
    BSSID,
    FREQUENCY,
    RSSI,
    LINK_SPEED,
    STATE,
}

data class LogSample(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val linkSpeed: Int,
    val state: String,
)

/**
 * Owns the `wifi-log` file of a measurement run. Writes a `CONNECTION` row whenever the connected access point
 * changes (polled every [intervalMs] and on network callbacks), and hands the open file to [scanRecorder] so
 * `SCAN` rows of the run land in the same file.
 */
@OpenClass
class ConnectionLogger(
    context: Context,
    private val wiFiManagerWrapper: WiFiManagerWrapper,
    private val intervalMs: Long = INTERVAL_MS,
    private val locationTracker: LocationTracker = MainContext.INSTANCE.locationTracker,
    private val scanRecorder: ScanRecorder = MainContext.INSTANCE.scanRecorder,
    private val logFile: LogFile = LogFile(context, LOG_FILE_PREFIX),
) {
    private val connectivityManager: ConnectivityManager =
        context.getSystemService(ConnectivityManager::class.java)
    private val wifiRequest: NetworkRequest =
        NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
    private val networkCallback: ConnectivityManager.NetworkCallback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TriggerCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO)
        } else {
            TriggerCallback()
        }

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var handler: Handler? = null

    @Volatile
    private var active = false

    // Accessed on the logger thread only.
    private var previous: LogSample? = null
    private var logging = false

    val running: Boolean get() = active

    fun start() {
        if (active) {
            return
        }
        val handler = handler ?: Handler(HandlerThread(THREAD_NAME).apply { start() }.looper)
        this.handler = handler
        active = true
        handler.post(::beginLog)
        handler.postDelayed(::poll, intervalMs)
        runCatching { connectivityManager.registerNetworkCallback(wifiRequest, networkCallback) }
    }

    fun stop() {
        if (!active) {
            return
        }
        active = false
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        handler?.apply {
            removeCallbacksAndMessages(null)
            post(::pauseLog)
        }
    }

    fun close(onClosed: ((List<Uri>) -> Unit)? = null) {
        stop()
        val handler = handler
        if (handler == null) {
            onClosed?.invoke(files())
            return
        }
        this.handler = null
        handler.post {
            closeLog()
            val files = files()
            onClosed?.let { callback -> mainHandler.post { callback(files) } }
        }
        handler.looper.quitSafely()
    }

    fun files(): List<Uri> = listOfNotNull(logFile.uri)

    internal fun changes(
        previous: LogSample?,
        current: LogSample?,
    ): List<LogEvent> =
        when {
            previous == null && current == null -> listOf()
            previous == null -> listOf(LogEvent.CONNECTED)
            current == null -> listOf(LogEvent.DISCONNECTED)
            else ->
                listOfNotNull(
                    LogEvent.SSID.takeIf { previous.ssid != current.ssid },
                    LogEvent.BSSID.takeIf { previous.bssid != current.bssid },
                    LogEvent.FREQUENCY.takeIf { previous.frequency != current.frequency },
                    LogEvent.RSSI.takeIf { previous.rssi != current.rssi },
                    LogEvent.LINK_SPEED.takeIf { previous.linkSpeed != current.linkSpeed },
                    LogEvent.STATE.takeIf { previous.state != current.state },
                )
        }

    internal fun line(
        timeMs: Long,
        events: List<LogEvent>,
        sample: LogSample?,
        location: LogLocation?,
    ): String =
        csvRow(
            timeValues(timeMs) +
                mapOf(
                    CsvColumn.ROW_TYPE to ROW_TYPE_CONNECTION,
                    CsvColumn.EVENT to events.joinToString(EVENT_SEPARATOR),
                    CsvColumn.SSID to sample?.ssid.orEmpty(),
                    CsvColumn.BSSID to sample?.bssid.orEmpty(),
                    CsvColumn.CONNECTED to (sample != null).toString(),
                    CsvColumn.RSSI to validRssi(sample?.rssi)?.toString().orEmpty(),
                    CsvColumn.FREQUENCY to positiveOrNull(sample?.frequency)?.toString().orEmpty(),
                    CsvColumn.LINK_SPEED to positiveOrNull(sample?.linkSpeed)?.toString().orEmpty(),
                    CsvColumn.SUPPLICANT_STATE to sample?.state.orEmpty(),
                ) +
                distanceValues(sample?.frequency, sample?.rssi) +
                locationValues(location),
        )

    private fun trigger() {
        if (active) {
            handler?.post { sample() }
        }
    }

    private fun poll() {
        if (!active) {
            return
        }
        sample()
        handler?.postDelayed(::poll, intervalMs)
    }

    private fun sample(event: LogEvent? = null) {
        val current = wiFiManagerWrapper.wiFiInfo()?.takeIf { it.bssid != null }?.toLogSample()
        val events = event?.let(::listOf) ?: changes(previous, current)
        if (events.isEmpty()) {
            return
        }
        previous = current
        write(events, current)
    }

    private fun write(
        events: List<LogEvent>,
        sample: LogSample?,
    ) {
        logFile.write(line(System.currentTimeMillis(), events, sample, locationTracker.current()))
    }

    private fun beginLog() {
        val event =
            if (logFile.opened) {
                LogEvent.RESUME
            } else {
                logFile.open(CSV_HEADER)
                LogEvent.START
            }
        // The START / RESUME row goes in first, so every SCAN row of this stretch comes after it.
        sample(event)
        logging = true
        scanRecorder.runLog = logFile
    }

    private fun pauseLog() {
        scanRecorder.runLog = null
        if (logging) {
            write(listOf(LogEvent.PAUSE), previous)
            logging = false
        }
    }

    private fun closeLog() {
        pauseLog()
        logFile.close()
        previous = null
    }

    private fun WifiInfo.toLogSample(): LogSample =
        LogSample(
            convertSSID(String.nullToEmpty(ssid)),
            String.nullToEmpty(bssid),
            rssi,
            frequency,
            linkSpeed,
            supplicantState.name,
        )

    private inner class TriggerCallback : ConnectivityManager.NetworkCallback {
        constructor() : super()

        @RequiresApi(Build.VERSION_CODES.S)
        constructor(flags: Int) : super(flags)

        override fun onAvailable(network: Network): Unit = trigger()

        override fun onLost(network: Network): Unit = trigger()

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ): Unit = trigger()
    }

    companion object {
        const val INTERVAL_MS: Long = 1000
        private const val THREAD_NAME = "ConnectionLogger"
        private const val LOG_FILE_PREFIX = "wifi-log"
        private const val EVENT_SEPARATOR = "+"
    }
}
