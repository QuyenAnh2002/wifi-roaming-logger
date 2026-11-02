# WiFiAnalyzer – bản sửa đổi (ghi log CSV, vị trí, Wi-Fi RTT)

> **Đây không phải repo chính thức của WiFiAnalyzer.** Đây là bản sửa đổi (modified version) của [WiFiAnalyzer](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer), một phần mềm mã nguồn mở của VREM Software Development, phát hành theo giấy phép [GNU GPLv3](LICENSE). Bản này không do tác giả gốc phát hành, xác nhận hay hỗ trợ. Lỗi của bản này hãy báo ở repo này, đừng gửi lên repo gốc.

Bản này giữ nguyên các chức năng phân tích Wi-Fi của bản gốc. Ngoài ra, nó ghi lại theo thời gian kết nối hiện tại, mọi BSSID quét thấy, vị trí thiết bị và khoảng cách đo bằng Wi-Fi RTT vào file CSV, rồi chia sẻ file qua bảng chia sẻ của Android.

**Lưu ý:** cũng như bản gốc, WiFiAnalyzer không phải công cụ dò mật khẩu hay lừa đảo (phishing) Wi-Fi.

## Mục lục

1. [Nguồn gốc](#1-nguồn-gốc)
2. [Giấy phép](#2-giấy-phép)
3. [Quyền riêng tư](#3-quyền-riêng-tư)
4. [Thay đổi so với bản gốc](#4-thay-đổi-so-với-bản-gốc)
5. [Build và kiểm tra](#5-build-và-kiểm-tra)
6. [Hạn chế đã biết](#6-hạn-chế-đã-biết)

Cách dùng trên điện thoại và định dạng file CSV: [CSV_FORMAT.md](CSV_FORMAT.md).

---

## 1. Nguồn gốc

| | |
|---|---|
| Dự án gốc | [VREMSoftwareDevelopment/WiFiAnalyzer](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer) |
| Tác giả gốc | VREM Software Development và [những người đóng góp](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer/graphs/contributors) cho repo gốc |
| Phiên bản gốc | WiFiAnalyzer 3.3.1 (versionCode 72), commit [`7e2019d`](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer/commit/7e2019d294d4ac40b70e770d9e2c55fdc1b18281) ngày 2026-10-03 ("Fix instrumented tests") |
| Người sửa đổi | QuyenAnh2002 và Pham Anh Dung, tháng 10–11/2026 (mục 4 và lịch sử git của repo này) |
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
| **State Changes** – ghi rõ thay đổi | Bản sửa đổi phải có thông báo rõ ràng rằng đã bị sửa, kèm thời điểm sửa (mục 5a) | Mục 4 của README này tóm tắt các thay đổi so với commit gốc; lệnh `git diff` ở mục 1 cho danh sách file chính xác. Header của mỗi file `.kt` / `.xml` đã sửa hoặc thêm có hai dòng ghi chú ngay dưới dòng bản quyền gốc (ví dụ bên dưới). Lịch sử git ghi lại từng thay đổi kèm ngày |

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

File mới thêm dùng hai dòng `Copyright (C) 2026 QuyenAnh2002, Pham Anh Dung` và `Added in 2026 to this modified version of WiFiAnalyzer; see README.md`. File XML dùng cùng nội dung, với tiền tố `~` thay cho `*`. Ảnh PNG (logo xám của bản debug) không có header; chúng được nêu ở mục 4.

**Khi sửa tiếp:** thêm hai dòng tương ứng vào header của file gốc vừa sửa lần đầu hoặc file mới, và cập nhật mục 4 (cùng [CSV_FORMAT.md](CSV_FORMAT.md) nếu định dạng CSV thay đổi). Script `scripts/check-license-headers.sh` (chạy trong CI) chỉ kiểm tra header có dòng của VREM và dòng GPL, nên sẽ không báo lỗi khi thiếu hai dòng ghi chú này.

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

## 4. Thay đổi so với bản gốc

So với WiFiAnalyzer 3.3.1 (commit `7e2019d`), bản này:

- **Export CSV:** thay việc chia sẻ văn bản phân cách bằng `|` bằng một file `.csv` 41 cột chứa mọi lần quét từ lúc mở app, chia sẻ qua bảng chia sẻ Android hoặc lưu ra file. `Export.kt` và `ExportIntent.kt` của bản gốc đã bị xóa, thay bằng `CsvExport.kt` và `CsvShare.kt`.
- **Ghi log theo lượt đo:** nút Play / Pause (ở bản gốc chỉ tạm dừng quét) giờ bắt đầu và kết thúc một lượt đo. Mỗi lượt đo ghi một file `wifi-log-*.csv` gồm các dòng `CONNECTION` (kết nối hiện tại, đọc mỗi giây), `SCAN` (mỗi BSSID quét thấy) và `RTT`. Bấm Pause thì đóng file và mở bảng chia sẻ. Cạnh nút có bộ đếm thời gian lượt đo.
- **Vị trí thiết bị** (GPS / network / fused) được gắn vào từng dòng CSV.
- **Khoảng cách ước lượng:** thêm mô hình log-distance (n = 2; 2,5; 3; 3,5) bên cạnh FSPL của bản gốc.
- **Wi-Fi RTT:** đo khoảng cách tới các AP hỗ trợ 802.11mc / 802.11az mỗi giây (đo one-sided với AP thường trên Android 14+). Màn hình chi tiết AP hiện thêm `RTT x.xm`.
- **Bản debug** dùng logo xám (`app/src/debug/res/`) để không nhầm với app gốc.
- **Manifest:** thêm quyền `ACCESS_NETWORK_STATE` và khai báo `FileProvider`.

Cách dùng trên điện thoại, ý nghĩa từng cột CSV và các hàm sinh ra dữ liệu được mô tả trong [CSV_FORMAT.md](CSV_FORMAT.md). Danh sách file chính xác đã thêm / sửa / xóa: chạy lệnh `git diff` ở mục 1.

---

## 5. Build và kiểm tra

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
- `ktlintCheck`: 0 lỗi. `lintDebug`: 0 lỗi; các cảnh báo còn lại là của repo gốc hoặc `MissingTranslation` (mục 6).
- **Chưa** chạy lại test instrumentation (`connectedDebugAndroidTest`) và **chưa** thử bản mới trên điện thoại thật.
- Đã thử trên vivo iQOO Z10 Turbo+ (Android 16).

---

## 6. Hạn chế đã biết

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
