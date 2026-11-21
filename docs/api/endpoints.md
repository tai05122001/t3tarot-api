# Danh sách endpoint

Cập nhật lần cuối: 2026-11-21 · Tổng: 10 endpoint

Tài liệu đọc nhanh mọi endpoint đang có trong code, dùng để thống kê và tra cứu khi chưa mở được Swagger UI. Nguồn sự thật vẫn là code và Swagger (`/swagger-ui/index.html`, `/v3/api-docs` khi `SWAGGER_ENABLED=true`). Workflow `create-endpoint` và `modify-endpoint` cập nhật file này mỗi lần thêm hoặc sửa endpoint; quy tắc ghi ở cuối file. Happy case: collection Bruno ở `bruno/` (mở bằng Open Collection, xem [bruno/README.md](../../bruno/README.md)), curl ở `curl/` ([curl/README.md](../../curl/README.md)).

## Thống kê

| Module | Feature | Số endpoint | Công khai | Cần token |
| --- | --- | --- | --- | --- |
| identity | register | 1 | 1 | 0 |
| identity | passwordlogin | 1 | 1 | 0 |
| identity | sociallogin | 1 | 1 | 0 |
| identity | session | 5 | 2 | 3 |
| identity | emailverification | 2 | 2 | 0 |
| **Tổng** | | **10** | **7** | **3** |

## Bảng tổng hợp

| # | Method | Path | Module / feature | Quyền | `Idempotency-Key` | Client | Thành công | Thêm ngày | Trạng thái | Curl |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST | `/auth/register` | identity / register | Công khai | Bắt buộc | Website, iOS, Android | 201 | 2026-11-19 | Đang phát triển | [01-register](../../curl/identity/01-register.curl) |
| 2 | POST | `/auth/login` | identity / passwordlogin | Công khai | Không | Website, iOS, Android | 200 | 2026-11-19 | Đang phát triển | [02-login](../../curl/identity/02-login.curl) |
| 3 | POST | `/auth/social/google` | identity / sociallogin | Công khai | Không | Website, iOS, Android | 200 | 2026-11-19 | Đang phát triển | [03-social-google](../../curl/identity/03-social-google.curl) |
| 4 | POST | `/auth/refresh` | identity / session | Công khai | Không | Website, iOS, Android | 200 | 2026-11-19 | Đang phát triển | [04-refresh](../../curl/identity/04-refresh.curl) |
| 5 | POST | `/auth/logout` | identity / session | Công khai | Không | Website, iOS, Android | 204 | 2026-11-19 | Đang phát triển | [05-logout](../../curl/identity/05-logout.curl) |
| 6 | POST | `/auth/logout-all` | identity / session | Access token | Không | Website, iOS, Android | 204 | 2026-11-19 | Đang phát triển | [06-logout-all](../../curl/identity/06-logout-all.curl) |
| 7 | GET | `/auth/sessions` | identity / session | Access token | Không | Website, iOS, Android | 200 | 2026-11-19 | Đang phát triển | [07-sessions-list](../../curl/identity/07-sessions-list.curl) |
| 8 | DELETE | `/auth/sessions/{sessionId}` | identity / session | Access token | Không | Website, iOS, Android | 204 | 2026-11-19 | Đang phát triển | [08-session-revoke](../../curl/identity/08-session-revoke.curl) |
| 9 | POST | `/auth/email/verify` | identity / emailverification | Công khai | Không | Website, iOS, Android | 204 | 2026-11-19 | Đang phát triển | [09-email-verify](../../curl/identity/09-email-verify.curl) |
| 10 | POST | `/auth/email/resend` | identity / emailverification | Công khai | Bắt buộc | Website, iOS, Android | 202 | 2026-11-19 | Đang phát triển | [10-email-resend](../../curl/identity/10-email-resend.curl) |

Client: theo changelog `identity-authentication`, website và mobile dùng chung hợp đồng; chưa có app phát hành nên chưa có phiên bản app tối thiểu.

## Quy ước chung

