---
name: create-endpoint
description: Workflow tạo endpoint mới cho T3Tarot từ mô tả, qua controller, interface công khai, use case, SQL và test, chạy test đến khi xanh. Dùng khi người dùng yêu cầu thêm API mới.
---

# Workflow: tạo endpoint

Chi tiết: `skills/agent-workflows/modify-endpoint/references/workflow.md` và `skills/README.md`. Workflow này điều phối các skill `project-structure`, `new-module`, `client-compatibility`, `hot-path-sql`, `flyway-migration`, `run-tests`, `java-code-review`.

## Các bước

1. **Xác định client:** endpoint dành cho website, mobile app (iOS, Android) hay cả hai. Chưa rõ thì hỏi, không đoán.
2. **Góp ý yêu cầu trước khi làm:** chỉ ra rủi ro (IDOR, idempotency, quota, tiền, race, rò rỉ dữ liệu, prompt injection). Việc nào mơ hồ về nghiệp vụ thì liệt kê các cách hiểu kèm hệ quả và hỏi, không tự chọn.
3. **Scope và giả định:** nêu rõ phần làm, phần không làm, giả định cần xác minh. **Dừng chờ xác nhận** (endpoint mới là thay đổi API contract).
4. **Hợp đồng:** DTO là `record`, annotation OpenAPI, mã lỗi; mã lỗi nghiệp vụ thuộc module, không đặt vào `common`. Một hợp đồng chung cho mọi client. Kiểm bằng `client-compatibility`: idempotency cho app retry, phân trang, ngày giờ ISO-8601, ảnh dùng URL; nếu mobile dùng thì thiết kế để sau này chỉ cần thêm field, không phải đổi field.
5. **Code theo Facade + Use case** (mọi file mới đặt chỗ và đặt tên theo `project-structure`): controller (`web`) → interface công khai ở package gốc → use case (`@Transactional` ở đây) → repository. Thao tác ghi chạm hạn mức, trạng thái, hoàn lượt dùng `hot-path-sql`.
6. **Migration nếu cần:** dừng và hỏi trước (`flyway-migration`). Thêm dependency cũng phải hỏi trước.
7. **Test:** `MockMvc` cho controller, unit test cho use case, Testcontainers cho SQL, test đồng thời nếu là đường nóng, test IDOR và validate, test timeout LLM nếu có gọi LLM. Mobile dùng: test gửi lại cùng `Idempotency-Key`; xử lý khác theo nền tảng: test với header từng nền tảng.
8. **Chạy test:** dùng `run-tests` (tối đa 3 vòng, không làm xanh giả, coverage ≥ 80%).
9. **Tự review** bằng `java-code-review` cho phần đã viết.
10. **Tổng kết:** Session Feature Summary (tính năng, file, thay đổi chính, cần test gì), bảng ảnh hưởng theo client (website, iOS, Android: việc phía client, phiên bản app tối thiểu), Lưu ý, 2–4 câu hỏi tiếp.

## Điểm dừng chờ người dùng

Khi chưa rõ client; sau bước scope; trước migration hoặc đổi contract hoặc công thức tiền; trước khi thêm dependency; khi hết 3 vòng vẫn đỏ; khi test sai cần sửa test.

## Quy tắc code

Không comment trong code; Sonar sạch; không `float`/`double` cho tiền; `VietnamClock`; không log câu hỏi. Chỉ hiển thị phần code thay đổi khi báo cáo.
