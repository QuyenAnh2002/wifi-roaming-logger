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

import android.content.Intent
import android.content.pm.LabeledIntent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vrem.wifianalyzer.RobolectricUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.CINNAMON_BUN])
class CsvShareTest {
    private val mainActivity = RobolectricUtil.INSTANCE.mainActivity
    private val fixture = CsvShare(mainActivity) { Uri.parse("content://test.fileprovider/exports/" + it.name) }

    @Test
    fun prepareKeepsOnlyLatestExport() {
        // Arrange
        fixture.write("first.csv", "first")
        // Act
        val actual = fixture.write("second.csv", "second")
        // Assert
        assertThat(actual.readText()).isEqualTo("second")
        assertThat(actual.parentFile!!.listFiles()!!.map { it.name }).containsExactly("second.csv")
        assertThat(fixture.latest()).isEqualTo(actual)
    }

    @Test
    fun latestWithoutExport() {
        // Arrange
        fixture.write("first.csv", "first").parentFile!!.deleteRecursively()
        // Act & Assert
        assertThat(fixture.latest()).isNull()
    }

    @Test
    fun chooserSharesFileAndOffersSaveToFile() {
        // Arrange
        val file = fixture.write("export.csv", "data")
        val saveIntent = Intent("save")
        // Act
        val actual = fixture.chooser(file, saveIntent)
        // Assert
        assertThat(actual.action).isEqualTo(Intent.ACTION_CHOOSER)
        val send = actual.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertThat(send.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(send.type).isEqualTo(CsvShare.MIME_TYPE)
        assertThat(send.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("export.csv")
        assertThat(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotZero
        val uri = send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)!!
        assertThat(uri.scheme).isEqualTo("content")
        assertThat(uri.toString()).isEqualTo("content://test.fileprovider/exports/export.csv")
        assertThat(send.clipData!!.getItemAt(0).uri).isEqualTo(uri)
        assertAlternate(actual, Intent.ACTION_SEND)
        val initial = actual.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS, Parcelable::class.java)!!
        assertThat(initial).hasSize(1)
        val save = initial[0] as LabeledIntent
        assertThat(save.action).isEqualTo("save")
        assertThat(save.nonLocalizedLabel.toString()).isEqualTo("Save to file")
    }

    @Test
    fun prepareReturnsEmptyTargetInExportsDirectory() {
        // Act
        val actual = fixture.prepare("export.csv")
        // Assert
        assertThat(actual.name).isEqualTo("export.csv")
        assertThat(actual.exists()).isFalse
        assertThat(actual.parentFile!!.isDirectory).isTrue
    }

    @Test
    fun chooserSharesSingleRunFileAsOneDocument() {
        // Arrange
        val uri = Uri.parse("content://media/external/downloads/1")
        // Act
        val actual = fixture.chooser(listOf(uri))
        // Assert
        assertThat(actual.action).isEqualTo(Intent.ACTION_CHOOSER)
        val send = actual.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertThat(send.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(send.type).isEqualTo(CsvShare.MIME_TYPE)
        assertThat(send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)).isEqualTo(uri)
        assertThat(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotZero
        assertThat(send.clipData!!.getItemAt(0).uri).isEqualTo(uri)
        assertAlternate(actual, Intent.ACTION_SEND)
    }

    @Test
    fun chooserSharesMultipleFiles() {
        // Arrange
        val first = Uri.parse("content://media/external/downloads/1")
        val second = Uri.parse("content://media/external/downloads/2")
        // Act
        val actual = fixture.chooser(listOf(first, second))
        // Assert
        assertThat(actual.action).isEqualTo(Intent.ACTION_CHOOSER)
        val send = actual.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertThat(send.action).isEqualTo(Intent.ACTION_SEND_MULTIPLE)
        assertThat(send.type).isEqualTo(CsvShare.MIME_TYPE)
        val streams = send.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        assertThat(streams).containsExactly(first, second)
        assertThat(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotZero
        assertThat(send.clipData!!.itemCount).isEqualTo(2)
        assertAlternate(actual, Intent.ACTION_SEND_MULTIPLE)
    }

    private fun assertAlternate(
        chooser: Intent,
        action: String,
    ) {
        val alternates = chooser.getParcelableArrayExtra(Intent.EXTRA_ALTERNATE_INTENTS, Intent::class.java)!!
        assertThat(alternates).hasSize(1)
        val alternate = alternates[0]
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertThat(alternate.action).isEqualTo(action)
        assertThat(alternate.type).isEqualTo(CsvShare.ALTERNATE_MIME_TYPE)
        assertThat(alternate.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotZero
        assertThat(alternate.clipData!!.itemCount).isEqualTo(send.clipData!!.itemCount)
    }

    private fun CsvShare.write(
        fileName: String,
        data: String,
    ): File = prepare(fileName).apply { writeText(data) }
}
