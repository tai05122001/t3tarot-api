# Đăng ký, đăng nhập và token

Cập nhật lần cuối: 2026-11-21 · Trạng thái: Đang phát triển

## Tóm tắt

Module `identity` cấp JWT riêng cho website và mobile: đăng ký bằng username, email và mật khẩu (bắt buộc xác minh email), đăng nhập bằng mật khẩu hoặc Google, làm mới và thu hồi refresh token, quản lý phiên. Facebook, quên mật khẩu, nhà cung cấp gửi mail thật chưa làm.

## Thay đổi gì

- Cấu trúc theo feature (luật ở `.claude/skills/project-structure/references/structure.md`, mục "Cấu trúc trong module"): `identity/{register,passwordlogin,sociallogin,session,emailverification,shared}/`, mỗi feature có `controller`, `dto/request`, `dto/response`, `service` (một `<Feature>Facade` public, use case package-private), `repository`, `entity`, `enumeration`, `policy`, `client`, `config` khi cần.
- Endpoint (JSON camelCase, lỗi chỉ có mã):

  | Endpoint | Quyền | Kết quả | Mã lỗi riêng |
  | --- | --- | --- | --- |
  | `POST /auth/register` | public, cần `Idempotency-Key` | `201 {userId}` | `error.username.already_taken` (409) |
  | `POST /auth/login` | public | `200` token | `error.auth.invalid_credentials` (401), `error.auth.email_not_verified` (403) |
  | `POST /auth/social/google` | public | `200` token + `isNewUser` | `error.auth.id_token_invalid` (401), `error.auth.email_already_registered` (409) |
  | `POST /auth/refresh` | public | `200` token | `error.auth.refresh_token_invalid` (401) |
  | `POST /auth/logout` | public | `204`, luôn 204 | |
  | `POST /auth/logout-all` | access token | `204` | |
  | `GET /auth/sessions` | access token | `[{sessionId, createdAt, expiresAt, userAgent, isCurrent}]` | |
  | `DELETE /auth/sessions/{sessionId}` | access token | `204`; phiên người khác hoặc không có: `404` | |
  | `POST /auth/email/verify` | public | `204` | `error.verification_token.invalid` (400) |
  | `POST /auth/email/resend` | public, cần `Idempotency-Key` | `202`, luôn 202 | |

- Token response: `{accessToken, refreshToken, tokenType: "Bearer", expiresIn}`. Access token RS256 có `iss`, `aud`, `sub`, `exp` như bộ xác minh sẵn có, thêm claim `sid` (id phiên) và header `kid`.
- Mật khẩu: BCrypt (strength mặc định 12, cấu hình `t3tarot.identity.password-hash-strength`), tối đa 72 byte, tối thiểu 10 ký tự. Khi tên đăng nhập không tồn tại vẫn băm một mật khẩu giả để thời gian phản hồi không lộ ra điều đó.
- Khóa tài khoản: 5 lần sai (`maxFailedLogins`) khóa 15 phút (`lockoutDuration`); đang khóa vẫn trả `invalid_credentials`. Bộ đếm một câu SQL, giới hạn bằng `LEAST` để không tràn `smallint`.
- Refresh token: 32 byte `SecureRandom`, chỉ lưu SHA-256. Xoay bằng một câu `UPDATE ... WHERE` (compare-and-set). Dùng lại token cũ trong cửa sổ ân hạn (mặc định 10 giây tính từ lần xoay đầu) thì cấp cặp mới và giữ nguyên mốc ân hạn; quá cửa sổ thì thu hồi cả phiên. Phiên hết hạn tuyệt đối 30 ngày từ lúc đăng nhập (không kéo dài khi refresh).
- Google: xác minh chữ ký qua JWKS của Google (timeout 2 giây kết nối, 3 giây đọc), kiểm `iss`, `aud` (danh sách `GOOGLE_CLIENT_IDS`), `exp`, `email_verified`. Không tự gộp với tài khoản mật khẩu cùng email: trả `email_already_registered`. JWKS không tới được: `503 error.server.unavailable`.
- Xác minh email: token 32 byte, chỉ lưu hash, hạn 24 giờ, dùng một lần; gửi lại tối đa một lần mỗi 60 giây. Gửi mail qua `VerificationMailSender` (`emailverification/client`); bản cài đặt hiện chỉ ghi log và không gửi.
- Email trùng khi đăng ký trả `201` như bình thường và không tạo gì, để không lộ email đã có tài khoản.
- `common`: `RsaPrivateKeys`, `PemKeySource`, `UuidV7Generator`, `AccessTokenClaims.SESSION_ID`, `AccessTokenAuthentication.sessionId()` và `user()`.
- Log để truy vết (SLF4J, `requestId` có sẵn trong MDC): `common/web/AccessLoggingFilter` ghi `METHOD path status thời-gian` cho mọi request (không ghi query, header, body); mỗi `Facade` ghi một dòng `info` khi thành công, ví dụ `register userId=...`, `session.revoke userId=... sessionId=...`, `login.google isNewUser=...`. Không log email, username, mật khẩu, token. Mẫu log console đổi sang `logging.pattern.console` có giờ, `requestId`, tên logger.

