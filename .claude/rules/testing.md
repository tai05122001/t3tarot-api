# Test

## Test coverage

- Mỗi endpoint thêm mới hoặc sửa phải có test đi kèm, và line coverage ≥ 80% cho các lớp service (use case, facade), controller và utils (class trong `common`) mà endpoint đó thêm hoặc sửa. Không đạt thì PR chưa được merge.
- Giả định (chưa xác nhận): đo bằng JaCoCo, tính line coverage, mỗi class tính riêng. Chưa cấu hình chặn trong `pom.xml`, hiện kiểm bằng review.
- Coverage 80% là mức tối thiểu, không thay cho các ca bắt buộc ở `.claude/references/conventions.md` (đồng thời, timeout LLM, ranh giới giờ VN).

## Chạy và sửa test

- "Test xanh" nghĩa là mọi test pass. Coverage ≥ 80% là cổng riêng, kiểm sau khi test xanh.
- Mỗi task chạy–sửa tối đa 3 vòng. Hết 3 vòng vẫn đỏ thì dừng, báo test nào đỏ, nguyên nhân (đánh dấu rõ đâu là giả định) và hỏi hướng xử lý.
- Test đỏ vì code sai thì sửa code. Test đỏ vì test sai thì nói rõ vì sao sai và hỏi trước khi sửa test.
- Cấm để test "xanh giả": xoá test, `@Disabled`, nới assertion, bắt rộng exception, `-DskipTests`, hạ ngưỡng coverage, loại class khỏi báo cáo JaCoCo. Test chập chờn (flaky) phải tìm nguyên nhân, không retry cho qua.
