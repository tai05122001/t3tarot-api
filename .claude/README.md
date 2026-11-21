# .claude

Toàn bộ cấu hình AI của repo. `CLAUDE.md` ở gốc repo là bộ não (luôn được đọc); thư mục này chứa phần còn lại và được commit, Claude Code đọc trực tiếp sau khi clone. Tài liệu gốc của dự án (spec, ERD) nằm ở `docs/`, changelog theo feature ở `docs/changelog/`.

| Thư mục | Vai trò | Claude Code nạp thế nào |
| --- | --- | --- |
| `agents/` | Agent theo vai trò (người trong team) | Subagent; gọi bằng `@<tên>` hoặc để Claude tự giao việc |
| `commands/` | Lệnh người dùng gõ `/<tên>` | Slash command |
| `skills/` | Tay nghề: các bước, quy tắc, "hỏi trước khi làm" của một việc | Tự nạp khi khớp `description`, hoặc gọi `/<tên>` |
| `workflows/` | Dây chuyền nhiều bước, gọi nhiều skill, có điểm dừng | Không tự nạp; chạy qua command cùng tên |
| `rules/` | Luật luôn áp dụng | Tự nạp mọi file khi mở session |
| `policies/` | Phanh: việc phải hỏi trước | Import từ `CLAUDE.md` bằng `@` |
| `references/` | Tài liệu dùng chung từ 2 skill trở lên | Skill trỏ tới khi cần |
| `output-styles/` | Cách trả bài | Bật bằng `/output-style` |
| `hooks/` | Script automation | Chưa có hook; khi thêm thì khai báo trong `settings.json` |

Không commit `settings.local.json` (cấu hình cá nhân).

## Agent

| Agent | Việc |
| --- | --- |
| `software-engineer` | Viết, sửa code nghiệp vụ, endpoint, SQL |
| `qa-test-engineer` | Viết, chạy test, coverage, tìm ca thiếu |
| `devops-engineer` | Docker dev, build Maven, cấu hình, biến môi trường |
| `sre-ops-engineer` | Metric, log, dashboard, PromQL / LogQL |
| `project-manager` | Chia việc, scope, theo dõi việc còn mở; không code |

Không có agent UI/UX vì UI nằm ở repo khác.

## Skill

Mỗi skill gồm `SKILL.md` (phần đầu `name` và `description`; thân là các bước, quy tắc bắt buộc, danh sách "hỏi trước khi làm") và thư mục `references/` chứa quy tắc chi tiết. Claude Code chỉ đọc `skills/<tên>/SKILL.md` một cấp, không chia thư mục con.

| Skill | Dùng khi | Nguồn |
| --- | --- | --- |
| `hot-path-sql` | Viết hoặc review câu ghi chạm hạn mức, lá bài, trạng thái, hoàn lượt, lease | `.claude/skills/hot-path-sql/references/hot-path-sql.md` |
| `new-module` | Thêm module Modulith mới theo Facade + Use case | `.claude/skills/new-module/references/architecture.md`, `.claude/references/conventions.md` |
| `flyway-migration` | Viết migration mới hoặc dựng schema từ ERD; luôn hỏi trước | `.claude/references/conventions.md`, ERD |
| `llm-safety-review` | Viết hoặc review prompt, validator, filter khủng hoảng | `.claude/skills/llm-safety-review/references/llm-safety.md` |
| `java-code-review` | Review code hoặc PR theo thứ tự ưu tiên của dự án | `CLAUDE.md`, `.claude/references/conventions.md` |
| `observability` | Thêm metric, dựng dashboard Grafana, truy vấn PromQL / LogQL | `.claude/references/conventions.md`, `.claude/skills/llm-safety-review/references/llm-safety.md` |
| `run-tests` | Chạy test, sửa tối đa 3 vòng, không làm xanh giả, kiểm coverage JaCoCo | `.claude/rules/testing.md`, `.claude/references/conventions.md` |
| `client-compatibility` | Kiểm endpoint chạy đúng cho website và mobile app, không phá app phiên bản cũ | `.claude/skills/client-compatibility/references/client-platforms.md` |
| `project-structure` | Quyết định file mới đặt ở thư mục, package nào và đặt tên ra sao | `.claude/skills/project-structure/references/structure.md`, `.claude/skills/project-structure/references/naming.md` |
| `changelog` | Sau mỗi task có sửa code: ghi hoặc sửa đè `docs/changelog/<feature>.md` cho đúng trạng thái hiện hành | `.claude/skills/changelog/references/changelog.md` |

