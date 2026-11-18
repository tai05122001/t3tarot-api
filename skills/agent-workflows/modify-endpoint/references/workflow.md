# Workflow sửa endpoint

Dùng khi sửa một endpoint đã có. Mục tiêu: sửa đúng chỗ cần sửa, không làm hỏng phần đang chạy ổn định, và test lại theo mức ảnh hưởng.

## Bước 1. Xác định endpoint

Lập bản đồ luồng, chỉ đọc, chưa sửa gì:

- Method, path, controller method, request/response DTO, mã lỗi trả ra.
- Interface công khai của module và use case xử lý, repository và câu SQL (đường ghi nóng nếu có).
- Nơi gọi endpoint: website, mobile app (iOS, Android và các phiên bản app đang chạy), module khác (qua interface công khai), event.
- Test hiện có chạm tới endpoint, và mô tả OpenAPI.
- Ghi rõ phần chưa tìm ra; không đoán.

## Bước 2. Phân loại thay đổi và đánh giá ảnh hưởng

Phân loại (một thay đổi có thể thuộc nhiều loại):

| Loại | Ví dụ | Hỏi trước? |
| --- | --- | --- |
| Hợp đồng API | Thêm, bớt, đổi nghĩa field; đổi status code hoặc mã lỗi | Có (đổi API contract) |
| Logic nghiệp vụ | Đổi điều kiện, thứ tự xử lý | Có nếu chạm tiền, hạn mức, state machine |
| SQL hoặc schema | Đổi câu truy vấn, thêm cột | Có nếu cần migration |
| Hiệu năng hoặc kỹ thuật | Cache, index, đổi cách đọc | Không, trừ khi đổi hành vi |
| Refactor thuần | Đổi cấu trúc, không đổi hành vi | Không |

Đánh giá ảnh hưởng, mỗi mục ghi "có / không / chưa rõ":

- Người gọi: website, mobile app, module khác, event consumer; hợp đồng cũ còn chạy được không (tương thích ngược).
- App đã phát hành: người dùng cập nhật app chậm, phiên bản cũ vẫn gọi API. Kiểm theo bảng "được làm / phá vỡ" trong `skills/agent-skills/client-compatibility/references/client-platforms.md`.
- DTO hoặc class dùng chung với endpoint khác.
- Đường ghi nóng: hạn mức, `followup_count`, lease, hoàn lượt; concurrency và idempotency.
- Bảo mật: IDOR (`user_id` trong điều kiện), phân quyền, validate input, rò rỉ dữ liệu trong log và response.
- State machine của reading / followup: có thêm hoặc đổi cạnh chuyển trạng thái không.
- Dữ liệu đang có: bản ghi cũ có hợp lệ với logic mới không.
- Observability: metric, log, dashboard đang dựa vào endpoint này.
- Test hiện có sẽ vỡ, tài liệu và changelog cần cập nhật.

Kết luận mức ảnh hưởng:

| Mức | Khi nào |
| --- | --- |
| Thấp | Không đổi hợp đồng, không đổi hành vi (refactor, đổi tên nội bộ, sửa log) |
| Trung | Đổi logic hoặc hợp đồng theo hướng tương thích ngược, không chạm đường ghi nóng |
| Cao | Đổi hợp đồng phá vỡ (luôn là Cao nếu app đã phát hành dùng endpoint), đổi SQL hoặc schema, chạm đường ghi nóng, tiền, hạn mức hoặc state machine, hoặc còn mục "chưa rõ" quan trọng |

## Bước 3. Checklist để duyệt (dừng chờ xác nhận)

Trình bày một checklist rồi **dừng**, chỉ làm tiếp khi được duyệt:

1. Mô tả thay đổi và loại thay đổi, mức ảnh hưởng và lý do.
2. File sẽ sửa (kèm lý do từng file).
3. File và logic **không được đụng**: phần đang chạy ổn định, nằm ngoài phạm vi.
4. Các điểm cần hỏi trước: contract, migration, công thức tiền, chuyển trạng thái; ghi giả định cần xác minh.
5. Test sẽ chạy lại và test sẽ thêm (xem Bước 5).
6. Kế hoạch lùi nếu có vấn đề (revert diff; migration thì phương án riêng).

## Bước 4. Thực thi, giữ thay đổi nhỏ nhất

- Chạy test hiện có của endpoint và module trước khi sửa để lấy mốc. Mốc đã đỏ thì dừng và báo, không sửa lỗi không liên quan.
- Phần sắp sửa mà chưa có test thì viết test mô tả hành vi hiện tại trước khi sửa (characterization test), để so sánh sau.
- Chỉ sửa các file trong checklist. Không đổi tên, không format lại, không refactor kèm theo, không thêm dependency.
- Ưu tiên thêm mới hơn sửa đè: thêm field mới thay vì đổi nghĩa field cũ, thêm nhánh xử lý thay vì viết lại hàm đang ổn định.
- Khi app cũ còn dùng endpoint: không xoá, đổi tên, đổi nghĩa field; field request mới phải tùy chọn và có mặc định giữ hành vi cũ.
- Phát hiện phải sửa thêm ngoài checklist thì dừng và hỏi, không tự mở rộng phạm vi.
- Giữ quy tắc của `CLAUDE.md`: không comment, không `float`, `VietnamClock`, kèm `user_id`, không log câu hỏi.

## Bước 5. Test lại theo mức ảnh hưởng

| Mức | Phải chạy và phải có |
| --- | --- |
| Thấp | Test của các class đã sửa; `ModularityTests`; coverage ≥ 80% cho các class đã sửa |
| Trung | Toàn bộ test của endpoint và module; test mới cho nhánh mới (happy path, edge case, lỗi validate); `MockMvc` cho controller; coverage ≥ 80% cho service, controller, utils đã sửa |
| Cao | `mvn clean verify` toàn bộ; test đồng thời cho đường ghi nóng; test IDOR và idempotency retry; test với dữ liệu cũ nếu có; test các nơi gọi endpoint (module khác, event); coverage ≥ 80% |

Mobile dùng endpoint (mức Trung và Cao): thêm test request như app cũ gửi vẫn chạy như trước, response giữ đủ field cũ, gửi lại cùng `Idempotency-Key` không tạo bản ghi mới. Xem mục "Test cần có khi mobile dùng endpoint" trong `skills/agent-skills/client-compatibility/references/client-platforms.md`.

Chạy và sửa tối đa 3 vòng, theo quy tắc ở `CLAUDE.md`. Không sửa test cho xanh; xem các điều cấm ở đó.

## Bước 6. Tổng kết

Xuất: diff đã sửa, test đã chạy và kết quả, coverage của các class đã sửa, các mục ảnh hưởng đã xử lý, mục chưa xử lý. Kèm bảng ảnh hưởng theo client (website, iOS, Android, phiên bản app tối thiểu). Kèm changelog theo mẫu: thay đổi gì, vì sao, API / cờ bị ảnh hưởng, QA cần test gì, ghi chú rollout. Cuối cùng là Lưu ý và 2–4 câu hỏi tiếp.

