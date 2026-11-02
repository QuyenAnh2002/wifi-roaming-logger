# WiFiAnalyzer – bản nội bộ (đo Wi-Fi, ghi log CSV, chia sẻ kết quả)

Bản này được phát triển từ **WiFiAnalyzer** mã nguồn mở:

| | |
|---|---|
| Repo gốc | https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer |
| Commit gốc | `7e2019d294d4ac40b70e770d9e2c55fdc1b18281` (2026-10-03, "Fix instrumented tests") |
| Giấy phép | GNU GPLv3 (giữ nguyên `LICENSE` và header bản quyền trong từng file) |
| Package bản debug | `com.vrem.wifianalyzer.BETA` (cài song song được với app gốc trên Google Play) |

> **Giấy phép:** đây là sản phẩm phái sinh của một dự án GPLv3. Dùng nội bộ không có ràng buộc gì thêm, nhưng nếu phân phối APK ra bên ngoài thì phải cung cấp kèm mã nguồn theo GPLv3.

Các tài liệu `USER_MANUAL.md`, `CONTRIBUTING.md`, `SECURITY.md`, `AGENTS.md` là tài liệu của repo gốc, mô tả các chức năng gốc của app.

---

## 1. Chức năng cải tiến so với repo gốc

| # | Chức năng | Repo gốc | Bản này |
|---|---|---|---|
| 1 | **Export CSV** | Gửi nội dung dạng văn bản, phân cách bằng `\|`, qua bảng chia sẻ; chỉ là ảnh chụp danh sách AP lúc bấm | Tạo **file `.csv` thật** theo định dạng chung 41 cột (thêm vị trí, khoảng cách ước lượng, hãng sản xuất, cờ `connected`…) chứa **toàn bộ chuỗi thời gian từ lúc mở app**: mỗi lần một BSSID được quét thấy là một dòng, với RSSI thô và thời điểm thực sự quét thấy. Mở **bảng chia sẻ Android** (Zalo, Telegram, Gmail…) kèm mục **"Save to file"** để lưu vào chỗ tùy chọn |
| 2 | **Log kết nối theo thời gian** (dòng `CONNECTION` trong `wifi-log-*.csv`) | Không có | Ghi trạng thái AP đang kết nối (SSID, BSSID, RSSI, tần số, tốc độ, trạng thái kết nối) **mỗi khi có thay đổi**, đọc mỗi 1 giây, kèm sự kiện roam / mất kết nối |
| 3 | **Log tất cả BSSID theo thời gian** (dòng `SCAN` trong **cùng file** `wifi-log-*.csv`) | Không có | Mỗi lần quét, ghi **mỗi BSSID một dòng**, kể cả nhiều BSSID cùng SSID. Dùng RSSI thô của lần quét và thời điểm thực sự quét thấy. **Mỗi lượt đo chỉ tạo 1 file CSV**. Dòng `SCAN` của log và của Export sinh ra từ **cùng một chỗ** (`ScanRecorder`) nên giống hệt nhau |
| 4 | **Vị trí thiết bị** | Không có | Mỗi dòng CSV có vĩ độ, kinh độ, sai số, độ cao, tốc độ, nguồn vị trí (GPS / network / fused). Lấy **bản định vị gần nhất với thời điểm của dòng** (với dòng `SCAN`: lúc BSSID được quét thấy), không phải vị trí lúc ghi file |
| 5 | **Ước lượng khoảng cách tới AP** | Chỉ hiển thị `~x.xm` trên màn hình (FSPL) | Thêm vào CSV: `distance_fspl_m` và log-distance với n = 2; 2,5; 3; 3,5 |
| 6 | **Điều khiển phiên đo bằng nút Play / Pause** | Nút chỉ tạm dừng việc quét | App vẫn quét và hiển thị AP như bản gốc, nhưng **chỉ ghi log khi bấm Play**. **Bấm Pause thì đóng file và mở ngay bảng chia sẻ** file CSV của lượt đo. Mở app **không tự bắt đầu đo** |
| 7 | **Bộ đếm thời gian lượt đo** | Không có | Hiển thị `HH:MM:SS` cạnh nút Play / Pause; dừng và làm mờ khi Pause |
| 8 | **Logo bản debug màu xám** | Logo xanh | Bản BETA dùng logo xám để không nhầm với app gốc |
| 9 | **Công cụ vẽ bản đồ** (ngoài repo) | Không có | Script `plot_roaming.py`: vẽ đường đi, tô màu theo RSSI, đánh dấu các điểm đổi BSSID, bật/tắt `accuracy_m` và `distance_fspl_m` |
| 10 | **Đo khoảng cách bằng Wi-Fi RTT** (dòng `RTT`) | Chỉ hiển thị cờ 802.11mc của AP | Với AP hỗ trợ 802.11mc (FTM) hoặc 802.11az, app đo khoảng cách thật bằng thời gian bay của tín hiệu, **mỗi giây một lần**, tối đa 10 AP mỗi lần (AP đang kết nối trước, rồi theo RSSI). Trên Android 14+ có hỗ trợ one-sided RTT thì đo cả AP thường. Mỗi kết quả là một dòng `RTT` (khoảng cách, độ lệch chuẩn, số lần đo thành công). Màn hình hiển thị thêm `RTT x.xm` cạnh khoảng cách ước lượng |

---

## 2. Danh sách file đã sửa / thêm (so với commit `7e2019d`)

### 2.1. File mã nguồn mới

