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

import android.net.wifi.ScanResult
import android.net.wifi.ScanResult.InformationElement
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vrem.wifianalyzer.RobolectricUtil
import com.vrem.wifianalyzer.export.CSV_BOM
import com.vrem.wifianalyzer.export.CSV_HEADER
import com.vrem.wifianalyzer.export.CsvExport
import com.vrem.wifianalyzer.wifi.model.WiFiDetail
import com.vrem.wifianalyzer.wifi.model.WiFiWidth
import com.vrem.wifianalyzer.wifi.scanner.whenSsid
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import java.io.File
import java.util.Date
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.CINNAMON_BUN])
class ScanRecorderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val mainActivity = RobolectricUtil.INSTANCE.mainActivity
    private val csvExport: CsvExport = mock()
    private val logFile: LogFile = mock()
    private val location = LogLocation(21.0, 105.8, 5f, null, null, "gps", 0L)
    private val locationRequests = mutableListOf<Long>()
    private val directExecutor = Executor { it.run() }
    private lateinit var history: File
    private lateinit var fixture: ScanRecorder

    @Before
    fun setUp() {
        history = File(temporaryFolder.root, "history.csv").apply { writeText("previous session") }
        fixture =
            ScanRecorder(
                mainActivity,
                { locationRequests.add(it).let { location } },
                history,
                directExecutor,
                directExecutor,
                csvExport,
                { NOW_MS },
                { NOW_NANOS },
            )
        whenever(csvExport.row(any(), any(), any(), any(), any())).thenAnswer {
            (it.arguments[2] as WiFiDetail).wiFiIdentifier.bssid + ROW_END
        }
    }

    @Test
    fun startsWithEmptyHistoryOfThisSession() {
        // Assert
        assertThat(history.exists()).isFalse
        assertThat(fixture.empty).isTrue
    }

    @Test
    fun recordWritesEveryBssidIncludingSameSsidInObservationOrder() {
        // Arrange
        val late = scanResult("SSID", "BSSID2", SEEN_MICROS + 1_000_000)
        val early = scanResult("SSID", "BSSID1", SEEN_MICROS)
        // Act
        fixture.record(listOf(late, early), "bssid2")
        // Assert
        val details = argumentCaptor<WiFiDetail>()
        val connected = argumentCaptor<Boolean>()
        verify(csvExport, times(2)).row(eq(mainActivity), any(), details.capture(), connected.capture(), eq(location))
        assertThat(details.allValues.map { it.wiFiIdentifier.bssid }).containsExactly("BSSID1", "BSSID2")
        assertThat(details.allValues.map { it.wiFiIdentifier.ssid }).containsExactly("SSID", "SSID")
        assertThat(connected.allValues).containsExactly(false, true)
        assertThat(history.readText()).isEqualTo("BSSID1$ROW_END" + "BSSID2$ROW_END")
        assertThat(fixture.empty).isFalse
    }

    @Test
    fun recordStampsRowsWithTheMomentTheBssidWasSeen() {
        // Arrange
        val dates = argumentCaptor<Date>()
        // Act
        fixture.record(listOf(scanResult("SSID", "BSSID1", SEEN_MICROS)), null)
        // Assert
        verify(csvExport).row(any(), dates.capture(), any(), any(), any())
        assertThat(dates.firstValue.time).isEqualTo(NOW_MS - 3_250L)
        assertThat(locationRequests).containsExactly(SEEN_MICROS * 1_000L)
    }

    @Test
    fun recordUsesRawLevelOfTheScan() {
        // Arrange
        val details = argumentCaptor<WiFiDetail>()
        // Act
        fixture.record(listOf(scanResult("SSID", "BSSID1", SEEN_MICROS, -83)), null)
        // Assert
        verify(csvExport).row(any(), any(), details.capture(), any(), any())
        assertThat(details.firstValue.wiFiSignal.level).isEqualTo(-83)
    }

    @Test
    fun recordFallsBackToNowWhenTimestampIsMissingOrInTheFuture() {
        // Arrange
        val dates = argumentCaptor<Date>()
        // Act
        fixture.record(
            listOf(scanResult("SSID", "BSSID1", 0L), scanResult("SSID", "BSSID2", NOW_NANOS / 1_000L + 1)),
            null,
        )
        // Assert
        verify(csvExport, times(2)).row(any(), dates.capture(), any(), any(), any())
        assertThat(dates.allValues.map { it.time }).containsOnly(NOW_MS)
        assertThat(locationRequests).containsOnly(NOW_NANOS)
    }

    @Test
    fun recordSkipsObservationsAlreadyRecorded() {
        // Arrange
        val first = scanResult("SSID", "BSSID1", SEEN_MICROS)
        val second = scanResult("SSID", "BSSID2", SEEN_MICROS)
        fixture.record(listOf(first, second), null)
        // Act
        fixture.record(listOf(first, second), null)
        second.timestamp = SEEN_MICROS + 1
        fixture.record(listOf(first, second), null)
        // Assert
        verify(csvExport, times(3)).row(any(), any(), any(), any(), any())
        assertThat(history.readLines()).hasSize(3)
    }

    @Test
    fun recordWithoutNewObservationsWritesNothing() {
        // Act
        fixture.record(listOf(), null)
        // Assert
        assertThat(history.exists()).isFalse
        assertThat(fixture.empty).isTrue
    }

    @Test
    fun recordAlsoWritesToActiveRunLog() {
        // Arrange
        fixture.runLog = logFile
        // Act
        fixture.record(listOf(scanResult("SSID", "BSSID1", SEEN_MICROS)), null)
        fixture.runLog = null
        fixture.record(listOf(scanResult("SSID", "BSSID2", SEEN_MICROS)), null)
        // Assert
        verify(logFile).write("BSSID1$ROW_END")
        verify(logFile, never()).write("BSSID2$ROW_END")
        assertThat(history.readLines()).containsExactly("BSSID1", "BSSID2")
    }

    @Test
    fun appendRowsWritesToHistoryAndActiveRunLog() {
        // Arrange
        fixture.runLog = logFile
        // Act
        fixture.appendRows("RTT1$ROW_END")
        fixture.appendRows("")
        // Assert
        verify(logFile).write("RTT1$ROW_END")
        assertThat(history.readText()).isEqualTo("RTT1$ROW_END")
        assertThat(fixture.empty).isFalse
    }

    @Test
    fun exportWritesHeaderAndWholeSessionHistory() {
        // Arrange
        fixture.record(listOf(scanResult("SSID", "BSSID1", SEEN_MICROS)), null)
        fixture.record(listOf(scanResult("SSID", "BSSID2", SEEN_MICROS + 5_000_000)), null)
        val target = File(temporaryFolder.root, "export.csv")
        var success: Boolean? = null
        // Act
        fixture.export(target) { success = it }
        // Assert
        assertThat(success).isTrue
        assertThat(target.readText()).isEqualTo(CSV_BOM + CSV_HEADER + "BSSID1$ROW_END" + "BSSID2$ROW_END")
    }

    @Test
    fun exportWithoutHistoryWritesHeaderOnly() {
        // Arrange
        val target = File(temporaryFolder.root, "export.csv")
        var success: Boolean? = null
        // Act
        fixture.export(target) { success = it }
        // Assert
        assertThat(success).isTrue
        assertThat(target.readText()).isEqualTo(CSV_BOM + CSV_HEADER)
    }

    @Test
    fun exportReportsFailure() {
        // Arrange
        val target = File(temporaryFolder.root, "missing/export.csv")
        var success: Boolean? = null
        // Act
        fixture.export(target) { success = it }
        // Assert
        assertThat(success).isFalse
    }

    private fun scanResult(
        ssid: String,
        bssid: String,
        timestamp: Long,
        level: Int = LEVEL,
    ): ScanResult {
        val scanResult: ScanResult = mock()
        whenSsid(scanResult, ssid)
        scanResult.BSSID = bssid
        scanResult.timestamp = timestamp
        scanResult.frequency = FREQUENCY
        scanResult.centerFreq0 = FREQUENCY
        scanResult.level = level
        scanResult.channelWidth = WiFiWidth.MHZ_20.channelWidth
        doReturn(0).whenever(scanResult).wifiStandard
        doReturn(intArrayOf()).whenever(scanResult).securityTypes
        doReturn(listOf<InformationElement>()).whenever(scanResult).informationElements
        return scanResult
    }

    private companion object {
        const val ROW_END = "\r\n"
        const val FREQUENCY = 2437
        const val LEVEL = -55
        const val NOW_MS = 1_791_640_797_747L
        const val NOW_NANOS = 100_000_000_000L
        const val SEEN_MICROS = 96_750_000L
    }
}
