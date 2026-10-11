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
package com.vrem.wifianalyzer.navigation.items

import android.widget.Toast
import com.vrem.wifianalyzer.MainActivity
import com.vrem.wifianalyzer.MainContext
import com.vrem.wifianalyzer.R
import com.vrem.wifianalyzer.navigation.NavigationMenu
import com.vrem.wifianalyzer.wifi.logger.ScanRecorder

internal class ExportItem(
    private val scanRecorder: () -> ScanRecorder = MainContext.INSTANCE::scanRecorder,
) : NavigationItem {
    override fun activate(
        mainActivity: MainActivity,
        navigationMenu: NavigationMenu,
    ) {
        if (scanRecorder().empty) {
            Toast.makeText(mainActivity, R.string.no_data, Toast.LENGTH_LONG).show()
            return
        }
        mainActivity.exportCsv()
    }
}
