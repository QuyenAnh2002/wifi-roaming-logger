# WiFiAnalyzer – modified version (CSV logging, location, Wi-Fi RTT)

> **This is not the official WiFiAnalyzer repository.** It is a modified version of [WiFiAnalyzer](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer), open-source software by VREM Software Development, released under the [GNU GPLv3](LICENSE). This version is not released, endorsed or supported by the original authors. Please report problems with this version here, not to the original repository.

This version keeps all Wi-Fi analysis features of the original. In addition, it records the current connection, every scanned BSSID, the device location and Wi-Fi RTT distance measurements over time into CSV files, and shares those files through the Android share sheet.

**Note:** like the original, WiFiAnalyzer is not a Wi-Fi password cracking or phishing tool.

## Table of Contents

1. [Origin](#1-origin)
2. [License](#2-license)
3. [Privacy](#3-privacy)
4. [Changes from the original](#4-changes-from-the-original)
5. [Build and test](#5-build-and-test)
6. [Known limitations](#6-known-limitations)

Using the app on a phone and the CSV file format: [CSV_FORMAT.md](CSV_FORMAT.md).

---

## 1. Origin

| | |
|---|---|
| Original project | [VREMSoftwareDevelopment/WiFiAnalyzer](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer) |
| Original authors | VREM Software Development and the [contributors](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer/graphs/contributors) to the original repository |
| Original version | WiFiAnalyzer 3.3.1 (versionCode 72), commit [`7e2019d`](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer/commit/7e2019d294d4ac40b70e770d9e2c55fdc1b18281) of 2026-10-03 ("Fix instrumented tests") |
| Modified by | QuyenAnh2002 and Pham Anh Dung, October–November 2026 (section 4 and the git history of this repository) |
| License | GNU GPL version 3 or later, same as the original (section 2) |
| Debug package | `com.vrem.wifianalyzer.BETA` with a grey icon, installable side by side with the original app |

The git history of this repository starts at commit `a946ef0` (2026-10-10). That commit contains the original source at commit `7e2019d` plus the first changes, so this repository does not include the history of the original repository. To see every difference from the original:

```bash
git fetch --depth=1 https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer.git 7e2019d294d4ac40b70e770d9e2c55fdc1b18281
git diff --stat FETCH_HEAD HEAD
```

**Citation:** citation metadata for this version is in [CITATION.cff](CITATION.cff); GitHub shows it through the "Cite this repository" button. When you use this version in a paper, please also cite the original WiFiAnalyzer (listed under `references` in that file).

`USER_MANUAL.md`, `CONTRIBUTING.md`, `SECURITY.md` and `AGENTS.md` are documents of the original repository (kept unchanged) and describe the original features of the app. The contribution guide and bug report links in them point to the original repository: report a problem there only if it also happens with the original app.

---

## 2. License

[<img src="https://www.gnu.org/graphics/gplv3-127x51.png" alt="GPLv3">](https://www.gnu.org/licenses/gpl-3.0.html)

WiFiAnalyzer is licensed under the GNU General Public License v3.0 (GPLv3), and so is this modified version. All source code in this repository, including the newly written code, is released under the GPL version 3 or (at your option) any later version, with no additional terms. The full license text is in [LICENSE](LICENSE), identical to the file in the original repository.

```
WiFiAnalyzer (modified version)
Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung (modifications)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
```

### 2.1. How this version meets the key GPLv3 requirements

The README of the original repository lists four key GPLv3 requirements. This version meets them as follows:

| Requirement | What it means (GPLv3 clause) | This version |
|---|---|---|
| **Disclose Source** | Anyone who receives a build (APK) must also be able to get the complete corresponding source code, including build scripts (clauses 1, 6) | All source code and build scripts are in this repository. The repository is currently **private**, so follow section 2.2 before giving an APK to anyone |
| **License and Copyright Notice** | Keep the license, the copyright notices and the no-warranty notices of the original authors intact; interactive user interfaces must display them (clauses 4, 5d) | `LICENSE` and the VREM Software Development copyright header are kept in every file, including new files. The app's **About** screen still shows the copyright notice and the full GPLv3 text, together with the Apache 2.0 license of the third-party libraries (this version adds no new libraries) |
| **Same License** | The whole modified version must be released under the GPLv3, with no further restrictions on recipients (clauses 5c, 10) | The whole repository is GPLv3 or later, with no additional terms |
| **State Changes** | A modified version must carry prominent notices stating that it was modified, with a relevant date (clause 5a) | Section 4 of this README summarizes the changes from the original commit; the `git diff` command in section 1 gives the exact list of files. The header of every added or modified `.kt` / `.xml` file has two notice lines right below the original copyright line (example below). The git history records each change with its date |

Header of a modified original file:

```kotlin
/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 * Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung (modifications)
 * Modified in 2026 from the original WiFiAnalyzer; changes are listed in README.md
 *
 * This program is free software: you can redistribute it and/or modify
 * ...
 */
```

New files use the two lines `Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung` and `Added in 2026 to this modified version of WiFiAnalyzer; see README.md`. XML files use the same text with a `~` prefix instead of `*`. PNG images (the grey debug icon) have no header; they are mentioned in section 4.

**When making further changes:** add the matching two lines to the header of every original file you modify for the first time and of every new file, and update section 4 (and [CSV_FORMAT.md](CSV_FORMAT.md) if the CSV format changes). `scripts/check-license-headers.sh` (run in CI) only checks for the VREM line and the GPL line, so it will not catch a missing notice.

### 2.2. Before giving an APK to others

As long as builds are only used internally and not given to anyone, the GPLv3 requires nothing more. When you give an APK (or any other build) to someone else, even free of charge:

1. **Provide the corresponding source code** for exactly that build (the commit it was built from), in one of the ways listed in clause 6 of the GPLv3. For example, ship the source together with the APK, or put a source download link right next to the APK download link. This repository is currently **private**, so a link to it is **not enough**: either make the repository public or ship the source, for example:
   ```bash
   git archive --format=zip -o wifianalyzer-src.zip HEAD
   ```
2. **Include the license:** recipients must receive a copy of the GPLv3. The `LICENSE` file is part of the source, and the app's About screen also shows the license.
3. **Add no restrictions** on the recipients' rights (no ban on redistribution or modification, no separate agreement to sign, …).
4. **Do not let recipients mistake it for the official app.** The GPLv3 does not require a new name, but the release build of this repository still uses the package `com.vrem.wifianalyzer` and the name "WiFiAnalyzer" of the original app on Google Play and F-Droid. Do not publish the release build to those stores. If wide distribution is needed, change the `applicationId` and the app name first. The About screen still shows the company name and home page link of the original. The SHA-256 fingerprint in `SECURITY.md` belongs to the official releases, so APKs built from this repository will not match it.

---

## 3. Privacy

The original app does not collect personal information and does not need Internet access. Compared with the original:

- This version still **does not request Internet access** and never sends data anywhere by itself. Data only leaves the device when the user shares it.
- **The CSV files contain sensitive data:** the precise location of the phone over time, plus the SSIDs / BSSIDs of nearby networks. Think before sharing them.
- Data is stored in `Download/WiFiAnalyzer/` (one log file per measurement run, kept until the user deletes it) and in `noBackupFilesDir/scan-history.csv` (history for Export, cleared when the app process restarts).
- New permission: `ACCESS_NETWORK_STATE` (to follow connection changes). When Play is pressed, the app also requests `NEARBY_WIFI_DEVICES` (already declared by the original) to measure RTT on Android 13+.
- Field measurement data is kept **outside the repository** (`~/data-wifi/`). Do not commit measured CSV files unless really needed.

---

## 4. Changes from the original

Compared with WiFiAnalyzer 3.3.1 (commit `7e2019d`), this version:

- **CSV export:** replaces sharing `|`-separated text with a 41-column `.csv` file holding every scan since the app was opened, shared through the Android share sheet or saved to a file. The original `Export.kt` and `ExportIntent.kt` were removed and replaced by `CsvExport.kt` and `CsvShare.kt`.
- **Logging per measurement run:** the Play / Pause button (which only paused scanning in the original) now starts and ends a measurement run. Each run writes one `wifi-log-*.csv` file with `CONNECTION` rows (current connection, read every second), `SCAN` rows (one per scanned BSSID) and `RTT` rows. Pressing Pause closes the file and opens the share sheet. A run timer is shown next to the button.
- **Device location** (GPS / network / fused) is attached to every CSV row.
- **Estimated distance:** adds the log-distance model (n = 2, 2.5, 3, 3.5) next to the original FSPL estimate.
- **Wi-Fi RTT:** measures the distance to access points that support 802.11mc / 802.11az every second (one-sided ranging to other APs on Android 14+). The AP detail view also shows `RTT x.xm`.
- **Debug build** uses a grey icon (`app/src/debug/res/`) so it is not mistaken for the original app.
- **Manifest:** adds the `ACCESS_NETWORK_STATE` permission and a `FileProvider`.

Using the app on a phone, the meaning of each CSV column and the functions that produce the data are described in [CSV_FORMAT.md](CSV_FORMAT.md). For the exact list of added / modified / deleted files, run the `git diff` command in section 1.

---

## 5. Build and test

**Requirements:** JDK 21 and the Android SDK with `platforms;android-37.0` and `build-tools;37.0.0`. Set the SDK path in `local.properties` (already in `.gitignore`):
```properties
sdk.dir=/home/<user>/Android/Sdk
```

**Commands:**
```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew ktlintCheck
./gradlew lintDebug
./gradlew testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Machines with little RAM:** by default up to 4 test processes run in parallel (2 GB heap each). On a 14 GB machine with many other applications open, running the full test suite can freeze the machine. Limit it:
```bash
./gradlew --max-workers=2 -Dorg.gradle.jvmargs="-Xmx3g -XX:MaxMetaspaceSize=1g" -Pkotlin.compiler.execution.strategy=in-process testDebugUnitTest
./gradlew --stop
```

**Test status at handover (2026-10-11, after Export was switched to a time series):**
- All 3763 unit tests (`./gradlew testDebugUnitTest`) pass. Among them, `MainRobolectricTest.scanner` (an original test that expected the app to start scanning / running when opened) was updated to the behavior of this version: when the app opens the button shows ▶, pressing it shows ⏸, pressing it again shows ▶.
- `ktlintCheck`: 0 errors. `lintDebug`: 0 errors; the remaining warnings come from the original repository or are `MissingTranslation` (section 6).
- The instrumented tests (`connectedDebugAndroidTest`) have **not** been rerun, and the new version has **not** been tried on a real phone.
- Tested on a vivo iQOO Z10 Turbo+ (Android 16).

---

## 6. Known limitations

- **RTT only works with APs that support 802.11mc/802.11az** (column `is_80211mc = true`). In the field data collected so far, **no AP** supports 802.11mc, so there are no `RTT` rows there and distance is still only estimated from RSSI. RTT needs an AP with an FTM responder enabled (for example Google/Nest Wifi, an enterprise AP with FTM enabled, or a Linux PC with an Intel AX200/AX210 card running `hostapd` with `ftm_responder=1`).
- **RTT needs the "Nearby devices" permission (`NEARBY_WIFI_DEVICES`)** on Android 13+; the app requests it when Play is pressed. If it is denied, the app still works but does not measure RTT.
- **Android restricts RTT for background apps**, so the RTT loop stops while the app is not visible.
- **`connected` / `bssid` in the log mean "associated with an AP"**, which may not have reached `COMPLETED` yet. Filter by `supplicant_state` when analyzing.
- **The logger only runs while the app is visible on screen.** Logging with the screen off would need a foreground service of type `location`.
- **The first row after opening the app may carry a stale location** taken from `getLastKnownLocation`. Check the `location_age_ms` column.
- **The history for Export is kept in internal storage** (`noBackupFilesDir/scan-history.csv`) with no size limit; frequent scanning (every 5 s, ~50 BSSIDs) uses about 10–15 MB per hour. The history is cleared when the app process restarts.
- **`SCAN` rows are ordered by time within each scan**; rows of two consecutive scans may overlap slightly. Sort by `epoch_ms` when analyzing.
- **Location updates run continuously while the app is in the foreground** (so that Export has locations even without pressing Play), which uses more battery than the original.
- **80+80 MHz channels** (very rare): the original app uses the center of the second 80 MHz segment as `center_frequency_mhz`, so `range_*` only describes that segment.
- **Indoors, the location comes from `network`** (10–100 m error) and updates roughly every 20 seconds.
- **New strings are English only** (lint reports `MissingTranslation`).
- **On the graph screens, the band selector may be in the ⋮ menu** because the title bar is too narrow.
- **The About screen is still the original one:** the company name, home page, FAQ and privacy policy links point to the original WiFiAnalyzer, and it does not say that this is a modified version or where its source code is (see section 2.2).
- **Privacy:** see section 3.
