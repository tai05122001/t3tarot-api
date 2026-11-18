# Skill

Mỗi skill gồm `SKILL.md` (các bước, quy tắc bắt buộc, danh sách "hỏi trước khi làm") và thư mục `references/` chứa quy tắc chi tiết. `CLAUDE.md` trỏ vào các reference này; tài liệu gốc của dự án (spec, ERD) nằm ở `docs/`, changelog theo feature nằm ở `docs/changelog/`.

Tên skill không mang tên dự án. Vị trí nguồn tách hai loại:

- `skills/agent-skills/<tên>/SKILL.md`: agent skill.
- `skills/agent-workflows/<tên>/SKILL.md`: agent workflow.

Mỗi `SKILL.md` có phần đầu `name` và `description`. Claude Code chỉ đọc skill của dự án ở `.claude/skills/<tên>/SKILL.md` (một cấp, không chia thư mục con), nên sau khi tải repo cần sao chép sang đó (công cụ ghi file từ xa không được phép ghi vào `.claude`). Trên Windows, từ gốc repo:

```
xcopy skills\agent-skills .claude\skills /E /I /Y
xcopy skills\agent-workflows .claude\skills /E /I /Y
```

Công cụ khác (Codex, Copilot, Junie...) chưa được kiểm tra có đọc định dạng và thư mục này không; cần xác minh riêng trước khi dựa vào.

Hai loại:

- **Agent skill**: làm một việc, dùng độc lập hoặc được workflow gọi.
- **Agent workflow**: điều phối nhiều agent skill theo thứ tự, có điểm dừng chờ xác nhận. Tách riêng khỏi agent skill, cả về thư mục lẫn cách dùng.

## Agent skill

| Skill | Dùng khi | Nguồn |
| --- | --- | --- |
| `hot-path-sql` | Viết hoặc review câu ghi chạm hạn mức, lá bài, trạng thái, hoàn lượt, lease | `skills/agent-skills/hot-path-sql/references/hot-path-sql.md` |
| `new-module` | Thêm module Modulith mới theo Facade + Use case | `skills/agent-skills/new-module/references/architecture.md`, `skills/agent-skills/java-code-review/references/conventions.md` |
| `flyway-migration` | Viết migration mới hoặc dựng schema từ ERD; luôn hỏi trước | `skills/agent-skills/java-code-review/references/conventions.md`, ERD |
| `llm-safety-review` | Viết hoặc review prompt, validator, filter khủng hoảng | `skills/agent-skills/llm-safety-review/references/llm-safety.md` |
| `java-code-review` | Review code hoặc PR theo thứ tự ưu tiên của dự án | `CLAUDE.md`, `skills/agent-skills/java-code-review/references/conventions.md` |
| `observability` | Thêm metric, dựng dashboard Grafana, truy vấn PromQL / LogQL | `skills/agent-skills/java-code-review/references/conventions.md`, `skills/agent-skills/llm-safety-review/references/llm-safety.md` |
| `run-tests` | Chạy test, sửa tối đa 3 vòng, không làm xanh giả, kiểm coverage JaCoCo | `CLAUDE.md`, `skills/agent-skills/java-code-review/references/conventions.md` |
| `client-compatibility` | Kiểm endpoint chạy đúng cho website và mobile app, không phá app phiên bản cũ | `skills/agent-skills/client-compatibility/references/client-platforms.md` |
| `project-structure` | Quyết định file mới đặt ở thư mục, package nào và đặt tên ra sao | `skills/agent-skills/project-structure/references/structure.md`, `skills/agent-skills/project-structure/references/naming.md` |
| `changelog` | Sau mỗi task có sửa code: ghi hoặc sửa đè `docs/changelog/<feature>.md` cho đúng trạng thái hiện hành | `skills/agent-skills/changelog/references/changelog.md` |

## Agent workflow

| Agent workflow | Luồng | Gọi các skill | Điểm dừng chờ xác nhận |
| --- | --- | --- | --- |
| `create-endpoint` | Xác định client (website, app, cả hai) → góp ý yêu cầu → scope và giả định → DTO và OpenAPI → controller → interface công khai → use case → SQL → test → chạy test → tự review → changelog → Session Feature Summary | `new-module`, `client-compatibility`, `hot-path-sql`, `flyway-migration`, `run-tests`, `java-code-review`, `changelog` | Khi chưa rõ client; sau bước scope; trước migration; trước khi thêm dependency; khi hết 3 vòng vẫn đỏ; khi test sai cần sửa test |
| `modify-endpoint` | Xác định endpoint và client đang gọi → phân loại và đánh giá ảnh hưởng → checklist để duyệt → sửa nhỏ nhất, không đụng logic ổn định → test lại theo mức Thấp / Trung / Cao → changelog (sửa đè) → tổng kết | `client-compatibility`, `hot-path-sql`, `flyway-migration`, `run-tests`, `java-code-review`, `changelog` | Sau checklist (luôn dừng); khi cần sửa ngoài checklist; trước migration, đổi contract, phá vỡ app cũ hoặc công thức tiền; khi mốc test đã đỏ; khi hết 3 vòng; khi test sai cần sửa test |

Chi tiết từng bước: `skills/agent-workflows/modify-endpoint/references/workflow.md`. Điều kiện chạy: cần `mvn` và Docker trên máy chạy. Quy tắc chạy và sửa test: `CLAUDE.md` mục "Chạy và sửa test".

## Quy ước viết skill

- `description` nêu rõ khi nào dùng, tối đa vài câu, bằng tiếng Việt kèm thuật ngữ tiếng Anh.
- Thân skill ngắn: các bước, quy tắc bắt buộc, danh sách "hỏi trước khi làm", đường dẫn tới `references/`.
- SQL mẫu và quy tắc dài đặt trong `references/` của skill sở hữu; skill khác trỏ tới đó, không chép lại.
- Skill phải nhắc các bước dễ quên: kèm `user_id`, idempotency key, `VietnamClock`, không log PII, hỏi trước khi chạm công thức tiền, migration hay API contract.
- Workflow gọi agent skill theo tên, không lặp lại nội dung của chúng.

## Còn thiếu

- Chưa có skill `java-performance` (N+1, pool Hikari, virtual thread bị pin, cache) vì chưa chốt có làm không.
- Đăng nhập đã chốt trước mắt (username + mật khẩu, Google, Facebook) nhưng chưa có skill cho `identity`; viết khi bắt đầu làm module.
- LLM provider chưa chọn nên `llm-safety-review` chưa có phần riêng của provider (thứ tự field, prompt caching).
- Chưa có mẫu dashboard Grafana (JSON) để `observability` tham chiếu.
- Chưa có workflow cho migration và cho review PR.
