---
name: new-module
description: Thêm một module Spring Modulith mới cho T3Tarot theo kiểu Facade + Use case (package gốc công khai, web, usecase, domain, infra). Dùng khi cần module nghiệp vụ mới hoặc chia lại ranh giới module.
---

# Thêm module mới

Nguồn chuẩn: `skills/agent-skills/new-module/references/architecture.md` (mục Module dự kiến và Quy ước cấu trúc), `skills/agent-skills/java-code-review/references/conventions.md`.

## Các bước

1. Xác nhận module có trách nhiệm riêng và danh sách bảng sở hữu; đối chiếu bảng module trong `skills/agent-skills/new-module/references/architecture.md`. Nếu trùng trách nhiệm với module có sẵn thì dừng và hỏi.
2. Khai báo hướng phụ thuộc với các module khác, không được tạo vòng. `common` không được phụ thuộc module này.
3. Tạo package `vn.t3tarot.api.<module>` (tên class và vị trí theo `project-structure`):
   - Package gốc: interface công khai (ví dụ `ReadingApi`), record DTO trả ra ngoài, domain event. Chỉ những thứ module khác được phép dùng.
   - `web`: controller, DTO request/response.
   - `usecase`: mỗi nghiệp vụ một class package-private; `@Transactional` đặt ở đây.
   - `domain`: entity, enum trạng thái, policy.
   - `infra`: repository, `*HotPathRepository`, client bên ngoài.
4. Không viết interface + Impl 1-1. Chỉ tạo interface khi có nhiều bản thực thi hoặc cần chặn phụ thuộc.
5. Không dùng MapStruct; DTO là `record` với `static from(...)`.
6. Module khác giao tiếp qua interface công khai hoặc event, không đụng repository hay bảng của nhau.
7. Chạy `ModularityTests`; thêm test cho use case đầu tiên. Dùng skill `run-tests`.

## Hỏi trước khi làm

- Thêm bảng hoặc cột (migration): dùng `flyway-migration`.
- Đổi API contract.

## Dễ quên

- Controller không được gọi repository trực tiếp.
- Không đặt nghiệp vụ vào `common`.
- Không log câu hỏi hoặc luận giải.
