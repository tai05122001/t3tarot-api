# Security: kiểm soát truy cập

Cập nhật lần cuối: 2026-11-21 · Trạng thái: Đang phát triển

## Tóm tắt

API là resource server stateless: mọi đường dẫn yêu cầu access token JWT (RS256) trong header `Authorization`, trừ đường dẫn khai báo công khai. Chưa có endpoint cấp token; module `identity` sẽ làm theo quyết định đăng nhập bên dưới.

## Thay đổi gì

- `common/security` (gốc nhóm: `SecurityConfig`, `SecurityProperties`, `AuthenticatedUser`, `ProblemDetailSecurityHandler`, `ManagementPortRequestMatcher`; package con `jwt` và `ratelimit`):
  - `SecurityConfig`: hai filter chain. `managementFilterChain` (thứ tự 1) chỉ áp dụng cho cổng quản trị; `apiFilterChain` bật CORS, tắt CSRF, không session, tắt form login, basic, logout và request cache, đặt header bảo mật, `anyRequest().authenticated()`, resource server JWT. `corsConfigurationSource` không áp CORS cho các đường dẫn khớp đúng trong `t3tarot.security.cors.excluded-paths` (hiện chỉ `/auth/email/verify-page`).
  - `SecurityProperties` (`t3tarot.security`): `cors` (danh sách origin chính xác, không `*`, không credentials), `jwt` (`issuer`, `audience`, `publicKey` hoặc `publicKeyLocation`; `publicKeyPem()` trả khoá đã chọn), `swaggerPublic`, `publicPaths`.
  - `jwt/JwtDecoderConfig`: Nimbus RS256; kiểm chữ ký, `iss`, `aud`, `exp`, `sub` có mặt.
  - `jwt/RsaPublicKeys.parse`: nhận PEM, base64 hoặc chuỗi có `\n` escape; từ chối RSA dưới 2048 bit.
  - `jwt/PemKeySource.read`: lấy khoá từ chuỗi trong biến môi trường hoặc từ file (Spring `Resource`, ví dụ `file:./secrets/jwt-public.pem`); phải đặt đúng một trong hai, thiếu cả hai, đặt cả hai hoặc file không đọc được thì ứng dụng không khởi động.
  - `AuthenticatedUser(userId)` lấy từ claim `sub` (UUID); `jwt/AccessTokenAuthentication`, `jwt/AccessTokenAuthenticationConverter` (từ chối `sub` không phải UUID).
  - `ProblemDetailSecurityHandler`: 401 và 403 sinh trong filter đi qua `HandlerExceptionResolver` để cùng định dạng `ProblemDetail` với `GlobalExceptionHandler`; 401 kèm `WWW-Authenticate: Bearer`.
  - `ManagementPortRequestMatcher`: chain quản trị chỉ khớp khi cổng quản trị khác cổng ứng dụng và request đến đúng cổng đó.
- Header bảo mật: `nosniff`, `Cache-Control: no-store`, CSP `default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`, `X-Frame-Options: DENY`, HSTS (`max-age=63072000; includeSubDomains`, không `preload`).
- Actuator chạy ở `MANAGEMENT_PORT` (mặc định 8081).
- `pom.xml`: thêm `spring-security-oauth2-resource-server`, `spring-security-oauth2-jose`.
- `application.yml`, `.env.example`, `README.md`: thêm khoá cấu hình và hướng dẫn sinh khoá.
- `CLAUDE.md` quy tắc 13: mặc định yêu cầu JWT, `userId` chỉ lấy từ `AuthenticatedUser`, actuator chỉ ở `MANAGEMENT_PORT`.

## Vì sao

- Stateless và bearer token để website và app dùng chung một API; không cookie nên CSRF tắt (Sonar S4502: đánh dấu Safe kèm lý do).
- Chỉ nhận token từ header `Authorization` để không rò token qua URL và log.
- Tách cổng quản trị để probe và metric không đi qua ingress.
- Trang xác nhận email là HTML do server render, không phải tài nguyên CORS. Trang đặt `Referrer-Policy: no-referrer` (chống rò token trong URL) nên trình duyệt gửi `Origin: null` khi submit form POST; `null` không nằm trong danh sách origin nên CORS trả `403 Invalid CORS request`. Giữ `no-referrer` và loại đường dẫn này khỏi CORS, thay vì thêm `null` hay origin của server vào `CORS_ALLOWED_ORIGINS`.
- Quyết định đã chốt với người dùng (2026-11-18): access token JWT ngắn hạn kèm refresh token opaque xoay vòng (băm lưu ở `auth_session`); tách management port; phạm vi gồm chain, lỗi, header, CORS.

## Quyết định đăng nhập (trước mắt, 2026-11-18)

