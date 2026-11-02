# Wi-Fi logging and CSV file format

Detailed documentation for the modified WiFiAnalyzer in this repository: how to log on the phone, what each column of the CSV files means, and which functions in the source code produce each column. Origin, license and a summary of the changes from the original are in [README.md](README.md).

## Table of Contents

1. [Using the app on a phone](#1-using-the-app-on-a-phone)
2. [CSV file format](#2-csv-file-format)
3. [Functions that produce the CSV data](#3-functions-that-produce-the-csv-data)
4. [Analysis tools (outside the repository)](#4-analysis-tools-outside-the-repository)

---

## 1. Using the app on a phone

1. **Open the app**, grant the **precise location** permission and turn Location on. The app scans and shows APs right away but **does not log yet** (timer `00:00:00`, ▶ button).
2. **Press ▶ (Play):** starts a measurement run. The timer runs and the app creates **one file** `wifi-log-YYYYMMDD-HHMMSS.csv` in `Download/WiFiAnalyzer/`.
3. **During a run**, opening Export, pressing Home or rotating the screen makes the logger write `PAUSE` / `RESUME` rows into the **same file**. The logger stops while the app is in the background.
4. **Press ⏸ (Pause):** ends the run, closes the file and **opens the share sheet** to send it. The file stays in `Download/WiFiAnalyzer/` even if it is not sent.
5. **Menu ☰ → Export:** exports **every scan since the app was opened** (no need to press Play) as `WiFiAnalyzer-*.csv`, then opens the share sheet (with a "Save to file" entry). Each Export contains the full history up to that moment. The history starts over when the app process restarts (app swiped away, background app killed by the system, …).

**Tips:**
- To scan more often (about every 5 seconds), go to **Developer options → turn off "Wi-Fi scan throttling"**. By default Android only allows about 4 scans per 2 minutes.
- Copy the files to a computer with adb instead of an MTP cable connection, because MTP may show stale file sizes:
  ```bash
  adb pull /sdcard/Download/WiFiAnalyzer/ ~/data-wifi/
  ```

---

## 2. CSV file format

Common format: UTF-8 with BOM, CRLF line endings, comma-separated; a cell containing `,`, `"` or a line break is enclosed in double quotes.

The app produces only **two kinds of files**, and both use **the same 41 columns** (defined by the `CsvColumn` enum in `export/CsvRow.kt`):

| File | Created | Row types |
|---|---|---|
| `wifi-log-YYYYMMDD-HHMMSS.csv` | For every measurement run (Play → Pause) | `CONNECTION`, `SCAN` and `RTT`, interleaved in the order they were written |
| `WiFiAnalyzer-YYYYMMDD-HHMMSS.csv` | When Export is pressed in the menu | `SCAN` and `RTT`: every BSSID observation and every RTT result from app start until Export |

### 2.1. Columns

`epoch_ms, time, row_type, event, ssid, bssid, connected, rssi_dbm, frequency_mhz, primary_channel, center_channel, center_frequency_mhz, width_mhz, range_start_mhz, range_end_mhz, link_speed_mbps, supplicant_state, distance_fspl_m, distance_logdistance_2, distance_logdistance_2_5, distance_logdistance_3, distance_logdistance_3_5, distance_rtt_m, distance_rtt_stddev_m, rtt_status, rtt_mode, rtt_success_count, rtt_attempt_count, standard, security, capabilities, fast_roaming, is_80211mc, vendor, latitude, longitude, accuracy_m, altitude_m, speed_mps, location_provider, location_age_ms`

| Column | `CONNECTION` (current connection) | `SCAN` (one scanned BSSID) | `RTT` (one RTT result) |
|---|---|---|---|
| `epoch_ms`, `time` | When the row was written | When the BSSID was actually seen by the scan (both log and Export) | When the measurement was taken (`RangingResult.getRangingTimestampMillis()`); when the result arrived if ranging failed |
| `event` | Why the row was written (section 2.3) | Empty | Empty |
| `ssid`, `bssid` | The connected AP (empty when disconnected) | The scanned AP | The ranged AP |
| `connected` | `true` while associated with an AP | `true` if this BSSID is the connected AP | `true` if the ranged AP is the connected AP |
| `rssi_dbm` | RSSI of the connection; empty when not yet measured | Raw RSSI of that scan (both log and Export) | RSSI measured during ranging; empty if ranging failed |
| `frequency_mhz` | Primary channel frequency | Primary channel frequency | Primary channel frequency of the AP (from the scan result) |
| `primary_channel` … `range_end_mhz` | Empty | Channel, center channel, channel width, frequency range | Empty |
| Unknown values | Android reports `-127` (RSSI) or `-1` (frequency, link speed) when unknown: the cell is left **empty**, never filled with a fake value | Same as on the left; a channel number outside the app's channel table (for example channel 14) is also left empty | Failed ranging: the measurement columns are empty |
| `link_speed_mbps`, `supplicant_state` | Link speed, connection stage (`COMPLETED` = usable) | Empty | Empty |
| `distance_*` | Computed from `rssi_dbm` and `frequency_mhz`; empty if the RSSI is invalid | Same as on the left | `distance_fspl_m`, `distance_logdistance_*` are computed from the RSSI of the RTT measurement; `distance_rtt_*`, `rtt_*`: RTT results (only on this row type) |
| `standard` … `vendor` | Empty | Wi-Fi standard, security, capabilities string, fast roaming, 802.11mc, vendor | Only `is_80211mc` |
| `latitude` … `location_age_ms` | Device location when the row was written | Same as on the left | Device location closest to the measurement time |

- **The values of `event`** (and of the other coded fields) are listed in section 2.3.
- **Filter by `row_type` when analyzing:** for example with pandas, `df[df.row_type == "CONNECTION"]`, `df[df.row_type == "SCAN"]` or `df[df.row_type == "RTT"]`.

### 2.2. Field reference

The **Rows** column shows which row types fill the field: **C** = `CONNECTION`, **S** = `SCAN`, **R** = `RTT`. On other row types the cell is empty.

| # | Field | Type / unit | Meaning | Rows | Example |
|---|---|---|---|---|---|
| 1 | `epoch_ms` | Integer, milliseconds (Unix, UTC) | Time of the row since 1970-01-01 UTC. **C:** when the row was written. **S:** when the BSSID was actually seen by the scan (`ScanResult.timestamp`, converted from the boot clock to wall-clock time; if Android reports a missing or future value, the time the result was received is used). Applies to both log and Export. Use this column to sort or to join with other data | C, S, R | `1791640797747` |
| 2 | `time` | ISO 8601, local time, with milliseconds and time zone | The same instant as `epoch_ms`, human-readable | C, S, R | `2026-10-10T20:59:57.747+07:00` |
| 3 | `row_type` | `CONNECTION` / `SCAN` / `RTT` | Row type: the current connection state, one scanned BSSID, or one Wi-Fi RTT result | C, S, R | `SCAN` |
| 4 | `event` | String; several values joined with `+` | Why the `CONNECTION` row was written (table in section 2.3) | C | `BSSID+FREQUENCY+RSSI` |
| 5 | `ssid` | String | Wi-Fi network name. **C:** the connected network, empty when disconnected. **S:** the network broadcast by this BSSID; `*hidden*` for a hidden network | C, S, R | `PHENIKAA-UNI` |
| 6 | `bssid` | MAC address `aa:bb:cc:dd:ee:ff` | MAC address of one radio of an AP. A physical AP usually has several BSSIDs (one per band and per SSID), so several BSSIDs can share one SSID | C, S, R | `76:3a:0e:32:4e:99` |
| 7 | `connected` | `true` / `false` | **C:** `true` while the phone is associated with an AP, `false` when disconnected. **S:** `true` if the row's BSSID is the connected AP. "Associated" does not necessarily mean fully connected; see `supplicant_state` | C, S, R | `true` |
| 8 | `rssi_dbm` | Integer, dBm | Received signal strength; the closer to 0, the stronger (table in section 2.3). **C:** RSSI of the connection, updated about every second; **empty** when Android has no measurement yet (`-127`). **S:** raw RSSI of that scan, in both log and Export (not the smoothed value shown on screen). **R:** RSSI of the FTM frames during ranging; empty if ranging failed | C, S, R | `-60` |
| 9 | `frequency_mhz` | MHz | Frequency of the primary 20 MHz channel. 2412–2484: 2.4 GHz band; 5160–5885: 5 GHz; 5955–7115: 6 GHz. Empty if Android does not know it | C, S, R | `5320` |
| 10 | `primary_channel` | Channel number | Primary channel number, matching `frequency_mhz`. Empty if the frequency is outside the app's channel table (for example channel 14 = 2484 MHz) | S | `64` |
| 11 | `center_channel` | Channel number | Channel at the center of the whole bandwidth when the AP bonds channels to 40/80/160/320 MHz; equal to `primary_channel` for a 20 MHz channel | S | `58` |
| 12 | `center_frequency_mhz` | MHz | Frequency at the center of the whole bandwidth | S | `5290` |
| 13 | `width_mhz` | MHz | Channel width: 20, 40, 80, 160 or 320 | S | `80` |
| 14 | `range_start_mhz` | MHz | Lowest frequency occupied by the AP = `center_frequency_mhz − width_mhz / 2` | S | `5250` |
| 15 | `range_end_mhz` | MHz | Highest frequency occupied by the AP = `center_frequency_mhz + width_mhz / 2` | S | `5330` |
| 16 | `link_speed_mbps` | Mbps | Physical-layer link speed reported by the Wi-Fi chip. **Not** the actual throughput. Empty if Android does not know it | C | `866` |
| 17 | `supplicant_state` | String | Wi-Fi connection stage (table in section 2.3). `COMPLETED` = connected and usable | C | `COMPLETED` |
| 18 | `distance_fspl_m` | Meters, 1 decimal | Estimated distance to the AP using FSPL (section 2.4), from `rssi_dbm` and `frequency_mhz`. Empty if the RSSI is invalid. **Use for relative comparison only** | C, S, R | `4.5` |
| 19 | `distance_logdistance_2` | Meters | Log-distance with path-loss exponent n = 2 (free space). Always equal to `distance_fspl_m` | C, S, R | `4.5` |
| 20 | `distance_logdistance_2_5` | Meters | Log-distance with n = 2.5 (open space, few obstacles) | C, S, R | `3.3` |
| 21 | `distance_logdistance_3` | Meters | Log-distance with n = 3 (office with partitions) | C, S, R | `2.7` |
| 22 | `distance_logdistance_3_5` | Meters | Log-distance with n = 3.5 (many concrete walls) | C, S, R | `2.4` |
| 23 | `distance_rtt_m` | Meters, 2 decimals, **can be negative** | Distance **measured** with Wi-Fi RTT (round-trip time of the FTM frames × speed of light / 2), averaged over the measurements of one burst. The error is typically 1–2 m with line of sight to the AP; walls make the result longer. Small negative values close to the AP come from calibration error. Only present when `rtt_status = SUCCESS` | R | `3.25` |
| 24 | `distance_rtt_stddev_m` | Meters | Standard deviation of the measurements in the burst; a large value means the result fluctuates (multipath, obstacles) | R | `0.42` |
| 25 | `rtt_status` | String | Result of ranging to this AP (table in section 2.3) | R | `SUCCESS` |
| 26 | `rtt_mode` | String | Ranging method: `80211MC`, `80211AZ` or `ONE_SIDED` (table in section 2.3) | R | `80211MC` |
| 27 | `rtt_success_count` | Integer | Number of successful measurements in the burst | R | `7` |
| 28 | `rtt_attempt_count` | Integer | Number of attempted measurements in the burst | R | `8` |
| 29 | `standard` | String | Highest Wi-Fi standard supported by the AP (table in section 2.3); empty if Android cannot tell | S | `802.11ax` |
| 30 | `security` | Space-separated values | Normalized security types (table in section 2.3) | S | `WPS WPA2` |
| 31 | `capabilities` | Raw Android string | Raw security description. Each pair of square brackets is one component: `[protocol-authentication-cipher]` (for example `WPA2-PSK-CCMP-128`: WPA2, pre-shared key, AES-CCMP 128-bit encryption), `[ESS]` = infrastructure network with an AP, `[WPS]` = WPS supported | S | `[WPA2-PSK-CCMP-128][RSN-PSK-CCMP-128][ESS][WPS]` |
| 32 | `fast_roaming` | Space-separated values | Fast roaming standards advertised by the AP (table in section 2.3); empty if none | S | `802.11k 802.11v` |
| 33 | `is_80211mc` | `true` / `false` | Whether the AP supports Wi-Fi RTT ranging (802.11mc, FTM). Only APs with `true` are ranged two-sided (`RTT` rows) | S, R | `false` |
| 34 | `vendor` | String | Manufacturer, looked up from the first 3 bytes (OUI) of the BSSID. **Empty** for virtual BSSIDs generated by the AP (first byte with the "locally administered" bit set, for example `5a:…`, `76:…`) or when not found in the lookup table | S | `TP LINK SYSTEMS INC` |
| 35 | `latitude` | Decimal degrees, WGS-84, 7 decimals | Latitude of the phone (positive = North), taken from the **location fix closest to the row's `epoch_ms`**. For `SCAN` rows this is the time the BSSID was seen | C, S, R | `21.0285123` |
| 36 | `longitude` | Decimal degrees, WGS-84 | Longitude (positive = East) | C, S, R | `105.8048456` |
| 37 | `accuracy_m` | Meters | Horizontal accuracy radius at 68% confidence, as estimated by the location provider | C, S, R | `13.9` |
| 38 | `altitude_m` | Meters | Altitude above the WGS-84 ellipsoid. Unreliable when the provider is `network` | C, S, R | `58.4` |
| 39 | `speed_mps` | m/s, 2 decimals | Speed of the phone; only present when the provider reports it (usually GPS) | C, S, R | `1.25` |
| 40 | `location_provider` | `gps` / `network` / `fused` | Source of the location (table in section 2.3) | C, S, R | `network` |
| 41 | `location_age_ms` | Milliseconds, **signed** | The row's `epoch_ms` minus the time of the location fix. Positive: the fix was taken before the row's time; **negative**: after it (only on `SCAN` rows). A large absolute value (for example > 5000) means the location does not match the time well; filter such rows out when location accuracy matters | C, S, R | `1988` |

### 2.3. Possible values

**`event`** (`CONNECTION` rows; several values can be joined with `+`, for example `BSSID+FREQUENCY+RSSI` when the phone roams to an AP on another band):

| Value | Meaning |
|---|---|
| `START` | First row of a measurement run (Play was just pressed) |
| `PAUSE` | The app paused logging (Export opened, Home pressed, screen turned off, …), or the last row when Pause is pressed |
| `RESUME` | The app came back and continues logging in the same run |
| `CONNECTED` | Associated with an AP after being disconnected |
| `DISCONNECTED` | The connection was just lost |
| `SSID` | Switched to another network |
| `BSSID` | Switched to another AP / radio (roaming) |
| `FREQUENCY` | Frequency / channel changed |
| `RSSI` | Signal strength of the connection changed |
| `LINK_SPEED` | Link speed changed |
| `STATE` | `supplicant_state` changed |

**`supplicant_state`** (Android connection stages, in their usual order):

| Value | Meaning |
|---|---|
| `SCANNING` | Scanning for networks |
| `AUTHENTICATING` | Authenticating with the AP |
| `ASSOCIATING` | Associating with the AP |
| `ASSOCIATED` | Associated with the AP, keys not exchanged yet |
| `FOUR_WAY_HANDSHAKE`, `GROUP_HANDSHAKE` | Exchanging encryption keys (WPA) |
| `COMPLETED` | **Connected and usable** |
| `DISCONNECTED`, `INACTIVE`, `DORMANT`, `INTERFACE_DISABLED`, `UNINITIALIZED`, `INVALID` | Not connected / Wi-Fi off / error state |

**`standard`:**

| Value | Generation |
|---|---|
| `802.11a/b/g` | Legacy Wi-Fi (Wi-Fi 1–3) |
| `802.11n` | Wi-Fi 4 |
| `802.11ac` | Wi-Fi 5 |
| `802.11ax` | Wi-Fi 6 / 6E |
| `802.11be` | Wi-Fi 7 |
| `802.11ad` | WiGig (60 GHz band) |
| *(empty)* | Unknown |

**`security`:** `NONE` (open network, no password), `WPS`, `WEP` (legacy, insecure), `WPA`, `WPA2`, `WPA3`. An AP can have several values, for example `WPS WPA WPA2`.

**`fast_roaming`:**

| Value | Meaning |
|---|---|
| `802.11k` | The AP sends a list of neighboring APs, helping the phone pick a new AP faster |
| `802.11v` | The AP can suggest / steer the phone to another AP |
| `802.11r` | Fast BSS Transition: switching APs without a full re-authentication |
| `OKC` | Opportunistic Key Caching: reuses existing keys when switching APs |

**`rtt_status`:**

| Value | Meaning |
|---|---|
| `SUCCESS` | Ranging succeeded; `distance_rtt_*`, `rtt_*_count` and `rssi_dbm` have values |
| `FAIL` | The AP did not answer the FTM frames (out of range, busy channel, FTM disabled on the AP, …) |
| `RESPONDER_NOT_SUPPORTED` | The AP reports that it does not support 802.11mc |
| `STATUS_<number>` | Another status code returned by a newer Android version |

**`rtt_mode`:**

| Value | Meaning |
|---|---|
| `80211MC` | Two-sided RTT per 802.11mc (FTM): the AP takes part in the measurement; most accurate (~1–2 m) |
| `80211AZ` | RTT per 802.11az (Wi-Fi 6/7, Android 15+) |
| `ONE_SIDED` | Only the phone measures (regular AP without FTM support). Used only when the phone reports one-sided RTT support (Android 14+). Less accurate because the AP's processing time is included |

**`location_provider`:**

| Value | Meaning | Typical error |
|---|---|---|
| `gps` | Satellite positioning (outdoors) | 1–5 m |
| `network` | Estimated from Wi-Fi / cell towers (Google Play Services) | 10–100 m |
| `fused` | Combination of several sources and sensors | Depends on the sources |

**Reading `rssi_dbm`:**

| RSSI | Quality |
|---|---|
| ≥ −50 dBm | Excellent (very close to the AP) |
| −50 … −67 dBm | Good, enough for any application |
| −67 … −70 dBm | Common threshold for reliable voice / video calls |
| −70 … −80 dBm | Weak, lower throughput |
| ≤ −80 dBm | Very weak, connection loss or roaming likely |
| *(empty)* | Invalid (Android reported `-127`: no measurement yet) |

### 2.4. Distance formulas (rough estimates)

Both formulas assume the AP transmits at 0 dBm with a 0 dBi antenna.

```
FSPL:          d = 10 ^ ((27.55 − 20·log10(f_MHz) + |RSSI|) / 20)
Log-distance:  d = 10 ^ ((|RSSI| − FSPL(1 m)) / (10·n)),   FSPL(1 m) = 20·log10(f_MHz) − 27.55
```

With n = 2, log-distance equals FSPL. Use the results for **relative comparison only**: the real transmit power of an AP (15–23 dBm) and obstacles distort the absolute numbers a lot.

---

## 3. Functions that produce the CSV data

### 3.1. Data flow

```
Android WifiManager ──► WiFiManagerWrapper.wiFiInfo() ──► ConnectionLogger.sample() ──► line() ─────────────────────┐
                    └─► ScannerCallback.onSuccess() ──► ScanRecorder.record() ──► CsvExport.row() ──┬─► LogFile (wifi-log, 1 file, during a run)
                        (WiFiManagerWrapper.scanResults(), broadcast SCAN_RESULTS_AVAILABLE)         └─► session history (noBackupFilesDir/scan-history.csv)
                    └─► ScannerCallback.onSuccess() ──► RttRanger.update() ──► WifiRttManager.startRanging() (every 1 s)
                        ──► RangingResultCallback ──► RttRanger.row() ──► ScanRecorder.appendRows() ──► LogFile + session history (like SCAN rows)
ConnectivityManager.NetworkCallback ──► ConnectionLogger.trigger() (reads immediately when the connection changes)
LocationManager (gps / network / fused) ──► LocationTracker.update() ──► LocationTracker.at(t) / current() ──► location columns
Export menu ──► MainActivity.exportCsv() ──► ScanRecorder.export() (header + session history) ──► CsvShare (share sheet)
```

### 3.2. Functions by column group

| Data | Functions | File | Underlying Android API |
|---|---|---|---|
| Current connection (ssid, bssid, rssi, frequency, link speed, state) | `WiFiManagerWrapper.wiFiInfo()` → `ConnectionLogger.sample()` → `WifiInfo.toLogSample()` | `wifi/manager/WiFiManagerWrapper.kt`, `wifi/logger/ConnectionLogger.kt` | `WifiManager.getConnectionInfo()`: `WifiInfo.getSSID/getBSSID/getRssi/getFrequency/getLinkSpeed/getSupplicantState` |
| Connection polling | `ConnectionLogger.poll()` (every `INTERVAL_MS` = 1000 ms), `ConnectionLogger.trigger()` | `wifi/logger/ConnectionLogger.kt` | `Handler.postDelayed`; `ConnectivityManager.registerNetworkCallback` (`onAvailable/onLost/onCapabilitiesChanged`) |
| Events (`event`) | `ConnectionLogger.changes(previous, current)` | `wifi/logger/ConnectionLogger.kt` | Compares two consecutive samples |
| Building a `CONNECTION` row | `ConnectionLogger.line(timeMs, events, sample, location)` | `wifi/logger/ConnectionLogger.kt` | |
| Scan results (every BSSID) | `ScannerCallback.onSuccess()` → `ScanRecorder.record()` → `rows()` (keeps new observations by BSSID + SSID and `ScanResult.timestamp`) | `wifi/scanner/ScannerCallback.kt`, `wifi/logger/ScanRecorder.kt` | `WifiManager.getScanResults()`; triggered by the `SCAN_RESULTS_AVAILABLE_ACTION` broadcast and the app's periodic scan |
| Time a BSSID was seen | `ScanRecorder.rows()` / `seenNanos()`: converts `ScanResult.timestamp` (µs since boot) to wall-clock time | `wifi/logger/ScanRecorder.kt` | `ScanResult.timestamp`, `SystemClock.elapsedRealtimeNanos()` |
| `ScanResult` → AP details | `ScanResult.toWiFiDetail(level)` | `wifi/scanner/Transformer.kt` | `ScanResult.SSID/wifiSsid/BSSID/level/frequency/centerFreq0/centerFreq1/channelWidth/capabilities` |
| Channel, frequency, width | `WiFiSignal.primaryWiFiChannel`, `centerWiFiChannel`, `wiFiWidth`, `wiFiChannelStart/End` | `wifi/model/WiFiSignal.kt`, `wifi/model/WiFiWidth.kt` | Computed from `ScanResult` |
| Wi-Fi standard (`standard`) | `WiFiStandard.findOne(scanResult)`, `WiFiSignalExtra.wiFiStandardDisplay()` | `wifi/model/WiFiStandard.kt`, `wifi/model/WiFiSignal.kt` | `ScanResult.getWifiStandard()` |
| Security (`security`, `capabilities`) | `WiFiSecurity(capabilities, WiFiSecurityType.find(scanResult)).securities` | `wifi/model/WiFiSecurity.kt` | `ScanResult.capabilities`, `ScanResult.getSecurityTypes()` |
| Fast roaming (`fast_roaming`) | `FastRoaming.find(scanResult)`, `WiFiSignalExtra.fastRoamingDisplay()` | `wifi/model/FastRoaming.kt` | `ScanResult.getInformationElements()` |
| 802.11mc (`is_80211mc`) | `ScanResult.is80211mcResponder` | `wifi/scanner/Transformer.kt` | |
| Choosing APs to range | `RttRanger.update(scanResults, connectedBssid)` → `targets()`: scan results from the last 30 s, connected AP first, then by RSSI; 802.11mc/az APs first, other APs (one-sided) only when the phone supports it; at most `RangingRequest.getMaxPeers()` (10) | `wifi/logger/RttRanger.kt` | `ScanResult.is80211mcResponder`, `ScanResult.is80211azNtbResponder` (Android 15+), `WifiRttManager.getRttCharacteristics()` (Android 14+) |
| RTT ranging | `RttRanger.rangeTargets()` → `request()`; repeated every `INTERVAL_MS` = 1000 ms (`finished()`); stopped by `stop()` (`MainActivity.onPause`) | `wifi/logger/RttRanger.kt` | `PackageManager.FEATURE_WIFI_RTT`, `WifiRttManager.isAvailable/startRanging`, `RangingRequest.Builder.addAccessPoint/addNon80211mcCapableAccessPoint`; permission `NEARBY_WIFI_DEVICES` (Android 13+) or `ACCESS_FINE_LOCATION` |
| RTT results (`distance_rtt_m`, `distance_rtt_stddev_m`, `rtt_*`, `rssi_dbm` of `RTT` rows) | `RttRanger.measurement(result)` → `row(measurement, target, connectedBssid, nowMs, nowNanos)` → `ScanRecorder.appendRows()` | `wifi/logger/RttRanger.kt`, `wifi/logger/ScanRecorder.kt` | `RangingResult.getStatus/getMacAddress/getDistanceMm/getDistanceStdDevMm/getNumSuccessfulMeasurements/getNumAttemptedMeasurements/getRssi/getRangingTimestampMillis` |
| RTT shown on screen | `RttRanger.latest(bssid)` → `WiFiDetailView.distance()` | `wifi/logger/RttRanger.kt`, `wifi/detailview/WiFiDetailView.kt` | |
| Manufacturer (`vendor`) | `VendorService.findVendorName(bssid)` | `vendor/model/VendorService.kt` | OUI lookup table bundled with the app |
| `connected` flag | `SCAN` (log and Export): compares the BSSID with `WifiInfo.getBSSID()` when the scan result arrives (`ScanRecorder.rows()`); `CONNECTION`: whether there is a BSSID (`ConnectionLogger.line()`) | `wifi/logger/ScanRecorder.kt`, `wifi/logger/ConnectionLogger.kt` | |
| Building a `SCAN` row (log and Export) | `CsvExport.row(context, date, wiFiDetail, connected, location)` | `export/CsvExport.kt` | |
| Common columns, header, time / location / distance values | `CsvColumn`, `CSV_HEADER`, `csvRow()`, `timeValues()`, `locationValues()`, `distanceValues()` | `export/CsvRow.kt` | |
| Location (lat, lon, accuracy, altitude, speed, provider, age) | `LocationTracker.start()` (while the app is in the foreground) → `update()` / `better()` → `at(t)` (fix closest to `t`) / `current()` | `wifi/logger/LocationTracker.kt` | `LocationManagerCompat.requestLocationUpdates` (gps / network / fused, every 1 s), `LocationManager.getLastKnownLocation`, `Location.getAccuracy/getAltitude/getSpeed/getElapsedRealtimeNanos` |
| Distance | `calculateDistance(frequency, rssi)`, `calculateDistanceLogDistance(frequency, rssi, n)` → `distanceFields()` | `wifi/model/WiFiUtils.kt`, `export/Csv.kt` | |
| Export data | `ScanRecorder.export(target, onDone)`: BOM + header + session history, on a background thread | `wifi/logger/ScanRecorder.kt`, `MainActivity.kt` | Raw scan results (not the smoothed screen `Cache`) |
| Writing the log file | `LogFile.open()` / `write()` / `close()`. One `LogFile` is shared by `ConnectionLogger` (logger thread) and `ScanRecorder` (its own writer thread); every operation is synchronized and each write is one block, so rows never interleave | `wifi/logger/LogFile.kt` | `MediaStore.Downloads` (Android 10+), `getExternalFilesDir` (older Android) |
| Writing and sharing the Export file | `CsvShare.prepare()`, `CsvShare.chooser(file, saveIntent)`; saving to a chosen location: `MainActivity.saveExport()` → `onExportCsvResult()` | `export/CsvShare.kt`, `MainActivity.kt` | `FileProvider`, `Intent.ACTION_SEND`, `ActivityResultContracts.CreateDocument` |
| Sharing the files of a run | `MainActivity.stopRun()` → `ConnectionLogger.close(callback)` → `files()` → `MainActivity.shareRun()` → `CsvShare.chooser(uris)` | `MainActivity.kt`, `wifi/logger/ConnectionLogger.kt`, `export/CsvShare.kt` | `Intent.ACTION_SEND` (1 file) or `ACTION_SEND_MULTIPLE` (several files), type `text/csv` + `Intent.EXTRA_ALTERNATE_INTENTS` with `application/vnd.ms-excel` |
| CSV formatting | `csvLine()`, `csvEscape()`, `csvFormat()`, `CSV_BOM`; invalid value filtering: `validRssi()`, `positiveOrNull()` | `export/Csv.kt` | |

(Paths in the table above are relative to `app/src/main/kotlin/com/vrem/wifianalyzer/`.)

---

## 4. Analysis tools (outside the repository)

- `plot_roaming.py` (**not in this repository**; kept in `~/data-wifi/` with the measurement data): builds an HTML map from one `wifi-log-*.csv` file. `SCAN` rows are used to draw the path and find BSSID changes; `CONNECTION` rows show when the connection was lost and the RSSI of the connection. The script still reads the older pair of files `wifi-scan-*.csv` + `wifi-log-*.csv`.
  ```bash
  python3 ~/data-wifi/plot_roaming.py ~/data-wifi/WiFiAnalyzer/wifi-log-YYYYMMDD-HHMMSS.csv
  ```