| File | Nội dung |
|---|---|
| `app/src/main/kotlin/com/vrem/wifianalyzer/export/Csv.kt` | Hàm tiện ích CSV dùng chung: escape, tạo dòng, định dạng số, BOM UTF-8, tính các cột khoảng cách |
| `app/src/main/kotlin/com/vrem/wifianalyzer/export/CsvRow.kt` | **Định dạng CSV chung 41 cột** (enum `CsvColumn`), `CSV_HEADER`, `csvRow()`, giá trị thời gian / vị trí / khoảng cách |
| `app/src/main/kotlin/com/vrem/wifianalyzer/export/CsvExport.kt` | Tạo dòng `SCAN` (dùng cho Export và cho log); tên file Export |
| `app/src/main/kotlin/com/vrem/wifianalyzer/export/CsvShare.kt` | Chuẩn bị file export trong cache, tạo bảng chia sẻ qua `FileProvider`. Một file gửi bằng `ACTION_SEND` (nhiều app nhận hơn `ACTION_SEND_MULTIPLE`); ngoài kiểu `text/csv` còn đính kèm kiểu thay thế `application/vnd.ms-excel` để Messenger, Zalo… (không nhận `text/csv`) cũng hiện trong bảng chia sẻ |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/logger/ConnectionLogger.kt` | Mở / đóng file `wifi-log` của lượt đo, ghi dòng `CONNECTION`, giao file đang mở cho `ScanRecorder` để dòng `SCAN` của lượt đo vào cùng file |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/logger/ScanRecorder.kt` | **Nơi duy nhất** biến kết quả quét thành dòng `SCAN`: lọc quan sát mới, đổi `ScanResult.timestamp` ra giờ thật, gắn vị trí; ghi vào lịch sử phiên (cho Export) và vào file của lượt đo (khi đang đo) |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/logger/RttRanger.kt` | Đo Wi-Fi RTT: chọn AP cần đo từ kết quả quét mới nhất, gửi `RangingRequest` mỗi giây, biến từng `RangingResult` thành dòng `RTT` (giao cho `ScanRecorder.appendRows()`), giữ kết quả gần nhất để hiển thị |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/logger/LocationTracker.kt` | Lấy vị trí từ GPS / network / fused; giữ lịch sử ~2 phút các bản định vị để chọn bản gần nhất với thời điểm của từng dòng |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/logger/LogFile.kt` | Tạo và ghi file trong `Download/WiFiAnalyzer` (MediaStore); an toàn khi ghi từ nhiều luồng |
| `app/src/main/kotlin/com/vrem/wifianalyzer/navigation/options/RunTimer.kt` | Bộ đếm thời gian lượt đo |

### 2.2. File mã nguồn đã sửa

| File | Thay đổi |
|---|---|
| `app/src/main/kotlin/com/vrem/wifianalyzer/MainActivity.kt` | Export ra bảng chia sẻ; xử lý "Save to file" (`onNewIntent`); `toggleRun` / `startRun` / `stopRun` / `shareRun`; logger dùng chung khi activity bị tạo lại; chỉ chạy logger khi đang có lượt đo; bật `LocationTracker` khi app ở foreground; cập nhật bộ đếm mỗi giây; khi bấm Play thì xin quyền `NEARBY_WIFI_DEVICES` (cần cho RTT trên Android 13+, từ chối thì chỉ tắt RTT); dừng vòng đo RTT khi app vào nền (`onPause`) |
| `app/src/main/kotlin/com/vrem/wifianalyzer/MainContext.kt` | Thêm `runTimer`, `locationTracker`, `scanRecorder`, `rttRanger` dùng chung toàn app (tạo một lần cho mỗi tiến trình, không mất khi xoay màn hình) |
| `app/src/main/kotlin/com/vrem/wifianalyzer/navigation/items/ExportItem.kt` | Export gọi `MainActivity.exportCsv()` (chia sẻ lịch sử phiên) thay cho luồng chia sẻ văn bản cũ |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/scanner/ScannerCallback.kt`, `ScannerService.kt` | Mỗi lần app nhận kết quả quét, chuyển nguyên kết quả thô cho `ScanRecorder` và `RttRanger` |
| `app/src/main/kotlin/com/vrem/wifianalyzer/navigation/items/NavigationItems.kt` | `ExportItem()` không còn cần tham số `Export` |
| `app/src/main/kotlin/com/vrem/wifianalyzer/navigation/options/OptionAction.kt` | Nút run gọi `MainActivity.toggleRun()` |
| `app/src/main/kotlin/com/vrem/wifianalyzer/navigation/availability/ScannerSwitch.kt` | Biểu tượng Play / Pause theo trạng thái ghi log; ẩn / hiện và làm mờ bộ đếm (`runTimerText`) |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/scanner/Transformer.kt` | Tách hàm `ScanResult.toWiFiDetail()` để `ScanRecorder` dùng chung |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/model/WiFiUtils.kt` | Thêm `calculateDistanceLogDistance()` (mô hình log-distance) |
| `app/src/main/kotlin/com/vrem/wifianalyzer/wifi/detailview/WiFiDetailView.kt` | Thêm `RTT x.xm` sau khoảng cách ước lượng khi có kết quả RTT trong 10 giây gần nhất |

### 2.3. Tài nguyên và cấu hình

| File | Thay đổi |
|---|---|
| `app/src/main/AndroidManifest.xml` | Thêm quyền `ACCESS_NETWORK_STATE` (cho `NetworkCallback`); khai báo `FileProvider` |
| `app/src/main/res/xml/file_paths.xml` (mới) | Thư mục được phép chia sẻ: `cache/exports/` và thư mục file riêng của app |
| `app/src/main/res/menu/optionmenu.xml` | Thêm mục bộ đếm `action_run_timer`; nút run luôn hiển thị (`showAsAction="always"`) |
| `app/src/main/res/layout/run_timer.xml` (mới) | Giao diện bộ đếm |
| `app/src/main/res/values/strings.xml` | Thêm `export_saved`, `export_save_to_file`, `run_timer_title`, `run_share_title` (chỉ có tiếng Anh) |
| `app/src/debug/res/drawable/ic_launcher_*.xml`, `app/src/debug/res/mipmap-*/ic_launcher*.png` (mới) | Logo xám, chỉ áp dụng cho bản debug |

### 2.4. Test

- **Test mới:** `CsvTest`, `CsvRowTest`, `CsvExportTest`, `CsvShareTest` (`app/src/test/.../export/`), `ConnectionLoggerTest`, `ScanRecorderTest`, `RttRangerTest`, `LocationTrackerTest`, `LogFileTest` (`app/src/test/.../wifi/logger/`), `RunTimerTest` (`app/src/test/.../navigation/options/`).
- **Test đã sửa:** `MainActivityTest`, `MainContextHelper`, `ExportItemTest`, `OptionActionTest`, `ScannerSwitchOnTest`, `ScannerSwitchOffTest`, `ScannerCallbackTest`, `ScannerServiceTest`, `TransformerTest`, `WiFiUtilsTest`.
- **Đã xóa:** `Export.kt`, `ExportIntent.kt` của repo gốc (không còn dùng) và test của chúng; `ScanLogger.kt` (thay bằng `ScanRecorder.kt`).

### 2.5. Công cụ phân tích (ngoài app)

- `plot_roaming.py` (**không nằm trong repo**, hiện ở `~/data-wifi/` cùng dữ liệu đo): tạo bản đồ HTML từ một file `wifi-log-*.csv`. Dòng `SCAN` dùng để vẽ đường đi và tìm điểm đổi BSSID; dòng `CONNECTION` cho biết lúc mất kết nối và RSSI của kết nối. Script vẫn đọc được cặp file cũ `wifi-scan-*.csv` + `wifi-log-*.csv`.
  ```bash
  python3 ~/data-wifi/plot_roaming.py ~/data-wifi/WiFiAnalyzer/wifi-log-YYYYMMDD-HHMMSS.csv
  ```

