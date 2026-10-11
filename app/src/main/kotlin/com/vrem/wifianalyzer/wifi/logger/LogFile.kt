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
package com.vrem.wifianalyzer.wifi.logger

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.vrem.annotation.OpenClass
import com.vrem.util.buildMinVersionQ
import com.vrem.wifianalyzer.export.CSV_BOM
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One CSV file in `Download/WiFiAnalyzer`. Connection rows and scan rows are written from different threads,
 * so every operation is synchronized and each [write] call lands as one uninterrupted block.
 */
@OpenClass
class LogFile(
    private val context: Context,
    private val prefix: String,
) {
    private var writer: Writer? = null

    @Volatile
    private var currentUri: Uri? = null

    val uri: Uri? get() = currentUri

    val opened: Boolean
        @Synchronized get() = writer != null

    @Synchronized
    fun open(header: String) {
        currentUri = null
        writer = runCatching { outputStream(fileName()) }.getOrNull()?.bufferedWriter(Charsets.UTF_8)
        write(CSV_BOM + header)
    }

    @Synchronized
    fun write(text: String) {
        val writer = writer ?: return
        // Storage can fill up or be revoked mid-run; losing a row must not crash the measurement.
        runCatching {
            writer.write(text)
            writer.flush()
        }
    }

    @Synchronized
    fun close() {
        runCatching { writer?.close() }
        writer = null
    }

    private fun fileName(): String = "$prefix-${SimpleDateFormat(FILE_NAME_FORMAT, Locale.US).format(Date())}.csv"

    private fun outputStream(fileName: String): OutputStream? =
        if (buildMinVersionQ()) {
            outputStreamDownloads(fileName)
        } else {
            outputStreamLegacy(fileName)
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun outputStreamDownloads(fileName: String): OutputStream? {
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER)
            }
        val resolver = context.contentResolver
        val inserted = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        currentUri = inserted
        return resolver.openOutputStream(inserted)
    }

    private fun outputStreamLegacy(fileName: String): OutputStream? {
        val file = context.getExternalFilesDir(null)?.let { File(it, fileName) } ?: return null
        currentUri =
            runCatching {
                FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
            }.getOrNull()
        return FileOutputStream(file)
    }

    private companion object {
        const val FOLDER = "WiFiAnalyzer"
        const val MIME_TYPE = "text/csv"
        const val FILE_NAME_FORMAT = "yyyyMMdd-HHmmss"
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