- Cách đăng nhập: username + mật khẩu (username kèm email bắt buộc, xác minh email) và social login Google, Facebook.
- Social login: client lấy ID token của nhà cung cấp, server xác minh chữ ký, `aud`, `exp` rồi cấp JWT của T3Tarot. Giữ stateless, dùng chung web và app.
- Tạm hoãn: OTP email, OTP SMS, Apple Sign-In. Chưa xoá khỏi thiết kế.
- Việc còn mở và việc phải hỏi trước khi làm nằm ở `.claude/skills/new-module/references/architecture.md` mục "Điểm còn mở": migration cho mật khẩu và username, thuật toán băm (Giả định Argon2id), chống brute force và tạo tài khoản hàng loạt, kênh gửi email, Apple Sign-In trên iOS.
- Tài liệu spec F8 và ERD vẫn mô tả bốn cách đăng nhập; cần cập nhật khi viết `identity`.

## Ảnh hưởng

- API / contract: mọi đường dẫn trả 401 khi thiếu hoặc sai token. Body lỗi theo [api-error-and-pagination](api-error-and-pagination.md). Swagger chỉ công khai khi `SWAGGER_ENABLED=true`.
- Cấu hình / biến môi trường: bắt buộc `JWT_ISSUER`, `JWT_AUDIENCE`, và đúng một trong `JWT_PUBLIC_KEY` (chuỗi khoá) hoặc `JWT_PUBLIC_KEY_LOCATION` (đường dẫn file PEM, có tiền tố `file:`); thêm `CORS_ALLOWED_ORIGINS`, `SWAGGER_ENABLED`, `MANAGEMENT_PORT`; `t3tarot.security.cors.excluded-paths` (danh sách đường dẫn khớp đúng, phân cách bằng dấu phẩy, mặc định rỗng; đọc bằng `@Value`, không nằm trong `SecurityProperties`). Thiếu `JWT_*` thì ứng dụng không khởi động (cố ý, fail fast).
- Database: Không.
- Client: gửi `Authorization: Bearer <token>`; origin web phải nằm trong `CORS_ALLOWED_ORIGINS`.
- Tiền, hạn mức, state machine: Không.

## QA cần test

- Không token, token hết hạn, sai chữ ký, sai `iss`, sai `aud`, thiếu `sub`, `sub` không phải UUID: đều 401 với `code` `error.auth.unauthenticated`.
- Token hợp lệ vào endpoint công khai và endpoint bảo vệ.
- Token truyền qua query string hoặc form bị từ chối.
- CORS: origin trong danh sách được phép, origin lạ bị chặn, preflight đúng.
- Submit form xác nhận email từ link Gmail (`Origin: null`) không bị CORS chặn; `Origin: null` vào đường dẫn khác vẫn bị chặn. Cần thử tay sau reverse proxy thật.
- Header bảo mật có mặt ở mọi response.
- Cổng 8081 trả actuator; cổng 8080 không trả actuator.
- Cấu hình hai cổng trùng nhau không làm mở toàn bộ ứng dụng.

## Rollout

- Sinh cặp khóa RSA; khóa công khai đặt vào `JWT_PUBLIC_KEY` hoặc mount file PEM rồi trỏ `JWT_PUBLIC_KEY_LOCATION` tới đó. Khóa riêng do module `identity` dùng, xem [identity-authentication](identity-authentication.md).
- Cập nhật liveness và readiness probe của Kubernetes, và nguồn scrape Prometheus, sang cổng 8081; manifest chưa được sửa (thuộc nhóm hỏi trước khi làm).
- Kiểm `RequestIdFilter` chạy trước security chain để 401 và 403 mang `requestId`; thứ tự hiện chưa xác minh.

## Trạng thái kiểm chứng

`mvn clean verify` (2026-11-21, cần Docker): 182 test xanh; coverage dòng `PemKeySource`, `JwtDecoderConfig` 100%. Test liên quan: `PemKeySourceTest`, `RsaPublicKeysTest`, `JwtDecoderConfigTest`, `AuthenticatedUserTest`, `AccessTokenAuthenticationConverterTest`, `ProblemDetailSecurityHandlerTest`, `ManagementPortRequestMatcherTest`, `SecurityPropertiesTest`, `SecurityConfigTest`, `TestJwtFactory`; `T3TarotApiApplicationTests` được sửa để cấp thuộc tính JWT.

Sau khi thêm `excluded-paths` (2026-11-21): `SecurityConfigTest` 28 test xanh, chạy riêng bằng `-Dtest=SecurityConfigTest`; chưa chạy lại `mvn verify` toàn bộ. Chưa thử tay trên trình duyệt sau khi sửa.

Chưa xác minh với Spring Security 7: `permissionsPolicy`, `frameOptions().deny()`, `xssProtection().disable()`, chuỗi HSTS đúng từng ký tự, tên bean `handlerExceptionResolver`.

## File chính

- `src/main/java/vn/t3tarot/api/common/security/`, `.../security/jwt/`, `.../security/ratelimit/`
- `src/main/resources/application.yml`, `.env.example`, `pom.xml`
- `src/test/java/vn/t3tarot/api/common/security/`, `.../security/jwt/`, `.../security/ratelimit/`

## Liên quan

- [api-error-and-pagination](api-error-and-pagination.md)
- Tài liệu tổng quan trên claude.ai (bảng "Quyết định đã chốt") ghi quyết định đăng nhập.
