# Kiến trúc

Nguồn: Đặc tả feature MVP (F1–F8, D1–D22), Thiết kế dữ liệu (ERD) và các buổi thảo luận kiến trúc 18/11/2026. Chỗ ghi **(đề xuất)** chưa được chốt.

## Đã chốt

| Mục | Quyết định |
| --- | --- |
| Hình dạng deploy | Modular monolith, một deployable Spring Boot |
| Điều phối sinh luận giải | Lease trong PostgreSQL, SSE đọc từ DB, báo bằng `LISTEN/NOTIFY`; không dùng Kafka / RabbitMQ trong MVP |
| Truy cập DB | Hibernate cho CRUD, `JdbcClient` cho đường ghi nóng |
| Migration | Flyway, file SQL |
| Web stack | Spring MVC + virtual threads + `SseEmitter` (đề xuất, chưa xác nhận) |

## Module dự kiến (đề xuất)

Mỗi module là một package con trực tiếp của `vn.t3tarot.api`. Module khác chỉ gọi qua interface công khai hoặc domain event, không đụng repository hay bảng của nhau.

Quy ước cấu trúc (đã chốt 18/11/2026, kiểu "Facade + Use case"): Modulith coi mọi thứ ở package gốc của module là công khai, mọi package con là nội bộ. Vì vậy:

- Package gốc của module chỉ chứa interface công khai của module (ví dụ `ReadingApi`), các record DTO trả ra ngoài và domain event.
- Package con `web`: controller và DTO request/response của HTTP.
- Package con `usecase`: mỗi nghiệp vụ một class package-private (`DrawDailyCard`, `CreateSpread`, `AskFollowup`). `@Transactional` đặt ở đây, không đặt ở interface công khai, vì lời gọi LLM phải nằm ngoài transaction.
- Package con `domain`: entity, enum trạng thái, policy. Package con `infra`: repository, `*HotPathRepository`, client LLM.
- Không viết interface + Impl 1-1. Chỉ tạo interface khi có nhiều bản thực thi hoặc cần chặn phụ thuộc (ví dụ `LlmClient`, cổng thanh toán sau MVP).
- Không dùng MapStruct. DTO là `record` với `static from(...)` viết tay; đường ghi nóng map thẳng từ `JdbcClient` ra record.

| Module | Trách nhiệm | Bảng |
| --- | --- | --- |
| `identity` | User, đăng nhập, cài đặt (`reversal_mode`, `question_retention_days`) | `app_user`, `user_identity`, `otp_challenge` (hoãn cùng OTP), `auth_session` |
| `content` | Card KB, deck, ảnh, content version | `card`, `deck`, `card_image`, `content_version`, `card_meaning` |
| `reading` | Reading, draw engine, quota, state machine | `reading`, `reading_shuffle`, `reading_card`, `reading_content`, `spread_quota` |
| `interpretation` | Prompt builder, gọi LLM, validate, lease worker | `llm_call` |
| `followup` | Hỏi tiếp, hoàn lượt, sweeper | `followup`, `followup_content` |
| `safety` | Classifier khủng hoảng, output filter | `safety_event` |
| `privacy` | Purge, "Xoá ngay" | (xoá `*_content`) |
| `audit` | Nhật ký append-only, product event | `audit_log`, `product_event` |
| `common` | Util, mã lỗi chung, cấu hình kỹ thuật; không có nghiệp vụ; module `OPEN` | không có |

Hướng phụ thuộc (không được có vòng): `interpretation` và `followup` phụ thuộc `reading`; `reading`, `interpretation`, `followup` phụ thuộc `content`; `privacy` phụ thuộc `reading` và `followup`; mọi module có thể ghi `audit`. `common` không phụ thuộc module nào. `ModularityTests` kiểm bằng `ApplicationModules.verify()`.

## State machine của reading (D20)

```
CREATED → DRAWN → INTERPRETING → COMPLETED
                      ↓  ↑
                    FAILED          INTERPRETING → STOPPED_CRISIS
```

| Cạnh | Điều kiện guard | Ghi chú |
| --- | --- | --- |
| → CREATED | Tx: trừ `spread_quota` + insert reading + shuffle | Chỉ SPREAD |
| → DRAWN | `INSERT ... ON CONFLICT (user_id, draw_date) DO NOTHING` | Chỉ DAILY |
| CREATED → DRAWN | `WHERE status = 'CREATED' AND user_id = :uid` | Chọn lá 2 lần: lần sau 0 dòng, trả lá đã có |
| DRAWN, FAILED → INTERPRETING | `WHERE status IN ('DRAWN','FAILED') AND interpret_attempts < 2` | Tối đa 2 lần gọi (gốc + retry 1) |
| INTERPRETING → COMPLETED | `WHERE status = 'INTERPRETING'` | `result_source` = LLM, KB_FALLBACK hoặc MIXED |
| INTERPRETING → STOPPED_CRISIS | `WHERE status = 'INTERPRETING'` | Trạng thái cuối; hoàn quota tuần (đề xuất) |

