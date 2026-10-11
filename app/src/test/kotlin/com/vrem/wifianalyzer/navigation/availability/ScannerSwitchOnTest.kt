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
package com.vrem.wifianalyzer.navigation.availability

import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.vrem.wifianalyzer.MainActivity
import com.vrem.wifianalyzer.R
import com.vrem.wifianalyzer.navigation.options.OptionMenu
import com.vrem.wifianalyzer.navigation.options.RunTimer
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class ScannerSwitchOnTest {
    private val mainActivity: MainActivity = mock()
    private val optionMenu: OptionMenu = mock()
    private val menu: Menu = mock()
    private val menuItem: MenuItem = mock()
    private val runTimer = RunTimer { 5_000L }
    private val fixture: NavigationOption = scannerMenuItem { runTimer }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(mainActivity)
        verifyNoMoreInteractions(optionMenu)
        verifyNoMoreInteractions(menu)
        verifyNoMoreInteractions(menuItem)
    }

    @Test
    fun navigationOptionScannerSwitchOn() {
        // Arrange
        whenever(mainActivity.optionMenu).thenReturn(optionMenu)
        whenever(optionMenu.menu).thenReturn(menu)
        whenever(menu.findItem(R.id.action_scanner)).thenReturn(menuItem)
        // Act
        fixture(mainActivity)
        // Assert
        verify(mainActivity).optionMenu
        verify(optionMenu).menu
        verify(menu).findItem(R.id.action_scanner)
        verify(menu, times(2)).findItem(R.id.action_run_timer)
        verify(menuItem).isVisible = true
        verify(menuItem).setTitle(R.string.scanner_play)
        verify(menuItem).setIcon(R.drawable.ic_play_arrow)
    }

    @Test
    fun navigationOptionScannerSwitchOnWithScannerRunningUpdateMenuItemIconAndTitle() {
        // Arrange
        runTimer.start()
        whenever(mainActivity.optionMenu).thenReturn(optionMenu)
        whenever(optionMenu.menu).thenReturn(menu)
        whenever(menu.findItem(R.id.action_scanner)).thenReturn(menuItem)
        // Act
        fixture(mainActivity)
        // Assert
        verify(mainActivity).optionMenu
        verify(optionMenu).menu
        verify(menu).findItem(R.id.action_scanner)
        verify(menu, times(2)).findItem(R.id.action_run_timer)
        verify(menuItem).isVisible = true
        verify(menuItem).setTitle(R.string.scanner_pause)
        verify(menuItem).setIcon(R.drawable.ic_pause)
    }

    @Test
    fun navigationOptionScannerSwitchOnWithScannerNotRunningUpdateMenuItemIconAndTitle() {
        // Arrange
        whenever(mainActivity.optionMenu).thenReturn(optionMenu)
        whenever(optionMenu.menu).thenReturn(menu)
        whenever(menu.findItem(R.id.action_scanner)).thenReturn(menuItem)
        // Act
        fixture(mainActivity)
        // Assert
        verify(mainActivity).optionMenu
        verify(optionMenu).menu
        verify(menu).findItem(R.id.action_scanner)
        verify(menu, times(2)).findItem(R.id.action_run_timer)
        verify(menuItem).isVisible = true
        verify(menuItem).setTitle(R.string.scanner_play)
        verify(menuItem).setIcon(R.drawable.ic_play_arrow)
    }

    @Test
    fun navigationOptionScannerSwitchOnWithNoMenuDoesNotSetVisibleTrue() {
        // Arrange
        whenever(mainActivity.optionMenu).thenReturn(optionMenu)
        whenever(optionMenu.menu).thenReturn(null)
        // Act
        fixture(mainActivity)
        // Assert
        verify(mainActivity).optionMenu
        verify(optionMenu).menu
        verify(menu, never()).findItem(R.id.action_scanner)
        verify(menuItem, never()).isVisible = true
    }

    @Test
    fun navigationOptionScannerSwitchOnShowsRunTimer() {
        // Arrange
        val runTimerMenuItem: MenuItem = mock()
        val runTimerView: TextView = mock()
        val runTimer = RunTimer { 5_000L }
        runTimer.start()
        whenever(mainActivity.optionMenu).thenReturn(optionMenu)
        whenever(optionMenu.menu).thenReturn(menu)
        whenever(menu.findItem(R.id.action_scanner)).thenReturn(menuItem)
        whenever(menu.findItem(R.id.action_run_timer)).thenReturn(runTimerMenuItem)
        whenever(runTimerMenuItem.actionView).thenReturn(runTimerView)
        // Act
        scannerMenuItem { runTimer }(mainActivity)
        // Assert
        verify(mainActivity).optionMenu
        verify(optionMenu).menu
        verify(menu).findItem(R.id.action_scanner)
        verify(menu, times(2)).findItem(R.id.action_run_timer)
        verify(menuItem).isVisible = true
        verify(menuItem).setTitle(R.string.scanner_pause)
        verify(menuItem).setIcon(R.drawable.ic_pause)
        verify(runTimerMenuItem).isVisible = true
        verify(runTimerView).text = "00:00:00"
        verify(runTimerView).alpha = 1f
    }

    @Test
    fun runTimerTextDimsStoppedTimer() {
        // Arrange
        val runTimerMenuItem: MenuItem = mock()
        val runTimerView: TextView = mock()
        var now = 0L
        val runTimer = RunTimer { now }
        runTimer.start()
        now = 65_000L
        runTimer.stop()
        whenever(menu.findItem(R.id.action_run_timer)).thenReturn(runTimerMenuItem)
        whenever(runTimerMenuItem.actionView).thenReturn(runTimerView)
        // Act
        runTimerText(menu, runTimer)
        // Assert
        verify(menu).findItem(R.id.action_run_timer)
        verify(runTimerView).text = "00:01:05"
        verify(runTimerView).alpha = 0.5f
    }
}
