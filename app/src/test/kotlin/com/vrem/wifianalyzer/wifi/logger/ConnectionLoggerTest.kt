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

import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vrem.wifianalyzer.RobolectricUtil
import com.vrem.wifianalyzer.export.CSV_HEADER
import com.vrem.wifianalyzer.wifi.manager.WiFiManagerWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.CINNAMON_BUN])
class ConnectionLoggerTest {
    private val mainActivity = RobolectricUtil.INSTANCE.mainActivity
    private val wiFiManagerWrapper: WiFiManagerWrapper = mock()
    private val locationTracker: LocationTracker = mock()
    private val logFile: LogFile = mock()
    private val scanRecorder: ScanRecorder = mock()
    private val sample = LogSample("SSID", "BSSID", -50, 5180, 866, "COMPLETED")
    private val location = LogLocation(20.98765432, 105.76543210, 4.5f, 12.34, 1.25f, "fused", 250L)
    private val fixture = ConnectionLogger(mainActivity, wiFiManagerWrapper)

    @Test
    fun changesWhenStillDisconnected() {
        // Act & Assert
        assertThat(fixture.changes(null, null)).isEmpty()
    }

    @Test
    fun changesWhenConnected() {
        // Act & Assert
        assertThat(fixture.changes(null, sample)).containsExactly(LogEvent.CONNECTED)
    }

    @Test
    fun changesWhenDisconnected() {
        // Act & Assert
        assertThat(fixture.changes(sample, null)).containsExactly(LogEvent.DISCONNECTED)
    }

    @Test
    fun changesWhenNothingChanged() {
        // Act & Assert
        assertThat(fixture.changes(sample, sample.copy())).isEmpty()
    }

    @Test
    fun changesWhenRoaming() {
        // Arrange
        val current = sample.copy(bssid = "BSSID2", frequency = 2412, rssi = -60)
        // Act
        val actual = fixture.changes(sample, current)
        // Assert
        assertThat(actual).containsExactly(LogEvent.BSSID, LogEvent.FREQUENCY, LogEvent.RSSI)
    }

    @Test
    fun changesWhenSSIDChanged() {
        // Act & Assert
        assertThat(fixture.changes(sample, sample.copy(ssid = "OTHER"))).containsExactly(LogEvent.SSID)
    }

    @Test
    fun changesWhenLinkSpeedAndStateChanged() {
        // Arrange
        val current = sample.copy(linkSpeed = 433, state = "ASSOCIATED")
        // Act
        val actual = fixture.changes(sample, current)
        // Assert
        assertThat(actual).containsExactly(LogEvent.LINK_SPEED, LogEvent.STATE)
    }

    @Test
    fun line() {
        // Act
        val actual = fixture.line(1000L, listOf(LogEvent.BSSID, LogEvent.RSSI), sample.copy(ssid = "a,b"), location)
        // Assert
        assertThat(actual).startsWith("1000,")
        assertThat(actual).endsWith(
            ",CONNECTION,BSSID+RSSI,\"a,b\",BSSID,true,-50,5180,,,,,,,866,COMPLETED," +
                "1.5,1.5,1.4,1.3,1.2,,,,,,,20.9876543,105.7654321,4.5,12.3,1.25,fused,250\r\n",
        )
    }

    @Test
    fun lineWithLocationWithoutOptionalValues() {
        // Arrange
        val partial = location.copy(accuracy = null, altitude = null, speed = null)
        // Act
        val actual = fixture.line(1000L, listOf(LogEvent.RSSI), sample, partial)
        // Assert
        assertThat(actual).endsWith(",COMPLETED,1.5,1.5,1.4,1.3,1.2,,,,,,,20.9876543,105.7654321,,,,fused,250\r\n")
    }

    @Test
    fun lineWithoutLocation() {
        // Act
        val actual = fixture.line(1000L, listOf(LogEvent.RSSI), sample, null)
        // Assert
        assertThat(actual).endsWith(",COMPLETED,1.5,1.5,1.4,1.3,1.2" + ",".repeat(13) + "\r\n")
    }