---

## 3. Cách sử dụng trên điện thoại

1. **Mở app** và cấp quyền **Vị trí chính xác**, bật Location. App quét và hiển thị AP ngay nhưng **chưa ghi log** (bộ đếm `00:00:00`, nút ▶).
2. **Bấm ▶ (Play):** bắt đầu lượt đo. Bộ đếm chạy, app tạo **một file** `wifi-log-YYYYMMDD-HHMMSS.csv` trong `Download/WiFiAnalyzer/`.
3. **Trong lúc đo**, nếu mở Export, bấm Home hay xoay màn hình, logger ghi dòng `PAUSE` / `RESUME` vào **cùng file**. Logger dừng khi app chạy nền.
4. **Bấm ⏸ (Pause):** kết thúc lượt đo, đóng file và **mở bảng chia sẻ** để gửi nó đi. File vẫn nằm trong `Download/WiFiAnalyzer/` kể cả khi không gửi.
5. **Menu ☰ → Export:** xuất **mọi lần quét từ lúc mở app** (không cần bấm Play) thành `WiFiAnalyzer-*.csv`, rồi mở bảng chia sẻ (có mục "Save to file"). Bấm Export nhiều lần thì mỗi lần nhận toàn bộ lịch sử đến thời điểm đó. Lịch sử bắt đầu lại khi tiến trình app khởi động lại (vuốt tắt app, hệ thống dọn app chạy nền…).

**Mẹo:**
- Muốn quét dày hơn (khoảng mỗi 5 giây), vào **Tùy chọn nhà phát triển → tắt "Hạn chế quét tìm Wi-Fi"**. Mặc định Android chỉ cho quét khoảng 4 lần / 2 phút.
- Chép file về máy tính bằng adb thay vì cáp MTP, vì MTP có thể hiện kích thước file cũ:
  ```bash
  adb pull /sdcard/Download/WiFiAnalyzer/ ~/data-wifi/
  ```

---

## 4. Định dạng file CSV

Định dạng chung: UTF-8 có BOM, xuống dòng CRLF, phân cách bằng dấu phẩy; ô nào chứa `,`, `"` hoặc xuống dòng thì được bọc trong ngoặc kép.

App chỉ sinh **hai loại file**, và cả hai dùng **cùng một bộ 41 cột** (định nghĩa ở enum `CsvColumn`, file `export/CsvRow.kt`):

| File | Khi nào tạo | Chứa loại dòng |
|---|---|---|
| `wifi-log-YYYYMMDD-HHMMSS.csv` | Mỗi lượt đo (Play → Pause) | `CONNECTION`, `SCAN` và `RTT`, xen kẽ theo thời gian ghi |
| `WiFiAnalyzer-YYYYMMDD-HHMMSS.csv` | Khi bấm Export trong menu | `SCAN` và `RTT`: mọi quan sát BSSID và mọi kết quả RTT từ lúc mở app tới lúc bấm Export |

### 4.1. Danh sách cột

`epoch_ms, time, row_type, event, ssid, bssid, connected, rssi_dbm, frequency_mhz, primary_channel, center_channel, center_frequency_mhz, width_mhz, range_start_mhz, range_end_mhz, link_speed_mbps, supplicant_state, distance_fspl_m, distance_logdistance_2, distance_logdistance_2_5, distance_logdistance_3, distance_logdistance_3_5, distance_rtt_m, distance_rtt_stddev_m, rtt_status, rtt_mode, rtt_success_count, rtt_attempt_count, standard, security, capabilities, fast_roaming, is_80211mc, vendor, latitude, longitude, accuracy_m, altitude_m, speed_mps, location_provider, location_age_ms`

| Cột | `CONNECTION` (kết nối hiện tại) | `SCAN` (một BSSID quét thấy) | `RTT` (một kết quả đo RTT) |
|---|---|---|---|
| `epoch_ms`, `time` | Lúc ghi dòng | Lúc BSSID thực sự được quét thấy (cả log lẫn Export) | Lúc thực hiện phép đo (`RangingResult.getRangingTimestampMillis()`); lúc nhận kết quả nếu đo thất bại |
| `event` | Lý do ghi dòng (mục 4.3) | Trống | Trống |
| `ssid`, `bssid` | AP đang kết nối (trống khi mất kết nối) | AP quét thấy | AP được đo |
| `connected` | `true` khi đang gắn vào một AP | `true` nếu BSSID này là AP đang kết nối | `true` nếu AP được đo là AP đang kết nối |
| `rssi_dbm` | RSSI của kết nối; trống khi chưa đo được | RSSI thô của lần quét (cả log lẫn Export) | RSSI đo được trong lúc đo RTT; trống nếu đo thất bại |
| `frequency_mhz` | Tần số kênh chính | Tần số kênh chính | Tần số kênh chính của AP (lấy từ kết quả quét) |
| `primary_channel` … `range_end_mhz` | Trống | Kênh, tâm kênh, độ rộng kênh, dải tần | Trống |
| Giá trị không xác định | Android trả `-127` (RSSI) hoặc `-1` (tần số, tốc độ) khi chưa biết: ô để **trống**, không ghi giá trị giả | Như bên trái; số kênh nằm ngoài bảng kênh của app (ví dụ kênh 14) cũng để trống | Đo thất bại: các cột đo đạc để trống |
| `link_speed_mbps`, `supplicant_state` | Tốc độ liên kết, giai đoạn kết nối (`COMPLETED` = dùng được) | Trống | Trống |
| `distance_*` | Tính từ `rssi_dbm` và `frequency_mhz`; trống nếu RSSI không hợp lệ | Như bên trái | `distance_fspl_m`, `distance_logdistance_*` tính từ RSSI của phép đo RTT; `distance_rtt_*`, `rtt_*`: kết quả RTT (chỉ có ở dòng này) |
| `standard` … `vendor` | Trống | Chuẩn Wi-Fi, bảo mật, chuỗi capabilities, roam nhanh, 802.11mc, hãng | Chỉ có `is_80211mc` |
| `latitude` … `location_age_ms` | Vị trí thiết bị lúc ghi dòng | Như bên trái | Vị trí thiết bị gần nhất với lúc đo |

- **Các giá trị của `event`** (và các trường dạng mã khác) được liệt kê ở mục 4.3.
- **Khi phân tích, lọc theo `row_type`:** ví dụ với pandas, `df[df.row_type == "CONNECTION"]`, `df[df.row_type == "SCAN"]` hoặc `df[df.row_type == "RTT"]`.

