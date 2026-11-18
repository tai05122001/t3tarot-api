# Cấu trúc repo T3Tarot API

Chỗ ghi **(đề xuất)** là quy ước mình đặt để dùng được ngay, chưa được chốt; đổi thì sửa file này.

## Cây thư mục

```
t3tarot-api/
├── CLAUDE.md                     quy tắc chung cho agent
├── pom.xml, lombok.config, docker-compose.yml, .env.example
├── docs/                         chỉ tài liệu gốc của dự án (spec, ERD)
├── skills/
│   ├── README.md                 danh sách skill, cách sao chép sang .claude/skills
│   ├── agent-skills/<tên>/       SKILL.md + references/
│   └── agent-workflows/<tên>/    SKILL.md + references/
└── src/
    ├── main/java/vn/t3tarot/api/
    │   ├── T3TarotApiApplication.java
    │   ├── common/               module OPEN, không nghiệp vụ
    │   │   ├── error/            ErrorCode (interface), CommonErrorCode, ApiException, FieldViolation, GlobalExceptionHandler
    │   │   ├── idempotency/      IdempotencyKey
    │   │   ├── random/           bean SecureRandom
    │   │   ├── time/             VietnamClock, bean Clock
    │   │   └── web/              RequestIdFilter, PageQuery, PageResponse
    │   └── <module>/             identity, content, reading, interpretation, followup, safety, privacy, audit
    │       ├── <Module>Api.java  interface công khai, record trả ra ngoài, event
    │       ├── web/
    │       ├── usecase/
    │       ├── domain/
    │       └── infra/
    ├── main/resources/
    │   ├── application.yml       một file cấu hình, giá trị lấy từ biến môi trường
    │   └── db/migration/         V{n}__{mo_ta}.sql
    └── test/java/vn/t3tarot/api/ cùng package với class được test
        ├── ModularityTests.java, T3TarotApiApplicationTests.java
        └── TestcontainersConfiguration.java
```

## Loại file và vị trí

| Loại file | Vị trí |
| --- | --- |
| Class Java chạy thật | `src/main/java/vn/t3tarot/api/<module>/...` hoặc `.../common/<nhóm>/` |
| Test | `src/test/java/` cùng package với class được test |
| Cấu hình dùng chung cho test (Testcontainers) | `src/test/java/vn/t3tarot/api/` |
| Migration | `src/main/resources/db/migration/V{n}__{mo_ta}.sql`, `n` là số kế tiếp |
| Cấu hình ứng dụng | `src/main/resources/application.yml` (thêm khoá, không tạo file mới) |
| Biến môi trường mới | thêm vào `.env.example` với giá trị giả |
| Tài liệu gốc (spec, ERD) | `docs/` |
| Quy tắc, SQL mẫu, quy trình | `skills/<loại>/<tên>/references/` của skill sở hữu |
| Skill mới | `skills/agent-skills/<tên>/` hoặc `skills/agent-workflows/<tên>/`; tên không chứa tên dự án |

## Cây quyết định cho class trong module

1. Module khác hoặc controller cần gọi nó? → **package gốc**: interface `<Module>Api`, record DTO trả ra, domain event.
2. Nhận HTTP request, SSE, hoặc là DTO của HTTP? → **`web`**.
3. Thực hiện một nghiệp vụ, mở transaction, hoặc cài đặt `<Module>Api`? → **`usecase`**.
4. Entity, enum trạng thái, policy, value object nghiệp vụ, mã lỗi riêng của module? → **`domain`**.
5. Nói chuyện với DB, LLM, dịch vụ ngoài? → **`infra`**.

Không khớp nhánh nào thì dừng và hỏi.

## Đặt tên

Toàn bộ chuẩn đặt tên (class, method, biến, test, bảng, migration, API, metric, branch, commit) và từ điển thuật ngữ nằm ở `naming.md` cùng thư mục.

## Phạm vi truy cập

- Class dùng chéo giữa các package con trong cùng module (entity, repository) để `public`; Spring Modulith vẫn chặn module khác dùng chúng.
- Use case, facade, class chỉ dùng trong một package: package-private.
- Module khác chỉ được import package gốc của module này.

## Test

- Mọi test, kể cả test chạy Testcontainers, đặt tên `*Test` (hoặc `*Tests` cho test cấp ứng dụng có sẵn). Surefire mặc định không chạy `*IT`, và pom chưa có failsafe.
- Test đồng thời cho đường ghi nóng đặt cùng package với `*HotPathRepository`.

## Còn mở

- Các mục ghi "(đề xuất)" trong `naming.md`.
