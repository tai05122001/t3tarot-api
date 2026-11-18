# API error, phân trang và Idempotency-Key

Cập nhật lần cuối: 2026-11-18 · Trạng thái: Đang phát triển

## Tóm tắt

Quy ước chung cho mọi endpoint: thành công trả thẳng body (không envelope); lỗi trả `ProblemDetail` (RFC 9457) chỉ có mã lỗi `error.<nhóm>.<lý do>` (không `message`, `messageCode`, `title`, `detail`) để FE tự parse; danh sách phân trang theo offset; header `Idempotency-Key` là UUID.

## Thay đổi gì

- `common/error`:
  - `ErrorCode` (interface: `code()`, `httpStatus()`); mỗi module có `<Module>ErrorCode` riêng trong `domain`, `common` chỉ có `CommonErrorCode`.
  - `CommonErrorCode` (mã dạng `error.<nhóm>.<lý do>`, đúng 3 đoạn, snake_case):

    | Hằng | Mã | HTTP |
    | --- | --- | --- |
    | `INVALID_REQUEST` | `error.invalid_request.malformed` | 400 |
    | `INVALID_PARAM` | `error.invalid_param.is_invalid` | 400 |
    | `IDEMPOTENCY_KEY_MISSING` | `error.idempotency_key.is_missing` | 400 |
    | `IDEMPOTENCY_KEY_INVALID` | `error.idempotency_key.is_invalid` | 400 |
    | `IDEMPOTENCY_KEY_REUSED` | `error.idempotency_key.is_reused` | 422 |
    | `IDEMPOTENCY_KEY_IN_PROGRESS` | `error.idempotency_key.in_progress` | 409 |
    | `UNAUTHENTICATED` | `error.auth.unauthenticated` | 401 |
    | `FORBIDDEN` | `error.auth.forbidden` | 403 |
    | `NOT_FOUND` | `error.resource.not_found` | 404 |
    | `METHOD_NOT_ALLOWED` | `error.http.method_not_allowed` | 405 |
    | `UNSUPPORTED_MEDIA_TYPE` | `error.http.unsupported_media_type` | 415 |
    | `CONFLICT` | `error.resource.conflict` | 409 |
    | `RATE_LIMITED` | `error.rate_limit.exceeded` | 429 |
    | `INTERNAL` | `error.server.internal` | 500 |
    | `SERVICE_UNAVAILABLE` | `error.server.unavailable` | 503 |
    | `UPSTREAM_TIMEOUT` | `error.server.upstream_timeout` | 504 |

    `fromStatus(HttpStatusCode)` đổi status Spring sang mã (400 chung là `INVALID_REQUEST`).
  - `ErrorCodeFormat.isValid(String)`: kiểm một mã đúng định dạng; mọi `<Module>ErrorCode` phải có test dùng hàm này.
  - `ViolationCodes.of(constraint)`: đổi tên constraint của Bean Validation sang mã `error.invalid_param.*`: `NotNull`, `NotBlank`, `NotEmpty` → `is_missing`; `Size`, `Length` → `length_out_of_range`; `Min`, `Max`, `DecimalMin`, `DecimalMax`, `Range`, `Positive*`, `Negative*` → `out_of_range`; `Pattern`, `Email` → `invalid_format`; `typeMismatch` → `invalid_type`; còn lại → `is_invalid`.
  - `ApiException(ErrorCode[, debugMessage[, cause]])`; `FieldViolation(field, code)`.
  - `GlobalExceptionHandler` xử lý `ApiException`, `ConstraintViolationException`, validate body (`INVALID_PARAM` kèm `errors`), thiếu query param (`field` là tên param, `is_missing`), sai kiểu param (`invalid_type`), vi phạm constraint ở tham số controller (`HandlerMethodValidationException`: `field` là tên `@RequestParam` / `@PathVariable` / `@RequestHeader`, nếu không có thì tên tham số Java; lỗi trong bean `@Valid` dùng tên field của bean), `AuthenticationException` (401), `AccessDeniedException` (403) và lỗi không bắt được (500).
- Body lỗi: `ProblemDetail` chỉ có `status`, `code`, `requestId` (lấy từ MDC khoá `requestId`; bỏ qua nếu rỗng) và `errors: [{field, code}]` khi lỗi tham số; thêm `type` và `instance` mặc định của Spring nếu có. `title` bị đặt rỗng và `detail` bị xoá nên không có mặt; không có `message`, `messageCode`; không echo giá trị bị từ chối. Lý do: trước đây `title` do Spring tự điền bằng câu tiếng Anh theo status ("Bad Request").
- `common/web`: `PageQuery` (`page` từ 0, mặc định 0; `size` mặc định 20, tối đa 50), `PageResponse<T>` (`items`, `page`, `size`, `totalItems`, `totalPages`, `hasNext`). Thứ tự sắp xếp do server quyết định.
- `common/idempotency/IdempotencyKey`: record bọc `UUID`, `HEADER_NAME = "Idempotency-Key"`, `parse(String)` ném `ApiException` với `IDEMPOTENCY_KEY_MISSING` hoặc `IDEMPOTENCY_KEY_INVALID`.

## Vì sao

