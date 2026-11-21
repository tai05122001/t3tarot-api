---
name: hot-path-sql
description: Viết hoặc review câu SQL ghi chạm hạn mức (daily, quota tuần, hỏi tiếp), lá bài, chuyển trạng thái, hoàn lượt hoặc lease của T3Tarot, cần chống request đồng thời. Không dùng cho CRUD thường.
---

# Đường ghi nóng (hot path)

Nguồn chuẩn: `.claude/skills/hot-path-sql/references/hot-path-sql.md`. Đọc file đó trước, không chép lại SQL vào đây.

## Các bước

1. Xác định quyết định cần bảo vệ: hạn mức, lá bài, chuyển trạng thái, hoàn lượt hay lease. Đối chiếu với Q1–Q7 trong reference; dùng lại mẫu có sẵn nếu khớp.
2. Viết một câu lệnh duy nhất, điều kiện nằm trong `WHERE`; kết quả đọc từ số dòng cập nhật hoặc `RETURNING`. Không kiểm trước rồi ghi sau.
3. Chuyển trạng thái bằng compare-and-set: `UPDATE ... WHERE status = :cũ`.
4. Luôn kèm `user_id` trong điều kiện; bài của người khác trả 404, không trả 403.
5. Đặt trong class `*HotPathRepository` của module sở hữu bảng, dùng `JdbcClient`. Entity Hibernate không map setter cho cột nóng, dùng `@Column(updatable = false)`.
6. Hạn mức lấy từ policy class hoặc cấu hình, không viết cứng trong SQL.
7. Không giữ transaction qua lời gọi LLM.
8. Viết test đồng thời chạy song song thật trên Postgres (Testcontainers), mỗi request một connection, theo mẫu trong reference. Kịch bản bắt buộc: N request cùng lúc chỉ đúng số lần cho phép thành công; hoàn lượt chỉ một lần; cùng idempotency key không tốn thêm lượt.
9. Chạy test bằng skill `run-tests`.

## Hỏi trước khi làm

- Đổi công thức tiền, hạn mức, hoặc cạnh chuyển trạng thái.
- Cần thêm cột hoặc bảng (migration), ví dụ cột lease.

## Dễ quên

- 0 dòng là kết quả nghiệp vụ hợp lệ ("hết lượt", "đã có lá"), không ném exception chung.
- Giờ và ngày chỉ qua `VietnamClock`; tuần lịch tính từ Thứ 2 00:00 giờ VN (chưa chốt).
- Không log câu hỏi của người dùng.
