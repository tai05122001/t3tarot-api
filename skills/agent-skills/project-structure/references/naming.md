# Chuẩn đặt tên

Mục tiêu: nhìn tên là biết file thuộc module nào, làm vai trò gì; tìm một khái niệm bằng `grep` là ra đủ API, class, bảng, metric, test. Chỗ ghi **(đề xuất)** chưa được chốt.

## Năm nguyên tắc

1. **Tên nói vai trò.** Hậu tố cho biết lớp (`Controller`, `Api`, `HotPathRepository`...). Lý do: đọc stack trace, log, diff mà không cần mở file.
2. **Code, đường dẫn, tên file dùng tiếng Anh.** Tiếng Việt chỉ dùng trong nội dung tài liệu và skill. Lý do: IDE, `grep`, Sonar và thư viện đều làm việc với tên tiếng Anh; trộn hai ngôn ngữ làm tìm kiếm sót.
3. **Một khái niệm, một tên, dùng xuyên suốt** từ API tới class, bảng, metric (xem Từ điển). Lý do: `grep followup` phải ra mọi thứ liên quan; nếu chỗ gọi `followup`, chỗ gọi `question`, chỗ gọi `followUp` thì tracking đứt.
4. **Không viết tắt**, trừ danh sách cho phép: `id`, `api`, `url`, `uuid`, `llm`, `sse`, `kb` (Card KB), `vnd`, `usd`, `jdbc`, `sql`. Lý do: `rdg`, `fu`, `qt` không ai nhớ nghĩa sau ba tháng.
5. **Không đưa số thứ tự, ngày, tên người vào tên class hay file** (trừ migration). Lý do: `ReadingServiceV2`, `NewReadingController`, `TaiUtils` không nói gì về vai trò và sẽ sai khi code đổi.

## Từ điển thuật ngữ

| Khái niệm | Java | DB | API | Metric, log |
| --- | --- | --- | --- | --- |
| Một lượt xem bài | `Reading` | `reading` | `/readings` | `reading_*`, `readingId` |
| Lá hằng ngày | `kind = DAILY` | `kind = 'DAILY'` | chưa có | `flow="daily"` |
| Trải 3 hoặc 5 lá | `kind = SPREAD`, `SpreadType` | `kind = 'SPREAD'`, `spread_type` | chưa có | `flow="spread"` |
| Câu hỏi tiếp | `Followup` | `followup` | `/readings/{readingId}/followups` | `followup_*`, `followupId` |
| Lượt trải mỗi tuần | `SpreadQuota` | `spread_quota` | không có | `quota_*` |
| Lá bài, bộ bài | `Card`, `Deck` | `card`, `deck` | `/cards` (đề xuất) | `cardId`, `deckId` |
| Lá ngược | `isReversed` | `is_reversed` | `isReversed` | |
| Chế độ lá ngược | `ReversalMode` | `reversal_mode` | `reversalMode` | |
| Phiên bản nội dung | `ContentVersion` | `content_version` | | `contentVersion` |
| Hoàn lượt | `refund...` | | | `followup_refund_total` |
| Lease sinh luận giải | `lease...` | `lease_owner`, `lease_until` | | |

Viết `followup` liền, không `followUp`, không `follow_up`. Thêm khái niệm mới thì thêm dòng vào bảng này trước khi đặt tên ở chỗ khác.

## Java

### Package

- Chữ thường, một từ, số ít, không gạch dưới: `reading`, `followup`, `usecase`, `infra`.
- Module: `identity`, `content`, `reading`, `interpretation`, `followup`, `safety`, `privacy`, `audit`, `common`.
- Package con trong module chỉ dùng: `web`, `usecase`, `domain`, `infra`. Trong `common` chia theo nhóm kỹ thuật: `error`, `idempotency`, `random`, `time`, `web`.

### Class theo vai trò

