---
name: modify-endpoint
description: Workflow sửa endpoint đã có của T3Tarot - xác định endpoint, đánh giá ảnh hưởng, lập checklist để duyệt, sửa nhỏ nhất, test lại theo mức ảnh hưởng. Dùng khi sửa logic, hợp đồng hoặc SQL của một API đang chạy.
---

# Workflow: sửa endpoint

Chi tiết từng bước và bảng mức ảnh hưởng: `.claude/workflows/modify-endpoint/references/workflow.md` (mục "Sửa endpoint"); quy ước Swagger: `.claude/references/swagger.md`. Điều phối `client-compatibility`, `hot-path-sql`, `flyway-migration`, `run-tests`, `java-code-review`, `changelog`.

## Các bước

1. **Xác định endpoint** (chỉ đọc): method + path, controller, DTO, interface công khai, use case, repository và SQL, nơi gọi (website, mobile app và các phiên bản app đang chạy, module khác, event), test hiện có, OpenAPI. Chỗ chưa tìm ra thì ghi rõ, không đoán.
2. **Phân loại và đánh giá ảnh hưởng:** hợp đồng API / logic nghiệp vụ / SQL hoặc schema / hiệu năng / refactor thuần. Kiểm người gọi, tương thích với app đã phát hành (`client-compatibility`; phá vỡ với app cũ luôn là mức Cao), DTO dùng chung, đường ghi nóng, bảo mật (IDOR), state machine, dữ liệu cũ, metric và dashboard, test sẽ vỡ. Kết luận mức **Thấp / Trung / Cao**.
3. **Checklist để duyệt, rồi dừng:** thay đổi và mức ảnh hưởng; file sẽ sửa (kể cả annotation Swagger trên controller và DTO); file và logic **không được đụng**; điểm cần hỏi trước; test sẽ chạy và thêm; kế hoạch lùi. Chỉ làm tiếp khi được duyệt.
4. **Thực thi, giữ thay đổi nhỏ nhất:**
   - Chạy test hiện có trước để lấy mốc; mốc đã đỏ thì dừng và báo, không sửa lỗi không liên quan.
   - Phần sắp sửa chưa có test thì viết test mô tả hành vi hiện tại trước khi sửa.
   - Chỉ sửa file trong checklist; không đổi tên, không format lại, không refactor kèm theo, không thêm dependency.
   - Ưu tiên thêm mới hơn sửa đè (field mới thay vì đổi nghĩa field cũ). Khi app cũ còn dùng: không xoá, đổi tên, đổi nghĩa field; field request mới phải tùy chọn có mặc định.
   - Cần sửa ngoài checklist thì dừng và hỏi.
   - Cần tạo file mới thì đặt chỗ và đặt tên theo `project-structure`, và ghi file đó vào checklist.
5. **Test lại theo mức:**
   - Thấp: test của class đã sửa, `ModularityTests`, coverage ≥ 80% cho class sửa.
   - Trung: toàn bộ test của endpoint và module, test mới cho nhánh mới, `MockMvc` cho controller. Mobile dùng: thêm test request như app cũ gửi (không có field mới) vẫn chạy như trước.
   - Cao: `mvn clean verify` toàn bộ, test đồng thời, IDOR, idempotency retry, dữ liệu cũ, test các nơi gọi, test request như app cũ gửi.
   Dùng `run-tests` (tối đa 3 vòng, không làm xanh giả).
6. **Cập nhật Swagger:** theo `.claude/references/swagger.md`, mục "Khi sửa endpoint": field mới thì thêm `@Schema`, đổi validate thì sửa `@Schema`, thêm hoặc bỏ mã lỗi hay đổi status thì sửa `@ApiResponse`, field bị thay thế thì `deprecated = true` thay vì xoá, đổi client hoặc công khai / cần token thì sửa `@Operation`. Refactor thuần thì kiểm `/v3/api-docs` không đổi. File annotation phải nằm trong checklist ở bước 3.
7. **Changelog:** dùng `changelog`: tìm changelog của feature đang sửa trong `docs/changelog/` và sửa đè cho đúng trạng thái sau thay đổi (không nối thêm lịch sử); cập nhật cả changelog khác nhắc tới phần vừa đổi.
8. **Danh sách endpoint:** sửa đè dòng và mục chi tiết của endpoint trong `docs/api/endpoints.md` cho đúng hành vi sau thay đổi (field, ràng buộc, mã lỗi, quyền, idempotency, client, trạng thái), sửa thống kê nếu đổi quyền, và ngày cập nhật. Theo mục "Quy tắc cập nhật file này" ở cuối file đó. Refactor thuần không đổi hợp đồng thì không sửa. Đổi header, body, path hoặc quyền thì sửa luôn `bruno/<module>/<NN>-<tên>.bru` và `curl/<module>/<NN>-<tên>.curl` cho happy case vẫn chạy (xem README trong hai thư mục đó).
9. **Tổng kết:** diff, test đã chạy và kết quả, coverage, phần Swagger và `docs/api/endpoints.md` đã sửa (Swagger đã kiểm bằng UI hay chưa), mục ảnh hưởng đã xử lý và chưa xử lý, đường dẫn changelog đã sửa, bảng ảnh hưởng theo client kèm phiên bản app tối thiểu, Lưu ý, 2–4 câu hỏi tiếp.

## Điểm dừng chờ người dùng

Sau checklist (luôn dừng); khi cần sửa ngoài checklist; trước migration, đổi contract, phá vỡ tương thích app cũ hoặc công thức tiền; khi mốc test đã đỏ; khi hết 3 vòng; khi test sai cần sửa test.
