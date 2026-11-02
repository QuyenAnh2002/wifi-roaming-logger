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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.vrem.annotation.OpenClass
import com.vrem.util.buildMinVersionP
import com.vrem.util.ssid
import com.vrem.wifianalyzer.export.CsvColumn
import com.vrem.wifianalyzer.export.ROW_TYPE_RTT
import com.vrem.wifianalyzer.export.csvFormat
import com.vrem.wifianalyzer.export.csvRow
import com.vrem.wifianalyzer.export.distanceValues
import com.vrem.wifianalyzer.export.locationValues
import com.vrem.wifianalyzer.export.timeValues
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * One Wi-Fi RTT result for one access point. The measured values are only present when [status] is
 * [RTT_STATUS_SUCCESS]; [elapsedMs] is on the [SystemClock.elapsedRealtime] clock.
 */
data class RttMeasurement(
    val bssid: String,
    val status: String,
    val distanceMm: Int? = null,
    val distanceStdDevMm: Int? = null,
    val successCount: Int? = null,
    val attemptCount: Int? = null,
    val rssi: Int? = null,
    val elapsedMs: Long? = null,
)

const val RTT_STATUS_SUCCESS = "SUCCESS"

/**
 * Wi-Fi RTT (IEEE 802.11mc Fine Timing Measurement) ranging to the access points of the latest scan.
 *
 * Each scan hands its results to [update]. While there are targets, one ranging request is repeated every
 * [intervalMs], independent of the (throttled) scan rate. Every per-AP result becomes one `RTT` row passed to
 * [sink], and successful results are kept for display ([latest]).
 *
 * Targets are the connected AP first, then by signal: 802.11mc/802.11az responders, then, only when the device
 * reports one-sided RTT support (Android 14+), the other APs ranged one-sided. A request holds at most
 * `RangingRequest.getMaxPeers()` APs. All ranging work runs on [executor].
 */