## Vì sao

- Facade theo feature để controller và feature khác chỉ có một cửa vào; đọc một tính năng chỉ cần mở một thư mục.
- Cửa sổ ân hạn neo vào lần xoay đầu vì app mất mạng sẽ gửi lại cùng token nhiều lần; nếu mỗi lần dùng lại làm trượt cửa sổ thì lần thứ ba bị đăng xuất.
- Toàn bộ `identity` dùng `JdbcClient`: mọi bảng ở đây nằm trên đường compare-and-set hoặc ràng buộc UNIQUE. Giả định: không cần Hibernate cho module này.
- Miễn `Idempotency-Key` cho login, refresh, logout, verify (tự idempotent hoặc đã bị ràng buộc bởi credential); `register` và `resend` chỉ kiểm key hợp lệ. Giả định: chống trùng bằng UNIQUE là đủ cho MVP.
- Đã chốt với chủ dự án: chặn đăng nhập đến khi xác minh email; chỉ Google trước; xoay refresh token có phát hiện dùng lại.

## Ảnh hưởng

- API / contract: 10 endpoint mới ở trên; app cũ chưa dùng nên không phá tương thích. Access token vẫn hợp lệ đến hết hạn (15 phút) sau khi thu hồi phiên.
- Cấu hình / biến môi trường: `JWT_PRIVATE_KEY` (chuỗi khoá) hoặc `JWT_PRIVATE_KEY_LOCATION` (đường dẫn file PEM, ví dụ `file:./secrets/jwt-private.pem`): PKCS#8, bắt buộc đúng một trong hai, phải khớp khoá công khai, kiểm lúc khởi động, `JWT_KEY_ID`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`, `AUTH_REFRESH_GRACE`, `GOOGLE_CLIENT_IDS`. `t3tarot.security.public-paths` có thêm 7 đường dẫn công khai (đăng ký, đăng nhập, social, refresh, logout, verify, resend) vì người dùng chưa có token khi gọi.
- Database: migration `V2__add_refresh_rotation_and_email_verification.sql` (hai cột trên `auth_session`, bảng `email_verification`). Bảng tài khoản nằm ở `V1`.
- Client: website và mobile dùng chung hợp đồng; app lưu refresh token ở nơi an toàn, gọi refresh một luồng duy nhất.
- Tiền, hạn mức, state machine: Không.

## QA cần test

- Đăng ký (trùng username không phân biệt hoa thường, trùng email, mật khẩu quá 72 byte, thiếu `Idempotency-Key`), xác minh (hết hạn, dùng lại), đăng nhập (sai mật khẩu, chưa xác minh, khóa, hết khóa), refresh (cửa sổ ân hạn, dùng lại sau cửa sổ, phiên hết hạn), logout, logout-all, IDOR khi thu hồi phiên, Google (sai audience, hết hạn, email chưa xác minh, trùng email mật khẩu).
- Refresh song song cùng một token (xem `RefreshSessionConcurrencyTest`).
- Chưa kiểm với Google thật (chỉ khóa giả); cần thử tay một lần với client ID thật.

## Rollout

Cấp khóa riêng và khóa công khai trước khi triển khai (cùng cặp), đặt `GOOGLE_CLIENT_IDS` cho web, iOS, Android. Chưa có nhà cung cấp gửi mail nên đăng ký bằng mật khẩu chưa dùng được ngoài môi trường test cho đến khi cài `VerificationMailSender` thật. Chưa nối bộ giới hạn tốc độ (`RateLimitCounter`) vào đường dẫn đăng nhập. Lùi: gỡ các đường dẫn khỏi `public-paths`; migration V2 chỉ thêm cột và bảng nên không cần lùi dữ liệu.

## Trạng thái kiểm chứng

`mvn clean verify` (cần Docker): toàn bộ test xanh, gồm `ModularityTests` và test tích hợp Testcontainers PostgreSQL 17 cho từng controller và khóa/xoay token. Coverage dòng của service, controller, client, config và util `common` thuộc task đều ≥ 80% (đo bằng JaCoCo `jacoco.csv`). Chưa chạy: Google thật, hạ tầng Kubernetes. Phần log (`AccessLoggingFilter`, log ở các Facade) chưa có test theo yêu cầu của chủ dự án (2026-11-21), nên chưa đạt ngưỡng coverage 80% cho `AccessLoggingFilter`.

## File chính

- `src/main/java/vn/t3tarot/api/identity/`
- `src/main/resources/db/migration/V2__add_refresh_rotation_and_email_verification.sql`
- `src/main/java/vn/t3tarot/api/common/security/jwt/RsaPrivateKeys.java`, `.../common/random/UuidV7Generator.java`
- `src/test/java/vn/t3tarot/api/identity/`

## Liên quan

- [security-access-control](security-access-control.md): bộ xác minh JWT, `public-paths`.
- [api-error-and-pagination](api-error-and-pagination.md): mã lỗi và `Idempotency-Key`.
- `docs/T3Tarot — Thiết kế dữ liệu_ ERD, UML & bảng PostgreSQL.md`: bảng tài khoản.