- JSON camelCase; thời điểm theo ISO-8601 UTC (`Instant`).
- Endpoint cần token: header `Authorization: Bearer <accessToken>`. `userId` lấy từ token, không gửi trong body, query hay path.
- `Idempotency-Key`: UUID. Thiếu thì `400 error.idempotency_key.is_missing`, sai định dạng thì `400 error.idempotency_key.is_invalid`.
- Body lỗi chỉ có mã, không có câu chữ: `{status, code, requestId, errors?: [{field, code}]}`. Chi tiết ở `docs/changelog/api-error-and-pagination.md`.
- Lỗi chung mọi endpoint có thể trả:

  | HTTP | Mã | Khi nào |
  | --- | --- | --- |
  | 400 | `error.invalid_request.malformed` | Body không phải JSON hợp lệ |
  | 400 | `error.invalid_param.is_invalid` | Vi phạm validate; chi tiết từng field trong `errors` (`error.invalid_param.is_missing`, `length_out_of_range`, `invalid_format`, `out_of_range`, `invalid_type`) |
  | 401 | `error.auth.unauthenticated` | Endpoint cần token mà thiếu, hết hạn hoặc sai token |
  | 405 | `error.http.method_not_allowed` | Sai method |
  | 415 | `error.http.unsupported_media_type` | Không gửi `Content-Type: application/json` |
  | 500 | `error.server.internal` | Lỗi không lường trước |

- Token response dùng chung (endpoint 2, 4): `{accessToken: string, refreshToken: string, tokenType: "Bearer", expiresIn: number}`. `expiresIn` tính bằng giây (mặc định 900).

## Chi tiết

### 1. POST `/auth/register`

Đăng ký bằng username, email, mật khẩu. Tài khoản phải xác minh email trước khi đăng nhập.

| Header | Bắt buộc | Ghi chú |
| --- | --- | --- |
| `Idempotency-Key` | Có | UUID |

| Field body | Kiểu | Bắt buộc | Ràng buộc |
| --- | --- | --- | --- |
| `username` | string | Có | `^[A-Za-z0-9_.]{3,32}$`; không phân biệt hoa thường khi kiểm trùng |
| `email` | string | Có | Email hợp lệ, tối đa 320 ký tự |
| `password` | string | Có | Tối thiểu 10 ký tự, tối đa 72 byte |

Thành công `201`: `{userId: uuid}`. Email đã có tài khoản vẫn trả `201` và không tạo gì (không lộ email đã đăng ký).

| HTTP | Mã lỗi riêng |
| --- | --- |
| 409 | `error.username.already_taken` |

### 2. POST `/auth/login`

Đăng nhập bằng username và mật khẩu.

| Field body | Kiểu | Bắt buộc | Ràng buộc |
| --- | --- | --- | --- |
| `username` | string | Có | Tối đa 32 ký tự |
| `password` | string | Có | Tối đa 72 byte |

Thành công `200`: token response. Sai 5 lần liên tiếp thì khoá 15 phút; khi đang khoá vẫn trả `invalid_credentials`.

| HTTP | Mã lỗi riêng |
| --- | --- |
| 401 | `error.auth.invalid_credentials` |
| 403 | `error.auth.email_not_verified` |

### 3. POST `/auth/social/google`

Đăng nhập bằng ID token Google mà client lấy được. Server kiểm chữ ký, `iss`, `aud`, `exp`, `email_verified`.

| Field body | Kiểu | Bắt buộc | Ràng buộc |
| --- | --- | --- | --- |
| `idToken` | string | Có | Tối đa 4096 ký tự |

Thành công `200`: token response thêm `isNewUser: boolean`.

| HTTP | Mã lỗi riêng |
| --- | --- |
| 401 | `error.auth.id_token_invalid` |
| 409 | `error.auth.email_already_registered` (email đã dùng cho tài khoản mật khẩu) |
| 503 | `error.server.unavailable` (không tới được JWKS của Google) |

### 4. POST `/auth/refresh`

Đổi refresh token lấy cặp token mới; refresh token cũ bị xoay. Gửi lại token cũ trong 10 giây sau lần xoay đầu vẫn được cấp cặp mới; quá thời gian đó thì thu hồi cả phiên.

