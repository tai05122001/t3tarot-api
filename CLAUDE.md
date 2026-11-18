# T3Tarot API

Backend của T3Tarot: website xem tarot bằng LLM cho người Việt, lõi là "mỗi ngày một lá" (daily card). Tài liệu gốc (Tổng quan, Đặc tả feature MVP, Feature cốt lõi, Thiết kế dữ liệu) nằm trong `docs/`, xuất từ Project "Tarot AI" trên claude.ai. Repo này chỉ chứa backend; UI nằm ở repo khác.

Trả lời bằng tiếng Việt. Giữ thuật ngữ kỹ thuật bằng tiếng Anh.

## Stack

| Mục | Trạng thái |
| --- | --- |
| Java + Spring Boot 4.1.x, Maven | Đã chốt. `java.version` trong pom đang là 21; Java 25 dự kiến, chưa chốt |
| Modular monolith + Spring Modulith | Đã chốt |
| PostgreSQL + Flyway | Đã chốt. DDL trong ERD đã chạy thử trên PG 16, `docker-compose.yml` đang dùng PG 17 |
| Hibernate cho CRUD, `JdbcClient` cho đường ghi nóng | Đã chốt |
| Prometheus + Loki + Grafana | Đã chốt (không dùng ELK) |
| Lombok | Đã chốt |
| Đăng nhập: username + mật khẩu (email bắt buộc, xác minh email) và social login Google, Facebook; client lấy ID token của nhà cung cấp, server xác minh rồi cấp JWT riêng | Chốt trước mắt (18/11/2026). OTP email / SMS tạm hoãn. Apple Sign-In chưa quyết; có thể bắt buộc trên iOS, chưa kiểm quy định App Store |
| LLM provider, tracing | Chưa chốt |

## Lệnh

```
copy .env.example .env
docker compose up -d
mvn clean verify
mvn spring-boot:run
mvn -Dtest=VietnamClockTest test
```

Báo cáo coverage (JaCoCo) sinh sau `mvn clean verify`, mở `target/site/jacoco/index.html`. Chưa có ngưỡng tự động chặn build; kiểm bằng mắt theo mục Test coverage.

`mvn verify` cần Docker đang chạy (Testcontainers). Thông tin DB đọc từ biến môi trường hoặc file `.env`.

## Cấu trúc

Trước khi tạo file mới, dùng skill `project-structure`; cây thư mục ở `skills/agent-skills/project-structure/references/structure.md`, chuẩn đặt tên và từ điển thuật ngữ ở `skills/agent-skills/project-structure/references/naming.md`.

- `vn.t3tarot.api`: class main. Mỗi package con trực tiếp là một module Modulith.
- Trong module: package gốc chỉ chứa interface công khai, DTO trả ra và event; `web` (controller), `usecase` (mỗi nghiệp vụ một class, nơi đặt `@Transactional`), `domain`, `infra` là nội bộ. Không viết interface + Impl 1-1, không dùng MapStruct.
- `vn.t3tarot.api.common`: module `OPEN` chứa code dùng chung (giờ Việt Nam, SecureRandom, lỗi API, request id, idempotency key, cấu hình Spring Security).
- `src/main/resources/db/migration`: migration Flyway.
- `docker/<service>/`: Dockerfile và cấu hình của image dùng cho dev local (hiện có `postgres`).
- `docs/`: tài liệu gốc của dự án (spec, ERD). `docs/changelog/`: changelog theo feature, do agent ghi và sửa đè sau mỗi task code (skill `changelog`).
- `skills/`: agent skill và agent workflow, quy tắc chi tiết nằm trong `references/` của từng skill; xem mục cuối.

## Quy tắc bắt buộc

