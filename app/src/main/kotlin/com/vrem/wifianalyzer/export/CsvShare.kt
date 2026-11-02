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
package com.vrem.wifianalyzer.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.vrem.annotation.OpenClass
import com.vrem.util.buildMinVersionQ
import com.vrem.wifianalyzer.R
import java.io.File

@OpenClass
class CsvShare(
    private val context: Context,
    private val uriForFile: (File) -> Uri = {
        FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, it)
    },
) {
    /** Returns where a new export named [fileName] goes; earlier exports are removed so only one is kept. */
    fun prepare(fileName: String): File {
        val directory =
            exportsDirectory().apply {
                deleteRecursively()
                mkdirs()
            }
        return File(directory, fileName)
    }

    fun latest(): File? = exportsDirectory().listFiles()?.maxByOrNull { it.lastModified() }

    fun chooser(
        file: File,
        saveIntent: Intent,
    ): Intent {
        val save =
            LabeledIntent(
                saveIntent,
                context.packageName,
                context.getString(R.string.export_save_to_file),
                R.drawable.ic_import_export,
            )
        return chooser(listOf(uriForFile(file)), file.name, context.getString(R.string.action_export)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(save))
        }
    }

    fun chooser(uris: List<Uri>): Intent =
        chooser(uris, uris.singleOrNull()?.let(::displayName), context.getString(R.string.run_share_title))

    /**
     * Many messaging apps (Messenger, Zalo, ...) do not list `text/csv` in their share filters, so the same files
     * are also offered as [ALTERNATE_MIME_TYPE]; the share sheet then shows apps that accept either type.
     */
    private fun chooser(
        uris: List<Uri>,
        name: String?,
        title: String,
    ): Intent =
        Intent.createChooser(send(uris, name, MIME_TYPE), title).apply {
            if (buildMinVersionQ()) {
                putExtra(Intent.EXTRA_ALTERNATE_INTENTS, arrayOf(send(uris, name, ALTERNATE_MIME_TYPE)))
            }
        }

    // A single file goes as ACTION_SEND: far more apps accept one document that way than via ACTION_SEND_MULTIPLE.
    private fun send(
        uris: List<Uri>,
        name: String?,
        mimeType: String,
    ): Intent =
        Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            name?.let {
                putExtra(Intent.EXTRA_SUBJECT, it)
                putExtra(Intent.EXTRA_TITLE, it)
            }
            // Explicit types: the receiving app is granted read access through this ClipData.
            clipData =
                ClipData(name.orEmpty(), arrayOf(mimeType), ClipData.Item(uris.first())).apply {
                    uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun displayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull()

    private fun exportsDirectory(): File = File(context.cacheDir, EXPORTS_DIRECTORY)

    companion object {
        const val MIME_TYPE = "text/csv"
        const val ALTERNATE_MIME_TYPE = "application/vnd.ms-excel"
        private const val EXPORTS_DIRECTORY = "exports"
        private const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