| Field body | Kiểu | Bắt buộc | Ràng buộc |
| --- | --- | --- | --- |
| `refreshToken` | string | Có | Tối đa 128 ký tự |

Thành công `200`: token response.

| HTTP | Mã lỗi riêng |
| --- | --- |
| 401 | `error.auth.refresh_token_invalid` |

### 5. POST `/auth/logout`

Thu hồi phiên của refresh token. Luôn trả `204`, kể cả token không tồn tại.

| Field body | Kiểu | Bắt buộc | Ràng buộc |
| --- | --- | --- | --- |
| `refreshToken` | string | Có | Tối đa 128 ký tự |

Thành công `204`, không body. Không có mã lỗi riêng.

### 6. POST `/auth/logout-all`

Thu hồi mọi phiên của người gọi. Không body.

Thành công `204`. Không có mã lỗi riêng. Access token đã cấp vẫn dùng được đến khi hết hạn (tối đa 15 phút).

### 7. GET `/auth/sessions`

Liệt kê phiên đang hoạt động của người gọi.

Thành công `200`: mảng `[{sessionId: uuid, createdAt: instant, expiresAt: instant, userAgent: string, isCurrent: boolean}]`. `isCurrent` là phiên của access token đang gọi.

### 8. DELETE `/auth/sessions/{sessionId}`

Thu hồi một phiên của người gọi.

| Path | Kiểu | Ghi chú |
| --- | --- | --- |
| `sessionId` | uuid | Sai định dạng: `400` với `error.invalid_param.invalid_type` |

Thành công `204`.

| HTTP | Mã lỗi riêng |
| --- | --- |
| 404 | `error.resource.not_found` (phiên không tồn tại hoặc của người khác) |

### 9. POST `/auth/email/verify`

Xác minh email bằng token trong mail. Token hạn 24 giờ, dùng một lần.

| Field body | Kiểu | Bắt buộc | Ràng buộc |
| --- | --- | --- | --- |
| `token` | string | Có | Tối đa 128 ký tự |

Thành công `204`.

| HTTP | Mã lỗi riêng |
| --- | --- |
| 400 | `error.verification_token.invalid` (sai, hết hạn hoặc đã dùng) |

### 10. POST `/auth/email/resend`

Gửi lại mail xác minh nếu email đã đăng ký và chưa xác minh; tối đa một lần mỗi 60 giây. Luôn trả `202` để không lộ email có tồn tại hay không. Hiện chưa gửi mail thật (bản cài đặt chỉ ghi log).

| Header | Bắt buộc | Ghi chú |
| --- | --- | --- |
| `Idempotency-Key` | Có | UUID |

| Field body | Kiểu | Bắt buộc | Ràng buộc |
| --- | --- | --- | --- |
| `email` | string | Có | Email hợp lệ, tối đa 320 ký tự |

Thành công `202`. Không có mã lỗi riêng.

## Quy tắc cập nhật file này

- Thêm endpoint: thêm một dòng vào "Bảng tổng hợp" (số thứ tự tăng dần, không đánh lại số cũ, cột Curl trỏ tới file happy case trong `curl/<module>/` ở gốc repo; file `.bru` cùng số ở `bruno/<module>/`), sửa "Thống kê", thêm mục "Chi tiết" cùng số, sửa "Tổng" và ngày cập nhật ở đầu file.
- Sửa endpoint: sửa đè dòng và mục chi tiết cho đúng hành vi hiện hành; không ghi lịch sử (git giữ lịch sử, changelog ghi lý do).
- Bỏ endpoint (chỉ khi không còn client nào dùng): đổi cột "Trạng thái" thành "Đã gỡ" kèm ngày, giữ số thứ tự; đánh dấu field cũ là "deprecated" khi field chỉ bị thay thế.
- Chỉ ghi điều có trong code; không ghi token, khoá, dữ liệu người dùng thật, nội dung câu hỏi hay luận giải. Ví dụ dùng dữ liệu giả.
- Khớp với Swagger annotation (`.claude/references/swagger.md`) và changelog của feature.
