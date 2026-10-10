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
package com.vrem.wifianalyzer.navigation.availability

import android.view.Menu
import android.widget.TextView
import com.vrem.wifianalyzer.MainContext
import com.vrem.wifianalyzer.R
import com.vrem.wifianalyzer.navigation.options.RunTimer

private const val RUN_TIMER_RUNNING_ALPHA = 1f
private const val RUN_TIMER_STOPPED_ALPHA = 0.5f

internal val navigationOptionScannerSwitchOff: NavigationOption = {
    it.optionMenu.menu?.let { menu ->
        menu.findItem(R.id.action_scanner).isVisible = false
        menu.findItem(R.id.action_run_timer)?.isVisible = false
    }
}

internal fun scannerMenuItem(runTimer: () -> RunTimer = MainContext.INSTANCE::runTimer): NavigationOption =
    {
        it.optionMenu.menu?.let { menu ->
            val menuItem = menu.findItem(R.id.action_scanner)
            menuItem.isVisible = true
            if (runTimer().running) {
                menuItem.setTitle(R.string.scanner_pause)
                menuItem.setIcon(R.drawable.ic_pause)
            } else {
                menuItem.setTitle(R.string.scanner_play)
                menuItem.setIcon(R.drawable.ic_play_arrow)
            }
            menu.findItem(R.id.action_run_timer)?.isVisible = true
            runTimerText(menu, runTimer())
        }
    }

internal fun runTimerText(
    menu: Menu,
    runTimer: RunTimer,
) {
    (menu.findItem(R.id.action_run_timer)?.actionView as? TextView)?.apply {
        text = runTimer.display()
        alpha = if (runTimer.running) RUN_TIMER_RUNNING_ALPHA else RUN_TIMER_STOPPED_ALPHA
    }
}

internal val navigationOptionScannerSwitchOn: NavigationOption = scannerMenuItem()
