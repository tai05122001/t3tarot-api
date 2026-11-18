# Quy ước code

## Code

- Không viết comment trong code. Tên và cấu trúc phải tự giải thích.
- Package theo module (`vn.t3tarot.api.<module>`), không theo tầng kỹ thuật ở cấp cao nhất. Trong module: package gốc công khai, các package con `web / usecase / domain / infra` là nội bộ (xem `skills/agent-skills/new-module/references/architecture.md`).
- Module `common` chỉ chứa util, mã lỗi chung và cấu hình kỹ thuật, không có nghiệp vụ và không import module nghiệp vụ. Mã lỗi riêng của nghiệp vụ (ví dụ hết lượt hỏi tiếp) thuộc về module sở hữu nó, không đặt vào `common`.
- Hằng số nghiệp vụ (3 câu hỏi tiếp, N lượt trải) đặt trong một policy class, không viết cứng ở SQL hay controller.
- Giờ và ngày: chỉ qua `VietnamClock` (`today()`, `now()`). Không gọi `LocalDate.now()`, `Instant.now()` hay `Clock.systemDefaultZone()`.
- Ngẫu nhiên: chỉ dùng bean `SecureRandom`. Không `Math.random()`, `new Random()` (Sonar `java:S2245`).
- Tiền: `long` VND hoặc `BigDecimal` kèm `RoundingMode` rõ ràng; không `float`, `double`. Chi phí LLM lưu micro-USD kiểu `long`.
- Response thành công trả thẳng body, không bọc envelope. Lỗi trả `ProblemDetail` (RFC 9457) qua `GlobalExceptionHandler`, thêm field `code` (dạng `error.<nhóm>.<lý do>`, ví dụ `error.invalid_param.is_missing`), `requestId`, và `errors` (`[{field, code}]`) khi tham số sai. Chỉ trả mã lỗi: không `message`, `messageCode`, `title`, `detail`, không message của exception, không giá trị người dùng gửi lên; FE tự parse mã. Mỗi `<Module>ErrorCode` có test kiểm mọi mã qua `ErrorCodeFormat.isValid` và không trùng nhau.
- Lỗi nghiệp vụ: ném `ApiException(<Module>ErrorCode.X)`; enum `<Module>ErrorCode` đặt trong `domain` của module và cài đặt interface `ErrorCode` của `common`. Lỗi kỹ thuật dùng `CommonErrorCode`. Thêm mã mới thì thêm vào enum của module, không sửa `common`.
- Phân trang: offset, query `page` (bắt đầu từ 0) và `size` qua `PageQuery`; trả `PageResponse<T>` (`items`, `page`, `size`, `totalItems`, `totalPages`, `hasNext`). Sort do server quyết định, không nhận sort từ client.
- Bảo mật HTTP (`common/security`): mọi đường dẫn mặc định `authenticated()`; endpoint công khai chỉ thêm qua `t3tarot.security.public-paths` và nêu lý do trong PR. `csrf.disable()` bị Sonar S4502 flag: đánh dấu hotspot là Safe trong SonarQube, lý do là token đi bằng header `Authorization`, không dùng cookie hay session.
- Danh tính người gọi chỉ lấy từ `@AuthenticationPrincipal AuthenticatedUser` (claim `sub`). Không nhận `userId` từ body, query hay path; mọi truy vấn theo `reading_id` vẫn kèm `user_id`.
- Access token: JWT RS256, kiểm chữ ký, `iss`, `aud`, `exp`, `sub`; không nhận token ở query hoặc form. Khoá công khai nạp từ `JWT_PUBLIC_KEY`, không có giá trị mặc định (Sonar S2068); khoá riêng không nằm trong repo.
- CORS: liệt kê origin cụ thể trong `t3tarot.security.cors.allowed-origins`, không dùng `*` (Sonar S5122); mobile không cần CORS.
- Actuator chạy ở `MANAGEMENT_PORT`, chỉ cho nội bộ cluster; không mở cổng này ra ingress. Trên cổng API, `/actuator/**` bị chặn.
- Header `Idempotency-Key` là UUID; parse bằng `IdempotencyKey.parse(...)`.
- Mỗi request nhận `X-Request-Id` do server sinh (không tin giá trị client gửi), đưa vào MDC `requestId` và log.

## Lombok

- Dùng `@Getter`, `@RequiredArgsConstructor`, `@Builder` cho DTO và value object.
- Không dùng `@Data`, `@EqualsAndHashCode` trên entity JPA: hash theo mọi field kéo theo quan hệ lazy. Tự viết `equals/hashCode` theo ID.
- Không sinh setter cho các cột nóng của `Reading` (`followup_count`, `status`, `interpret_attempts`, `lease_*`).
- `lombok.config` bật `lombok.addLombokGeneratedAnnotation` để JaCoCo và Sonar bỏ qua code sinh ra.

## SonarQube — các rule hay dính

| Rule | Nội dung | Cách tránh |
| --- | --- | --- |
| `java:S2245` | PRNG không an toàn | Chỉ `SecureRandom` |
| `java:S2068` | Mật khẩu ghi cứng | Đọc từ biến môi trường, không đặt mặc định |
| `java:S3776` | Cognitive complexity > 15 | Tách method, dùng early return |
| `java:S1192` | Chuỗi lặp lại | Hằng số; SQL dùng text block một lần |
| `java:S1166` | Bắt exception mà mất nguyên nhân | Truyền `cause` khi ném lại |
| `java:S5145` | Log chứa dữ liệu người dùng chưa làm sạch | Không log input của người dùng |
| `java:S4502` | Tắt CSRF | Chỉ tắt cho API stateless có chủ đích, ghi rõ lý do trong PR |
| `java:S5122` | CORS quá rộng | Liệt kê origin cụ thể |
| `java:S2699` | Test không có assertion | Mỗi test có ít nhất một assertion |

