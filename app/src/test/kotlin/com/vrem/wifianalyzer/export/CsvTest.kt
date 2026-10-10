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
package com.vrem.wifianalyzer.export

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CsvTest {
    @Test
    fun csvEscape() {
        // Act & Assert
        assertThat(csvEscape("plain")).isEqualTo("plain")
        assertThat(csvEscape("a,b")).isEqualTo("\"a,b\"")
        assertThat(csvEscape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"")
        assertThat(csvEscape("line\nbreak")).isEqualTo("\"line\nbreak\"")
    }

    @Test
    fun csvLine() {
        // Act & Assert
        assertThat(csvLine(listOf("a", "b,c", ""))).isEqualTo("a,\"b,c\",\r\n")
    }

    @Test
    fun csvFormat() {
        // Act & Assert
        assertThat(20.987654321.csvFormat("%.7f")).isEqualTo("20.9876543")
        assertThat(4.5f.csvFormat("%.1f")).isEqualTo("4.5")
        assertThat((null as Double?).csvFormat("%.1f")).isEmpty()
    }

    @Test
    fun distanceHeaders() {
        // Act & Assert
        assertThat(DISTANCE_HEADERS).containsExactly(
            "distance_fspl_m",
            "distance_logdistance_2",
            "distance_logdistance_2_5",
            "distance_logdistance_3",
            "distance_logdistance_3_5",
        )
    }

    @Test
    fun distanceFields() {
        // Act & Assert
        assertThat(distanceFields(5500, -80)).containsExactly("43.4", "43.4", "20.4", "12.3", "8.6")
    }

    @Test
    fun distanceFieldsWithoutValidRssi() {
        // Act & Assert
        assertThat(distanceFields(5500, -127)).containsExactly("", "", "", "", "")
        assertThat(distanceFields(5500, null)).containsExactly("", "", "", "", "")
        assertThat(distanceFields(null, -80)).containsExactly("", "", "", "", "")
    }

    @Test
    fun csvBom() {
        // Act & Assert
        assertThat(CSV_BOM).isEqualTo("﻿")
    }
}