### 4.2. Ý nghĩa từng trường

Cột **Dòng** cho biết trường có giá trị ở loại dòng nào: **C** = `CONNECTION`, **S** = `SCAN`, **R** = `RTT`. Ở loại dòng khác, ô để trống.

| # | Trường | Kiểu / đơn vị | Ý nghĩa | Dòng | Ví dụ |
|---|---|---|---|---|---|
| 1 | `epoch_ms` | Số nguyên, mili-giây (Unix, UTC) | Thời điểm của dòng, tính từ 1970-01-01 UTC. **C:** lúc ghi dòng. **S:** lúc BSSID thực sự được quét thấy (`ScanResult.timestamp`, đổi từ đồng hồ khởi động sang giờ thật; nếu Android trả giá trị thiếu hoặc ở tương lai thì dùng lúc nhận kết quả). Áp dụng cho cả log và Export. Dùng cột này để sắp xếp hoặc ghép với dữ liệu khác | C, S, R | `1791640797747` |
| 2 | `time` | ISO 8601, giờ địa phương, có mili-giây và múi giờ | Cùng thời điểm với `epoch_ms`, ở dạng dễ đọc | C, S, R | `2026-10-10T20:59:57.747+07:00` |
| 3 | `row_type` | `CONNECTION` / `SCAN` / `RTT` | Loại dòng: trạng thái kết nối hiện tại, một BSSID quét thấy, hay một kết quả đo Wi-Fi RTT | C, S, R | `SCAN` |
| 4 | `event` | Chuỗi; nhiều giá trị ghép bằng `+` | Lý do ghi dòng `CONNECTION` (bảng 4.3) | C | `BSSID+FREQUENCY+RSSI` |
| 5 | `ssid` | Chuỗi | Tên mạng Wi-Fi. **C:** mạng đang kết nối, trống khi mất kết nối. **S:** mạng phát ra BSSID này; `*hidden*` nếu mạng ẩn tên | C, S, R | `PHENIKAA-UNI` |
| 6 | `bssid` | Địa chỉ MAC `aa:bb:cc:dd:ee:ff` | Địa chỉ MAC của một radio trên AP. Một AP vật lý thường có nhiều BSSID (mỗi băng tần, mỗi SSID một BSSID), nên nhiều BSSID có thể cùng một SSID | C, S, R | `76:3a:0e:32:4e:99` |
| 7 | `connected` | `true` / `false` | **C:** `true` khi điện thoại đang gắn vào một AP, `false` khi mất kết nối. **S:** `true` nếu BSSID của dòng là AP đang kết nối. "Gắn vào" chưa chắc đã kết nối xong; xem `supplicant_state` | C, S, R | `true` |
| 8 | `rssi_dbm` | Số nguyên, dBm | Cường độ tín hiệu nhận được, càng gần 0 càng mạnh (bảng 4.3). **C:** RSSI của kết nối, cập nhật khoảng mỗi giây; **trống** khi Android chưa đo được (`-127`). **S:** RSSI thô của lần quét đó, cả log lẫn Export (không phải giá trị đã làm mượt hiển thị trên màn hình). **R:** RSSI của các khung FTM trong lúc đo RTT; trống nếu đo thất bại | C, S, R | `-60` |
| 9 | `frequency_mhz` | MHz | Tần số của kênh chính 20 MHz. 2412–2484: băng 2,4 GHz; 5160–5885: 5 GHz; 5955–7115: 6 GHz. Trống nếu Android không biết | C, S, R | `5320` |
| 10 | `primary_channel` | Số kênh | Số kênh chính, ứng với `frequency_mhz`. Trống nếu tần số nằm ngoài bảng kênh của app (ví dụ kênh 14 = 2484 MHz) | S | `64` |
| 11 | `center_channel` | Số kênh | Kênh ở tâm cả dải khi AP ghép kênh 40/80/160/320 MHz; bằng `primary_channel` nếu kênh 20 MHz | S | `58` |
| 12 | `center_frequency_mhz` | MHz | Tần số ở tâm cả dải kênh | S | `5290` |
| 13 | `width_mhz` | MHz | Độ rộng kênh: 20, 40, 80, 160 hoặc 320 | S | `80` |
| 14 | `range_start_mhz` | MHz | Tần số thấp nhất của dải AP chiếm = `center_frequency_mhz − width_mhz / 2` | S | `5250` |
| 15 | `range_end_mhz` | MHz | Tần số cao nhất của dải AP chiếm = `center_frequency_mhz + width_mhz / 2` | S | `5330` |
| 16 | `link_speed_mbps` | Mbps | Tốc độ liên kết ở tầng vật lý do card Wi-Fi báo. **Không phải** tốc độ tải thực tế. Trống nếu Android không biết | C | `866` |
| 17 | `supplicant_state` | Chuỗi | Giai đoạn kết nối Wi-Fi (bảng 4.3). `COMPLETED` = kết nối xong, dùng được | C | `COMPLETED` |
| 18 | `distance_fspl_m` | Mét, 1 chữ số thập phân | Khoảng cách ước lượng tới AP theo FSPL (mục 4.4), từ `rssi_dbm` và `frequency_mhz`. Trống nếu RSSI không hợp lệ. **Chỉ nên so sánh tương đối** | C, S, R | `4.5` |
| 19 | `distance_logdistance_2` | Mét | Log-distance với hệ số suy hao n = 2 (không gian trống). Luôn bằng `distance_fspl_m` | C, S, R | `4.5` |
| 20 | `distance_logdistance_2_5` | Mét | Log-distance với n = 2,5 (không gian mở, ít vật cản) | C, S, R | `3.3` |
| 21 | `distance_logdistance_3` | Mét | Log-distance với n = 3 (văn phòng, có vách ngăn) | C, S, R | `2.7` |
| 22 | `distance_logdistance_3_5` | Mét | Log-distance với n = 3,5 (nhiều tường bê tông) | C, S, R | `2.4` |
| 23 | `distance_rtt_m` | Mét, 2 chữ số thập phân, **có thể âm** | Khoảng cách **đo được** bằng Wi-Fi RTT (thời gian bay khứ hồi của khung FTM × tốc độ ánh sáng / 2), trung bình các lần đo trong một burst. Sai số thường 1–2 m khi nhìn thấy trực tiếp AP; tường làm kết quả dài ra. Giá trị âm nhỏ ở sát AP là do sai số hiệu chuẩn. Chỉ có khi `rtt_status = SUCCESS` | R | `3.25` |
| 24 | `distance_rtt_stddev_m` | Mét | Độ lệch chuẩn của các lần đo trong burst; lớn nghĩa là kết quả dao động (đa đường, vật cản) | R | `0.42` |
| 25 | `rtt_status` | Chuỗi | Kết quả phép đo với AP này (bảng 4.3) | R | `SUCCESS` |
| 26 | `rtt_mode` | Chuỗi | Cách đo: `80211MC`, `80211AZ` hay `ONE_SIDED` (bảng 4.3) | R | `80211MC` |
| 27 | `rtt_success_count` | Số nguyên | Số lần đo thành công trong burst | R | `7` |
| 28 | `rtt_attempt_count` | Số nguyên | Số lần đo đã thử trong burst | R | `8` |
| 29 | `standard` | Chuỗi | Chuẩn Wi-Fi cao nhất AP hỗ trợ (bảng 4.3); trống nếu Android không xác định được | S | `802.11ax` |
| 30 | `security` | Các giá trị cách nhau bởi dấu cách | Kiểu bảo mật đã chuẩn hóa (bảng 4.3) | S | `WPS WPA2` |
| 31 | `capabilities` | Chuỗi gốc của Android | Mô tả bảo mật gốc. Mỗi cặp ngoặc vuông là một thành phần: `[giao thức-xác thực-mã hóa]` (ví dụ `WPA2-PSK-CCMP-128`: WPA2, mật khẩu chung, mã hóa AES-CCMP 128 bit), `[ESS]` = mạng có AP, `[WPS]` = hỗ trợ WPS | S | `[WPA2-PSK-CCMP-128][RSN-PSK-CCMP-128][ESS][WPS]` |
| 32 | `fast_roaming` | Các giá trị cách nhau bởi dấu cách | Các chuẩn hỗ trợ chuyển AP nhanh mà AP công bố (bảng 4.3); trống nếu không có | S | `802.11k 802.11v` |
| 33 | `is_80211mc` | `true` / `false` | AP có hỗ trợ đo khoảng cách bằng Wi-Fi RTT (802.11mc, FTM) hay không. AP có giá trị `true` mới được đo RTT hai chiều (dòng `RTT`) | S, R | `false` |
| 34 | `vendor` | Chuỗi | Hãng sản xuất, tra từ 3 byte đầu (OUI) của BSSID. **Trống** với BSSID ảo do AP tự sinh (byte đầu có bit "locally administered", ví dụ `5a:…`, `76:…`) hoặc khi không có trong bảng tra | S | `TP LINK SYSTEMS INC` |
| 35 | `latitude` | Độ thập phân, hệ WGS-84, 7 chữ số | Vĩ độ của điện thoại (dương = Bắc), lấy từ **bản định vị gần nhất với `epoch_ms` của dòng**. Với dòng `SCAN`, đó là lúc BSSID được quét thấy | C, S, R | `21.0285123` |
| 36 | `longitude` | Độ thập phân, WGS-84 | Kinh độ (dương = Đông) | C, S, R | `105.8048456` |
| 37 | `accuracy_m` | Mét | Bán kính sai số vị trí theo phương ngang, độ tin cậy 68%, do nguồn vị trí tự ước lượng | C, S, R | `13.9` |
| 38 | `altitude_m` | Mét | Độ cao so với mặt ellipsoid WGS-84. Không đáng tin khi nguồn là `network` | C, S, R | `58.4` |
| 39 | `speed_mps` | m/s, 2 chữ số thập phân | Tốc độ di chuyển của điện thoại; chỉ có khi nguồn vị trí cung cấp (thường là GPS) | C, S, R | `1.25` |
| 40 | `location_provider` | `gps` / `network` / `fused` | Nguồn của vị trí (bảng 4.3) | C, S, R | `network` |
| 41 | `location_age_ms` | Mili-giây, **có dấu** | `epoch_ms` của dòng trừ thời điểm của bản định vị. Dương: bản định vị lấy trước thời điểm của dòng; **âm**: lấy sau (chỉ gặp ở dòng `SCAN`). Trị tuyệt đối lớn (ví dụ > 5000) nghĩa là vị trí không khớp thời gian, nên lọc bỏ khi cần độ chính xác vị trí | C, S, R | `1988` |

