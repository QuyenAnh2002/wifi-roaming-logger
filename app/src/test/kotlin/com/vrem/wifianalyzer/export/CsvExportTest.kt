/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 * Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung
 * Added in 2026 to this modified version of WiFiAnalyzer; see README.md
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
package com.vrem.wifianalyzer.export

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vrem.wifianalyzer.RobolectricUtil
import com.vrem.wifianalyzer.vendor.model.VendorService
import com.vrem.wifianalyzer.wifi.logger.LogLocation
import com.vrem.wifianalyzer.wifi.model.FastRoaming
import com.vrem.wifianalyzer.wifi.model.WiFiDetail
import com.vrem.wifianalyzer.wifi.model.WiFiIdentifier
import com.vrem.wifianalyzer.wifi.model.WiFiSecurity
import com.vrem.wifianalyzer.wifi.model.WiFiSignal
import com.vrem.wifianalyzer.wifi.model.WiFiSignalExtra
import com.vrem.wifianalyzer.wifi.model.WiFiStandard
import com.vrem.wifianalyzer.wifi.model.WiFiWidth
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.GregorianCalendar

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.CINNAMON_BUN])
class CsvExportTest {
    private val context = RobolectricUtil.INSTANCE.mainActivity
    private val vendorService: VendorService = mock()
    private val fixture = CsvExport { vendorService }
    private val date = GregorianCalendar(2026, Calendar.OCTOBER, 10, 9, 8, 7).time
    private val connectedSignal =
        WiFiSignal(
            5745,
            5775,
            WiFiWidth.MHZ_80,
            -30,
            WiFiSignalExtra(true, WiFiStandard.AC, listOf(FastRoaming.FR_802_11R)),
        )
    private val connected =
        WiFiDetail(WiFiIdentifier("Lab, 2", "78:20:51:fe:3b:e9"), WiFiSecurity("[WPA2-PSK-CCMP][ESS]"), connectedSignal)
    private val other =
        WiFiDetail(
            WiFiIdentifier("", "56:3a:0e:32:4e:99"),
            WiFiSecurity("[ESS]"),
            WiFiSignal(2437, 2437, WiFiWidth.MHZ_20, -70),
        )
    private val location = LogLocation(20.98765432, 105.7654321, 4.5f, null, null, "gps", 250L)

    @Test
    fun rowOfConnectedAccessPoint() {
        // Arrange
        whenever(vendorService.findVendorName("78:20:51:fe:3b:e9")).thenReturn("TP-LINK")
        val standard = context.getString(WiFiStandard.AC.fullResource)
        val fastRoaming = context.getString(FastRoaming.FR_802_11R.textResource)
        val securities = connected.wiFiSecurity.securities.joinToString(" ") { it.name }
        // Act
        val actual = fixture.row(context, date, connected, true, location)
        // Assert
        assertThat(actual).startsWith("${date.time},2026-10-10T09:08:07.000")
        assertThat(actual).endsWith(
            ",SCAN,,\"Lab, 2\",78:20:51:fe:3b:e9,true,-30,5745,149,155,5775,80,5735,5815,,," +
                "0.1,0.1,0.2,0.3,0.3,,,,,,,$standard,$securities,[WPA2-PSK-CCMP][ESS],$fastRoaming,true,TP-LINK," +
                "20.9876543,105.7654321,4.5,,,gps,250\r\n",
        )
    }

    @Test
    fun rowOfHiddenAccessPoint() {
        // Arrange
        whenever(vendorService.findVendorName("56:3a:0e:32:4e:99")).thenReturn("")
        val standard = context.getString(WiFiStandard.UNKNOWN.fullResource)
        val securities = other.wiFiSecurity.securities.joinToString(" ") { it.name }
        // Act
        val actual = fixture.row(context, date, other, false, location)
        // Assert
        assertThat(actual).endsWith(
            ",SCAN,,*hidden*,56:3a:0e:32:4e:99,false,-70,2437,6,6,2437,20,2427,2447,,," +
                "30.9,30.9,15.6,9.9,7.1,,,,,,,$standard,$securities,[ESS],,false,," +
                "20.9876543,105.7654321,4.5,,,gps,250\r\n",
        )
    }

    @Test
    fun rowWithoutLocation() {
        // Arrange
        whenever(vendorService.findVendorName("78:20:51:fe:3b:e9")).thenReturn("TP-LINK")
        // Act
        val actual = fixture.row(context, date, connected, true, null)
        // Assert
        assertThat(actual).endsWith(",true,TP-LINK,,,,,,,\r\n")
    }

    @Test
    fun rowComputesBandEdgesOutsideTheChannelTable() {
        // Arrange
        whenever(vendorService.findVendorName(any())).thenReturn("")
        val channel14 = other.copy(wiFiSignal = WiFiSignal(2484, 2484, WiFiWidth.MHZ_20, -70))
        // Act
        val actual = fixture.row(context, date, channel14, false, null)
        // Assert
        assertThat(actual).contains(",false,-70,2484,,,2484,20,2474,2494,,,")
    }

    @Test
    fun rowLeavesSentinelValuesEmpty() {
        // Arrange
        whenever(vendorService.findVendorName(any())).thenReturn("")
        val unknown = other.copy(wiFiSignal = WiFiSignal(0, 0, WiFiWidth.MHZ_20, -127))
        // Act
        val actual = fixture.row(context, date, unknown, false, null)
        // Assert
        assertThat(actual).contains(",false,,,,,,20,,," + ",".repeat(7))
    }

    @Test
    fun fileName() {
        // Act
        val actual = fixture.fileName(date)
        // Assert
        assertThat(actual).isEqualTo("WiFiAnalyzer-20261010-090807.csv")
    }
}