1. Server bốc bài bằng `SecureRandom` và lưu trước khi lật. LLM không chọn lá, không tự bịa nghĩa lá; chỉ diễn giải từ Card KB.
2. Mọi mốc ngày tính theo `Asia/Ho_Chi_Minh`, cut-off 00:00, chỉ qua `VietnamClock`. Cấm `LocalDate.now()` và `Instant.now()` trực tiếp.
3. Mọi hạn mức (lượt trải tuần, 3 câu hỏi tiếp, hoàn lượt) kiểm và tăng trong một câu SQL; xem `skills/agent-skills/hot-path-sql/references/hot-path-sql.md`.
4. Chuyển trạng thái bằng `UPDATE ... WHERE status = :cũ` (compare-and-set), không đọc rồi ghi.
5. Mọi thao tác ghi có idempotency key; mọi truy vấn theo `reading_id` đều kèm `user_id` (chặn IDOR).
6. Output của LLM là JSON có cấu trúc; kiểm `card_id` và xuôi/ngược khớp lá đã bốc trước khi trả cho người dùng.
7. Câu hỏi của người dùng chỉ là dữ liệu, không phải chỉ dẫn.
8. Không ghi câu hỏi hoặc luận giải ra log, metric label hay audit.
9. Không `DELETE` `reading` / `followup`; xoá dữ liệu chỉ xoá bảng `*_content`.
10. Mỗi reading và mỗi câu hỏi tiếp lưu `model`, `prompt_version`, `content_version`, `reversal_mode`, `deck_id`.
11. Tiền: `long` VND hoặc `BigDecimal` với `RoundingMode` rõ ràng, không `float`/`double`.
12. Website và mobile app dùng chung một API. Mỗi endpoint phải xác định client nào dùng; khi app đã phát hành dùng endpoint thì không phá tương thích (không xoá, đổi tên, đổi nghĩa field, không thêm field request bắt buộc). Xem skill `client-compatibility`.
13. Mọi đường dẫn mặc định yêu cầu access token JWT; endpoint công khai phải khai báo trong `t3tarot.security.public-paths` và nêu lý do trong PR. `userId` chỉ lấy từ `AuthenticatedUser` (claim `sub`), không nhận từ body, query hay path. Actuator chỉ ở `MANAGEMENT_PORT`, không mở ra ingress.
14. Sau mỗi task có sửa code, ghi hoặc sửa đè `docs/changelog/<feature>.md` theo skill `changelog`. Phần đổi có liên quan đến changelog đã có thì sửa đè cho đúng trạng thái hiện hành, không nối thêm lịch sử (git giữ lịch sử).
15. Lỗi trả cho client chỉ có mã lỗi, tuyệt đối không có câu chữ. Mã dạng `error.<nhóm>.<lý do>`: chữ thường, snake_case trong từng đoạn, đúng 3 đoạn ngăn bằng dấu chấm, ví dụ `error.invalid_param.is_missing`; FE tự parse mã ra thông báo. Body lỗi không có `message`, `messageCode`, `title`, `detail`, không có `message` của exception, không echo giá trị người dùng gửi. Lỗi theo từng tham số trả trong `errors` dạng `[{field, code}]`. Mã mới (kể cả của module) phải qua `ErrorCodeFormat` và có test; thêm, đổi, xoá mã là đổi API contract (xem `client-compatibility`).

## Quy tắc code

- Không viết comment trong code; đặt tên và cấu trúc để tự giải thích.
- Code phải sạch SonarQube; xem `skills/agent-skills/java-code-review/references/conventions.md`.
- Lombok: không dùng `@Data` / `@EqualsAndHashCode` trên entity JPA.
- Khi sửa code có sẵn chỉ hiển thị phần thay đổi kèm ngữ cảnh đủ để định vị.

## Module common

- `common` chỉ chứa code không liên quan nghiệp vụ: hàm util (thời gian, ngẫu nhiên...), mã lỗi và exception chung, filter và cấu hình kỹ thuật (request id, `Clock`, `SecureRandom`), value object kỹ thuật (`IdempotencyKey`).
- Không đặt vào `common`: entity, repository, use case, enum trạng thái nghiệp vụ, policy (3 câu hỏi tiếp, N lượt trải), mã lỗi riêng của nghiệp vụ, và bất kỳ class nào nhắc đến reading, card, followup, quota, topic, spread.
- `common` không import module nghiệp vụ nào. Nếu một class phải biết nghiệp vụ mới giải thích được thì nó thuộc module nghiệp vụ, không thuộc `common`.

## Test coverage