### 4.3. Các giá trị có thể gặp

**`event`** (dòng `CONNECTION`; nhiều giá trị có thể ghép bằng `+`, ví dụ `BSSID+FREQUENCY+RSSI` khi máy roam sang AP khác băng tần):

| Giá trị | Ý nghĩa |
|---|---|
| `START` | Dòng đầu tiên của lượt đo (vừa bấm Play) |
| `PAUSE` | App tạm dừng ghi (mở Export, bấm Home, tắt màn hình…), hoặc dòng cuối khi bấm Pause |
| `RESUME` | App quay lại và ghi tiếp trong cùng lượt đo |
| `CONNECTED` | Vừa gắn vào một AP sau khi mất kết nối |
| `DISCONNECTED` | Vừa mất kết nối |
| `SSID` | Đổi sang mạng khác |
| `BSSID` | Đổi sang AP / radio khác (roam) |
| `FREQUENCY` | Đổi tần số / kênh |
| `RSSI` | Cường độ tín hiệu của kết nối thay đổi |
| `LINK_SPEED` | Tốc độ liên kết thay đổi |
| `STATE` | `supplicant_state` thay đổi |

**`supplicant_state`** (các giai đoạn kết nối của Android, theo thứ tự thường gặp):

| Giá trị | Ý nghĩa |
|---|---|
| `SCANNING` | Đang quét tìm mạng |
| `AUTHENTICATING` | Đang xác thực với AP |
| `ASSOCIATING` | Đang gắn vào AP |
| `ASSOCIATED` | Đã gắn vào AP, chưa trao đổi khóa |
| `FOUR_WAY_HANDSHAKE`, `GROUP_HANDSHAKE` | Đang trao đổi khóa mã hóa (WPA) |
| `COMPLETED` | **Kết nối xong, dùng được** |
| `DISCONNECTED`, `INACTIVE`, `DORMANT`, `INTERFACE_DISABLED`, `UNINITIALIZED`, `INVALID` | Không kết nối / Wi-Fi tắt / trạng thái lỗi |

**`standard`:**

| Giá trị | Thế hệ |
|---|---|
| `802.11a/b/g` | Wi-Fi đời cũ (Wi-Fi 1–3) |
| `802.11n` | Wi-Fi 4 |
| `802.11ac` | Wi-Fi 5 |
| `802.11ax` | Wi-Fi 6 / 6E |
| `802.11be` | Wi-Fi 7 |
| `802.11ad` | WiGig (băng 60 GHz) |
| *(trống)* | Không xác định được |

