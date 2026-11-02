/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 * Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung
 * Added in 2026 to this modified version of WiFiAnalyzer; see README.md
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
import android.app.Application
import android.net.MacAddress
import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vrem.wifianalyzer.export.CSV_HEADER
import com.vrem.wifianalyzer.wifi.scanner.whenSsid
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.CINNAMON_BUN])
class RttRangerTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val rttManager: WifiRttManager = mock()
    private val executor: ScheduledExecutorService = mock()
    private val future: ScheduledFuture<*> = mock()
    private val location = LogLocation(21.0, 105.8, 5f, null, null, "gps", 0L)
    private val locationRequests = mutableListOf<Long>()
    private val rows = mutableListOf<String>()
    private val fixture =
        RttRanger(
            application,
            { locationRequests.add(it).let { location } },
            { rows.add(it) },
            rttManager,
            executor,
            INTERVAL_MS,
            { NOW_MS },
            { NOW_NANOS },
        )

    @Before
    fun setUp() {
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.NEARBY_WIFI_DEVICES)
        doAnswer { (it.arguments[0] as Runnable).run() }.whenever(executor).execute(any())
        doReturn(future).whenever(executor).schedule(any<Runnable>(), any(), any())
        whenever(rttManager.isAvailable).thenReturn(true)
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(future)
    }

    @Test
    fun supportedWhenTheDeviceHasWifiRtt() {
        // Assert
        assertThat(fixture.supported).isTrue
        assertThat(fixture.missingPermission).isNull()
    }

    @Test
    fun missingPermissionIsNearbyWifiDevices() {
        // Arrange
        Shadows.shadowOf(application).denyPermissions(Manifest.permission.NEARBY_WIFI_DEVICES)
        // Act & Assert
        assertThat(fixture.missingPermission).isEqualTo(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    @Test
    fun unsupportedDeviceNeverRanges() {
        // Arrange
        val unsupported = RttRanger(application, { location }, { rows.add(it) }, null, executor)
        // Act
        unsupported.update(listOf(scanResult(BSSID1, -50, responder = true)), null)
        // Assert
        assertThat(unsupported.supported).isFalse
        assertThat(unsupported.missingPermission).isNull()
        verify(executor, never()).execute(any())
    }

    @Test
    fun targetsAreConnectedFirstThenStrongestResponders() {
        // Arrange
        val weak = scanResult(BSSID1, -70, responder = true)
        val strong = scanResult(BSSID2, -50, responder = true)
        val connected = scanResult(BSSID3, -80, responder = true)
        val stale = scanResult(BSSID4, -40, responder = true, timestamp = STALE_MICROS)
        val plain = scanResult(BSSID5, -30, responder = false)
        // Act
        val actual = fixture.targets(listOf(weak, strong, connected, stale, plain), BSSID3.uppercase(), NOW_NANOS)
        // Assert
        assertThat(actual).containsExactly(connected, strong, weak)
    }

    @Test
    fun targetsIncludeOtherAccessPointsWhenOneSidedRttIsSupported() {
        // Arrange
        val characteristics =
            Bundle().apply {
                putBoolean(
                    WifiRttManager.CHARACTERISTICS_KEY_BOOLEAN_ONE_SIDED_RTT,
                    true,
                )
            }
        whenever(rttManager.rttCharacteristics).thenReturn(characteristics)
        val plain = scanResult(BSSID1, -30, responder = false)
        val responder = scanResult(BSSID2, -70, responder = true)
        // Act
        val actual = fixture.targets(listOf(plain, responder), null, NOW_NANOS)
        // Assert
        assertThat(actual).containsExactly(responder, plain)
    }

    @Test
    fun targetsAreLimitedToMaxPeers() {
        // Arrange
        val scanResults =
            (1..RangingRequest.getMaxPeers() + 2).map {
                scanResult(
                    "aa:bb:cc:dd:ee:%02x".format(it),
                    -it,
                    true,
                )
            }
        // Act
        val actual = fixture.targets(scanResults, null, NOW_NANOS)
        // Assert
        assertThat(actual).hasSize(RangingRequest.getMaxPeers())
    }

    @Test
    fun updateStartsOneRangingRequest() {
        // Arrange
        val scanResults = listOf(scanResult(BSSID1, -50, responder = true))
        // Act
        fixture.update(scanResults, null)
        fixture.update(scanResults, null)
        // Assert
        verify(rttManager).startRanging(any(), eq(executor), any())
    }

    @Test
    fun updateWithoutTargetsDoesNotRange() {
        // Act
        fixture.update(listOf(scanResult(BSSID1, -50, responder = false)), null)
        // Assert
        verify(rttManager, never()).startRanging(any(), any(), any())
    }

    @Test
    fun updateWithoutPermissionDoesNotRange() {
        // Arrange
        Shadows.shadowOf(application).denyPermissions(Manifest.permission.NEARBY_WIFI_DEVICES)
        // Act
        fixture.update(listOf(scanResult(BSSID1, -50, responder = true)), null)
        // Assert
        verify(rttManager, never()).startRanging(any(), any(), any())
    }

    @Test
    fun updateWhenRttIsNotAvailableDoesNotRange() {
        // Arrange
        whenever(rttManager.isAvailable).thenReturn(false)
        // Act
        fixture.update(listOf(scanResult(BSSID1, -50, responder = true)), null)
        // Assert
        verify(rttManager, never()).startRanging(any(), any(), any())
    }

    @Test
    fun resultsBecomeRowsAndRangingRepeats() {
        // Arrange
        val target = scanResult(BSSID1, -50, responder = true)
        val callback = range(listOf(target), BSSID1)
        // Act
        callback.onRangingResults(listOf(success(BSSID1), failure(BSSID2)))
        // Assert
        assertThat(rows).hasSize(1)
        val lines = rows[0].split("\r\n").filter { it.isNotEmpty() }
        assertThat(lines).hasSize(2)
        val success = columns(lines[0])
        assertThat(success["row_type"]).isEqualTo("RTT")
        assertThat(success["ssid"]).isEqualTo("SSID")
        assertThat(success["bssid"]).isEqualTo(BSSID1)
        assertThat(success["connected"]).isEqualTo("true")
        assertThat(success["rssi_dbm"]).isEqualTo("-48")
        assertThat(success["frequency_mhz"]).isEqualTo("5180")
        assertThat(success["distance_rtt_m"]).isEqualTo("3.25")
        assertThat(success["distance_rtt_stddev_m"]).isEqualTo("0.42")
        assertThat(success["rtt_status"]).isEqualTo("SUCCESS")
        assertThat(success["rtt_mode"]).isEqualTo(RttRanger.MODE_80211MC)
        assertThat(success["rtt_success_count"]).isEqualTo("7")
        assertThat(success["rtt_attempt_count"]).isEqualTo("8")
        assertThat(success["is_80211mc"]).isEqualTo("true")
        assertThat(success["epoch_ms"]).isEqualTo((NOW_MS - 1_500L).toString())
        assertThat(success["latitude"]).isEqualTo("21.0000000")
        assertThat(success["distance_fspl_m"]).isNotEmpty()
        val failed = columns(lines[1])
        assertThat(failed["bssid"]).isEqualTo(BSSID2)
        assertThat(failed["rtt_status"]).isEqualTo("FAIL")
        assertThat(failed["distance_rtt_m"]).isEmpty()
        assertThat(failed["rssi_dbm"]).isEmpty()
        assertThat(failed["epoch_ms"]).isEqualTo(NOW_MS.toString())
        assertThat(locationRequests).containsExactly(RANGED_MS * 1_000_000L, NOW_NANOS)
        verify(executor).schedule(any<Runnable>(), eq(INTERVAL_MS), eq(TimeUnit.MILLISECONDS))
    }

    @Test
    fun scheduledRangingStartsTheNextRequest() {
        // Arrange
        val callback = range(listOf(scanResult(BSSID1, -50, responder = true)), null)
        callback.onRangingFailure(RangingResultCallback.STATUS_CODE_FAIL)
        val next = argumentCaptor<Runnable>()
        verify(executor).schedule(next.capture(), eq(INTERVAL_MS), eq(TimeUnit.MILLISECONDS))
        // Act
        next.firstValue.run()
        // Assert
        assertThat(rows).isEmpty()
        verify(rttManager, times(2)).startRanging(any(), eq(executor), any())
    }

    @Test
    fun stopEndsTheRangingLoop() {
        // Arrange
        val callback = range(listOf(scanResult(BSSID1, -50, responder = true)), null)
        callback.onRangingResults(listOf())
        // Act
        fixture.stop()
        callback.onRangingResults(listOf())
        // Assert
        verify(future).cancel(false)
        verify(executor).schedule(any<Runnable>(), eq(INTERVAL_MS), eq(TimeUnit.MILLISECONDS))
    }

    @Test
    fun latestKeepsRecentSuccessfulMeasurements() {
        // Arrange
        val callback = range(listOf(scanResult(BSSID1, -50, responder = true)), null)
        // Act
        callback.onRangingResults(listOf(success(BSSID1), failure(BSSID2)))
        // Assert
        assertThat(fixture.latest(BSSID1.uppercase())?.distanceMm).isEqualTo(3_250)
        assertThat(fixture.latest(BSSID2)).isNull()
        verify(executor).schedule(any<Runnable>(), eq(INTERVAL_MS), eq(TimeUnit.MILLISECONDS))
    }

    @Test
    fun latestIgnoresOldMeasurements() {
        // Arrange
        val callback = range(listOf(scanResult(BSSID1, -50, responder = true)), null)
        // Act
        callback.onRangingResults(listOf(success(BSSID1, NOW_NANOS / 1_000_000L - 10_001L)))
        // Assert
        assertThat(fixture.latest(BSSID1)).isNull()
        verify(executor).schedule(any<Runnable>(), eq(INTERVAL_MS), eq(TimeUnit.MILLISECONDS))
    }

    @Test
    fun measurementOfFailedResultHasNoValues() {
        // Arrange
        val result = failure(BSSID1)
        // Act
        val actual = fixture.measurement(result)
        // Assert
        assertThat(actual).isEqualTo(RttMeasurement(BSSID1, "FAIL"))
        verify(result, never()).distanceMm
    }

    @Test
    fun rowHeaderMatchesColumns() {
        // Act
        val actual = fixture.row(RttMeasurement(BSSID1, "FAIL"), null, null, NOW_MS, NOW_NANOS)
        // Assert
        assertThat(actual.split(",")).hasSize(CSV_HEADER.split(",").size)
        assertThat(columns(actual)["rtt_mode"]).isEmpty()
    }

    private fun range(
        targets: List<ScanResult>,
        connectedBssid: String?,
    ): RangingResultCallback {
        fixture.update(targets, connectedBssid)
        val callback = argumentCaptor<RangingResultCallback>()
        verify(rttManager).startRanging(any(), eq(executor), callback.capture())
        return callback.firstValue
    }

    private fun columns(line: String): Map<String, String> =
        CSV_HEADER
            .trimEnd()
            .split(",")
            .zip(line.trimEnd().split(","))
            .toMap()

    private fun scanResult(
        bssid: String,
        level: Int,
        responder: Boolean,
        timestamp: Long = SEEN_MICROS,
    ): ScanResult {
        val scanResult: ScanResult = mock()
        whenSsid(scanResult, "SSID")
        scanResult.BSSID = bssid
        scanResult.level = level
        scanResult.frequency = FREQUENCY
        scanResult.timestamp = timestamp
        doReturn(responder).whenever(scanResult).is80211mcResponder
        return scanResult
    }

    private fun success(
        bssid: String,
        rangedMs: Long = RANGED_MS,
    ): RangingResult {
        val result: RangingResult = mock()
        whenever(result.macAddress).thenReturn(MacAddress.fromString(bssid))
        whenever(result.status).thenReturn(RangingResult.STATUS_SUCCESS)
        whenever(result.distanceMm).thenReturn(3_250)
        whenever(result.distanceStdDevMm).thenReturn(420)
        whenever(result.numSuccessfulMeasurements).thenReturn(7)
        whenever(result.numAttemptedMeasurements).thenReturn(8)
        whenever(result.rssi).thenReturn(-48)
        whenever(result.rangingTimestampMillis).thenReturn(rangedMs)
        return result
    }

    private fun failure(bssid: String): RangingResult {
        val result: RangingResult = mock()
        whenever(result.macAddress).thenReturn(MacAddress.fromString(bssid))
        whenever(result.status).thenReturn(RangingResult.STATUS_FAIL)
        return result
    }

    private companion object {
        const val BSSID1 = "aa:bb:cc:dd:ee:01"
        const val BSSID2 = "aa:bb:cc:dd:ee:02"
        const val BSSID3 = "aa:bb:cc:dd:ee:03"
        const val BSSID4 = "aa:bb:cc:dd:ee:04"
        const val BSSID5 = "aa:bb:cc:dd:ee:05"
        const val FREQUENCY = 5180
        const val INTERVAL_MS = 1000L
        const val NOW_MS = 1_791_640_797_747L
        const val NOW_NANOS = 100_000_000_000L
        const val RANGED_MS = 98_500L
        const val SEEN_MICROS = 96_750_000L
        const val STALE_MICROS = 60_000_000L
    }
}
