# Swagger (OpenAPI) cho endpoint

Dùng ở bước "Cập nhật Swagger" của workflow `create-endpoint` và `modify-endpoint`. Mục tiêu: đọc Swagger UI là đủ hiểu endpoint mà không cần mở code. Swagger phải khớp code; Swagger sai nghĩa là tài liệu sai, tính như bug.

Stack: `springdoc-openapi-starter-webmvc-ui` (khai báo trong `pom.xml`). Bật bằng `SWAGGER_ENABLED=true`: `/v3/api-docs` và `/swagger-ui/index.html`; mặc định tắt.

## Phải có cho mỗi endpoint

| Chỗ | Annotation | Nội dung |
| --- | --- | --- |
| Controller | `@Tag(name = ...)` | Nhóm theo module hoặc feature, viết thường, dùng lại tag đang có (ví dụ `auth`) thay vì tạo tag mới gần nghĩa |
| Method | `@Operation(summary = ..., description = ...)` | `summary` một câu tiếng Anh nói endpoint làm gì. `description`: client dùng (website, iOS, Android), cần token hay công khai, có idempotent không, hạn mức hoặc giới hạn tần suất nếu có |
| Method | `@ApiResponse` cho từng status code | Status thành công và mọi status lỗi endpoint có thể trả. Lỗi ghi danh sách mã `error.<nhóm>.<lý do>` trong `description` của response đó; schema lỗi trỏ tới body lỗi chung, không tự định nghĩa lại |
| Tham số | `@Parameter` | Path, query, header bắt buộc hoặc có ý nghĩa nghiệp vụ: `Idempotency-Key` (mọi thao tác ghi), `X-Client-Platform`, `X-App-Version` khi endpoint xử lý khác theo nền tảng |
| DTO request / response (`record`) | `@Schema` trên record và từng field | `description`, `example` (giá trị giả), `requiredMode`, `format` (`date`, `date-time`, `uuid`), giới hạn độ dài hoặc khoảng giá trị khớp với Bean Validation; enum ghi rõ các giá trị |
| Endpoint công khai | `@SecurityRequirements` rỗng hoặc ghi chú trong `description` | Phải khớp `t3tarot.security.public-paths`; endpoint còn lại hiển thị là cần bearer JWT |

## Quy tắc

- Không ghi câu chữ thông báo lỗi; chỉ liệt kê mã lỗi (quy tắc 15 trong `CLAUDE.md`).
- `example` dùng dữ liệu giả: không email, tên, số điện thoại, câu hỏi hay luận giải thật; không token thật.
- Không lộ chi tiết nội bộ: tên bảng, câu SQL, tên class, stack trace, prompt LLM.
- Không đưa `userId` thành tham số request; nó lấy từ token (quy tắc 13).
- Field bị thay thế nhưng app cũ còn dùng: giữ field, đánh `@Schema(deprecated = true)` và ghi field thay thế trong `description`; không xoá khỏi Swagger.
- Tiền ghi rõ đơn vị (VND, kiểu `long`) trong `description`.
- Không viết comment Java để giải thích; mọi giải thích cho người đọc đặt trong annotation.

## Khi sửa endpoint

| Thay đổi | Cập nhật Swagger |
| --- | --- |
| Thêm field request hoặc response | `@Schema` cho field mới, ghi mặc định khi field request tùy chọn |
| Đổi validate (độ dài, khoảng, bắt buộc) | Sửa `@Schema` cho khớp |
| Thêm hoặc bỏ mã lỗi, đổi status code | Sửa `@ApiResponse` tương ứng |
| Field thay thế, field cũ còn giữ | `deprecated = true` cho field cũ, trỏ sang field mới |
| Đổi client dùng, đổi công khai / cần token, đổi idempotency | Sửa `description` của `@Operation` |
| Refactor thuần, không đổi hợp đồng | Không sửa; vẫn kiểm `/v3/api-docs` không đổi |

## Kiểm tra

1. `mvn spring-boot:run` với `SWAGGER_ENABLED=true`.
2. Mở `/swagger-ui/index.html`, kiểm endpoint nằm đúng tag, đủ field, đủ status code, ví dụ hợp lý.
3. Hoặc lấy `/v3/api-docs` và so path, method, schema với code. Khi sửa endpoint, so với bản trước khi sửa để thấy đúng phần hợp đồng thay đổi.

Không chạy được (thiếu Docker, thiếu biến môi trường) thì ghi rõ trong tổng kết là chưa kiểm bằng UI, không báo đã kiểm.

Bản đọc nhanh và thống kê mọi endpoint nằm ở `docs/api/endpoints.md`; mỗi lần sửa Swagger thì sửa file đó cho khớp.