@OpenClass
class RttRanger(
    private val context: Context,
    private val location: (Long) -> LogLocation?,
    private val sink: (String) -> Unit,
    private val rttManager: WifiRttManager? = wifiRttManager(context),
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    private val intervalMs: Long = INTERVAL_MS,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val elapsedRealtimeNanos: () -> Long = SystemClock::elapsedRealtimeNanos,
) {
    // Accessed on the executor thread only.
    private var targets: List<ScanResult> = listOf()
    private var connectedBssid: String? = null
    private var rangingSinceNanos: Long? = null
    private var next: ScheduledFuture<*>? = null

    private val measurements = ConcurrentHashMap<String, RttMeasurement>()

    val supported: Boolean get() = rttManager != null

    /** The runtime permission ranging still needs, or null when it is granted or RTT is not supported. */
    val missingPermission: String?
        get() {
            if (!supported) {
                return null
            }
            val permission =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.NEARBY_WIFI_DEVICES
                } else {
                    Manifest.permission.ACCESS_FINE_LOCATION
                }
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            return if (granted) null else permission
        }

    fun update(
        scanResults: List<ScanResult>,
        connectedBssid: String?,
    ) {
        if (!supported) {
            return
        }
        val nowNanos = elapsedRealtimeNanos()
        executor.execute {
            if (buildMinVersionP()) {
                this.connectedBssid = connectedBssid
                targets = targets(scanResults, connectedBssid, nowNanos)
                if (idle(nowNanos) && next == null) {
                    range()
                }
            }
        }
    }

    /** Stops the ranging loop until the next [update]; a request already in flight still reports. */
    fun stop() {
        executor.execute {
            targets = listOf()
            next?.cancel(false)
            next = null
        }
    }

    /** The latest successful measurement of [bssid], when it was taken at most [SHOW_MS] ago. */
    fun latest(bssid: String): RttMeasurement? =
        measurements[bssid.lowercase()]?.takeIf { measurement ->
            measurement.elapsedMs?.let { elapsedRealtimeNanos() / NANOS_PER_MILLI - it <= SHOW_MS } ?: false
        }

    @RequiresApi(Build.VERSION_CODES.P)
    internal fun targets(
        scanResults: List<ScanResult>,
        connectedBssid: String?,
        nowNanos: Long,
    ): List<ScanResult> {
        val (responders, others) =
            scanResults
                .filter { fresh(it.timestamp, nowNanos) }
                .sortedWith(
                    compareByDescending<ScanResult> { it.BSSID.equals(connectedBssid, ignoreCase = true) }
                        .thenByDescending { it.level },
                ).partition { it.mode() != MODE_ONE_SIDED }
        val oneSided = if (oneSidedSupported()) others else listOf()
        return (responders + oneSided).take(RangingRequest.getMaxPeers())
    }

    @RequiresApi(Build.VERSION_CODES.P)
    internal fun measurement(result: RangingResult): RttMeasurement {
        val bssid =
            result.macAddress
                ?.toString()
                .orEmpty()
                .lowercase()
        if (result.status != RangingResult.STATUS_SUCCESS) {
            return RttMeasurement(bssid, status(result.status))
        }
        return RttMeasurement(
            bssid,
            RTT_STATUS_SUCCESS,
            result.distanceMm,
            result.distanceStdDevMm,
            result.numSuccessfulMeasurements,
            result.numAttemptedMeasurements,
            result.rssi,
            result.rangingTimestampMillis,
        )
    }

    internal fun row(
        measurement: RttMeasurement,
        target: ScanResult?,
        connectedBssid: String?,
        nowMs: Long,
        nowNanos: Long,
    ): String {
        val atNanos = measurement.elapsedMs?.times(NANOS_PER_MILLI)?.takeIf { it in 1..nowNanos } ?: nowNanos
        val frequency = target?.frequency
        return csvRow(
            timeValues(nowMs - (nowNanos - atNanos) / NANOS_PER_MILLI) +
                distanceValues(frequency, measurement.rssi) +
                locationValues(location(atNanos)) +
                mapOf(
                    CsvColumn.ROW_TYPE to ROW_TYPE_RTT,
                    CsvColumn.SSID to target?.ssid().orEmpty(),
                    CsvColumn.BSSID to measurement.bssid,
                    CsvColumn.CONNECTED to measurement.bssid.equals(connectedBssid, ignoreCase = true).toString(),
                    CsvColumn.RSSI to measurement.rssi?.toString().orEmpty(),
                    CsvColumn.FREQUENCY to frequency?.toString().orEmpty(),
                    CsvColumn.DISTANCE_RTT to measurement.distanceMm.meters(),
                    CsvColumn.DISTANCE_RTT_STDDEV to measurement.distanceStdDevMm.meters(),
                    CsvColumn.RTT_STATUS to measurement.status,
                    CsvColumn.RTT_MODE to target?.mode().orEmpty(),
                    CsvColumn.RTT_SUCCESS_COUNT to measurement.successCount?.toString().orEmpty(),
                    CsvColumn.RTT_ATTEMPT_COUNT to measurement.attemptCount?.toString().orEmpty(),
                    CsvColumn.IS_80211MC to target?.is80211mcResponder?.toString().orEmpty(),
                ),
        )
    }

    private fun idle(nowNanos: Long): Boolean = rangingSinceNanos?.let { nowNanos - it > TIMEOUT_NANOS } ?: true

    private fun range() {
        next = null
        if (buildMinVersionP()) {
            rangeTargets()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission") // missingPermission is checked first
    private fun rangeTargets() {
        val rttManager = rttManager ?: return
        if (targets.isEmpty() || missingPermission != null || !rttManager.isAvailable) {
            return
        }
        val request = targets
        val connectedBssid = connectedBssid
        runCatching { rttManager.startRanging(request(request), executor, Callback(request, connectedBssid)) }
            .onSuccess { rangingSinceNanos = elapsedRealtimeNanos() }
            .onFailure { Log.w(TAG, "RTT ranging request rejected", it) }
    }

    private fun finished() {
        rangingSinceNanos = null
        if (targets.isNotEmpty()) {
            next = executor.schedule(Runnable { range() }, intervalMs, TimeUnit.MILLISECONDS)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun request(targets: List<ScanResult>): RangingRequest =
        RangingRequest
            .Builder()
            .apply {
                targets.forEach { target ->
                    if (target.mode() == MODE_ONE_SIDED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        addNon80211mcCapableAccessPoint(target)
                    } else {
                        addAccessPoint(target)
                    }
                }
            }.build()

    // The capability is only queryable on Android 14+, so one-sided ranging is used from there on.
    private fun oneSidedSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            rttManager?.rttCharacteristics?.getBoolean(WifiRttManager.CHARACTERISTICS_KEY_BOOLEAN_ONE_SIDED_RTT) == true

    private fun ScanResult.mode(): String =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && is80211azNtbResponder -> MODE_80211AZ
            is80211mcResponder -> MODE_80211MC
            else -> MODE_ONE_SIDED
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private inner class Callback(
        private val request: List<ScanResult>,
        private val connectedBssid: String?,
    ) : RangingResultCallback() {
        override fun onRangingResults(results: List<RangingResult>) {
            val nowMs = currentTimeMillis()
            val nowNanos = elapsedRealtimeNanos()
            val rows =
                results.joinToString("") { result ->
                    val measurement = measurement(result)
                    if (measurement.status == RTT_STATUS_SUCCESS) {
                        measurements[measurement.bssid] = measurement
                    }
                    val target = request.find { it.BSSID.equals(measurement.bssid, ignoreCase = true) }
                    row(measurement, target, connectedBssid, nowMs, nowNanos)
                }
            if (rows.isNotEmpty()) {
                sink(rows)
            }
            finished()
        }

        override fun onRangingFailure(code: Int) {
            Log.w(TAG, "RTT ranging failed: $code")
            finished()
        }
    }

    companion object {
        const val INTERVAL_MS: Long = 1000
        const val MODE_80211MC = "80211MC"
        const val MODE_80211AZ = "80211AZ"
        const val MODE_ONE_SIDED = "ONE_SIDED"
        private const val TAG = "RttRanger"
        private const val METER_FORMAT = "%.2f"
        private const val MM_PER_METER = 1000.0
        private const val NANOS_PER_MICRO = 1_000L
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val SHOW_MS = 10_000L
        private const val MAX_AGE_NANOS = 30_000L * NANOS_PER_MILLI
        private const val TIMEOUT_NANOS = 10_000L * NANOS_PER_MILLI

        @RequiresApi(Build.VERSION_CODES.P)
        private fun status(code: Int): String =
            when (code) {
                RangingResult.STATUS_FAIL -> "FAIL"
                RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC -> "RESPONDER_NOT_SUPPORTED"
                else -> "STATUS_$code"
            }

        /** A scan result seen in the last [MAX_AGE_NANOS]; a missing or future timestamp counts as fresh. */
        private fun fresh(
            timestampMicros: Long,
            nowNanos: Long,
        ): Boolean {
            val nanos = timestampMicros * NANOS_PER_MICRO
            return timestampMicros <= 0 || nanos > nowNanos || nowNanos - nanos <= MAX_AGE_NANOS
        }

        private fun Int?.meters(): String = this?.div(MM_PER_METER).csvFormat(METER_FORMAT)

        private fun wifiRttManager(context: Context): WifiRttManager? =
            if (buildMinVersionP() && context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
                context.getSystemService(WifiRttManager::class.java)
            } else {
                null
            }
    }
}
