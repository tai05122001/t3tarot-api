---
name: flyway-migration
description: Viết migration Flyway mới cho T3Tarot hoặc dựng schema từ ERD (PostgreSQL). Dùng khi thêm hoặc sửa bảng, cột, index, ràng buộc. Luôn hỏi người dùng trước khi tạo file migration.
---

# Migration Flyway

Nguồn chuẩn: `.claude/skills/flyway-migration/references/database.md`, `.claude/skills/hot-path-sql/references/hot-path-sql.md`, và ERD `docs/T3Tarot — Thiết kế dữ liệu_ ERD, UML & bảng PostgreSQL.md` (DDL 20 bảng).

## Luôn hỏi trước

Mọi migration cần người dùng xác nhận. Trình bày trước: thay đổi gì, vì sao, ảnh hưởng dữ liệu đang có, cách lùi. Chỉ viết file sau khi được duyệt. Không chạy migration vào môi trường thật.

## Các bước

1. Đọc ERD để biết bảng và ràng buộc hiện có. Không bịa cột.
2. Đặt tên file `V{số}__{mô_tả}.sql`, chữ thường, gạch dưới. Không sửa migration đã chạy; sửa bằng migration mới.
3. Enum dùng `varchar` + `CHECK`, không `CREATE TYPE ... AS ENUM`. Khoá chính `uuid` cho thực thể nghiệp vụ, `bigint identity` cho bảng log, `smallint` cho `card`.
4. Mốc thời điểm dùng `timestamptz`; `draw_date` và `week_start` dùng `date` theo `Asia/Ho_Chi_Minh`.
5. Thêm ràng buộc unique và index mà đường ghi nóng cần, ví dụ `UNIQUE (user_id, draw_date) WHERE kind = 'DAILY'`.
6. `audit_log` append-only: trigger chặn UPDATE/DELETE và `REVOKE` cho role ứng dụng.
7. Bảng event publication của Spring Modulith phải nằm trong migration; không để Modulith tự tạo.
8. Kiểm bằng Testcontainers Postgres: Flyway chạy sạch trên DB trống, `ddl-auto=validate` vẫn qua. Dùng skill `run-tests`.

## Dễ quên

- Không `DELETE` `reading` / `followup`; dữ liệu nhạy cảm nằm ở bảng `*_content` để xoá được.
- Đổi cột đang có dữ liệu cần phương án chuyển dữ liệu và lùi riêng.
- Crypto-shredding cho câu hỏi và luận giải chưa chốt; hỏi trước khi viết migration có bảng nội dung.
