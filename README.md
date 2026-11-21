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

2. Build image và chạy PostgreSQL (image tự dựng từ `docker/postgres/Dockerfile`):

   ```bash
   docker compose up -d --build
   ```

   Đang có volume `t3tarot-pgdata` từ trước thì `--encoding` và `--data-checksums` không đổi (chỉ áp dụng khi khởi tạo volume mới); cấu hình trong `postgresql.conf` vẫn áp dụng. Muốn khởi tạo lại: `docker compose down -v` (xoá toàn bộ dữ liệu local).

3. Chạy ứng dụng:

   ```bash
   mvn spring-boot:run
   ```

Ứng dụng chạy ở cổng `8080` (đổi bằng `SERVER_PORT`). Health check: `GET /actuator/health` trên cổng `8081` (đổi bằng `MANAGEMENT_PORT`), không phải cổng 8080.

Ứng dụng không khởi động nếu thiếu `JWT_ISSUER`, `JWT_AUDIENCE`, khoá công khai hoặc khoá riêng. Mỗi khoá đặt theo một trong hai cách, không đặt cả hai:

- File PEM (mặc định trong `.env.example`): `JWT_PUBLIC_KEY_LOCATION`, `JWT_PRIVATE_KEY_LOCATION`, giá trị có tiền tố `file:`.
- Chuỗi khoá trong biến môi trường: `JWT_PUBLIC_KEY`, `JWT_PRIVATE_KEY` (PEM hoặc Base64 một dòng).

Tạo cặp khoá cho môi trường dev vào thư mục `secrets/` (đã có trong `.gitignore`, không commit):

```bash
mkdir -p secrets
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/jwt-private.pem
openssl rsa -in secrets/jwt-private.pem -pubout -out secrets/jwt-public.pem
```

Trên Kubernetes, mount secret thành file rồi trỏ `JWT_*_KEY_LOCATION` tới đường dẫn mount, ví dụ `file:/etc/t3tarot/jwt/jwt-private.pem`.

