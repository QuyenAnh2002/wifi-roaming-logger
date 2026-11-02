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

import com.vrem.wifianalyzer.wifi.logger.LogLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CsvRowTest {
    @Test
    fun csvHeaderHasAllColumnsInOrder() {
        // Act & Assert
        assertThat(CSV_HEADER).isEqualTo(
            "epoch_ms,time,row_type,event,ssid,bssid,connected,rssi_dbm,frequency_mhz,primary_channel," +
                "center_channel,center_frequency_mhz,width_mhz,range_start_mhz,range_end_mhz,link_speed_mbps," +
                "supplicant_state,distance_fspl_m,distance_logdistance_2,distance_logdistance_2_5," +
                "distance_logdistance_3,distance_logdistance_3_5,distance_rtt_m,distance_rtt_stddev_m,rtt_status," +
                "rtt_mode,rtt_success_count,rtt_attempt_count,standard,security,capabilities,fast_roaming," +
                "is_80211mc,vendor,latitude,longitude,accuracy_m,altitude_m,speed_mps,location_provider," +
                "location_age_ms\r\n",
        )
    }

    @Test
    fun distanceColumnsMatchDistanceHeaders() {
        // Act & Assert
        assertThat(DISTANCE_COLUMNS.map { it.header }).isEqualTo(DISTANCE_HEADERS)
    }

    @Test
    fun csvRowLeavesMissingColumnsEmpty() {
        // Act
        val actual = csvRow(mapOf(CsvColumn.ROW_TYPE to ROW_TYPE_SCAN, CsvColumn.LOCATION_AGE to "7"))
        // Assert
        assertThat(actual).isEqualTo(",,SCAN" + ",".repeat(38) + "7\r\n")
    }

    @Test
    fun distanceValues() {
        // Act & Assert
        assertThat(distanceValues(5500, -80)).isEqualTo(
            mapOf(
                CsvColumn.DISTANCE_FSPL to "43.4",
                CsvColumn.DISTANCE_LOG_2 to "43.4",
                CsvColumn.DISTANCE_LOG_2_5 to "20.4",
                CsvColumn.DISTANCE_LOG_3 to "12.3",
                CsvColumn.DISTANCE_LOG_3_5 to "8.6",
            ),
        )
    }

    @Test
    fun locationValues() {
        // Arrange
        val location = LogLocation(20.98765432, 105.7654321, 4.5f, 12.34, 1.25f, "gps", 250L)
        // Act
        val actual = locationValues(location)
        // Assert
        assertThat(actual[CsvColumn.LATITUDE]).isEqualTo("20.9876543")
        assertThat(actual[CsvColumn.LONGITUDE]).isEqualTo("105.7654321")
        assertThat(actual[CsvColumn.ACCURACY]).isEqualTo("4.5")
        assertThat(actual[CsvColumn.ALTITUDE]).isEqualTo("12.3")
        assertThat(actual[CsvColumn.SPEED]).isEqualTo("1.25")
        assertThat(actual[CsvColumn.LOCATION_PROVIDER]).isEqualTo("gps")
        assertThat(actual[CsvColumn.LOCATION_AGE]).isEqualTo("250")
    }

    @Test
    fun locationValuesWithoutLocation() {
        // Act & Assert
        assertThat(locationValues(null).values).allMatch { it.isEmpty() }
    }

    @Test
    fun timeValues() {
        // Act
        val actual = timeValues(1000L)
        // Assert
        assertThat(actual[CsvColumn.EPOCH_MS]).isEqualTo("1000")
        assertThat(actual[CsvColumn.TIME]).startsWith("1970-01-01T")
    }
}
