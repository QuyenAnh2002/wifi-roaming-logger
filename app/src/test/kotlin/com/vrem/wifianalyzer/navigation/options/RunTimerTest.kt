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
package com.vrem.wifianalyzer.navigation.options

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RunTimerTest {
    private var now = 10_000L
    private val fixture = RunTimer { now }

    @Test
    fun notRunningInitially() {
        // Act & Assert
        assertThat(fixture.running).isFalse
        assertThat(fixture.elapsedMs()).isZero
        assertThat(fixture.display()).isEqualTo("00:00:00")
    }

    @Test
    fun startCountsElapsedTime() {
        // Arrange
        fixture.start()
        // Act
        now += 3_723_500L
        // Assert
        assertThat(fixture.running).isTrue
        assertThat(fixture.elapsedMs()).isEqualTo(3_723_500L)
        assertThat(fixture.display()).isEqualTo("01:02:03")
    }

    @Test
    fun stopFreezesElapsedTime() {
        // Arrange
        fixture.start()
        now += 5_000L
        // Act
        fixture.stop()
        now += 60_000L
        // Assert
        assertThat(fixture.running).isFalse
        assertThat(fixture.display()).isEqualTo("00:00:05")
    }

    @Test
    fun startAfterStopRestartsFromZero() {
        // Arrange
        fixture.start()
        now += 5_000L
        fixture.stop()
        // Act
        fixture.start()
        now += 2_000L
        // Assert
        assertThat(fixture.display()).isEqualTo("00:00:02")
    }

    @Test
    fun resetClearsTimer() {
        // Arrange
        fixture.start()
        now += 7_000L
        // Act
        fixture.reset()
        // Assert
        assertThat(fixture.running).isFalse
        assertThat(fixture.elapsedMs()).isZero
    }

    @Test
    fun displayBeyondOneHundredHours() {
        // Arrange
        fixture.start()
        // Act
        now += 360_000_000L
        // Assert
        assertThat(fixture.display()).isEqualTo("100:00:00")
    }
}
