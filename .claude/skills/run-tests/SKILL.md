---
name: run-tests
description: Chạy test của T3Tarot, sửa lỗi theo vòng tối đa 3 lần, không làm xanh giả, rồi kiểm coverage JaCoCo. Dùng sau khi viết hoặc sửa code, và là bước chạy test của các workflow.
---

# Chạy và sửa test

Nguồn chuẩn: `.claude/rules/testing.md`, `.claude/skills/run-tests/references/testing.md`. Cần `mvn` và Docker đang chạy (Testcontainers). Nếu không chạy được lệnh trên máy hiện tại thì nói rõ và dừng, không giả vờ đã chạy.

## Các bước

1. Chạy phạm vi nhỏ trước: `mvn -Dtest=TênTest test`; sau đó `mvn clean verify` khi cần toàn bộ.
2. Test xanh là mọi test pass. Đỏ thì xác định nguyên nhân thật:
   - Code sai: sửa code.
   - Test sai: nói rõ vì sao sai và **hỏi trước khi sửa test**.
3. Tối đa 3 vòng chạy–sửa cho một task. Hết 3 vòng vẫn đỏ thì dừng, báo test nào đỏ, nguyên nhân (đánh dấu giả định) và hỏi hướng xử lý.
4. Sau khi xanh, mở `target/site/jacoco/index.html` và kiểm line coverage ≥ 80% cho service (use case, facade), controller và utils (`common`) đã thêm hoặc sửa. Giả định: tính từng class riêng.
5. Báo kết quả: số test chạy, test đỏ (nếu có), coverage của các class liên quan.

## Cấm làm xanh giả

Xoá test, `@Disabled`, nới assertion, bắt rộng exception, `-DskipTests`, hạ ngưỡng coverage, loại class khỏi báo cáo JaCoCo. Test chập chờn phải tìm nguyên nhân (thời gian, thứ tự, dữ liệu dùng chung), không retry cho qua.

## Dễ quên

- Test thời gian dùng `Clock` cố định, có ca 23:59:59 và 00:00:00 giờ VN.
- Test DB dùng Testcontainers Postgres, không dùng H2.
- Đường ghi nóng cần test đồng thời thật, xem `hot-path-sql`.
- Endpoint mobile dùng: có test request như app cũ gửi và test retry cùng `Idempotency-Key`, xem `client-compatibility`.