**`security`:** `NONE` (mạng mở, không mật khẩu), `WPS`, `WEP` (cũ, không an toàn), `WPA`, `WPA2`, `WPA3`. Một AP có thể có nhiều giá trị, ví dụ `WPS WPA WPA2`.

**`fast_roaming`:**

| Giá trị | Ý nghĩa |
|---|---|
| `802.11k` | AP gửi danh sách AP lân cận, giúp máy chọn AP mới nhanh hơn |
| `802.11v` | AP có thể gợi ý / điều hướng máy sang AP khác |
| `802.11r` | Chuyển AP nhanh (Fast BSS Transition), không phải xác thực lại từ đầu |
| `OKC` | Opportunistic Key Caching: dùng lại khóa đã có khi chuyển AP |

**`rtt_status`:**

| Giá trị | Ý nghĩa |
|---|---|
| `SUCCESS` | Đo thành công; các cột `distance_rtt_*`, `rtt_*_count`, `rssi_dbm` có giá trị |
| `FAIL` | AP không trả lời khung FTM (ra khỏi vùng phủ, kênh bận, AP tắt FTM…) |
| `RESPONDER_NOT_SUPPORTED` | AP báo không hỗ trợ 802.11mc |
| `STATUS_<số>` | Mã trạng thái khác do phiên bản Android mới hơn trả về |

**`rtt_mode`:**

| Giá trị | Ý nghĩa |
|---|---|
| `80211MC` | RTT hai chiều chuẩn 802.11mc (FTM): AP đo cùng, chính xác nhất (~1–2 m) |
| `80211AZ` | RTT chuẩn 802.11az (Wi-Fi 6/7, Android 15+) |
| `ONE_SIDED` | Chỉ điện thoại đo (AP thường, không hỗ trợ FTM). Chỉ dùng khi máy báo hỗ trợ one-sided RTT (Android 14+). Kém chính xác hơn vì có cả thời gian xử lý của AP |

**`location_provider`:**

| Giá trị | Ý nghĩa | Sai số thường gặp |
|---|---|---|
| `gps` | Vệ tinh định vị (ngoài trời) | 1–5 m |
| `network` | Ước lượng từ Wi-Fi / trạm di động (Google Play Services) | 10–100 m |
| `fused` | Kết hợp nhiều nguồn và cảm biến | Tùy nguồn |

**Cách đọc `rssi_dbm`:**

| RSSI | Chất lượng |
|---|---|
| ≥ −50 dBm | Rất mạnh (ở rất gần AP) |
| −50 … −67 dBm | Tốt, đủ cho mọi ứng dụng |
| −67 … −70 dBm | Ngưỡng thường dùng cho gọi thoại / video ổn định |
| −70 … −80 dBm | Yếu, tốc độ giảm |
| ≤ −80 dBm | Rất yếu, dễ mất kết nối hoặc chuyển AP |
| *(trống)* | Không hợp lệ (Android trả `-127`: chưa đo được) |

### 4.4. Công thức khoảng cách (ước lượng thô)

Cả hai công thức giả định AP phát 0 dBm và ăn-ten 0 dBi.

```
FSPL:          d = 10 ^ ((27.55 − 20·log10(f_MHz) + |RSSI|) / 20)
Log-distance:  d = 10 ^ ((|RSSI| − FSPL(1 m)) / (10·n)),   FSPL(1 m) = 20·log10(f_MHz) − 27.55
```

Với n = 2, log-distance trùng với FSPL. Kết quả chỉ nên dùng để **so sánh tương đối**, vì công suất phát thật của AP (15–23 dBm) và vật cản làm sai lệch con số tuyệt đối rất nhiều.

---

## 5. Các hàm lấy dữ liệu để ghi vào file CSV

### 5.1. Sơ đồ luồng dữ liệu

```
Android WifiManager ──► WiFiManagerWrapper.wiFiInfo() ──► ConnectionLogger.sample() ──► line() ─────────────────────┐
                    └─► ScannerCallback.onSuccess() ──► ScanRecorder.record() ──► CsvExport.row() ──┬─► LogFile (wifi-log, 1 file, khi đang đo)
                        (WiFiManagerWrapper.scanResults(), broadcast SCAN_RESULTS_AVAILABLE)         └─► lịch sử phiên (noBackupFilesDir/scan-history.csv)
                    └─► ScannerCallback.onSuccess() ──► RttRanger.update() ──► WifiRttManager.startRanging() (mỗi 1 s)
                        ──► RangingResultCallback ──► RttRanger.row() ──► ScanRecorder.appendRows() ──► LogFile + lịch sử phiên (như dòng SCAN)
ConnectivityManager.NetworkCallback ──► ConnectionLogger.trigger() (đọc ngay khi kết nối thay đổi)
LocationManager (gps / network / fused) ──► LocationTracker.update() ──► LocationTracker.at(t) / current() ──► cột vị trí
Menu Export ──► MainActivity.exportCsv() ──► ScanRecorder.export() (header + lịch sử phiên) ──► CsvShare (bảng chia sẻ)
```

### 5.2. Bảng hàm theo nhóm cột

