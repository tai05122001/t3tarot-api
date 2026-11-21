# Curl happy case

Mỗi endpoint có một file curl gọi happy case: `curl/<module>/<NN>-<tên>.curl`, `NN` trùng số thứ tự trong `docs/api/endpoints.md`. Mỗi file đúng một lệnh curl.

Dùng với Bruno thì mở collection `bruno` (xem [README](../bruno/README.md)), không cần import curl. Curl ở đây để gọi từ terminal hoặc dán vào Bruno bằng **New Request → From cURL** khi cần.

## Biến

URL ghi thẳng `http://localhost:8080` để bộ đọc cURL của Bruno nhận được. Các giá trị lấy từ response trước để dạng `{{tên}}`; gọi bằng terminal thì thay tay:

| Chỗ giữ | Lấy từ |
| --- | --- |
| `{{accessToken}}`, `{{refreshToken}}` | Response của `02-login`, `03-social-google`, `04-refresh` |
| `{{sessionId}}` | Response của `07-sessions-list` |
| `{{verificationToken}}` | Token trong mail xác minh |
| `{{googleIdToken}}` | ID token Google thật từ client |
| `{{$guid}}` | UUID mới cho `Idempotency-Key` (trong Bruno là biến động; ở terminal thay bằng UUID bất kỳ) |

Thứ tự chạy và cách xác minh email ở local: xem mục "Thứ tự chạy" trong [README của collection Bruno](../bruno/README.md).

## Quy tắc khi thêm hoặc sửa

- Chỉ happy case, một lệnh curl mỗi file, nhiều dòng nối bằng `\`.
- URL dùng `http://localhost:8080`; token, id và giá trị lấy từ response trước dùng `{{tên}}`; `Idempotency-Key` dùng `{{$guid}}`.
- Dữ liệu giả, khớp với file `.bru` cùng số; không token, khoá, dữ liệu người dùng thật.
- Endpoint đổi hợp đồng thì sửa file curl và file `.bru` trong cùng task.