- Mỗi endpoint thêm mới hoặc sửa phải có test đi kèm, và line coverage ≥ 80% cho các lớp service (use case, facade), controller và utils (class trong `common`) mà endpoint đó thêm hoặc sửa. Không đạt thì PR chưa được merge.
- Giả định (chưa xác nhận): đo bằng JaCoCo, tính line coverage, mỗi class tính riêng. Chưa cấu hình chặn trong `pom.xml`, hiện kiểm bằng review.
- Coverage 80% là mức tối thiểu, không thay cho các ca bắt buộc ở `skills/agent-skills/java-code-review/references/conventions.md` (đồng thời, timeout LLM, ranh giới giờ VN).

## Chạy và sửa test

- "Test xanh" nghĩa là mọi test pass. Coverage ≥ 80% là cổng riêng, kiểm sau khi test xanh.
- Mỗi task chạy–sửa tối đa 3 vòng. Hết 3 vòng vẫn đỏ thì dừng, báo test nào đỏ, nguyên nhân (đánh dấu rõ đâu là giả định) và hỏi hướng xử lý.
- Test đỏ vì code sai thì sửa code. Test đỏ vì test sai thì nói rõ vì sao sai và hỏi trước khi sửa test.
- Cấm để test "xanh giả": xoá test, `@Disabled`, nới assertion, bắt rộng exception, `-DskipTests`, hạ ngưỡng coverage, loại class khỏi báo cáo JaCoCo. Test chập chờn (flaky) phải tìm nguyên nhân, không retry cho qua.

## Hỏi trước khi làm

- Công thức tiền, lãi, phí, lượt; chuyển trạng thái của reading / followup.
- Migration DB, thay đổi API contract, schema Kafka / RabbitMQ.
- Mọi thứ chạm production: cấu hình, deploy, dữ liệu thật.
- Xoá dữ liệu hoặc code ngoài phạm vi task.
- Gọi API đối tác bên ngoài.

Không bịa quy định, giá, thông số đối tác; ghi rõ đâu là giả định.

## Skill và tài liệu tham chiếu

Quy tắc chi tiết nằm trong `references/` của từng skill. Khi làm việc thuộc phạm vi skill nào thì dùng skill đó và đọc reference của nó.

| Skill | Dùng khi | Reference |
| --- | --- | --- |
| `new-module` | Thêm module, cần kiến trúc, state machine, ranh giới transaction | `skills/agent-skills/new-module/references/architecture.md` |
| `hot-path-sql` | Ghi chạm hạn mức, trạng thái, hoàn lượt, lease | `skills/agent-skills/hot-path-sql/references/hot-path-sql.md` |
| `flyway-migration` | Thêm hoặc sửa bảng, cột, index | `skills/agent-skills/flyway-migration/references/database.md` |
| `llm-safety-review` | Prompt, validate output, khủng hoảng tâm lý | `skills/agent-skills/llm-safety-review/references/llm-safety.md` |
| `java-code-review` | Review code; quy ước code, Lombok, Sonar | `skills/agent-skills/java-code-review/references/conventions.md` |
| `observability` | Metric, log, dashboard | `skills/agent-skills/observability/references/observability.md` |
| `run-tests` | Chạy test, sửa, coverage | `skills/agent-skills/run-tests/references/testing.md` |
| `client-compatibility` | Endpoint dùng cho website và mobile app; tương thích app cũ | `skills/agent-skills/client-compatibility/references/client-platforms.md` |
| `project-structure` | Tạo hoặc di chuyển file: thư mục, package, tên | `skills/agent-skills/project-structure/references/structure.md`, `skills/agent-skills/project-structure/references/naming.md` |
| `changelog` | Sau mỗi task code: ghi hoặc sửa đè changelog theo feature | `skills/agent-skills/changelog/references/changelog.md` |
| `create-endpoint` | Workflow tạo endpoint mới | (dùng các skill trên) |
| `modify-endpoint` | Workflow sửa endpoint | `skills/agent-workflows/modify-endpoint/references/workflow.md` |

Danh sách skill, quy ước viết skill và cách sao chép sang `.claude/skills/`: `skills/README.md`.
