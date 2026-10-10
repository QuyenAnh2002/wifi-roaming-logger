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

import android.net.wifi.ScanResult
import android.net.wifi.ScanResult.InformationElement
import android.net.wifi.WifiInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vrem.wifianalyzer.RobolectricUtil
import com.vrem.wifianalyzer.export.CsvExport
import com.vrem.wifianalyzer.wifi.manager.WiFiManagerWrapper
import com.vrem.wifianalyzer.wifi.model.WiFiDetail
import com.vrem.wifianalyzer.wifi.model.WiFiWidth
import com.vrem.wifianalyzer.wifi.scanner.whenSsid
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.CINNAMON_BUN])
class ScanLoggerTest {
    private val mainActivity = RobolectricUtil.INSTANCE.mainActivity
    private val wiFiManagerWrapper: WiFiManagerWrapper = mock()
    private val csvExport: CsvExport = mock()
    private val logFile: LogFile = mock()
    private val location = LogLocation(21.0, 105.8, 5f, null, null, "gps", 0L)
    private val fixture = ScanLogger(mainActivity, wiFiManagerWrapper, { location }, logFile, csvExport)

    @Test
    fun logWritesEveryBssidIncludingSameSsid() {
        // Arrange
        withScanResults(scanResult("SSID", "BSSID1", 100L), scanResult("SSID", "BSSID2", 100L))
        withConnectedBssid("BSSID2")
        // Act
        fixture.log()
        // Assert
        val details = argumentCaptor<WiFiDetail>()
        val connected = argumentCaptor<Boolean>()
        verify(logFile, never()).open(any())
        verify(csvExport, times(2)).row(eq(mainActivity), any(), details.capture(), connected.capture(), eq(location))
        verify(logFile, times(2)).write(ROW)
        assertThat(details.allValues.map { it.wiFiIdentifier.ssid }).containsExactly("SSID", "SSID")
        assertThat(details.allValues.map { it.wiFiIdentifier.bssid }).containsExactly("BSSID1", "BSSID2")
        assertThat(connected.allValues).containsExactly(false, true)
    }

    @Test
    fun logSkipsObservationsAlreadyLogged() {
        // Arrange
        val first = scanResult("SSID", "BSSID1", 100L)
        val second = scanResult("SSID", "BSSID2", 100L)
        withScanResults(first, second)
        withConnectedBssid(null)
        fixture.log()
        // Act
        fixture.log()
        second.timestamp = 200L
        fixture.log()
        // Assert
        verify(logFile, times(3)).write(ROW)
    }

    @Test
    fun resetForgetsLoggedObservationsButKeepsSharedFileOpen() {
        // Arrange
        withScanResults(scanResult("SSID", "BSSID1", 100L))
        withConnectedBssid(null)
        fixture.log()
        // Act
        fixture.reset()
        fixture.log()
        // Assert
        verify(logFile, never()).close()
        verify(logFile, times(2)).write(ROW)
    }

    @Test
    fun startLogsCurrentScanResultsAndStopUnregisters() {
        // Arrange
        withScanResults()
        withConnectedBssid(null)
        // Act
        fixture.start(Handler(Looper.getMainLooper()))
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fixture.stop()
        // Assert
        verify(wiFiManagerWrapper).scanResults()
    }

    private fun withScanResults(vararg scanResults: ScanResult) {
        whenever(wiFiManagerWrapper.scanResults()).thenReturn(scanResults.toList())
        whenever(csvExport.row(any(), any(), any(), any(), any())).thenReturn(ROW)
    }

    private fun withConnectedBssid(bssid: String?) {
        val wifiInfo: WifiInfo = mock()
        doReturn(bssid).whenever(wifiInfo).bssid
        whenever(wiFiManagerWrapper.wiFiInfo()).thenReturn(wifiInfo)
    }

    private fun scanResult(
        ssid: String,
        bssid: String,
        timestamp: Long,
    ): ScanResult {
        val scanResult: ScanResult = mock()
        whenSsid(scanResult, ssid)
        scanResult.BSSID = bssid
        scanResult.timestamp = timestamp
        scanResult.frequency = FREQUENCY
        scanResult.centerFreq0 = FREQUENCY
        scanResult.level = LEVEL
        scanResult.channelWidth = WiFiWidth.MHZ_20.channelWidth
        doReturn(0).whenever(scanResult).wifiStandard
        doReturn(intArrayOf()).whenever(scanResult).securityTypes
        doReturn(listOf<InformationElement>()).whenever(scanResult).informationElements
        return scanResult
    }

    private companion object {
        const val ROW = "row"
        const val FREQUENCY = 2437
        const val LEVEL = -55
    }
}