### Biến môi trường

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Máy chủ PostgreSQL |
| `DB_PORT` | `5432` | Cổng PostgreSQL |
| `DB_NAME` | | Tên database |
| `DB_USERNAME` | | Tài khoản database |
| `DB_PASSWORD` | | Mật khẩu database |
| `DB_POOL_SIZE` | `10` | Số connection tối đa của Hikari |
| `DB_IMAGE_VERSION` | `17` | Chỉ cho `docker compose`: phiên bản PostgreSQL của image dev (build arg và tag `t3tarot-postgres:<phiên bản>`); ứng dụng không đọc biến này |
| `DB_TIMEZONE` | `UTC` | Chỉ cho `docker compose`: `timezone` của server PostgreSQL dev |
| `DB_SLOW_QUERY_MS` | `500` | Chỉ cho `docker compose`: ngưỡng ghi log câu lệnh chậm (ms); `0` ghi mọi câu lệnh, `-1` tắt |
| `SERVER_PORT` | `8080` | Cổng HTTP |
| `SWAGGER_ENABLED` | `false` | Bật Swagger UI và OpenAPI (và mở công khai các đường dẫn Swagger); chỉ bật ở local và dev |
| `MANAGEMENT_PORT` | `8081` | Cổng Actuator (health, prometheus); chỉ cho truy cập nội bộ cluster, không public ra ingress |
| `JWT_ISSUER` | | Giá trị claim `iss` mà access token phải có |
| `JWT_AUDIENCE` | | Giá trị claim `aud` mà access token phải có |
| `JWT_PUBLIC_KEY` | rỗng | Khoá công khai RSA (X.509, RS256, tối thiểu 2048 bit) để kiểm chữ ký access token; PEM hoặc Base64 một dòng. Bỏ trống khi dùng `JWT_PUBLIC_KEY_LOCATION` |
| `JWT_PUBLIC_KEY_LOCATION` | rỗng | Đường dẫn file PEM của khoá công khai, ví dụ `file:./secrets/jwt-public.pem` |
| `JWT_PRIVATE_KEY` | rỗng | Khoá riêng RSA (PKCS#8) để ký access token, phải cùng cặp với khoá công khai. Bỏ trống khi dùng `JWT_PRIVATE_KEY_LOCATION` |
| `JWT_PRIVATE_KEY_LOCATION` | rỗng | Đường dẫn file PEM của khoá riêng, ví dụ `file:./secrets/jwt-private.pem` |
| `MAIL_ENABLED` | `false` | `true` thì gửi mail xác minh thật qua SMTP; `false` thì chỉ ghi một dòng log và không gửi gì |
| `MAIL_HOST`, `MAIL_PORT` | rỗng, `587` | Máy chủ SMTP và cổng (STARTTLS bắt buộc). Gmail: `smtp.gmail.com`, `587` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | rỗng | Tài khoản SMTP. Gmail: địa chỉ Gmail và App Password 16 ký tự (không phải mật khẩu đăng nhập) |
| `MAIL_FROM`, `MAIL_FROM_NAME` | rỗng, `T3Tarot` | Địa chỉ và tên người gửi; bắt buộc có `MAIL_FROM` khi `MAIL_ENABLED=true`. Gmail luôn gửi từ chính tài khoản đã đăng nhập |
| `APP_PUBLIC_BASE_URL` | `http://localhost:8080` | Địa chỉ công khai của API, dùng dựng link trong mail (`<địa chỉ>/auth/email/verify-page?token=...`) |
| `APP_WEB_BASE_URL` | `http://localhost:5173` | Địa chỉ website; nút trên trang kết quả xác nhận trỏ về đây |
| `APP_WEB_LOGIN_PATH`, `APP_WEB_RESEND_PATH`, `APP_WEB_HOME_PATH` | `/login`, `/verify-email`, `/` | Đường dẫn trên website cho nút Đăng nhập, Gửi link mới, Vào T3Tarot |
| `CORS_ALLOWED_ORIGINS` | rỗng | Danh sách origin của web, cách nhau dấu phẩy, ví dụ `https://t3tarot.vn`; rỗng là chặn mọi origin; không nhận `*` |

Không commit file `.env`; chỉ `.env.example` được đưa lên git.

### Swagger

Đặt `SWAGGER_ENABLED=true` rồi mở `http://localhost:8080/swagger-ui/index.html`. Trang Swagger mở công khai khi bật cờ này; các endpoint API vẫn cần header `Authorization: Bearer <access token>`.

### Bảo mật

- Mọi đường dẫn mặc định yêu cầu access token JWT (RS256) trong header `Authorization`; không nhận token qua query hoặc form. Endpoint công khai khai báo trong `t3tarot.security.public-paths`.
- Không dùng session và cookie; CSRF tắt vì token đi bằng header. Nếu sau này refresh token đặt trong cookie thì endpoint refresh phải thêm chống CSRF.
- Lỗi 401 và 403 trả `ProblemDetail` với `code` là `error.auth.unauthenticated` hoặc `error.auth.forbidden`.
- Mọi lỗi chỉ trả mã lỗi dạng `error.<nhóm>.<lý do>` (ví dụ `error.invalid_param.is_missing`), không có `message`, `messageCode`, `title` hay `detail`; FE tự map mã ra thông báo.
- Cấp token (đăng nhập, refresh) chưa có; module `identity` sẽ làm. Cách đăng nhập đã chốt trước mắt: username + mật khẩu (email bắt buộc, xác minh email) và social login Google, Facebook. Với social login, client lấy ID token của nhà cung cấp, server xác minh chữ ký, `aud`, `exp` rồi cấp JWT của T3Tarot. OTP email / SMS và Apple tạm hoãn.

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
├── docker/postgres/          Dockerfile và postgresql.conf của image PostgreSQL dev
├── docs/                     tài liệu gốc: tổng quan, đặc tả MVP, feature cốt lõi, ERD
│   └── changelog/            changelog theo feature, sửa đè khi feature đổi
├── .claude/                  cấu hình AI cho Claude Code (agents, commands, skills, workflows, rules, policies...)
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
- Chuẩn đặt tên, từ điển thuật ngữ, quy tắc branch và commit: `.claude/skills/project-structure/references/naming.md`.
- Vị trí file mới: `.claude/skills/project-structure/references/structure.md`.

## Dùng với Claude Code

`CLAUDE.md` chứa quy tắc chung; phần còn lại nằm trong `.claude/` và được commit, Claude Code đọc trực tiếp sau khi clone, không cần sao chép. Bản đồ thư mục và danh sách skill: `.claude/README.md`.

## Giấy phép

MIT, xem [LICENSE](LICENSE).
