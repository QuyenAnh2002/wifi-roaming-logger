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

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.net.Uri
import android.os.Parcelable
import androidx.core.content.FileProvider
import com.vrem.annotation.OpenClass
import com.vrem.wifianalyzer.R
import java.io.File

@OpenClass
class CsvShare(
    private val context: Context,
    private val uriForFile: (File) -> Uri = {
        FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, it)
    },
) {
    fun write(
        fileName: String,
        data: String,
    ): File {
        val directory =
            exportsDirectory().apply {
                deleteRecursively()
                mkdirs()
            }
        return File(directory, fileName).apply { writeText(data) }
    }

    fun latest(): File? = exportsDirectory().listFiles()?.maxByOrNull { it.lastModified() }

    fun chooser(
        file: File,
        saveIntent: Intent,
    ): Intent {
        val uri = uriForFile(file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TITLE, file.name)
                clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        val save =
            LabeledIntent(
                saveIntent,
                context.packageName,
                context.getString(R.string.export_save_to_file),
                R.drawable.ic_import_export,
            )
        return Intent.createChooser(send, context.getString(R.string.action_export)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(save))
        }
    }

    fun chooser(uris: List<Uri>): Intent {
        val send =
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = MIME_TYPE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                clipData =
                    ClipData.newRawUri(null, uris.first()).apply {
                        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                    }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return Intent.createChooser(send, context.getString(R.string.run_share_title))
    }

    private fun exportsDirectory(): File = File(context.cacheDir, EXPORTS_DIRECTORY)

    companion object {
        const val MIME_TYPE = "text/csv"
        private const val EXPORTS_DIRECTORY = "exports"
        private const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
