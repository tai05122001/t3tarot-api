# Database và Flyway

- Migration: `src/main/resources/db/migration/V{số}__{mô_tả}.sql`, đặt tên chữ thường, gạch dưới. Không sửa migration đã chạy; sửa bằng migration mới.
- `spring.jpa.hibernate.ddl-auto=validate`; schema chỉ do Flyway tạo.
- Enum: `varchar` + `CHECK` (không `CREATE TYPE ... AS ENUM`); map bằng Java enum.
- Khoá chính: `uuid` (UUIDv7 sinh ở app) cho thực thể nghiệp vụ, `bigint identity` cho bảng log, `smallint` cho `card`.
- Thời gian: `timestamptz` cho mốc thời điểm; `date` cho `draw_date`, `week_start` theo `Asia/Ho_Chi_Minh`.
- Không `DELETE` `reading` / `followup`; xoá dữ liệu nhạy cảm bằng cách xoá `reading_content`, `followup_content`.
- `audit_log` append-only: trigger chặn UPDATE/DELETE và `REVOKE UPDATE, DELETE` cho role ứng dụng.
- Migration đầu tiên dựng từ DDL trong ERD (20 bảng), tách theo nhóm. Cần thêm bảng event publication của Spring Modulith vào migration; không để Modulith tự tạo.
- Mọi migration cần hỏi trước khi chạy (xem CLAUDE.md).

