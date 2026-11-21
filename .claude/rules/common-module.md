# Module common

- `common` chỉ chứa code không liên quan nghiệp vụ: hàm util (thời gian, ngẫu nhiên...), mã lỗi và exception chung, filter và cấu hình kỹ thuật (request id, `Clock`, `SecureRandom`), value object kỹ thuật (`IdempotencyKey`).
- Không đặt vào `common`: entity, repository, use case, enum trạng thái nghiệp vụ, policy (3 câu hỏi tiếp, N lượt trải), mã lỗi riêng của nghiệp vụ, và bất kỳ class nào nhắc đến reading, card, followup, quota, topic, spread.
- `common` không import module nghiệp vụ nào. Nếu một class phải biết nghiệp vụ mới giải thích được thì nó thuộc module nghiệp vụ, không thuộc `common`.
