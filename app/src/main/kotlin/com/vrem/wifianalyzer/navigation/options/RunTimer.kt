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
package com.vrem.wifianalyzer.navigation.options

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vrem.annotation.OpenClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@OpenClass
class RunTimer(
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private var startedAt: Long? = null
    private var stoppedAt: Long? = null

    val running: Boolean get() = startedAt != null && stoppedAt == null

    fun start() {
        startedAt = clock()
        stoppedAt = null
    }

    fun stop() {
        if (running) {
            stoppedAt = clock()
        }
    }

    fun reset() {
        startedAt = null
        stoppedAt = null
    }

    fun elapsedMs(): Long = startedAt?.let { (stoppedAt ?: clock()) - it } ?: 0L

    fun display(): String {
        val seconds = elapsedMs() / MILLIS_PER_SECOND
        return DISPLAY_FORMAT.format(
            Locale.US,
            seconds / SECONDS_PER_HOUR,
            seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
            seconds % SECONDS_PER_MINUTE,
        )
    }

    companion object {
        const val MILLIS_PER_SECOND = 1000L
        private const val SECONDS_PER_MINUTE = 60L
        private const val SECONDS_PER_HOUR = 3600L
        private const val DISPLAY_FORMAT = "%02d:%02d:%02d"
    }
}

fun LifecycleOwner.collectRunTimer(
    runTimer: RunTimer,
    update: (RunTimer) -> Unit,
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                update(runTimer)
                delay(RunTimer.MILLIS_PER_SECOND - runTimer.elapsedMs() % RunTimer.MILLIS_PER_SECOND)
            }
        }
    }
}
