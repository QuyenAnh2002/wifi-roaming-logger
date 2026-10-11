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

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.vrem.annotation.OpenClass
import kotlin.math.abs

/**
 * Device position attached to a CSV row.
 *
 * [ageMs] is the row time minus the fix time: positive when the fix was taken before the row's moment,
 * negative when the nearest fix was taken after it.
 */
data class LogLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
    val provider: String,
    val ageMs: Long,
)

/**
 * Tracks the device position from every available provider and keeps a short history of accepted fixes,
 * so each row gets the fix nearest to the moment it describes (for a scan row: when the BSSID was seen),
 * not just the latest fix at the time the row is written.
 *
 * Updates arrive on the looper given to [start]; reads may come from any thread.
 */
@OpenClass
class LocationTracker(
    context: Context,
    private val intervalMs: Long = INTERVAL_MS,
    private val elapsedRealtimeNanos: () -> Long = SystemClock::elapsedRealtimeNanos,
) {
    private val locationManager: LocationManager? = context.getSystemService(LocationManager::class.java)
    private val listener = LocationListenerCompat { update(it) }

    // Accepted fixes, ordered by elapsedRealtimeNanos. Guarded by itself.
    private val fixes = ArrayDeque<Location>()

    @Volatile
    private var started = false

    @SuppressLint("MissingPermission")
    fun start(looper: Looper) {
        if (started) {
            return
        }
        val locationManager = locationManager ?: return
        val request =
            LocationRequestCompat
                .Builder(intervalMs)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(0f)
                .build()
        val registered =
            PROVIDERS.filter { it in locationManager.allProviders }.map { provider ->
                runCatching { locationManager.getLastKnownLocation(provider)?.let(::update) }
                runCatching {
                    LocationManagerCompat.requestLocationUpdates(locationManager, provider, request, listener, looper)
                }.isSuccess
            }
        // Stay restartable when nothing could be registered (e.g. permission not granted yet).
        started = registered.any { it }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!started) {
            return
        }
        started = false
        locationManager?.let { runCatching { LocationManagerCompat.removeUpdates(it, listener) } }
    }

    /** The fix nearest to now. */
    fun current(): LogLocation? = at(elapsedRealtimeNanos())

    /** The fix nearest to [elapsedNanos] (same clock as [SystemClock.elapsedRealtimeNanos]). */
    fun at(elapsedNanos: Long): LogLocation? =
        synchronized(fixes) {
            fixes.minByOrNull { abs(it.elapsedRealtimeNanos - elapsedNanos) }
        }?.toLogLocation(elapsedNanos)

    internal fun update(location: Location) {
        synchronized(fixes) {
            if (!better(location, fixes.lastOrNull())) {
                return
            }
            val index = fixes.indexOfLast { it.elapsedRealtimeNanos <= location.elapsedRealtimeNanos } + 1
            fixes.add(index, location)
            val oldest = fixes.last().elapsedRealtimeNanos - HISTORY_NANOS
            while (fixes.size > MAX_FIXES || fixes.first().elapsedRealtimeNanos < oldest) {
                fixes.removeFirst()
            }
        }
    }

    internal fun better(
        candidate: Location,
        current: Location?,
    ): Boolean {
        if (current == null) {
            return true
        }
        val newer = candidate.elapsedRealtimeNanos - current.elapsedRealtimeNanos
        return when {
            newer > STALE_NANOS -> true
            newer < -STALE_NANOS -> false
            candidate.accuracy <= current.accuracy -> true
            else -> newer > 0 && candidate.provider == current.provider
        }
    }

    private fun Location.toLogLocation(atNanos: Long): LogLocation =
        LogLocation(
            latitude,
            longitude,
            accuracy.takeIf { hasAccuracy() },
            altitude.takeIf { hasAltitude() },
            speed.takeIf { hasSpeed() },
            provider.orEmpty(),
            (atNanos - elapsedRealtimeNanos) / NANOS_PER_MILLI,
        )

    companion object {
        const val INTERVAL_MS: Long = 1000
        private const val FUSED_PROVIDER = "fused"
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val STALE_NANOS = 10_000L * NANOS_PER_MILLI
        private const val HISTORY_NANOS = 120_000L * NANOS_PER_MILLI
        private const val MAX_FIXES = 600
        private val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, FUSED_PROVIDER)
    }
}