| Vai trò | Package | Tên | Ví dụ | Sai |
| --- | --- | --- | --- | --- |
| Interface công khai của module | gốc | `<Module>Api` | `ReadingApi` | `IReadingService`, `ReadingService` |
| Cài đặt interface công khai (đề xuất) | `usecase` | `<Module>Facade` | `ReadingFacade` | `ReadingApiImpl` |
| Use case | `usecase` | động từ + danh từ | `DrawDailyCard`, `CreateSpread`, `AskFollowup` | `ReadingUseCase`, `DailyCardHandler` |
| Controller | `web` | `<Tài nguyên>Controller` | `ReadingController`, `FollowupController` | `ApiController`, `ReadingRest` |
| DTO request HTTP (đề xuất) | `web` | `<Hành động><Tài nguyên>Request` | `CreateSpreadRequest`, `AskFollowupRequest` | `ReadingDto`, `Req` |
| DTO response HTTP (đề xuất) | `web` | `<Tài nguyên>Response` | `ReadingResponse` | `ReadingResult`, `ReadingVO` |
| DTO trả ra module khác (đề xuất) | gốc | `<Tài nguyên>View` | `ReadingView` | dùng thẳng entity |
| Domain event | gốc | `<Tài nguyên><Quá khứ>Event` | `ReadingCompletedEvent` | `ReadingEvent`, `CompleteReading` |
| Entity JPA | `domain` | danh từ số ít, khớp tên bảng | `Reading`, `SpreadQuota` | `ReadingEntity`, `Readings` |
| Enum trạng thái | `domain` | `<Tài nguyên>Status` | `ReadingStatus`, `FollowupStatus` | `Status`, `ReadingState` |
| Enum khác | `domain` | danh từ | `SpreadType`, `ReversalMode` | `Type`, `Mode` |
| Policy, hằng số nghiệp vụ | `domain` | `<Chủ đề>Policy` | `FollowupPolicy` | `Constants`, `AppConfig` |
| Mã lỗi riêng module | `domain` | `<Module>ErrorCode` | `ReadingErrorCode` | đặt trong `common` |
| Repository Spring Data | `infra` | `<Entity>Repository` | `ReadingRepository` | `ReadingDao` |
| Câu ghi nóng `JdbcClient` | `infra` | `<Entity>HotPathRepository` | `ReadingHotPathRepository` | `ReadingJdbc`, `ReadingQuery` |
| Client dịch vụ ngoài | `infra` | `<Dịch vụ>Client` | `LlmClient` | `LlmHelper` |
| Job định kỳ (đề xuất) | `infra` hoặc `usecase` | `<Việc>Job` | `PurgeExpiredContentJob`, `StaleFollowupSweepJob` | `Scheduler1` |
| Nghe event (đề xuất) | `usecase` của module nhận | `<Event không hậu tố>Listener` | `ReadingCompletedListener` | `EventHandler` |
| Cấu hình Spring | package của thứ được cấu hình | `<Chủ đề>Config` | `TimeConfig` | `Config`, `BeanConfig` |
| Thuộc tính cấu hình (đề xuất) | gốc hoặc `domain` | `<Module>Properties` | `FollowupProperties` | đọc `@Value` rải rác |

Không dùng tiền tố `I` cho interface, không dùng hậu tố `Impl`, `Manager`, `Helper`, `Util`, `Processor` chung chung.

### Method và biến

- Method: động từ, camelCase. Boolean bắt đầu bằng `is`, `has`, `can`.
- Use case có đúng một method công khai tên `execute` (đề xuất). Lý do: tìm `execute(` là ra mọi điểm vào nghiệp vụ.
- Repository Spring Data giữ tiền tố `find`, `exists`, `count`. Repository đường nóng đặt theo ý định nghiệp vụ, trả về kết quả rõ ràng: `insertDailyIfAbsent`, `claimFollowup` (trả `boolean`), `refundFollowup`, `claimInterpretationLease`.
- Biến: danh từ, camelCase. Khi có nhiều ID trong một hàm thì gọi rõ `readingId`, `userId`, không dùng `id`.
- Hậu tố theo kiểu dữ liệu:

| Kiểu | Hậu tố | Ví dụ |
| --- | --- | --- |
| Mốc thời điểm (`Instant`) | `At` | `drawnAt`, `expiresAt` |
| Ngày theo giờ VN (`LocalDate`) | `Date` | `drawDate`, `weekStartDate` |
| Khoảng thời gian (`Duration`) | `Duration` | `leaseDuration` |
| Tiền VND (`long`) | `Vnd` | `priceVnd` |
| Chi phí LLM (`long` micro-USD) | `MicroUsd` | `costMicroUsd` |
| Số lượng | `Count` | `followupCount` |

- Hằng số: `UPPER_SNAKE_CASE`, ví dụ `MAX_PER_READING`.
- Giá trị enum: `UPPER_SNAKE_CASE`, trùng chữ với giá trị `CHECK` trong DB (`INTERPRETING`, `STOPPED_CRISIS`).

## Test

- Class: `<Class được test>Test`. Test đồng thời: `<Class>ConcurrencyTest`. Không đặt `*IT` (surefire không chạy).
- Method: camelCase tiếng Anh mô tả hành vi, dạng `<kết quả>When<điều kiện>`.
  - Đúng: `returnsExistingCardWhenDrawnTwiceSameDay`, `onlyOneClaimSucceedsWhenFiveRequestsRace`, `refundsExactlyOnceWhenLlmFails`.
  - Sai: `test1`, `testDraw`, `shouldWork`, `happyCase`.
  - Lý do: khi test đỏ, tên test đọc lên là biết quy tắc nghiệp vụ nào bị vỡ.
