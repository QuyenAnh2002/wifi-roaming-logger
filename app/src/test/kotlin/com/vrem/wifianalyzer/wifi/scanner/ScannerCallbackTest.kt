/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 * Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung (modifications)
 * Modified in 2026 from the original WiFiAnalyzer; changes are listed in README.md
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
package com.vrem.wifianalyzer.wifi.scanner

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import com.vrem.wifianalyzer.wifi.logger.RttRanger
import com.vrem.wifianalyzer.wifi.logger.ScanRecorder
import com.vrem.wifianalyzer.wifi.manager.WiFiManagerWrapper
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class ScannerCallbackTest {
    private val wiFiManagerWrapper: WiFiManagerWrapper = mock()
    private val cache: Cache = mock()
    private val scanRecorder: ScanRecorder = mock()
    private val rttRanger: RttRanger = mock()
    private val scanner: Scanner = mock()
    private val wifiInfo: WifiInfo = mock()
    private val scanResults: List<ScanResult> = listOf()
    private val fixture = ScannerCallback(wiFiManagerWrapper, cache, scanRecorder, rttRanger)

    @After
    fun tearDown() {
        verifyNoMoreInteractions(cache)
        verifyNoMoreInteractions(scanRecorder)
        verifyNoMoreInteractions(rttRanger)
        verifyNoMoreInteractions(scanner)
        verifyNoMoreInteractions(wiFiManagerWrapper)
    }

    @Test
    fun onSuccess() {
        // Arrange
        whenever(wiFiManagerWrapper.scanResults()).thenReturn(scanResults)
        whenever(wiFiManagerWrapper.wiFiInfo()).thenReturn(wifiInfo)
        whenever(wifiInfo.bssid).thenReturn(BSSID)
        // Act
        fixture.onSuccess()
        // Assert
        verify(wiFiManagerWrapper).scanResults()
        verify(wiFiManagerWrapper).wiFiInfo()
        verify(cache).add(scanResults)
        verify(cache).wifiInfo = wifiInfo
        verify(scanRecorder).record(scanResults, BSSID)
        verify(rttRanger).update(scanResults, BSSID)
    }

    @Test
    fun onSuccessWithoutConnection() {
        // Arrange
        whenever(wiFiManagerWrapper.scanResults()).thenReturn(scanResults)
        whenever(wiFiManagerWrapper.wiFiInfo()).thenReturn(null)
        // Act
        fixture.onSuccess()
        // Assert
        verify(wiFiManagerWrapper).scanResults()
        verify(wiFiManagerWrapper).wiFiInfo()
        verify(cache).add(scanResults)
        verify(cache).wifiInfo = null
        verify(scanRecorder).record(scanResults, null)
        verify(rttRanger).update(scanResults, null)
    }

    private companion object {
        const val BSSID = "aa:bb:cc:dd:ee:ff"
    }
}