    @Test
    fun lineWhenDisconnected() {
        // Act
        val actual = fixture.line(1000L, listOf(LogEvent.DISCONNECTED), null, null)
        // Assert
        assertThat(actual).startsWith("1000,")
        assertThat(actual).endsWith(",CONNECTION,DISCONNECTED,,,false" + ",".repeat(28) + "\r\n")
    }

    @Test
    fun lineWithInvalidRssiLeavesRssiAndDistancesEmpty() {
        // Act
        val actual = fixture.line(1000L, listOf(LogEvent.CONNECTED), sample.copy(rssi = -127), location)
        // Assert
        assertThat(actual).contains(",true,,5180,,,,,,,866,COMPLETED" + ",".repeat(12) + "20.9876543,")
    }

    @Test
    fun lineWithUnknownFrequencyAndLinkSpeedLeavesThemEmpty() {
        // Act
        val actual = fixture.line(1000L, listOf(LogEvent.CONNECTED), sample.copy(frequency = -1, linkSpeed = -1), null)
        // Assert
        assertThat(actual).contains(",true,-50,,,,,,,,,COMPLETED,,,,,,")
    }

    @Test
    fun startStopAndClose() {
        // Arrange
        val logger = withMocks()
        // Act
        logger.start()
        val started = logger.running
        logger.stop()
        val stopped = logger.running
        logger.start()
        val restarted = logger.running
        logger.close()
        // Assert
        assertThat(started).isTrue
        assertThat(stopped).isFalse
        assertThat(restarted).isTrue
        assertThat(logger.running).isFalse
        verify(locationTracker, never()).start(any())
        verify(locationTracker, never()).stop()
    }

    @Test
    fun startWritesStartRowAndHandsFileToScanRecorder() {
        // Arrange
        val logger = withMocks()
        val rows = argumentCaptor<String>()
        whenever(logFile.opened).thenReturn(false)
        whenever(locationTracker.current()).thenReturn(location)
        // Act
        logger.start()
        idleLoggerThread()
        logger.close()
        idleLoggerThread()
        // Assert
        verify(logFile).open(CSV_HEADER)
        verify(logFile, times(2)).write(rows.capture())
        assertThat(rows.firstValue).contains(",CONNECTION,START,,,false,")
        assertThat(rows.secondValue).contains(",CONNECTION,PAUSE,,,false,")
        verify(scanRecorder).runLog = logFile
        verify(scanRecorder, times(2)).runLog = null
        verify(logFile).close()
    }

    @Test
    fun stopWhenNotRunningDoesNothing() {
        // Arrange
        val logger = withMocks()
        // Act
        logger.stop()
        logger.close()
        // Assert
        assertThat(logger.running).isFalse
        verify(logFile, never()).open(any())
    }

    @Test
    fun closeReportsClosedFilesOnMainThread() {
        // Arrange
        val logger = withMocks()
        val logUri = Uri.parse("content://media/external/downloads/1")
        whenever(logFile.uri).thenReturn(logUri)
        var received: List<Uri>? = null
        logger.start()
        // Act
        logger.close { received = it }
        repeat(100) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (received == null) Thread.sleep(10)
        }
        // Assert
        assertThat(received).containsExactly(logUri)
        verify(logFile).open(CSV_HEADER)
        verify(logFile).close()
    }

    @Test
    fun closeWithoutStartReportsFilesImmediately() {
        // Arrange
        val logger = withMocks()
        var received: List<Uri>? = null
        // Act
        logger.close { received = it }
        // Assert
        assertThat(received).isEmpty()
    }

    private fun idleLoggerThread() {
        repeat(20) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    private fun withMocks(): ConnectionLogger =
        ConnectionLogger(
            mainActivity,
            wiFiManagerWrapper,
            ConnectionLogger.INTERVAL_MS,
            locationTracker,
            scanRecorder,
            logFile,
        )
}
