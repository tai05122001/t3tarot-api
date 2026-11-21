# Website và mobile app

T3Tarot có hai loại client: website và mobile app. Backend dùng chung một API. File này ghi những khác biệt làm thay đổi cách viết endpoint. Chỗ ghi "thường" là cách làm phổ biến, chưa phải quyết định của dự án.

## Khác biệt ảnh hưởng tới backend

| Mục | Website | Mobile app |
| --- | --- | --- |
| Phát hành | Deploy cùng backend; người dùng có bản mới sau khi tải lại trang | Qua App Store và Google Play; người dùng cập nhật chậm, nhiều phiên bản chạy song song |
| Xác thực (chưa chốt) | Thường dùng cookie HttpOnly, cần chống CSRF | Thường dùng access token và refresh token lưu trong bộ nhớ an toàn của máy |
| CORS | Cần cấu hình origin cụ thể | Không áp dụng |
| Mạng | Thường ổn định | Mất mạng, đổi 4G và Wi-Fi, app bị đưa xuống nền; client retry nhiều |
| SSE | `EventSource` của trình duyệt tự nối lại và gửi `Last-Event-ID` | Cần thư viện SSE hoặc tự nối lại; hệ điều hành cắt kết nối khi app xuống nền |
| Thông báo đẩy | Không | Có thể có (nhắc rút lá hằng ngày); chưa chốt |

## Quy tắc tương thích ngược (bắt buộc khi mobile dùng endpoint)

Được làm, không phá app cũ:

- Thêm field mới vào response.
- Thêm field tùy chọn vào request, có giá trị mặc định giữ nguyên hành vi cũ.
- Thêm endpoint mới.
- Thêm mã lỗi mới cho tình huống mới.

Phá vỡ, phải hỏi trước và coi là mức ảnh hưởng Cao:

- Xoá hoặc đổi tên field.
- Đổi kiểu hoặc đổi nghĩa của field.
- Thêm field bắt buộc vào request.
- Đổi status code hoặc mã lỗi của tình huống đang có.
- Đổi định dạng ngày giờ hoặc tiền.
- Thêm giá trị enum mới vào field mà app cũ đang xử lý theo danh sách cố định.
- Đổi path hoặc method.

Khi buộc phải phá vỡ: giữ hành vi cũ song song đến khi app cũ không còn dùng (đo bằng metric theo phiên bản app), hoặc mở phiên bản API mới. Chọn cách nào chưa chốt.

## Mạng chập chờn

- Mọi thao tác ghi bắt buộc `Idempotency-Key`; retry cùng key trả lại kết quả đã có, không trừ lượt lần hai.
- Endpoint SSE phát lại các phần đã ghi trong DB khi client nối lại (kiến trúc lease và phát lại trong `.claude/skills/new-module/references/architecture.md`). Hỗ trợ `Last-Event-ID` là đề xuất.
- Danh sách dài dùng phân trang theo cursor (đề xuất).
- Không trả dữ liệu thừa; ảnh lá bài trả URL, không nhúng base64.

## Test cần có khi mobile dùng endpoint

- Request như app cũ gửi (không có field mới) vẫn chạy và cho kết quả như trước.
- Response vẫn giữ đủ field cũ với đúng kiểu.
- Gửi lại cùng `Idempotency-Key` sau khi "mất mạng" không tạo bản ghi mới, không trừ lượt.
- Nếu xử lý khác nhau theo nền tảng: test với header của từng nền tảng.

## Đề xuất chưa chốt

| Điểm | Đề xuất | Cần chốt |
| --- | --- | --- |
| Header client | `X-Client-Platform` (`web`, `ios`, `android`) và `X-App-Version` | Tên header, có bắt buộc không |
| Cách đánh phiên bản API | Tiền tố `/api/v1`, chỉ tăng khi buộc phá vỡ | Dùng URL hay header |
| Phiên bản app tối thiểu | Endpoint cấu hình trả `minSupportedVersion`, app tự nhắc cập nhật | Có làm trong MVP không |
| Xác thực theo nền tảng | Cookie cho web, token cho app | Phụ thuộc phương án đăng nhập |
| API chung hay tách theo client (BFF) | Dùng chung một API trong MVP | Xác nhận |
| Metric | Thêm label `platform`; `app_version` để trong log, không làm label | Xác nhận |