Không có API nào dẫn tới: `COMPLETED → INTERPRETING`, `STOPPED_CRISIS → bất kỳ`, `DRAWN → CREATED`. Hỏi tiếp chỉ được khi `COMPLETED`.

State machine của followup: `PENDING → ANSWERED | FAILED`. Hoàn lượt chỉ xảy ra trên cạnh `PENDING → FAILED`, nên đúng một lần.

## Luồng trải 3/5 lá (theo ERD, đề xuất)

1. `POST /readings` kèm `Idempotency-Key`: classifier khủng hoảng chạy trước; rồi một transaction gồm trừ quota, tạo reading `CREATED`, xáo 78 lá và lưu `reading_shuffle` (không bao giờ trả về client).
2. Client gửi chỉ số 0–77 (không trùng): `CREATED → DRAWN`, ghi `reading_card`.
3. `GET /readings/{id}/stream` (SSE): đẩy các lá đã validate có sẵn trong DB, chiếm lease nếu chưa ai sinh luận giải, rồi nghe `LISTEN/NOTIFY` để đẩy tiếp từng lá mới ghi.

Chiếm lease (đề xuất): xem `skills/agent-skills/hot-path-sql/references/hot-path-sql.md`. Sinh luận giải chạy trên executor tách khỏi request SSE; mỗi lá validate xong ghi DB ngay, nên reconnect nối lại được.

## Ranh giới transaction

- Tạo reading: quota + reading + shuffle trong một transaction.
- Gọi LLM nằm ngoài mọi transaction (mỗi lượt 10–30 giây; giữ transaction sẽ cạn connection pool).
- Mỗi lá validate xong ghi bằng một transaction ngắn riêng.
- Hỏi tiếp: tăng `followup_count` và insert `followup` (PENDING) cùng một transaction; gọi LLM ngoài transaction; kết thúc bằng ANSWERED, hoặc FAILED kèm hoàn lượt trong một câu lệnh.
- `spring.jpa.open-in-view=false`. Pool Hikari là giới hạn thật dù bật virtual threads.

## Điểm còn mở ảnh hưởng thiết kế

| Điểm | Ảnh hưởng |
| --- | --- |
| ERD lưu `reading_shuffle` (xáo lúc CREATED, chọn lá chỉ tra chỉ số) trong khi một đề xuất khác gộp xáo + chọn vào một bước `POST /readings` | Quyết định có giữ bảng `reading_shuffle`, thêm trạng thái và endpoint chọn lá hay không |
| Cột lease (`lease_owner`, `lease_until`) chưa có trong ERD | Cần thêm vào `reading` nếu chốt cơ chế lease |
| Crypto-shredding cho câu hỏi và luận giải | Backup Postgres không xoá chọn lọc được; phải chốt trước khi viết migration đầu tiên |
| Daily card: gọi LLM khi mở, hay sinh sẵn nhiều biến thể theo `(lá, chiều)` rồi chọn | Ảnh hưởng chi phí token, độ trễ và F1 |
| N lượt trải mỗi tuần; tuần lịch hay 7 ngày cuốn chiếu | `spread_quota` chỉ đúng với tuần lịch |
| Liên kết tài khoản cùng email giữa các cách đăng nhập | Cách gộp daily và quota |
| Đăng nhập đã chốt trước mắt (18/11/2026): username + mật khẩu (email bắt buộc, xác minh email), Google, Facebook; client lấy ID token, server xác minh rồi cấp JWT riêng. Còn mở: C1 tự build hay C2 Keycloak / C3 managed; LLM provider | Chặn F1, F5; F8 cần viết lại theo cách đăng nhập mới |
| Schema chưa có chỗ cho mật khẩu: `user_identity.provider` chưa có giá trị `PASSWORD`, chưa có cột băm mật khẩu và username | Cần migration; hỏi trước khi viết (`flyway-migration`) |
| Thuật toán băm mật khẩu (giả định Argon2id, chưa xác minh), chính sách mật khẩu, khoá tạm khi đoán sai | Giả định cần xác minh trước khi code `identity` |
| Chống tạo hàng loạt tài khoản để lấy thêm daily card và hạn mức; chống brute force và credential stuffing ở endpoint đăng nhập (chưa có rate limit) | Ảnh hưởng quota và an ninh; cần phương án trước khi mở đăng ký |
| Quên mật khẩu và xác minh email cần kênh gửi email (nhà cung cấp chưa chọn) | Chặn luồng đăng ký bằng username |
| Apple Sign-In: nếu app iOS có social login của bên thứ ba, App Store có thể yêu cầu thêm đăng nhập tương đương (chưa kiểm hướng dẫn hiện hành) | Quyết định trước khi phát hành app iOS |
| Java 21 hay 25 | Virtual thread bị pin khi gặp `synchronized` trên Java 21; từ JDK 24 trở đi đã khắc phục |