| Nhóm dữ liệu | Hàm lấy dữ liệu | File | API Android bên dưới |
|---|---|---|---|
| Kết nối hiện tại (ssid, bssid, rssi, tần số, tốc độ, trạng thái) | `WiFiManagerWrapper.wiFiInfo()` → `ConnectionLogger.sample()` → `WifiInfo.toLogSample()` | `wifi/manager/WiFiManagerWrapper.kt`, `wifi/logger/ConnectionLogger.kt` | `WifiManager.getConnectionInfo()`: `WifiInfo.getSSID/getBSSID/getRssi/getFrequency/getLinkSpeed/getSupplicantState` |
| Chu kỳ đọc kết nối | `ConnectionLogger.poll()` (mỗi `INTERVAL_MS` = 1000 ms), `ConnectionLogger.trigger()` | `wifi/logger/ConnectionLogger.kt` | `Handler.postDelayed`; `ConnectivityManager.registerNetworkCallback` (`onAvailable/onLost/onCapabilitiesChanged`) |
| Sự kiện (`event`) | `ConnectionLogger.changes(previous, current)` | `wifi/logger/ConnectionLogger.kt` | So sánh hai mẫu liên tiếp |
| Ghép một dòng `CONNECTION` | `ConnectionLogger.line(timeMs, events, sample, location)` | `wifi/logger/ConnectionLogger.kt` | |
| Kết quả quét (mọi BSSID) | `ScannerCallback.onSuccess()` → `ScanRecorder.record()` → `rows()` (lọc quan sát mới theo cặp BSSID + SSID và `ScanResult.timestamp`) | `wifi/scanner/ScannerCallback.kt`, `wifi/logger/ScanRecorder.kt` | `WifiManager.getScanResults()`; kích hoạt bởi broadcast `SCAN_RESULTS_AVAILABLE_ACTION` và lần quét định kỳ của app |
| Thời điểm quét thấy BSSID | `ScanRecorder.rows()` / `seenNanos()`: đổi `ScanResult.timestamp` (µs từ lúc khởi động) sang giờ thật | `wifi/logger/ScanRecorder.kt` | `ScanResult.timestamp`, `SystemClock.elapsedRealtimeNanos()` |
| Chuyển `ScanResult` → thông tin AP | `ScanResult.toWiFiDetail(level)` | `wifi/scanner/Transformer.kt` | `ScanResult.SSID/wifiSsid/BSSID/level/frequency/centerFreq0/centerFreq1/channelWidth/capabilities` |
| Kênh, tần số, độ rộng | `WiFiSignal.primaryWiFiChannel`, `centerWiFiChannel`, `wiFiWidth`, `wiFiChannelStart/End` | `wifi/model/WiFiSignal.kt`, `wifi/model/WiFiWidth.kt` | Tính từ `ScanResult` |
| Chuẩn Wi-Fi (`standard`) | `WiFiStandard.findOne(scanResult)`, `WiFiSignalExtra.wiFiStandardDisplay()` | `wifi/model/WiFiStandard.kt`, `wifi/model/WiFiSignal.kt` | `ScanResult.getWifiStandard()` |
| Bảo mật (`security`, `capabilities`) | `WiFiSecurity(capabilities, WiFiSecurityType.find(scanResult)).securities` | `wifi/model/WiFiSecurity.kt` | `ScanResult.capabilities`, `ScanResult.getSecurityTypes()` |
| Roam nhanh (`fast_roaming`) | `FastRoaming.find(scanResult)`, `WiFiSignalExtra.fastRoamingDisplay()` | `wifi/model/FastRoaming.kt` | `ScanResult.getInformationElements()` |
| 802.11mc (`is_80211mc`) | `ScanResult.is80211mcResponder` | `wifi/scanner/Transformer.kt` | |
| Chọn AP để đo RTT | `RttRanger.update(scanResults, connectedBssid)` → `targets()`: kết quả quét trong 30 s gần nhất, AP đang kết nối trước, rồi theo RSSI; AP 802.11mc/az trước, AP thường (one-sided) chỉ khi máy hỗ trợ; tối đa `RangingRequest.getMaxPeers()` (10) | `wifi/logger/RttRanger.kt` | `ScanResult.is80211mcResponder`, `ScanResult.is80211azNtbResponder` (Android 15+), `WifiRttManager.getRttCharacteristics()` (Android 14+) |
| Đo RTT | `RttRanger.rangeTargets()` → `request()`; lặp lại sau mỗi `INTERVAL_MS` = 1000 ms (`finished()`); dừng ở `stop()` (`MainActivity.onPause`) | `wifi/logger/RttRanger.kt` | `PackageManager.FEATURE_WIFI_RTT`, `WifiRttManager.isAvailable/startRanging`, `RangingRequest.Builder.addAccessPoint/addNon80211mcCapableAccessPoint`; quyền `NEARBY_WIFI_DEVICES` (Android 13+) hoặc `ACCESS_FINE_LOCATION` |
| Kết quả RTT (`distance_rtt_m`, `distance_rtt_stddev_m`, `rtt_*`, `rssi_dbm` của dòng `RTT`) | `RttRanger.measurement(result)` → `row(measurement, target, connectedBssid, nowMs, nowNanos)` → `ScanRecorder.appendRows()` | `wifi/logger/RttRanger.kt`, `wifi/logger/ScanRecorder.kt` | `RangingResult.getStatus/getMacAddress/getDistanceMm/getDistanceStdDevMm/getNumSuccessfulMeasurements/getNumAttemptedMeasurements/getRssi/getRangingTimestampMillis` |
| Hiển thị RTT trên màn hình | `RttRanger.latest(bssid)` → `WiFiDetailView.distance()` | `wifi/logger/RttRanger.kt`, `wifi/detailview/WiFiDetailView.kt` | |
| Hãng sản xuất (`vendor`) | `VendorService.findVendorName(bssid)` | `vendor/model/VendorService.kt` | Bảng tra OUI có sẵn trong app |
| Cờ `connected` | `SCAN` (log và Export): so sánh BSSID với `WifiInfo.getBSSID()` lúc nhận kết quả quét (`ScanRecorder.rows()`); `CONNECTION`: có BSSID hay không (`ConnectionLogger.line()`) | `wifi/logger/ScanRecorder.kt`, `wifi/logger/ConnectionLogger.kt` | |
| Ghép một dòng `SCAN` (log và Export) | `CsvExport.row(context, date, wiFiDetail, connected, location)` | `export/CsvExport.kt` | |
| Bộ cột chung, header, giá trị thời gian / vị trí / khoảng cách | `CsvColumn`, `CSV_HEADER`, `csvRow()`, `timeValues()`, `locationValues()`, `distanceValues()` | `export/CsvRow.kt` | |
| Vị trí (lat, lon, accuracy, altitude, speed, provider, age) | `LocationTracker.start()` (khi app ở foreground) → `update()` / `better()` → `at(t)` (bản gần `t` nhất) / `current()` | `wifi/logger/LocationTracker.kt` | `LocationManagerCompat.requestLocationUpdates` (gps / network / fused, mỗi 1 s), `LocationManager.getLastKnownLocation`, `Location.getAccuracy/getAltitude/getSpeed/getElapsedRealtimeNanos` |
| Khoảng cách | `calculateDistance(frequency, rssi)`, `calculateDistanceLogDistance(frequency, rssi, n)` → `distanceFields()` | `wifi/model/WiFiUtils.kt`, `export/Csv.kt` | |
| Dữ liệu cho Export | `ScanRecorder.export(target, onDone)`: BOM + header + lịch sử phiên, chạy trên luồng nền | `wifi/logger/ScanRecorder.kt`, `MainActivity.kt` | Kết quả quét thô (không dùng `Cache` làm mượt của màn hình) |
| Ghi file log | `LogFile.open()` / `write()` / `close()`. Một `LogFile` dùng chung cho `ConnectionLogger` (luồng logger) và `ScanRecorder` (luồng ghi riêng); mọi thao tác được đồng bộ và mỗi lần ghi là một khối liền nên các dòng không bị xen lẫn | `wifi/logger/LogFile.kt` | `MediaStore.Downloads` (Android 10+), `getExternalFilesDir` (Android cũ hơn) |
| Ghi và chia sẻ file Export | `CsvShare.prepare()`, `CsvShare.chooser(file, saveIntent)`; lưu tùy chọn: `MainActivity.saveExport()` → `onExportCsvResult()` | `export/CsvShare.kt`, `MainActivity.kt` | `FileProvider`, `Intent.ACTION_SEND`, `ActivityResultContracts.CreateDocument` |
| Chia sẻ file của lượt đo | `MainActivity.stopRun()` → `ConnectionLogger.close(callback)` → `files()` → `MainActivity.shareRun()` → `CsvShare.chooser(uris)` | `MainActivity.kt`, `wifi/logger/ConnectionLogger.kt`, `export/CsvShare.kt` | `Intent.ACTION_SEND` (1 file) hoặc `ACTION_SEND_MULTIPLE` (nhiều file), kiểu `text/csv` + `Intent.EXTRA_ALTERNATE_INTENTS` với `application/vnd.ms-excel` |
| Định dạng CSV | `csvLine()`, `csvEscape()`, `csvFormat()`, `CSV_BOM`; lọc giá trị không hợp lệ: `validRssi()`, `positiveOrNull()` | `export/Csv.kt` | |

