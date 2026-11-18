# T3Tarot API

Backend của **T3Tarot**: website và mobile app xem tarot bằng LLM cho người Việt, lõi là "mỗi ngày một lá" (daily card).

Repo đang ở giai đoạn khung: đã có cấu hình, code dùng chung (`common`), test nền tảng, tài liệu dự án và bộ skill cho agent. Chưa có module nghiệp vụ.

## Nguyên tắc cốt lõi

- Server bốc bài bằng `SecureRandom` và lưu trước khi lật; LLM không chọn lá và không tự bịa nghĩa lá, chỉ diễn giải từ Card KB.
- Mỗi người một lá mỗi ngày, tính theo giờ `Asia/Ho_Chi_Minh`.
- Hạn mức (lượt trải bài, câu hỏi tiếp) kiểm và trừ trong một câu SQL để chống request đồng thời.
- Output của LLM là JSON có cấu trúc, được kiểm khớp với lá đã bốc trước khi trả cho người dùng.
- Câu hỏi của người dùng chỉ là dữ liệu, không phải chỉ dẫn; không ghi câu hỏi hay luận giải ra log.

## Công nghệ

| Thành phần | Lựa chọn |
| --- | --- |
| Ngôn ngữ, framework | Java 21, Spring Boot 4.1, Maven |
| Kiến trúc | Modular monolith với Spring Modulith |
| Cơ sở dữ liệu | PostgreSQL 17, Flyway |
| Truy cập dữ liệu | Hibernate cho CRUD, `JdbcClient` cho đường ghi nóng |
| Tài liệu API | springdoc-openapi (Swagger UI) |
| Test | JUnit 5, Testcontainers, JaCoCo |
| Quan sát | Micrometer, Prometheus, Loki, Grafana |
| Khác | Lombok, Spring Security, virtual threads |

## Yêu cầu

- JDK 21
- Maven 3.9 trở lên
- Docker (cho PostgreSQL local và Testcontainers)

## Chạy local

1. Tạo file biến môi trường từ mẫu, rồi sửa mật khẩu:

   ```bash
   cp .env.example .env        # Windows: copy .env.example .env
   ```

2. Chạy PostgreSQL:

   ```bash
   docker compose up -d
   ```

3. Chạy ứng dụng:

   ```bash
   mvn spring-boot:run
   ```

Ứng dụng chạy ở cổng `8080` (đổi bằng `SERVER_PORT`). Health check: `GET /actuator/health`.

### Biến môi trường

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Máy chủ PostgreSQL |
| `DB_PORT` | `5432` | Cổng PostgreSQL |
| `DB_NAME` | | Tên database |
| `DB_USERNAME` | | Tài khoản database |
| `DB_PASSWORD` | | Mật khẩu database |
| `DB_POOL_SIZE` | `10` | Số connection tối đa của Hikari |
| `SERVER_PORT` | `8080` | Cổng HTTP |
| `SWAGGER_ENABLED` | `false` | Bật Swagger UI và OpenAPI; chỉ bật ở local và dev |

Không commit file `.env`; chỉ `.env.example` được đưa lên git.

### Swagger

Đặt `SWAGGER_ENABLED=true` rồi mở `http://localhost:8080/swagger-ui/index.html`. Hiện Spring Security vẫn chặn mọi endpoint vì chưa có cấu hình bảo mật (phương án đăng nhập chưa chốt), nên Swagger UI sẽ trả 401 cho tới khi thêm `SecurityFilterChain`.

## Test và coverage

```bash
mvn clean verify
```

- Cần Docker đang chạy: test dùng Testcontainers với PostgreSQL thật, không dùng H2.
- Báo cáo coverage: `target/site/jacoco/index.html`.
- Chuẩn coverage: mỗi endpoint thêm hoặc sửa phải đạt line coverage từ 80% cho service, controller và utils liên quan (hiện kiểm bằng review, chưa chặn tự động trong build).
- `ModularityTests` kiểm ranh giới giữa các module.

## Cấu trúc repo

```
t3tarot-api/
├── CLAUDE.md                 quy tắc cho agent (Claude Code)
├── docs/                     tài liệu gốc: tổng quan, đặc tả MVP, feature cốt lõi, ERD
├── skills/
│   ├── README.md             danh sách skill và cách cài
│   ├── agent-skills/         skill làm một việc (SQL đường nóng, migration, review...)
│   └── agent-workflows/      workflow nhiều bước (tạo endpoint, sửa endpoint)
└── src/
    ├── main/java/vn/t3tarot/api/
    │   ├── common/           code dùng chung, không chứa nghiệp vụ
    │   └── <module>/         package gốc công khai + web, usecase, domain, infra
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/     migration Flyway
    └── test/java/            test cùng package với class được test
```

Module dự kiến: `identity`, `content`, `reading`, `interpretation`, `followup`, `safety`, `privacy`, `audit`.

## Quy ước

- Không viết comment trong code; tên và cấu trúc phải tự giải thích. Code phải sạch SonarQube.
- Tiền dùng `long` (VND) hoặc `BigDecimal`, không dùng `float`/`double`.
- Ngày giờ chỉ lấy qua `VietnamClock`.
- Chuẩn đặt tên, từ điển thuật ngữ, quy tắc branch và commit: `skills/agent-skills/project-structure/references/naming.md`.
- Vị trí file mới: `skills/agent-skills/project-structure/references/structure.md`.

## Dùng với Claude Code

`CLAUDE.md` chứa quy tắc chung; quy tắc chi tiết nằm trong `references/` của từng skill. Claude Code đọc skill ở `.claude/skills/`, nên sau khi clone cần sao chép (thư mục này không được commit):

```bash
# macOS, Linux
mkdir -p .claude/skills && cp -r skills/agent-skills/* skills/agent-workflows/* .claude/skills/

# Windows
xcopy skills\agent-skills .claude\skills /E /I /Y
xcopy skills\agent-workflows .claude\skills /E /I /Y
```

Danh sách skill: `skills/README.md`.

## Giấy phép

MIT, xem [LICENSE](LICENSE).
