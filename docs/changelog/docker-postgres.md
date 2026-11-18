# Docker: image PostgreSQL cho dev

Cập nhật lần cuối: 2026-11-18 · Trạng thái: Đang phát triển

## Tóm tắt

Image PostgreSQL dev tự dựng từ `docker/postgres/Dockerfile` (base `postgres:17`) với `postgresql.conf` riêng; `docker-compose.yml` build image này thay vì dùng `postgres:17` gốc, phiên bản, timezone và ngưỡng log câu lệnh chậm lấy từ `.env`. Chỉ dành cho dev local; chưa dùng cho Testcontainers và production.

## Thay đổi gì

- `docker/postgres/Dockerfile`: `ARG POSTGRES_VERSION=17`; `POSTGRES_INITDB_ARGS="--encoding=UTF8 --data-checksums"`; chép `postgresql.conf` vào `/etc/postgresql/`, chuẩn hoá xuống dòng LF (tránh lỗi khi checkout CRLF trên Windows); `CMD postgres -c config_file=/etc/postgresql/postgresql.conf`.
- `docker/postgres/postgresql.conf` (không còn dùng `postgresql.conf` trong thư mục dữ liệu):
  - `listen_addresses = '*'`: bắt buộc vì file thay thế hoàn toàn, mặc định chỉ nghe `localhost` sẽ làm cổng map ra host không kết nối được.
  - `log_timezone = 'Asia/Ho_Chi_Minh'`, `log_lock_waits = on`, `deadlock_timeout = 1s`.
  - `timezone` và `log_min_duration_statement` không đặt ở đây mà do compose truyền từ `.env`; chạy image mà không qua compose thì dùng mặc định của PostgreSQL.
  - `log_parameter_max_length = 0`, `log_parameter_max_length_on_error = 0`.
- `docker-compose.yml`: `build` từ `./docker/postgres`, build arg `POSTGRES_VERSION` và tag `t3tarot-postgres:<phiên bản>` lấy từ `DB_IMAGE_VERSION` (mặc định 17); `command` giữ `config_file` và thêm `-c timezone=${DB_TIMEZONE:-UTC}`, `-c log_min_duration_statement=${DB_SLOW_QUERY_MS:-500}`; giữ nguyên healthcheck, volume và biến `DB_*` sẵn có.
- `.env.example`, `README.md` (bảng biến môi trường): thêm `DB_IMAGE_VERSION`, `DB_TIMEZONE`, `DB_SLOW_QUERY_MS`. Ứng dụng Spring không đọc ba biến này.

## Vì sao

- Tắt log giá trị tham số: mặc định PostgreSQL ghi giá trị bind vào log câu lệnh chậm và câu lệnh lỗi; câu hỏi của người dùng nằm trong tham số, vi phạm quy tắc 8 của `CLAUDE.md` (không ghi câu hỏi ra log).
- `timezone = 'UTC'` thay vì `Asia/Ho_Chi_Minh`: nếu dev dùng giờ VN thì `CURRENT_DATE` và `now()::date` trong SQL cho ra ngày VN và che lỗi khi DB thật chạy UTC; mốc ngày chỉ được tính qua `VietnamClock` (quy tắc 2). Giả định: DB môi trường thật chạy UTC, chưa xác minh. pgJDBC đặt `TimeZone` của session theo JVM nên ảnh hưởng chủ yếu tới `psql` và công cụ khác. Muốn đổi sang giờ VN đặt `DB_TIMEZONE=Asia/Ho_Chi_Minh` trong `.env`.
- `--data-checksums`: phát hiện hỏng trang dữ liệu, chi phí nhỏ; chỉ áp dụng khi khởi tạo volume mới.
- Cấu hình bằng `CMD` thay vì script init: áp dụng cả khi volume đã có dữ liệu và không cần script shell.

## Ảnh hưởng

- API / contract: Không.
- Cấu hình / biến môi trường / cờ: thêm `DB_IMAGE_VERSION`, `DB_TIMEZONE`, `DB_SLOW_QUERY_MS` (chỉ docker compose). Lệnh chạy đổi thành `docker compose up -d --build`; đổi `DB_IMAGE_VERSION` cũng cần `--build`. Nâng phiên bản chính của PostgreSQL trên volume có dữ liệu cũ sẽ không khởi động được (cần dump/restore hoặc volume mới).
- Database: Không có migration. Volume có sẵn giữ nguyên encoding và checksums cũ.
- Client: Không.
- Tiền, hạn mức, state machine: Không.

## QA cần test

- `docker compose up -d --build` thành công, container `healthy`.
- `docker compose exec postgres psql -U <DB_USERNAME> -d <DB_NAME> -c "show config_file" -c "show timezone" -c "show log_parameter_max_length" -c "show listen_addresses"` trả về `/etc/postgresql/postgresql.conf`, `UTC`, `0`, `*`.
- Ứng dụng kết nối được từ máy host qua `DB_PORT`; Flyway chạy bình thường.
- Chạy một truy vấn chậm có tham số (`select pg_sleep(1), $1` qua JDBC) và kiểm log không chứa giá trị tham số.
- Volume mới: `show data_checksums` trả `on`.
- Đặt `DB_TIMEZONE=Asia/Ho_Chi_Minh`, `DB_SLOW_QUERY_MS=0`, chạy lại `docker compose up -d`: `show timezone` đổi theo, mọi câu lệnh được ghi log mà không có giá trị tham số.
- Không đặt các biến này trong `.env`: compose dùng mặc định `UTC`, `500`, `17`.

## Rollout

- Dev đang có volume cũ chỉ cần `docker compose up -d --build`; không mất dữ liệu.
- Không đụng Kubernetes, production; image này không dùng ngoài máy dev.

## Trạng thái kiểm chứng

Chưa build và chưa chạy: môi trường làm việc không có Docker daemon. Các điểm dựa trên hiểu biết về image chính thức và chưa kiểm: `PGDATA` được `postgres` tôn trọng khi dùng `config_file`, `-c` trong `command` ghi đè cùng tham số trong file, các tham số `log_parameter_max_length*` (có từ PostgreSQL 13).

## File chính

- `docker/postgres/Dockerfile`
- `docker/postgres/postgresql.conf`
- `docker-compose.yml`

## Liên quan

- `src/test/java/vn/t3tarot/api/TestcontainersConfiguration.java` vẫn dùng `postgres:17` gốc, chưa dùng chung image này; cấu hình log và timezone không áp dụng cho test. Testcontainers ghi đè `CMD` của image nên cần xử lý riêng.
