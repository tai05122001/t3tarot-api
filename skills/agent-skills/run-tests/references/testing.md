# Test

- Tổ chức theo suite cho từng feature / luồng.
- Mỗi luồng phủ: happy path, edge case, lỗi hoặc timeout của đối tác (LLM), và đồng thời với đường ghi nóng (xem `skills/agent-skills/hot-path-sql/references/hot-path-sql.md`).
- Test DB dùng Testcontainers Postgres, không dùng H2.
- Endpoint mobile dùng: test request như app cũ gửi (không có field mới) và test retry cùng `Idempotency-Key`; xem `skills/agent-skills/client-compatibility/references/client-platforms.md`.
- `ModularityTests` giữ ranh giới module.
- Mỗi endpoint thêm mới hoặc sửa: bắt buộc line coverage ≥ 80% cho service (use case, facade), controller và utils (class trong `common`) liên quan. Giả định chưa xác nhận: đo bằng JaCoCo, tính từng class riêng. Controller test bằng `MockMvc`; use case test bằng unit test cộng Testcontainers khi có truy vấn.
- Utils trong `common` đạt ≥ 80% bằng unit test thuần, không cần Spring context.
- Test thời gian: dùng `Clock` cố định; bắt buộc có ca 23:59:59 và 00:00:00 giờ VN.
- Báo cáo coverage: `mvn clean verify` rồi mở `target/site/jacoco/index.html` (JaCoCo 0.8.14). Chưa cấu hình ngưỡng chặn build.

## Chạy và sửa test

- "Test xanh" nghĩa là mọi test pass; coverage ≥ 80% là cổng riêng, kiểm sau khi test xanh.
- Mỗi task chạy–sửa tối đa 3 vòng. Hết 3 vòng vẫn đỏ thì dừng, báo test đỏ, nguyên nhân (đánh dấu giả định) và hỏi hướng xử lý.
- Test đỏ vì code sai: sửa code. Test đỏ vì test sai: nói rõ vì sao sai và hỏi trước khi sửa test.
- Không được làm xanh giả: xoá test, `@Disabled`, nới assertion, bắt rộng exception, `-DskipTests`, hạ ngưỡng coverage, loại class khỏi báo cáo JaCoCo.
- Test flaky: tìm nguyên nhân (thường là thời gian, thứ tự, dữ liệu dùng chung), không retry cho qua.

