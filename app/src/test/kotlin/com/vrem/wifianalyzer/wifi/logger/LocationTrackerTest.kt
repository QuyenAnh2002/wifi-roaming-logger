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

import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vrem.wifianalyzer.RobolectricUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.CINNAMON_BUN])
class LocationTrackerTest {
    private val mainActivity = RobolectricUtil.INSTANCE.mainActivity
    private var nowNanos = 0L
    private val fixture = LocationTracker(mainActivity, LocationTracker.INTERVAL_MS) { nowNanos }

    @Test
    fun currentWithoutLocation() {
        // Act & Assert
        assertThat(fixture.current()).isNull()
    }

    @Test
    fun currentAfterUpdate() {
        // Arrange
        val location =
            location("gps", 1, 5f).apply {
                latitude = 21.0
                longitude = 105.8
                altitude = 10.0
                speed = 1.5f
            }
        // Act
        fixture.update(location)
        val actual = fixture.current()
        // Assert
        assertThat(actual).isNotNull
        assertThat(actual!!.latitude).isEqualTo(21.0)
        assertThat(actual.longitude).isEqualTo(105.8)
        assertThat(actual.accuracy).isEqualTo(5f)
        assertThat(actual.altitude).isEqualTo(10.0)
        assertThat(actual.speed).isEqualTo(1.5f)
        assertThat(actual.provider).isEqualTo("gps")
    }

    @Test
    fun currentWithoutOptionalValues() {
        // Act
        fixture.update(Location("network"))
        val actual = fixture.current()
        // Assert
        assertThat(actual).isNotNull
        assertThat(actual!!.accuracy).isNull()
        assertThat(actual.altitude).isNull()
        assertThat(actual.speed).isNull()
        assertThat(actual.provider).isEqualTo("network")
    }

    @Test
    fun betterWhenNoCurrent() {
        // Act & Assert
        assertThat(fixture.better(location("gps", 0, 5f), null)).isTrue
    }

    @Test
    fun betterWhenMuchNewer() {
        // Act & Assert
        assertThat(fixture.better(location("network", 11, 50f), location("gps", 0, 5f))).isTrue
    }

    @Test
    fun notBetterWhenMuchOlder() {
        // Act & Assert
        assertThat(fixture.better(location("gps", 0, 3f), location("network", 11, 50f))).isFalse
    }

    @Test
    fun betterWhenMoreAccurate() {
        // Act & Assert
        assertThat(fixture.better(location("gps", 4, 3f), location("network", 5, 20f))).isTrue
    }

    @Test
    fun betterWhenNewerFromSameProvider() {
        // Act & Assert
        assertThat(fixture.better(location("gps", 6, 8f), location("gps", 5, 5f))).isTrue
    }

    @Test
    fun notBetterWhenNewerFromOtherProviderAndLessAccurate() {
        // Act & Assert
        assertThat(fixture.better(location("network", 6, 30f), location("gps", 5, 5f))).isFalse
    }

    @Test
    fun updateKeepsBetterLocation() {
        // Arrange
        fixture.update(location("gps", 5, 5f))
        // Act
        fixture.update(location("network", 6, 30f))
        // Assert
        assertThat(fixture.current()!!.provider).isEqualTo("gps")
    }

    @Test
    fun currentReportsAgeOfLatestFix() {
        // Arrange
        fixture.update(location("gps", 5, 5f))
        nowNanos = 7 * NANOS_PER_SECOND
        // Act
        val actual = fixture.current()
        // Assert
        assertThat(actual!!.ageMs).isEqualTo(2_000L)
    }

    @Test
    fun atPicksFixNearestToTheObservation() {
        // Arrange
        fixture.update(location("gps", 1, 5f).apply { latitude = 1.0 })
        fixture.update(location("gps", 2, 5f).apply { latitude = 2.0 })
        fixture.update(location("gps", 3, 5f).apply { latitude = 3.0 })
        nowNanos = 10 * NANOS_PER_SECOND
        // Act
        val before = fixture.at(2_200 * NANOS_PER_MILLI)
        val after = fixture.at(2_700 * NANOS_PER_MILLI)
        // Assert
        assertThat(before!!.latitude).isEqualTo(2.0)
        assertThat(before.ageMs).isEqualTo(200L)
        assertThat(after!!.latitude).isEqualTo(3.0)
        assertThat(after.ageMs).isEqualTo(-300L)
    }

    @Test
    fun atKeepsFixesInTimeOrderWhenAnOlderMoreAccurateFixArrives() {
        // Arrange
        fixture.update(location("network", 5, 30f).apply { latitude = 5.0 })
        // Act
        fixture.update(location("gps", 4, 3f).apply { latitude = 4.0 })
        // Assert
        assertThat(fixture.at(4 * NANOS_PER_SECOND)!!.latitude).isEqualTo(4.0)
        assertThat(fixture.at(5 * NANOS_PER_SECOND)!!.latitude).isEqualTo(5.0)
    }

    @Test
    fun atForgetsFixesOlderThanTheHistoryWindow() {
        // Arrange
        fixture.update(location("gps", 0, 5f).apply { latitude = 0.5 })
        fixture.update(location("gps", 200, 5f).apply { latitude = 200.5 })
        // Act
        val actual = fixture.at(0)
        // Assert
        assertThat(actual!!.latitude).isEqualTo(200.5)
    }

    @Test
    fun atWithoutFix() {
        // Act & Assert
        assertThat(fixture.at(0)).isNull()
    }

    @Test
    fun startAndStop() {
        // Act
        fixture.start(Looper.getMainLooper())
        fixture.start(Looper.getMainLooper())
        fixture.stop()
        fixture.stop()
        // Assert
        assertThat(fixture.current()).isNull()
    }

    private fun location(
        provider: String,
        seconds: Long,
        accuracy: Float,
    ): Location =
        Location(provider).apply {
            elapsedRealtimeNanos = seconds * NANOS_PER_SECOND
            this.accuracy = accuracy
        }

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
