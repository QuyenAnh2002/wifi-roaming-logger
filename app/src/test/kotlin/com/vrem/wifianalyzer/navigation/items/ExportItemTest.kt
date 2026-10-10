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
package com.vrem.wifianalyzer.navigation.items

import android.os.Build
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vrem.wifianalyzer.RobolectricUtil
import com.vrem.wifianalyzer.navigation.NavigationMenu
import com.vrem.wifianalyzer.wifi.model.WiFiConnection
import com.vrem.wifianalyzer.wifi.model.WiFiData
import com.vrem.wifianalyzer.wifi.model.WiFiDetail
import com.vrem.wifianalyzer.wifi.scanner.ScannerService
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.CINNAMON_BUN])
class ExportItemTest {
    private val mainActivity = RobolectricUtil.INSTANCE.mainActivity
    private val scanner: ScannerService = mock()

    private val fixture = ExportItem { scanner }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(scanner)
    }

    @Test
    fun registered() {
        // Act & Assert
        assertThat(fixture.registered).isFalse
    }

    @Test
    fun visibility() {
        // Act & Assert
        assertThat(fixture.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun activate() {
        // Arrange
        val activity = spy(mainActivity)
        val wiFiData = withWiFiData()
        doReturn(MutableStateFlow(wiFiData)).whenever(scanner).wiFiData()
        doNothing().whenever(activity).exportCsv(wiFiData)
        // Act
        fixture.activate(activity, NavigationMenu.EXPORT)
        // Assert
        verify(scanner).wiFiData()
        verify(activity).exportCsv(wiFiData)
    }

    @Test
    fun activateWithNoWiFiData() {
        // Arrange
        val activity = spy(mainActivity)
        val wiFiData = WiFiData(listOf(), WiFiConnection.EMPTY)
        doReturn(MutableStateFlow(wiFiData)).whenever(scanner).wiFiData()
        // Act
        fixture.activate(activity, NavigationMenu.EXPORT)
        // Assert
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("No Data")
        verify(scanner).wiFiData()
        verify(activity, never()).exportCsv(any())
    }

    private fun withWiFiData(): WiFiData = WiFiData(listOf(WiFiDetail.EMPTY), WiFiConnection.EMPTY)
}
