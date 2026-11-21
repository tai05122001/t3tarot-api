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

Báo cáo coverage (JaCoCo) sinh sau `mvn clean verify`, mở `target/site/jacoco/index.html`. Chưa có ngưỡng tự động chặn build; kiểm bằng mắt theo `.claude/rules/testing.md`.

`mvn verify` cần Docker đang chạy (Testcontainers). Thông tin DB đọc từ biến môi trường hoặc file `.env`.

## Cấu trúc

Trước khi tạo file mới, dùng skill `project-structure`; cây thư mục ở `.claude/skills/project-structure/references/structure.md`, chuẩn đặt tên và từ điển thuật ngữ ở `.claude/skills/project-structure/references/naming.md`.

- `vn.t3tarot.api`: class main. Mỗi package con trực tiếp là một module Modulith.
- Trong module chia theo tính năng trước, vai trò sau (bắt buộc): `<module>/<feature>/<vai-trò>/Class.java`, vai trò chỉ gồm `controller`, `dto` (`request`, `response`), `service`, `repository`, `entity`, `enumeration`, `policy`, `client`, `config`; phần dùng chung ở `<module>/shared/`. Mỗi feature có một `<Feature>Facade` public trong `service` làm cửa vào, use case package-private và đặt `@Transactional` ở đó; feature khác chỉ gọi qua Facade và `dto`. Package gốc module chỉ chứa thứ module khác được dùng. Luật đầy đủ ở `structure.md`, mục "Cấu trúc trong module". Không viết interface + Impl 1-1, không dùng MapStruct.
- `vn.t3tarot.api.common`: module `OPEN` chứa code dùng chung (giờ Việt Nam, SecureRandom, lỗi API, request id, idempotency key, cấu hình Spring Security). Chia theo nhóm (`constant`, `error`, `security`, `time`, `web`...); hằng số dùng chung vào `common/constant`; nhóm có từ 3 class cùng một mối quan tâm thì tách package con một cấp (`security/jwt`, `security/ratelimit`). Quy tắc ở mục "Đặt file trong `common`" của `structure.md`.
- `src/main/resources/db/migration`: migration Flyway.
- `docker/<service>/`: Dockerfile và cấu hình của image dùng cho dev local (hiện có `postgres`).
- `docs/`: tài liệu gốc của dự án (spec, ERD). `docs/changelog/`: changelog theo feature, do agent ghi và sửa đè sau mỗi task code (skill `changelog`).
- `.claude/`: cấu hình AI (agents, commands, skills, workflows, rules, policies, references, output-styles, hooks); bản đồ ở `.claude/README.md`.

## Quy tắc bắt buộc

1. Server bốc bài bằng `SecureRandom` và lưu trước khi lật. LLM không chọn lá, không tự bịa nghĩa lá; chỉ diễn giải từ Card KB.
2. Mọi mốc ngày tính theo `Asia/Ho_Chi_Minh`, cut-off 00:00, chỉ qua `VietnamClock`. Cấm `LocalDate.now()` và `Instant.now()` trực tiếp.
3. Mọi hạn mức (lượt trải tuần, 3 câu hỏi tiếp, hoàn lượt) kiểm và tăng trong một câu SQL; xem `.claude/skills/hot-path-sql/references/hot-path-sql.md`.
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

Không bịa quy định, giá, thông số đối tác; ghi rõ đâu là giả định.

## Luật và phanh

- Luật (Claude Code tự nạp mọi file trong `.claude/rules/`): `java-code.md` (comment, Sonar, Lombok), `common-module.md` (cái gì được vào `common`), `testing.md` (coverage, chạy và sửa test, cấm xanh giả).
- Phanh (việc phải hỏi trước khi làm): @.claude/policies/ask-before-acting.md

## Skill và tài liệu tham chiếu

Quy tắc chi tiết nằm trong `references/` của từng skill. Khi làm việc thuộc phạm vi skill nào thì dùng skill đó và đọc reference của nó.

| Skill | Dùng khi | Reference |
| --- | --- | --- |
| `new-module` | Thêm module, cần kiến trúc, state machine, ranh giới transaction | `.claude/skills/new-module/references/architecture.md` |
| `hot-path-sql` | Ghi chạm hạn mức, trạng thái, hoàn lượt, lease | `.claude/skills/hot-path-sql/references/hot-path-sql.md` |
| `flyway-migration` | Thêm hoặc sửa bảng, cột, index | `.claude/skills/flyway-migration/references/database.md` |
| `llm-safety-review` | Prompt, validate output, khủng hoảng tâm lý | `.claude/skills/llm-safety-review/references/llm-safety.md` |
| `java-code-review` | Review code; quy ước code, Lombok, Sonar | `.claude/references/conventions.md` |
| `observability` | Metric, log, dashboard | `.claude/skills/observability/references/observability.md` |
| `run-tests` | Chạy test, sửa, coverage | `.claude/skills/run-tests/references/testing.md` |
| `client-compatibility` | Endpoint dùng cho website và mobile app; tương thích app cũ | `.claude/skills/client-compatibility/references/client-platforms.md` |
| `project-structure` | Tạo hoặc di chuyển file: thư mục, package, tên | `.claude/skills/project-structure/references/structure.md`, `.claude/skills/project-structure/references/naming.md` |
| `changelog` | Sau mỗi task code: ghi hoặc sửa đè changelog theo feature | `.claude/skills/changelog/references/changelog.md` |
| `/create-endpoint` (command) | Workflow tạo endpoint mới | `.claude/workflows/create-endpoint/WORKFLOW.md` |
| `/modify-endpoint` (command) | Workflow sửa endpoint | `.claude/workflows/modify-endpoint/WORKFLOW.md`, `.claude/workflows/modify-endpoint/references/workflow.md` |

Tài liệu dùng chung nhiều skill ở `.claude/references/`. Agent theo vai trò ở `.claude/agents/`. Danh sách đầy đủ và quy ước viết skill: `.claude/README.md`.
