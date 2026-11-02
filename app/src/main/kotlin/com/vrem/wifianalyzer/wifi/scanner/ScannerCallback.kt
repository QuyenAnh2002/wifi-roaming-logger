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

import com.vrem.annotation.OpenClass
import com.vrem.wifianalyzer.wifi.logger.RttRanger
import com.vrem.wifianalyzer.wifi.logger.ScanRecorder
import com.vrem.wifianalyzer.wifi.manager.WiFiManagerWrapper

@OpenClass
internal class ScannerCallback(
    private val wiFiManagerWrapper: WiFiManagerWrapper,
    private val cache: Cache,
    private val scanRecorder: ScanRecorder,
    private val rttRanger: RttRanger,
) : Callback {
    override fun onSuccess() {
        val scanResults = wiFiManagerWrapper.scanResults()
        val wifiInfo = wiFiManagerWrapper.wiFiInfo()
        cache.add(scanResults)
        cache.wifiInfo = wifiInfo
        scanRecorder.record(scanResults, wifiInfo?.bssid)
        rttRanger.update(scanResults, wifiInfo?.bssid)
    }
}
