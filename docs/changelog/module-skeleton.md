# Module skeleton

Cập nhật lần cuối: 2026-11-18 · Trạng thái: Đang phát triển

## Tóm tắt

Tách code thành 8 module Spring Modulith (`identity`, `content`, `reading`, `interpretation`, `followup`, `safety`, `privacy`, `audit`) cạnh module `common` (OPEN). Mới có khung package, chưa có nghiệp vụ.

## Thay đổi gì

- Mỗi module có `package-info.java` với `@ApplicationModule(allowedDependencies = {...})`.
- Mỗi module có bốn package nội bộ `web`, `usecase`, `domain`, `infra`, mỗi package một `package-info.java`.
- Phụ thuộc cho phép: `identity`, `content`, `safety` → `audit`, `common`; `reading` → `content`, `audit`, `common`; `interpretation`, `followup` → `reading`, `content`, `audit`, `common`; `privacy` → `reading`, `followup`, `audit`, `common`; `audit` → `common`.
- Chưa có `<Module>Api` (interface công khai) ở package gốc của module nào.

## Vì sao

Chốt ranh giới trước khi viết nghiệp vụ để `ModularityTests` chặn phụ thuộc sai ngay từ đầu. Bản đồ module lấy từ `skills/agent-skills/new-module/references/architecture.md`.

## Ảnh hưởng

- API / contract: Không.
- Cấu hình / biến môi trường / cờ: Không.
- Database: Không.
- Client: Không.
- Tiền, hạn mức, state machine: Không.

## QA cần test

- `ModularityTests` pass: không có vòng phụ thuộc, không module nào vượt `allowedDependencies`.
- Thêm một import trái phép thử trên nhánh riêng và thấy `ModularityTests` đỏ.

## Rollout

Không ảnh hưởng runtime.

## Trạng thái kiểm chứng

Chưa chạy `mvn clean verify`: môi trường làm việc không tải được dependency từ Maven Central. Chỉ kiểm cú pháp bằng `javac`. Cần chạy trên máy dev.

## File chính

- `src/main/java/vn/t3tarot/api/<module>/package-info.java`
- `src/main/java/vn/t3tarot/api/<module>/{web,usecase,domain,infra}/package-info.java`
- `src/test/java/vn/t3tarot/api/ModularityTests.java`

## Liên quan

- `skills/agent-skills/new-module/references/architecture.md`
- [api-error-and-pagination](api-error-and-pagination.md): mã lỗi riêng của module đặt ở `<Module>/domain`.