- FE tự parse `code` thành thông báo nên server không trả bất kỳ câu chữ nào (`message`, `messageCode`, `title`, `detail`), cũng để tránh lộ dữ liệu. Quy ước chốt ngày 2026-11-18, ví dụ `error.invalid_param.is_missing`.
- Giả định: `error.invalid_param.is_invalid` là mã cấp trên cho mọi lỗi tham số; chi tiết từng tham số nằm ở `errors[].code`. Bảng ánh xạ constraint → lý do và tên nhóm cho các lỗi chung do tôi đề xuất, chưa được chốt với FE.
- Offset đủ cho lịch sử xem bài ở quy mô MVP; Giả định: chưa cần keyset pagination.

## Ảnh hưởng

- API / contract: đổi toàn bộ mã lỗi chung sang định dạng 3 đoạn (chưa có endpoint hay app dùng nên không phá tương thích); `errors[].reason` đổi thành `errors[].code`; định hình mọi response lỗi và response danh sách của endpoint sau này. Chưa có endpoint nghiệp vụ nào dùng. Mã lỗi (`error.*`) là một phần contract với client: đổi tên sau khi app phát hành là phá tương thích.
- Cấu hình / biến môi trường / cờ: Không.
- Database: Không.
- Client: website và app map lỗi theo `code`; gửi `Idempotency-Key` dạng UUID cho mọi thao tác ghi.
- Tiền, hạn mức, state machine: Không. Việc kiểm trùng `Idempotency-Key` (REUSED, IN_PROGRESS) chưa có; mới có mã lỗi và bước parse.

## QA cần test

- Body thiếu field hoặc vi phạm `@Valid`: trả `error.invalid_param.is_invalid` kèm `errors[].field` và `errors[].code` (ví dụ `error.invalid_param.is_missing`).
- Body JSON hỏng: `error.invalid_request.malformed`.
- Thiếu hoặc sai kiểu query param: `errors[0].field` là tên param, `is_missing` hoặc `invalid_type`.
- Tham số controller vi phạm constraint (`@Size`, `@Min`... trên `@RequestParam`, `@PathVariable`, `@RequestHeader`): `errors[].field` là tên trên HTTP, `errors[].code` theo `ViolationCodes`; nhiều tham số sai thì liệt kê đủ.
- Controller vừa có tham số có constraint vừa có `@Valid @RequestBody`: body sai vẫn trả `errors` theo field của body.
- Mọi response lỗi không có `message`, `messageCode`, `title`, `detail`, và mọi `code` qua `ErrorCodeFormat.isValid`.
- `Idempotency-Key` thiếu, không phải UUID, hoa thường, có khoảng trắng.
- `page` âm, `size` bằng 0, `size` lớn hơn 50.
- Lỗi 500 không lộ stack trace hay giá trị đầu vào; `requestId` có trong body khi request đi qua filter request id.
- Response lỗi không chứa nội dung câu hỏi hoặc luận giải.

## Rollout

Không ảnh hưởng runtime cho tới khi có endpoint đầu tiên.

## Trạng thái kiểm chứng

Chưa chạy `mvn clean verify` (không tải được dependency trong môi trường làm việc); mới kiểm cú pháp bằng `javac`. Test đã viết: `CommonErrorCodeTest`, `ErrorCodeFormatTest`, `ViolationCodesTest`, `ApiExceptionTest`, `GlobalExceptionHandlerTest`, `PageQueryTest`, `PageResponseTest`, `IdempotencyKeyTest`.

Rủi ro cần kiểm trên máy dev:

- `ErrorCode`, `ApiException`, `GlobalExceptionHandler` được ghi đè khi chưa đọc được bản cũ; code cũ còn dùng hằng số cũ sẽ lỗi biên dịch. Đã gặp `INVALID_IDEMPOTENCY_KEY`, nay là `IDEMPOTENCY_KEY_INVALID`; `grep` thử các tên cũ còn sót.
- Khoá MDC `requestId` phải trùng với khoá `RequestIdFilter` đang ghi.
- Mã cũ (`error.invalid.request`, `error.not.found`, `error.upstream.timeout`...) đã đổi; `grep` toàn repo xem còn nơi nào dùng chuỗi cũ.
- Chưa xác minh: `title` rỗng bị Jackson bỏ qua (test `ResponseNeverCarriesMessageFields` sẽ báo nếu không), chữ ký `handleMissingServletRequestParameter` và `handleTypeMismatch` trong Spring Framework 7.
- Chưa xác minh: `handleHandlerMethodValidationException`, `getValueResults()` và `getBeanResults()` còn nguyên trong Spring Framework 7; test dùng MockMvc standalone và giả định method validation của MVC tự bật ở đó (nếu test báo sai loại exception thì cần bật validator thủ công).
- Chưa xử lý: lỗi validate giá trị trả về (`ConstraintViolationException` từ AOP) và cross-parameter constraint.

## File chính

- `src/main/java/vn/t3tarot/api/common/error/`
- `src/main/java/vn/t3tarot/api/common/web/{PageQuery,PageResponse}.java`
- `src/main/java/vn/t3tarot/api/common/idempotency/IdempotencyKey.java`

## Liên quan

- [security-access-control](security-access-control.md): 401 và 403 sinh trong filter được chuyển về `GlobalExceptionHandler`.
- [module-skeleton](module-skeleton.md)