(Các đường dẫn ở bảng trên tính từ `app/src/main/kotlin/com/vrem/wifianalyzer/`.)

---

## 6. Build và kiểm tra

**Yêu cầu:** JDK 21, Android SDK với `platforms;android-37.0` và `build-tools;37.0.0`. Khai báo đường dẫn SDK trong `local.properties` (file này đã nằm trong `.gitignore`):
```properties
sdk.dir=/home/<user>/Android/Sdk
```

**Lệnh:**
```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew ktlintCheck
./gradlew lintDebug
./gradlew testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Máy ít RAM:** cấu hình mặc định cho phép tới 4 tiến trình test chạy song song (mỗi tiến trình 2 GB heap). Trên máy 14 GB RAM đang mở nhiều ứng dụng, chạy toàn bộ test có thể làm treo máy. Nên giới hạn:
```bash
./gradlew --max-workers=2 -Dorg.gradle.jvmargs="-Xmx3g -XX:MaxMetaspaceSize=1g" -Pkotlin.compiler.execution.strategy=in-process testDebugUnitTest
./gradlew --stop
```

**Trạng thái kiểm tra lúc bàn giao (2026-10-11, sau khi chuyển Export sang chuỗi thời gian):**
- Toàn bộ 3763 unit test (`./gradlew testDebugUnitTest`) đều pass. Trong đó `MainRobolectricTest.scanner` (test của repo gốc, kỳ vọng app tự quét / tự chạy khi mở) đã được sửa theo hành vi của bản này: mở app nút là ▶, bấm thì thành ⏸, bấm lần nữa lại về ▶.
- `ktlintCheck`: 0 lỗi. `lintDebug`: 0 lỗi; các cảnh báo còn lại là của repo gốc hoặc `MissingTranslation` (mục 7).
- **Chưa** chạy lại test instrumentation (`connectedDebugAndroidTest`) và **chưa** thử bản mới trên điện thoại thật.
- Đã thử trên vivo iQOO Z10 Turbo+ (Android 16).

---

## 7. Hạn chế đã biết

- **RTT chỉ đo được với AP hỗ trợ 802.11mc/802.11az** (cột `is_80211mc = true`). Trong dữ liệu đã đo ở trường, **không AP nào** hỗ trợ 802.11mc, nên ở đó sẽ không có dòng `RTT` và khoảng cách vẫn chỉ là ước lượng từ RSSI. Muốn có RTT cần AP bật FTM responder (ví dụ Google/Nest Wifi, AP doanh nghiệp bật FTM, hoặc PC Linux có card Intel AX200/AX210 chạy `hostapd` với `ftm_responder=1`).
- **RTT cần quyền "Thiết bị ở gần" (`NEARBY_WIFI_DEVICES`)** trên Android 13+; app xin quyền này khi bấm Play. Nếu từ chối, app vẫn chạy nhưng không đo RTT.
- **Android hạn chế RTT khi app ở nền**, nên vòng đo RTT dừng khi app không hiển thị.
- **`connected` / `bssid` trong log nghĩa là "đã gắn vào AP"**, có thể chưa đạt `COMPLETED`. Khi phân tích, hãy lọc theo `supplicant_state`.
- **Logger chỉ chạy khi app hiển thị trên màn hình.** Muốn ghi log khi tắt màn hình cần thêm foreground service loại `location`.
- **Dòng đầu tiên sau khi mở app có thể mang vị trí cũ**, lấy từ `getLastKnownLocation`. Xem cột `location_age_ms`.
- **Lịch sử cho Export nằm trên bộ nhớ trong** (`noBackupFilesDir/scan-history.csv`), không giới hạn dung lượng; quét dày (mỗi 5 s, ~50 BSSID) tốn khoảng 10–15 MB mỗi giờ. Lịch sử bị xóa khi tiến trình app khởi động lại.
- **Dòng `SCAN` được sắp theo thời gian trong từng lần quét**; giữa hai lần quét liên tiếp có thể lệch nhẹ. Khi phân tích, sắp xếp theo `epoch_ms`.
- **Định vị chạy liên tục khi app ở foreground** (để Export có vị trí cả khi không bấm Play), tốn pin hơn bản gốc.
- **Kênh ghép 80+80 MHz** (rất hiếm): app gốc lấy tâm của đoạn 80 MHz thứ hai làm `center_frequency_mhz`, nên `range_*` chỉ mô tả đoạn đó.
- **Trong nhà, vị trí lấy từ `network`** (sai số 10–100 m) và cập nhật khoảng mỗi 20 giây.
- **Chuỗi mới chỉ có tiếng Anh** (lint báo `MissingTranslation`).
- **Ở màn hình biểu đồ, nút chọn băng tần có thể nằm trong menu ⋮** vì thanh tiêu đề không đủ chỗ.
- **Quyền riêng tư:** CSV chứa tọa độ chính xác của thiết bị. `AGENTS.md` của repo gốc ghi app "không thu thập dữ liệu cá nhân"; bản này chỉ lưu dữ liệu trong máy và chỉ gửi đi khi người dùng chủ động chia sẻ. Dữ liệu đo được giữ **ngoài repo** (`~/data-wifi/`); không commit file CSV đo lên repo nếu không thật sự cần.
