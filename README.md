# WiFiAnalyzer – bản sửa đổi (ghi log CSV, vị trí, Wi-Fi RTT)

> **Đây không phải repo chính thức của WiFiAnalyzer.** Đây là bản sửa đổi (modified version) của [WiFiAnalyzer](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer), một phần mềm mã nguồn mở của VREM Software Development, phát hành theo giấy phép [GNU GPLv3](LICENSE). Bản này không do tác giả gốc phát hành, xác nhận hay hỗ trợ. Lỗi của bản này hãy báo ở repo này, đừng gửi lên repo gốc.

Bản này giữ nguyên các chức năng phân tích Wi-Fi của bản gốc. Ngoài ra, nó ghi lại theo thời gian kết nối hiện tại, mọi BSSID quét thấy, vị trí thiết bị và khoảng cách đo bằng Wi-Fi RTT vào file CSV, rồi chia sẻ file qua bảng chia sẻ của Android.

**Lưu ý:** cũng như bản gốc, WiFiAnalyzer không phải công cụ dò mật khẩu hay lừa đảo (phishing) Wi-Fi.

## Mục lục

1. [Nguồn gốc](#1-nguồn-gốc)
2. [Giấy phép](#2-giấy-phép)
3. [Quyền riêng tư](#3-quyền-riêng-tư)
4. [Chức năng thay đổi so với bản gốc](#4-chức-năng-thay-đổi-so-với-bản-gốc)
5. [Danh sách file đã thêm / sửa / xóa](#5-danh-sách-file-đã-thêm--sửa--xóa-so-với-commit-7e2019d)
6. [Cách sử dụng trên điện thoại](#6-cách-sử-dụng-trên-điện-thoại)
7. [Định dạng file CSV](#7-định-dạng-file-csv)
8. [Các hàm lấy dữ liệu để ghi vào file CSV](#8-các-hàm-lấy-dữ-liệu-để-ghi-vào-file-csv)
9. [Build và kiểm tra](#9-build-và-kiểm-tra)
10. [Hạn chế đã biết](#10-hạn-chế-đã-biết)

---

## 1. Nguồn gốc

| | |
|---|---|
| Dự án gốc | [VREMSoftwareDevelopment/WiFiAnalyzer](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer) |
| Tác giả gốc | VREM Software Development và [những người đóng góp](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer/graphs/contributors) cho repo gốc |
| Phiên bản gốc | WiFiAnalyzer 3.3.1 (versionCode 72), commit [`7e2019d`](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer/commit/7e2019d294d4ac40b70e770d9e2c55fdc1b18281) ngày 2026-10-03 ("Fix instrumented tests") |
| Người sửa đổi | QuyenAnh2002 và Pham Anh Dung, tháng 10–11/2026 (mục 4, 5 và lịch sử git của repo này) |
| Giấy phép | GNU GPL phiên bản 3 hoặc mới hơn, giống bản gốc (mục 2) |
| Package bản debug | `com.vrem.wifianalyzer.BETA`, logo xám, cài song song được với app gốc |

Lịch sử git của repo này bắt đầu từ commit `a946ef0` (2026-10-10). Commit đó gồm mã nguồn gốc ở commit `7e2019d` và các thay đổi đầu tiên, nên repo này không chứa lịch sử của repo gốc. Để xem chính xác mọi khác biệt so với bản gốc:

```bash
git fetch --depth=1 https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer.git 7e2019d294d4ac40b70e770d9e2c55fdc1b18281
git diff --stat FETCH_HEAD HEAD
```

**Trích dẫn:** thông tin trích dẫn bản này nằm trong [CITATION.cff](CITATION.cff); GitHub hiển thị nó qua nút "Cite this repository". Khi dùng bản này trong bài báo, hãy trích dẫn cả WiFiAnalyzer gốc (đã khai báo trong mục `references` của file đó).

Các file `USER_MANUAL.md`, `CONTRIBUTING.md`, `SECURITY.md`, `AGENTS.md` là tài liệu của repo gốc (giữ nguyên), mô tả các chức năng gốc của app. Hướng dẫn đóng góp và link báo lỗi trong đó trỏ tới repo gốc: chỉ gửi lên đó những lỗi xảy ra cả trên app gốc.

---

## 2. Giấy phép

[<img src="https://www.gnu.org/graphics/gplv3-127x51.png" alt="GPLv3">](https://www.gnu.org/licenses/gpl-3.0.html)

WiFiAnalyzer được phát hành theo GNU General Public License v3.0 (GPLv3), và bản sửa đổi này cũng vậy. Toàn bộ mã nguồn trong repo, kể cả phần mới viết, được phát hành theo GPL phiên bản 3 hoặc (tùy bạn chọn) bất kỳ phiên bản nào mới hơn, không kèm điều khoản bổ sung nào. Toàn văn giấy phép nằm trong file [LICENSE](LICENSE), giống hệt file của repo gốc.

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

### 2.1. Cách bản này đáp ứng các yêu cầu chính của GPLv3

README của repo gốc nêu 4 yêu cầu chính của GPLv3. Bản này đáp ứng như sau:

| Yêu cầu | Nội dung (điều khoản GPLv3) | Bản này |
|---|---|---|
| **Disclose Source** – công bố mã nguồn | Ai nhận được bản chạy (APK) cũng phải nhận được toàn bộ mã nguồn tương ứng, kể cả script build (mục 1, 6) | Toàn bộ mã nguồn và script build nằm trong repo này. Repo hiện để **private**, nên phải làm theo mục 2.2 trước khi đưa APK cho người khác |
| **License and Copyright Notice** – giữ giấy phép và thông báo bản quyền | Giữ nguyên giấy phép, các thông báo bản quyền và thông báo không bảo hành của tác giả gốc; giao diện tương tác phải hiển thị các thông báo đó (mục 4, 5d) | Giữ nguyên `LICENSE` và header bản quyền của VREM Software Development trong mọi file, kể cả file mới. Màn hình **About** của app vẫn hiển thị bản quyền và toàn văn GPLv3, cùng giấy phép Apache 2.0 của các thư viện bên thứ ba (bản này không thêm thư viện mới) |
| **Same License** – cùng giấy phép | Toàn bộ bản sửa đổi phải phát hành theo GPLv3, không thêm hạn chế nào cho người nhận (mục 5c, 10) | Toàn bộ repo theo GPLv3 hoặc mới hơn, không có điều khoản bổ sung |
| **State Changes** – ghi rõ thay đổi | Bản sửa đổi phải có thông báo rõ ràng rằng đã bị sửa, kèm thời điểm sửa (mục 5a) | Mục 4 và 5 của README này liệt kê mọi chức năng và file đã thêm / sửa / xóa so với commit gốc. Header của mỗi file `.kt` / `.xml` đã sửa hoặc thêm có hai dòng ghi chú ngay dưới dòng bản quyền gốc (ví dụ bên dưới). Lịch sử git ghi lại từng thay đổi kèm ngày |

Header của một file gốc đã sửa:

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

File mới thêm dùng hai dòng `Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung` và `Added in 2026 to this modified version of WiFiAnalyzer; see README.md`. File XML dùng cùng nội dung, với tiền tố `~` thay cho `*`. Ảnh PNG không có header; chúng được liệt kê ở mục 5.3.

**Khi sửa tiếp:** thêm hai dòng tương ứng vào header của file gốc vừa sửa lần đầu hoặc file mới, và cập nhật mục 4–5. Script `scripts/check-license-headers.sh` (chạy trong CI) chỉ kiểm tra header có dòng của VREM và dòng GPL, nên sẽ không báo lỗi khi thiếu hai dòng ghi chú này.

### 2.2. Khi đưa APK cho người khác

Nếu chỉ dùng nội bộ, không đưa bản build cho ai, GPLv3 không đòi hỏi gì thêm. Khi đưa APK (hoặc bất kỳ bản build nào) cho người khác, kể cả miễn phí:

1. **Kèm mã nguồn tương ứng** với đúng bản build đó (đúng commit đã dùng để build), theo một trong các cách ở mục 6 của GPLv3. Ví dụ: gửi kèm mã nguồn cùng APK, hoặc ghi link tải mã nguồn ngay cạnh link tải APK. Repo này đang để **private**, nên chỉ đưa link repo là **chưa đủ**: phải chuyển repo sang public, hoặc gửi kèm mã nguồn, ví dụ:
   ```bash
   git archive --format=zip -o wifianalyzer-src.zip HEAD
   ```
2. **Kèm giấy phép:** người nhận phải nhận được bản GPLv3. File `LICENSE` có trong mã nguồn, và màn hình About của app cũng hiển thị giấy phép này.
3. **Không thêm hạn chế** lên quyền của người nhận (cấm chia sẻ lại, cấm sửa, bắt ký thỏa thuận riêng…).
4. **Không để người nhận nhầm với app chính thức.** GPLv3 không bắt đổi tên, nhưng bản release của repo này vẫn dùng package `com.vrem.wifianalyzer` và tên "WiFiAnalyzer" của app gốc trên Google Play và F-Droid. Không đăng bản release lên các cửa hàng đó. Nếu cần phát hành rộng rãi, hãy đổi `applicationId` và tên app trước. Màn hình About vẫn ghi tên công ty và link trang chủ của bản gốc. Mã SHA-256 trong `SECURITY.md` là chữ ký của bản phát hành chính thức, nên APK build từ repo này sẽ không khớp.

---

## 3. Quyền riêng tư

Bản gốc không thu thập thông tin cá nhân và không cần quyền Internet. So với bản gốc:

- Bản này vẫn **không xin quyền Internet** và không tự gửi dữ liệu đi đâu. Dữ liệu chỉ rời máy khi người dùng tự bấm chia sẻ.
- **File CSV chứa dữ liệu nhạy cảm:** tọa độ chính xác của điện thoại theo thời gian, cùng SSID / BSSID của các mạng xung quanh. Hãy cân nhắc trước khi chia sẻ file.
- Dữ liệu nằm ở `Download/WiFiAnalyzer/` (file log của từng lượt đo, giữ lại cho đến khi người dùng xóa) và `noBackupFilesDir/scan-history.csv` (lịch sử cho Export, xóa khi tiến trình app khởi động lại).
- Quyền mới: `ACCESS_NETWORK_STATE` (theo dõi thay đổi kết nối). Khi bấm Play, app xin thêm quyền `NEARBY_WIFI_DEVICES` (đã khai báo sẵn ở bản gốc) để đo RTT trên Android 13+.
- Dữ liệu đo ở trường được giữ **ngoài repo** (`~/data-wifi/`). Đừng commit file CSV đo được lên repo nếu không thật sự cần.

---

## 4. Chức năng thay đổi so với bản gốc

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

## 5. Danh sách file đã thêm / sửa / xóa (so với commit `7e2019d`)

Danh sách này được đối chiếu với commit gốc `7e2019d` bằng lệnh ở mục 1. Mọi file `.kt` / `.xml` dưới đây có thêm hai dòng ghi chú sửa đổi trong header (mục 2.1). Các file không có trong danh sách giữ nguyên như bản gốc.

### 5.1. File mã nguồn mới

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

### 5.2. File mã nguồn đã sửa

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

### 5.3. Tài nguyên và cấu hình

| File | Thay đổi |
|---|---|
| `app/src/main/AndroidManifest.xml` | Thêm quyền `ACCESS_NETWORK_STATE` (cho `NetworkCallback`); khai báo `FileProvider` |
| `app/src/main/res/xml/file_paths.xml` (mới) | Thư mục được phép chia sẻ: `cache/exports/` và thư mục file riêng của app |
| `app/src/main/res/menu/optionmenu.xml` | Thêm mục bộ đếm `action_run_timer`; nút run luôn hiển thị (`showAsAction="always"`) |
| `app/src/main/res/layout/run_timer.xml` (mới) | Giao diện bộ đếm |
| `app/src/main/res/values/strings.xml` | Thêm `export_saved`, `export_save_to_file`, `run_timer_title`, `run_share_title` (chỉ có tiếng Anh) |
| `app/src/debug/res/drawable/ic_launcher_*.xml`, `app/src/debug/res/mipmap-*/ic_launcher*.png` (mới) | Logo xám, chỉ áp dụng cho bản debug |
| `app/build.properties` | Chỉ khác `version_build` (bộ đếm tự tăng mỗi lần chạy `testDebugUnitTest`); phiên bản vẫn là 3.3.1, versionCode 72 như bản gốc |
| `README.md` | Thay README của repo gốc bằng file này |

### 5.4. Test

- **Test mới:** `CsvTest`, `CsvRowTest`, `CsvExportTest`, `CsvShareTest` (`app/src/test/.../export/`), `ConnectionLoggerTest`, `ScanRecorderTest`, `RttRangerTest`, `LocationTrackerTest`, `LogFileTest` (`app/src/test/.../wifi/logger/`), `RunTimerTest` (`app/src/test/.../navigation/options/`).
- **Test đã sửa:** `MainActivityTest`, `MainContextHelper`, `MainRobolectricTest`, `ExportItemTest`, `OptionActionTest`, `ScannerSwitchOnTest`, `ScannerSwitchOffTest`, `ScannerCallbackTest`, `ScannerServiceTest`, `TransformerTest`, `WiFiUtilsTest`.

### 5.5. File đã xóa

- `app/src/main/kotlin/com/vrem/wifianalyzer/export/Export.kt`, `ExportIntent.kt`: luồng Export dạng văn bản của bản gốc, thay bằng `CsvExport.kt` và `CsvShare.kt`.
- `app/src/test/kotlin/com/vrem/wifianalyzer/export/ExportTest.kt`, `ExportIntentTest.kt`: test của hai file trên.

### 5.6. Công cụ phân tích (ngoài app)

- `plot_roaming.py` (**không nằm trong repo**, hiện ở `~/data-wifi/` cùng dữ liệu đo): tạo bản đồ HTML từ một file `wifi-log-*.csv`. Dòng `SCAN` dùng để vẽ đường đi và tìm điểm đổi BSSID; dòng `CONNECTION` cho biết lúc mất kết nối và RSSI của kết nối. Script vẫn đọc được cặp file cũ `wifi-scan-*.csv` + `wifi-log-*.csv`.
  ```bash
  python3 ~/data-wifi/plot_roaming.py ~/data-wifi/WiFiAnalyzer/wifi-log-YYYYMMDD-HHMMSS.csv
  ```

---

## 6. Cách sử dụng trên điện thoại

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

## 7. Định dạng file CSV

Định dạng chung: UTF-8 có BOM, xuống dòng CRLF, phân cách bằng dấu phẩy; ô nào chứa `,`, `"` hoặc xuống dòng thì được bọc trong ngoặc kép.

App chỉ sinh **hai loại file**, và cả hai dùng **cùng một bộ 41 cột** (định nghĩa ở enum `CsvColumn`, file `export/CsvRow.kt`):

| File | Khi nào tạo | Chứa loại dòng |
|---|---|---|
| `wifi-log-YYYYMMDD-HHMMSS.csv` | Mỗi lượt đo (Play → Pause) | `CONNECTION`, `SCAN` và `RTT`, xen kẽ theo thời gian ghi |
| `WiFiAnalyzer-YYYYMMDD-HHMMSS.csv` | Khi bấm Export trong menu | `SCAN` và `RTT`: mọi quan sát BSSID và mọi kết quả RTT từ lúc mở app tới lúc bấm Export |

### 7.1. Danh sách cột

`epoch_ms, time, row_type, event, ssid, bssid, connected, rssi_dbm, frequency_mhz, primary_channel, center_channel, center_frequency_mhz, width_mhz, range_start_mhz, range_end_mhz, link_speed_mbps, supplicant_state, distance_fspl_m, distance_logdistance_2, distance_logdistance_2_5, distance_logdistance_3, distance_logdistance_3_5, distance_rtt_m, distance_rtt_stddev_m, rtt_status, rtt_mode, rtt_success_count, rtt_attempt_count, standard, security, capabilities, fast_roaming, is_80211mc, vendor, latitude, longitude, accuracy_m, altitude_m, speed_mps, location_provider, location_age_ms`

| Cột | `CONNECTION` (kết nối hiện tại) | `SCAN` (một BSSID quét thấy) | `RTT` (một kết quả đo RTT) |
|---|---|---|---|
| `epoch_ms`, `time` | Lúc ghi dòng | Lúc BSSID thực sự được quét thấy (cả log lẫn Export) | Lúc thực hiện phép đo (`RangingResult.getRangingTimestampMillis()`); lúc nhận kết quả nếu đo thất bại |
| `event` | Lý do ghi dòng (mục 7.3) | Trống | Trống |
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

- **Các giá trị của `event`** (và các trường dạng mã khác) được liệt kê ở mục 7.3.
- **Khi phân tích, lọc theo `row_type`:** ví dụ với pandas, `df[df.row_type == "CONNECTION"]`, `df[df.row_type == "SCAN"]` hoặc `df[df.row_type == "RTT"]`.

### 7.2. Ý nghĩa từng trường

Cột **Dòng** cho biết trường có giá trị ở loại dòng nào: **C** = `CONNECTION`, **S** = `SCAN`, **R** = `RTT`. Ở loại dòng khác, ô để trống.

| # | Trường | Kiểu / đơn vị | Ý nghĩa | Dòng | Ví dụ |
|---|---|---|---|---|---|
| 1 | `epoch_ms` | Số nguyên, mili-giây (Unix, UTC) | Thời điểm của dòng, tính từ 1970-01-01 UTC. **C:** lúc ghi dòng. **S:** lúc BSSID thực sự được quét thấy (`ScanResult.timestamp`, đổi từ đồng hồ khởi động sang giờ thật; nếu Android trả giá trị thiếu hoặc ở tương lai thì dùng lúc nhận kết quả). Áp dụng cho cả log và Export. Dùng cột này để sắp xếp hoặc ghép với dữ liệu khác | C, S, R | `1791640797747` |
| 2 | `time` | ISO 8601, giờ địa phương, có mili-giây và múi giờ | Cùng thời điểm với `epoch_ms`, ở dạng dễ đọc | C, S, R | `2026-10-10T20:59:57.747+07:00` |
| 3 | `row_type` | `CONNECTION` / `SCAN` / `RTT` | Loại dòng: trạng thái kết nối hiện tại, một BSSID quét thấy, hay một kết quả đo Wi-Fi RTT | C, S, R | `SCAN` |
| 4 | `event` | Chuỗi; nhiều giá trị ghép bằng `+` | Lý do ghi dòng `CONNECTION` (bảng 7.3) | C | `BSSID+FREQUENCY+RSSI` |
| 5 | `ssid` | Chuỗi | Tên mạng Wi-Fi. **C:** mạng đang kết nối, trống khi mất kết nối. **S:** mạng phát ra BSSID này; `*hidden*` nếu mạng ẩn tên | C, S, R | `PHENIKAA-UNI` |
| 6 | `bssid` | Địa chỉ MAC `aa:bb:cc:dd:ee:ff` | Địa chỉ MAC của một radio trên AP. Một AP vật lý thường có nhiều BSSID (mỗi băng tần, mỗi SSID một BSSID), nên nhiều BSSID có thể cùng một SSID | C, S, R | `76:3a:0e:32:4e:99` |
| 7 | `connected` | `true` / `false` | **C:** `true` khi điện thoại đang gắn vào một AP, `false` khi mất kết nối. **S:** `true` nếu BSSID của dòng là AP đang kết nối. "Gắn vào" chưa chắc đã kết nối xong; xem `supplicant_state` | C, S, R | `true` |
| 8 | `rssi_dbm` | Số nguyên, dBm | Cường độ tín hiệu nhận được, càng gần 0 càng mạnh (bảng 7.3). **C:** RSSI của kết nối, cập nhật khoảng mỗi giây; **trống** khi Android chưa đo được (`-127`). **S:** RSSI thô của lần quét đó, cả log lẫn Export (không phải giá trị đã làm mượt hiển thị trên màn hình). **R:** RSSI của các khung FTM trong lúc đo RTT; trống nếu đo thất bại | C, S, R | `-60` |
| 9 | `frequency_mhz` | MHz | Tần số của kênh chính 20 MHz. 2412–2484: băng 2,4 GHz; 5160–5885: 5 GHz; 5955–7115: 6 GHz. Trống nếu Android không biết | C, S, R | `5320` |
| 10 | `primary_channel` | Số kênh | Số kênh chính, ứng với `frequency_mhz`. Trống nếu tần số nằm ngoài bảng kênh của app (ví dụ kênh 14 = 2484 MHz) | S | `64` |
| 11 | `center_channel` | Số kênh | Kênh ở tâm cả dải khi AP ghép kênh 40/80/160/320 MHz; bằng `primary_channel` nếu kênh 20 MHz | S | `58` |
| 12 | `center_frequency_mhz` | MHz | Tần số ở tâm cả dải kênh | S | `5290` |
| 13 | `width_mhz` | MHz | Độ rộng kênh: 20, 40, 80, 160 hoặc 320 | S | `80` |
| 14 | `range_start_mhz` | MHz | Tần số thấp nhất của dải AP chiếm = `center_frequency_mhz − width_mhz / 2` | S | `5250` |
| 15 | `range_end_mhz` | MHz | Tần số cao nhất của dải AP chiếm = `center_frequency_mhz + width_mhz / 2` | S | `5330` |
| 16 | `link_speed_mbps` | Mbps | Tốc độ liên kết ở tầng vật lý do card Wi-Fi báo. **Không phải** tốc độ tải thực tế. Trống nếu Android không biết | C | `866` |
| 17 | `supplicant_state` | Chuỗi | Giai đoạn kết nối Wi-Fi (bảng 7.3). `COMPLETED` = kết nối xong, dùng được | C | `COMPLETED` |
| 18 | `distance_fspl_m` | Mét, 1 chữ số thập phân | Khoảng cách ước lượng tới AP theo FSPL (mục 7.4), từ `rssi_dbm` và `frequency_mhz`. Trống nếu RSSI không hợp lệ. **Chỉ nên so sánh tương đối** | C, S, R | `4.5` |
| 19 | `distance_logdistance_2` | Mét | Log-distance với hệ số suy hao n = 2 (không gian trống). Luôn bằng `distance_fspl_m` | C, S, R | `4.5` |
| 20 | `distance_logdistance_2_5` | Mét | Log-distance với n = 2,5 (không gian mở, ít vật cản) | C, S, R | `3.3` |
| 21 | `distance_logdistance_3` | Mét | Log-distance với n = 3 (văn phòng, có vách ngăn) | C, S, R | `2.7` |
| 22 | `distance_logdistance_3_5` | Mét | Log-distance với n = 3,5 (nhiều tường bê tông) | C, S, R | `2.4` |
| 23 | `distance_rtt_m` | Mét, 2 chữ số thập phân, **có thể âm** | Khoảng cách **đo được** bằng Wi-Fi RTT (thời gian bay khứ hồi của khung FTM × tốc độ ánh sáng / 2), trung bình các lần đo trong một burst. Sai số thường 1–2 m khi nhìn thấy trực tiếp AP; tường làm kết quả dài ra. Giá trị âm nhỏ ở sát AP là do sai số hiệu chuẩn. Chỉ có khi `rtt_status = SUCCESS` | R | `3.25` |
| 24 | `distance_rtt_stddev_m` | Mét | Độ lệch chuẩn của các lần đo trong burst; lớn nghĩa là kết quả dao động (đa đường, vật cản) | R | `0.42` |
| 25 | `rtt_status` | Chuỗi | Kết quả phép đo với AP này (bảng 7.3) | R | `SUCCESS` |
| 26 | `rtt_mode` | Chuỗi | Cách đo: `80211MC`, `80211AZ` hay `ONE_SIDED` (bảng 7.3) | R | `80211MC` |
| 27 | `rtt_success_count` | Số nguyên | Số lần đo thành công trong burst | R | `7` |
| 28 | `rtt_attempt_count` | Số nguyên | Số lần đo đã thử trong burst | R | `8` |
| 29 | `standard` | Chuỗi | Chuẩn Wi-Fi cao nhất AP hỗ trợ (bảng 7.3); trống nếu Android không xác định được | S | `802.11ax` |
| 30 | `security` | Các giá trị cách nhau bởi dấu cách | Kiểu bảo mật đã chuẩn hóa (bảng 7.3) | S | `WPS WPA2` |
| 31 | `capabilities` | Chuỗi gốc của Android | Mô tả bảo mật gốc. Mỗi cặp ngoặc vuông là một thành phần: `[giao thức-xác thực-mã hóa]` (ví dụ `WPA2-PSK-CCMP-128`: WPA2, mật khẩu chung, mã hóa AES-CCMP 128 bit), `[ESS]` = mạng có AP, `[WPS]` = hỗ trợ WPS | S | `[WPA2-PSK-CCMP-128][RSN-PSK-CCMP-128][ESS][WPS]` |
| 32 | `fast_roaming` | Các giá trị cách nhau bởi dấu cách | Các chuẩn hỗ trợ chuyển AP nhanh mà AP công bố (bảng 7.3); trống nếu không có | S | `802.11k 802.11v` |
| 33 | `is_80211mc` | `true` / `false` | AP có hỗ trợ đo khoảng cách bằng Wi-Fi RTT (802.11mc, FTM) hay không. AP có giá trị `true` mới được đo RTT hai chiều (dòng `RTT`) | S, R | `false` |
| 34 | `vendor` | Chuỗi | Hãng sản xuất, tra từ 3 byte đầu (OUI) của BSSID. **Trống** với BSSID ảo do AP tự sinh (byte đầu có bit "locally administered", ví dụ `5a:…`, `76:…`) hoặc khi không có trong bảng tra | S | `TP LINK SYSTEMS INC` |
| 35 | `latitude` | Độ thập phân, hệ WGS-84, 7 chữ số | Vĩ độ của điện thoại (dương = Bắc), lấy từ **bản định vị gần nhất với `epoch_ms` của dòng**. Với dòng `SCAN`, đó là lúc BSSID được quét thấy | C, S, R | `21.0285123` |
| 36 | `longitude` | Độ thập phân, WGS-84 | Kinh độ (dương = Đông) | C, S, R | `105.8048456` |
| 37 | `accuracy_m` | Mét | Bán kính sai số vị trí theo phương ngang, độ tin cậy 68%, do nguồn vị trí tự ước lượng | C, S, R | `13.9` |
| 38 | `altitude_m` | Mét | Độ cao so với mặt ellipsoid WGS-84. Không đáng tin khi nguồn là `network` | C, S, R | `58.4` |
| 39 | `speed_mps` | m/s, 2 chữ số thập phân | Tốc độ di chuyển của điện thoại; chỉ có khi nguồn vị trí cung cấp (thường là GPS) | C, S, R | `1.25` |
| 40 | `location_provider` | `gps` / `network` / `fused` | Nguồn của vị trí (bảng 7.3) | C, S, R | `network` |
| 41 | `location_age_ms` | Mili-giây, **có dấu** | `epoch_ms` của dòng trừ thời điểm của bản định vị. Dương: bản định vị lấy trước thời điểm của dòng; **âm**: lấy sau (chỉ gặp ở dòng `SCAN`). Trị tuyệt đối lớn (ví dụ > 5000) nghĩa là vị trí không khớp thời gian, nên lọc bỏ khi cần độ chính xác vị trí | C, S, R | `1988` |

### 7.3. Các giá trị có thể gặp

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

### 7.4. Công thức khoảng cách (ước lượng thô)

Cả hai công thức giả định AP phát 0 dBm và ăn-ten 0 dBi.

```
FSPL:          d = 10 ^ ((27.55 − 20·log10(f_MHz) + |RSSI|) / 20)
Log-distance:  d = 10 ^ ((|RSSI| − FSPL(1 m)) / (10·n)),   FSPL(1 m) = 20·log10(f_MHz) − 27.55
```

Với n = 2, log-distance trùng với FSPL. Kết quả chỉ nên dùng để **so sánh tương đối**, vì công suất phát thật của AP (15–23 dBm) và vật cản làm sai lệch con số tuyệt đối rất nhiều.

---

## 8. Các hàm lấy dữ liệu để ghi vào file CSV

### 8.1. Sơ đồ luồng dữ liệu

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

### 8.2. Bảng hàm theo nhóm cột

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

## 9. Build và kiểm tra

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
- `ktlintCheck`: 0 lỗi. `lintDebug`: 0 lỗi; các cảnh báo còn lại là của repo gốc hoặc `MissingTranslation` (mục 10).
- **Chưa** chạy lại test instrumentation (`connectedDebugAndroidTest`) và **chưa** thử bản mới trên điện thoại thật.
- Đã thử trên vivo iQOO Z10 Turbo+ (Android 16).

---

## 10. Hạn chế đã biết

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
- **Màn hình About vẫn là của bản gốc:** tên công ty, link trang chủ, FAQ và chính sách quyền riêng tư trỏ tới WiFiAnalyzer gốc, chưa nói rằng đây là bản sửa đổi hay mã nguồn của bản này ở đâu (xem mục 2.2).
- **Quyền riêng tư:** xem mục 3.