## Workflow

Mỗi workflow là `workflows/<tên>/WORKFLOW.md` (kèm `references/` nếu cần) và một command `commands/<tên>.md` để gọi.

| Workflow | Command | Luồng | Gọi các skill | Điểm dừng chờ xác nhận |
| --- | --- | --- | --- | --- |
| `create-endpoint` | `/create-endpoint` | Xác định client (website, app, cả hai) → góp ý yêu cầu → scope và giả định → DTO và OpenAPI → controller → interface công khai → use case → SQL → test → chạy test → tự review → changelog → Session Feature Summary | `new-module`, `client-compatibility`, `hot-path-sql`, `flyway-migration`, `run-tests`, `java-code-review`, `changelog` | Khi chưa rõ client; sau bước scope; trước migration; trước khi thêm dependency; khi hết 3 vòng vẫn đỏ; khi test sai cần sửa test |
| `modify-endpoint` | `/modify-endpoint` | Xác định endpoint và client đang gọi → phân loại và đánh giá ảnh hưởng → checklist để duyệt → sửa nhỏ nhất, không đụng logic ổn định → test lại theo mức Thấp / Trung / Cao → changelog (sửa đè) → tổng kết | `client-compatibility`, `hot-path-sql`, `flyway-migration`, `run-tests`, `java-code-review`, `changelog` | Sau checklist (luôn dừng); khi cần sửa ngoài checklist; trước migration, đổi contract, phá vỡ app cũ hoặc công thức tiền; khi mốc test đã đỏ; khi hết 3 vòng; khi test sai cần sửa test |

Chi tiết từng bước: `.claude/workflows/modify-endpoint/references/workflow.md`. Điều kiện chạy: cần `mvn` và Docker trên máy chạy. Quy tắc chạy và sửa test: `.claude/rules/testing.md`.

## Quy ước viết skill

- Tên skill không mang tên dự án.
- `description` nêu rõ khi nào dùng, tối đa vài câu, bằng tiếng Việt kèm thuật ngữ tiếng Anh.
- Thân skill ngắn: các bước, quy tắc bắt buộc, danh sách "hỏi trước khi làm", đường dẫn tới `references/`.
- SQL mẫu và quy tắc dài đặt trong `references/` của skill sở hữu; skill khác trỏ tới đó, không chép lại. Tài liệu mà từ 2 skill trở lên cùng dựa vào đặt ở `.claude/references/`.
- Skill phải nhắc các bước dễ quên: kèm `user_id`, idempotency key, `VietnamClock`, không log PII, hỏi trước khi chạm công thức tiền, migration hay API contract.
- Workflow gọi agent skill theo tên, không lặp lại nội dung của chúng.

Công cụ khác (Codex, Copilot, Junie...) chưa được kiểm tra có đọc định dạng và thư mục này không; cần xác minh riêng trước khi dựa vào.

## Còn thiếu

- Chưa có skill `java-performance` (N+1, pool Hikari, virtual thread bị pin, cache) vì chưa chốt có làm không.
- Đăng nhập đã chốt trước mắt (username + mật khẩu, Google, Facebook) nhưng chưa có skill cho `identity`; viết khi bắt đầu làm module.
- LLM provider chưa chọn nên `llm-safety-review` chưa có phần riêng của provider (thứ tự field, prompt caching).
- Chưa có mẫu dashboard Grafana (JSON) để `observability` tham chiếu.
- Chưa có workflow cho migration và cho review PR.
- Chưa có hook nào; `policies/` hiện chỉ là chỉ dẫn, chưa chặn cứng bằng `permissions.deny` hay hook.
