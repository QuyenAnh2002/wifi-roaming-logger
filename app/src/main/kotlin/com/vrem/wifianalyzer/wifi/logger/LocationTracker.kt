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

data class LogLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
    val provider: String,
    val ageMs: Long,
)

@OpenClass
class LocationTracker(
    context: Context,
    private val intervalMs: Long = INTERVAL_MS,
) {
    private val locationManager: LocationManager? = context.getSystemService(LocationManager::class.java)
    private val listener = LocationListenerCompat { update(it) }

    @Volatile
    private var best: Location? = null

    @SuppressLint("MissingPermission")
    fun start(looper: Looper) {
        best = null
        val locationManager = locationManager ?: return
        val request =
            LocationRequestCompat
                .Builder(intervalMs)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(0f)
                .build()
        PROVIDERS.filter { it in locationManager.allProviders }.forEach { provider ->
            runCatching { locationManager.getLastKnownLocation(provider)?.let(::update) }
            runCatching {
                LocationManagerCompat.requestLocationUpdates(locationManager, provider, request, listener, looper)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        locationManager?.let { runCatching { LocationManagerCompat.removeUpdates(it, listener) } }
    }

    fun current(): LogLocation? = best?.toLogLocation()

    internal fun update(location: Location) {
        if (better(location, best)) {
            best = location
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

    private fun Location.toLogLocation(): LogLocation =
        LogLocation(
            latitude,
            longitude,
            accuracy.takeIf { hasAccuracy() },
            altitude.takeIf { hasAltitude() },
            speed.takeIf { hasSpeed() },
            provider.orEmpty(),
            (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / NANOS_PER_MILLI,
        )

    companion object {
        const val INTERVAL_MS: Long = 1000
        private const val FUSED_PROVIDER = "fused"
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val STALE_NANOS = 10_000L * NANOS_PER_MILLI
        private val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, FUSED_PROVIDER)
    }
}