- Nhóm ca bằng `@Nested` theo kịch bản (ví dụ `WhenQuotaExhausted`), tương ứng "suite theo luồng nghiệp vụ".
- Dữ liệu giả đặt tên theo vai: `USER_A`, `USER_B`, `READING_OF_USER_A`. Không dùng dữ liệu thật.

## Database và migration

Theo quy ước trong ERD:

| Đối tượng | Quy tắc | Ví dụ |
| --- | --- | --- |
| Bảng | snake_case, số ít | `reading`, `spread_quota` |
| Cột | snake_case | `followup_count` |
| Khoá ngoại | `<bảng>_id` | `reading_id`, `user_id` |
| Mốc thời điểm | `*_at`, kiểu `timestamptz` | `drawn_at` |
| Ngày | `*_date`, kiểu `date` | `draw_date` |
| Boolean | `is_*` | `is_reversed` |
| Tiền | `*_vnd`, `*_micro_usd` | `cost_micro_usd` |
| Index | `ix_<bảng>_<mục đích>` | `ix_reading_purge` |
| Unique index | `ux_<bảng>_<mục đích>` | `ux_reading_daily` |
| Check, khoá ngoại có tên (đề xuất) | `ck_<bảng>_<cột>`, `fk_<bảng>_<bảng tham chiếu>` | `ck_reading_status` |

Migration: `V{n}__{động_từ}_{đối_tượng}.sql`, chữ thường, gạch dưới.

- Đúng: `V3__add_lease_columns_to_reading.sql`, `V4__create_event_publication_table.sql`.
- Sai: `V3__update.sql`, `V3__fix.sql`, `V3__Tai_changes.sql`.
- `n` là số kế tiếp. Hai nhánh cùng tạo một số thì nhánh merge sau đổi số trước khi merge; không sửa migration đã chạy.

## API

- Đường dẫn: danh từ số nhiều, kebab-case, biến đường dẫn ghi rõ (`{readingId}`, không `{id}`).
  - Đúng: `/readings/{readingId}/followups`, `/readings/{readingId}/stream`.
  - Sai: `/getReading`, `/reading/create`, `/readings/{id}/doFollowup`.
- Tiền tố phiên bản `/api/v1`: đề xuất, chưa chốt (xem `client-compatibility`).
- Field JSON và query param: camelCase (`readingId`, `isReversed`).
- Header: `Idempotency-Key`, `X-Request-Id`; header client đề xuất `X-Client-Platform`, `X-App-Version`.
- Mã lỗi: `UPPER_SNAKE_CASE`. Mã chung không tiền tố (`VALIDATION_FAILED`); mã nghiệp vụ có tiền tố module (đề xuất): `FOLLOWUP_LIMIT_REACHED`, `READING_NOT_FOUND`.

## Cấu hình và biến môi trường

- Khoá trong `application.yml` của dự án: `t3tarot.<module>.<khoá-kebab-case>` (đề xuất), ví dụ `t3tarot.followup.max-per-reading`.
- Biến môi trường: `UPPER_SNAKE_CASE`, tiền tố theo nhóm: `DB_`, `LLM_`, `SWAGGER_`. Mỗi biến mới phải có trong `.env.example` với giá trị giả.

## Observability

- Metric: snake_case, `<chủ đề>_<thứ đo>_<đơn vị>`, theo quy ước Prometheus: bộ đếm kết thúc `_total`, thời gian `_seconds`. Ví dụ `followup_refund_total`, `llm_duration_seconds`.
- Khoá MDC và field log: camelCase, trùng tên biến Java: `requestId`, `userId`, `readingId`, `followupId`. Lý do: cùng một chuỗi tìm được ở log Loki và trong code.
- Không đặt nội dung câu hỏi hay luận giải vào tên metric, label hay khoá log.

## Tài liệu và skill

- `docs/`: giữ nguyên tên tài liệu gốc.
- Skill: thư mục kebab-case tiếng Anh, không chứa tên dự án (`hot-path-sql`). File trong `references/` kebab-case `.md`.

## Tracking

- Đường dẫn đọc thành câu: `reading/usecase/DrawDailyCard.java` nghĩa là module reading, lớp use case, nghiệp vụ rút lá hằng ngày.
- Khi tạo file, báo cáo cuối task có bảng:

| File | Module | Lớp | Vai trò | Thuộc tính năng |
| --- | --- | --- | --- | --- |
| `reading/usecase/DrawDailyCard.java` | reading | usecase | Rút lá hằng ngày | Daily card |

- Branch (đề xuất): `feat/<module>-<mo-ta-ngan>`, `fix/<module>-<mo-ta-ngan>`, ví dụ `feat/reading-daily-card`.
- Commit (đề xuất): Conventional Commits, phạm vi là tên module: `feat(reading): add daily card endpoint`, `fix(followup): refund once on llm timeout`. Lý do: `git log --grep "(followup)"` ra mọi thay đổi của một module.
