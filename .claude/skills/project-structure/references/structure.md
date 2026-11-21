# Cấu trúc repo T3Tarot API

Chỗ ghi **(đề xuất)** là quy ước mình đặt để dùng được ngay, chưa được chốt; đổi thì sửa file này.

## Cây thư mục

```
t3tarot-api/
├── CLAUDE.md                     quy tắc chung cho agent
├── pom.xml, lombok.config, docker-compose.yml, .env.example
├── docker/<service>/             Dockerfile + cấu hình image dev (hiện có postgres/)
├── docs/                         tài liệu gốc của dự án (spec, ERD)
│   └── changelog/                changelog theo feature (README.md là mục lục), sửa đè khi feature đổi
├── .claude/                      cấu hình AI, bản đồ ở .claude/README.md
│   ├── agents/<vai-trò>.md       agent theo vai trò
│   ├── commands/<tên>.md         lệnh gọi bằng /<tên>
│   ├── hooks/                    script hook (khai báo trong settings.json)
│   ├── output-styles/<tên>.md    cách trả bài
│   ├── policies/<tên>.md         phanh: việc phải hỏi trước, việc cấm
│   ├── references/               tài liệu dùng chung nhiều skill
│   ├── rules/<chủ-đề>.md         luật, Claude Code tự nạp
│   ├── skills/<tên>/             SKILL.md + references/
│   └── workflows/<tên>/          WORKFLOW.md + references/, gọi qua commands/
└── src/
    ├── main/java/vn/t3tarot/api/
    │   ├── T3TarotApiApplication.java
    │   ├── common/               module OPEN, không nghiệp vụ
    │   │   ├── constant/         ErrorCode (interface), CommonErrorCode, ViolationCodes, ErrorResponseKeys (mã và khoá hằng)
    │   │   ├── error/            ApiException, FieldViolation, ErrorCodeFormat, GlobalExceptionHandler (xử lý lỗi)
    │   │   ├── idempotency/      IdempotencyKey
    │   │   ├── random/           bean SecureRandom
    │   │   ├── security/         SecurityConfig, SecurityProperties, AuthenticatedUser, ProblemDetailSecurityHandler, ManagementPortRequestMatcher
    │   │   │   ├── jwt/          JwtDecoderConfig, RsaPublicKeys, AccessTokenAuthentication, AccessTokenAuthenticationConverter
    │   │   │   └── ratelimit/    RateLimitConfig, RateLimitProperties, RateLimitCounter, RateLimitDecision
    │   │   ├── time/             VietnamClock, bean Clock
    │   │   └── web/              RequestIdFilter, PageQuery, PageResponse
    │   └── <module>/             identity, content, reading, interpretation, followup, safety, privacy, audit
    │       ├── <Module>Api.java  chỉ khi module khác cần gọi: interface công khai, record trả ra ngoài, event
    │       ├── <feature>/        mỗi tính năng một thư mục (identity: register, passwordlogin, sociallogin, session, emailverification)
    │       │   └── controller/ dto/{request,response}/ service/ repository/ entity/ enumeration/ policy/ client/ config/
    │       └── shared/           thứ từ 2 feature trở lên dùng chung, cùng bộ thư mục vai trò như trên
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
| Dockerfile, cấu hình image dev | `docker/<service>/` |
| Tài liệu gốc (spec, ERD) | `docs/` |
| Changelog của feature | `docs/changelog/<feature>.md`, mục lục `docs/changelog/README.md` |
| Quy tắc, SQL mẫu, quy trình của một skill | `.claude/skills/<tên>/references/` hoặc `.claude/workflows/<tên>/references/` của skill sở hữu |
| Tài liệu dùng chung từ 2 skill trở lên | `.claude/references/` |
| Skill mới | `.claude/skills/<tên>/SKILL.md`; tên không chứa tên dự án |
| Workflow mới | `.claude/workflows/<tên>/WORKFLOW.md` kèm command `.claude/commands/<tên>.md` |
| Agent theo vai trò | `.claude/agents/<vai-trò>.md` |
| Luật luôn áp dụng / phanh | `.claude/rules/<chủ-đề>.md` / `.claude/policies/<tên>.md` |

## Cấu trúc trong module (bắt buộc, không ngoại lệ)

Chia **tính năng trước, vai trò sau**: `<module>/<feature>/<vai-trò>/Class.java`. Muốn đọc một tính năng thì mở đúng một thư mục `<feature>/`.

Ví dụ module `identity`:

```
identity/
├── register/            controller/ dto/{request,response}/ service/
├── passwordlogin/       controller/ dto/request/ service/
├── sociallogin/         controller/ dto/{request,response}/ service/ client/ entity/
├── session/             controller/ dto/{request,response}/ service/ repository/ entity/ config/
├── emailverification/   controller/ dto/request/ service/ repository/ client/ config/
└── shared/              config/ controller/ entity/ enumeration/ policy/ repository/ service/
```

1. Mọi class thuộc đúng một đường dẫn `<module>/<feature>/<vai-trò>/`. Cấm package theo tầng ngay dưới module (`domain`, `infra`, `usecase`, `web`, `service`, `controller`...), cấm class đặt thẳng trong thư mục feature, và cấm class đặt thẳng trong `dto` (phải vào `dto/request` hoặc `dto/response`). Module chưa có feature thì chỉ có package gốc và `package-info.java`, không giữ thư mục rỗng.
2. **Feature** là một khả năng mà người dùng gọi tới hoặc một mảng nghiệp vụ độc lập (`passwordlogin`, `sociallogin`, `session`). Tên một từ, chữ thường, số ít, không gạch dưới, đặt theo từ điển thuật ngữ. Mỗi controller thuộc đúng một feature. Thêm feature mới: dừng và hỏi.
3. **Thư mục vai trò** chỉ được dùng danh sách dưới đây, chỉ tạo khi có class, không tạo tên khác:

   | Thư mục | Chứa | Truy cập |
   | --- | --- | --- |
   | `controller` | Controller HTTP, SSE | package-private |
   | `dto/request` | Record request của HTTP (có annotation validate) | `public` khi feature khác dùng |
   | `dto/response` | Record response của HTTP | `public` khi feature khác dùng |
   | `service` | `<Feature>Facade`, use case, class hỗ trợ nghiệp vụ | Facade `public`, còn lại package-private |
   | `repository` | Spring Data repository, `*HotPathRepository` | `public` |
   | `entity` | Entity JPA, record ánh xạ dòng DB hoặc giá trị nghiệp vụ | `public` |
   | `enumeration` | Enum trạng thái, loại, `<Module>ErrorCode` | `public` |
   | `policy` | `<Chủ đề>Policy`, quy tắc và hằng số nghiệp vụ | `public` |
   | `client` | Gọi dịch vụ ngoài (LLM, Google, mail) và interface cổng ra | `public` |
   | `config` | `@Configuration`, `@ConfigurationProperties` | `public` |

4. **`service` có một cửa vào**: mỗi feature có đúng một `<Feature>Facade` (`@Service`, `public`) là chỗ duy nhất controller và feature khác được gọi. Use case (mỗi nghiệp vụ một class, tên động từ + danh từ, `@Transactional` đặt ở đây) và class hỗ trợ đều package-private, nằm sau Facade. Facade chỉ chuyển lời gọi, không chứa nghiệp vụ.
5. **`shared`** là feature đặc biệt cho thứ có từ 2 feature trở lên dùng (bảng chung, enum chung, mã lỗi của module, helper chung). Thứ chỉ một feature dùng thì ở lại feature đó, không đưa vào `shared` "cho tiện". `shared` không import feature nào.
6. **Hướng phụ thuộc**: `controller` → Facade (cùng feature) → use case → `repository` / `client`. Feature A chỉ dùng feature B qua `B/service/<B>Facade` và `B/dto`; cấm import use case, repository, entity, client, config của B. Không để hai feature gọi vòng nhau. Mọi feature được import `shared`.
7. **Gốc module** chỉ chứa thứ module khác được phép dùng: interface `<Module>Api`, record trả ra ngoài, domain event. Chưa có thì để trống (chỉ `package-info.java`). Module khác chỉ import package gốc.
8. **Test** đặt cùng đường dẫn với class được test (`src/test/java/.../<module>/<feature>/<vai-trò>/`). Helper test dùng chung cho module đặt ở gốc module trong `src/test`.
9. Class mới không khớp feature và vai trò nào: dừng và hỏi, không tạo thư mục tạm.
10. Chuyển hoặc tách file là đổi tên class đầy đủ: chạy `ModularityTests` và test của module, sửa đường dẫn trong `docs/changelog/`.

## Đặt file trong `common`

`common` chia hai cấp: **nhóm** (`common/<nhóm>/`) và, khi nhóm đầy, **package con** (`common/<nhóm>/<mối quan tâm>/`). Chỉ có hai cấp này.

### Bước 1: chọn nhóm

| Nhóm | Chứa | Không chứa |
| --- | --- | --- |
| `constant` | Hằng số dùng chung: interface và enum mã lỗi (`ErrorCode`, `CommonErrorCode`), chuỗi hằng (`ViolationCodes`), tên khoá response và MDC (`ErrorResponseKeys`) | Logic, bean, hằng chỉ một class dùng |
| `error` | Xử lý lỗi: `ApiException`, `FieldViolation`, `ErrorCodeFormat`, `GlobalExceptionHandler` | Mã lỗi và khoá hằng (vào `constant`), mã lỗi riêng của module (vào `<module>/shared/enumeration`) |
| `idempotency` | `IdempotencyKey` | |
| `random` | Bean `SecureRandom` | |
| `security` | Cấu hình và thành phần bảo mật | |
| `time` | `VietnamClock`, bean `Clock` | |
| `web` | `RequestIdFilter`, `PageQuery`, `PageResponse` | |

Nhóm mới chỉ tạo khi không nhóm nào ở trên khớp; tạo thì dừng và hỏi (xem `SKILL.md`).

### Bước 2: hằng số nằm ở đâu

- Hằng dùng từ hai class trở lên, hoặc ra ngoài `common`: vào `common/constant`.
- Hằng chỉ một class dùng (ví dụ `HSTS_MAX_AGE_SECONDS` trong `SecurityConfig`): để `private` hoặc package-private ngay trong class đó, không chuyển sang `constant`.
- Mã lỗi riêng của module: `<module>/shared/enumeration/<Module>ErrorCode`, không đặt trong `common/constant`.

### Bước 3: hướng phụ thuộc giữa các nhóm

- `constant` không import nhóm nào khác trong `common`.
- Các nhóm khác được import `constant`.
- Nhóm không import vòng nhau; cần vòng thì đẩy phần chung xuống `constant` hoặc gộp nhóm.

### Bước 4: khi nào tách package con

1. Tách khi một mối quan tâm riêng có từ 3 class trở lên và tự đứng được (`security/jwt`, `security/ratelimit`). Ít hơn 3 class thì để ở gốc nhóm.
2. Gốc nhóm giữ: class lắp ráp cả nhóm (`SecurityConfig`), cấu hình chung của nhóm (`SecurityProperties`), và kiểu `public` mà code ngoài nhóm dùng (`AuthenticatedUser`). Code ngoài nhóm chỉ import từ gốc nhóm (trừ `constant`, vì cả nhóm là danh mục công khai).
3. `<Chủ đề>Config` và `<Chủ đề>Properties` riêng của một mối quan tâm nằm trong package con của nó.
4. Tên package con: một từ, chữ thường, số ít, gọi theo mối quan tâm. Cấm tên theo loại class: `config`, `dto`, `util`, `helper`, `misc`, `base`, `impl`, `model`.
5. Không lồng sâu hơn một cấp. Cần sâu hơn thì nhóm đó đáng thành module riêng: dừng và hỏi.
6. Hướng phụ thuộc: package con được dùng gốc nhóm; gốc nhóm chỉ import package con để lắp ráp (ví dụ `SecurityConfig` ráp `AccessTokenAuthenticationConverter`); package con không import package con khác. Vi phạm thì gộp lại hoặc đẩy phần chung lên gốc nhóm.
7. Phạm vi truy cập: class dùng chéo package phải `public`; còn lại giữ package-private.

### Test và kiểm sau khi chuyển

- Test đặt cùng package với class được test. Helper test dùng chung nhiều package (như `TestJwtFactory`) đặt ở gốc nhóm trong `src/test` và để `public`.
- Chuyển file là đổi tên class đầy đủ: chạy `ModularityTests` và test của nhóm, sửa đường dẫn trong `docs/changelog/`, sửa mục này nếu cây thư mục đổi.

## Đặt tên

Toàn bộ chuẩn đặt tên (class, method, biến, test, bảng, migration, API, metric, branch, commit) và từ điển thuật ngữ nằm ở `naming.md` cùng thư mục.

## Phạm vi truy cập

- Quy tắc truy cập theo từng thư mục vai trò nằm ở bảng trong mục "Cấu trúc trong module". Class chỉ dùng trong một package để package-private.
- Module khác chỉ được import package gốc của module này; Spring Modulith chặn phần còn lại.

## Test

- Mọi test, kể cả test chạy Testcontainers, đặt tên `*Test` (hoặc `*Tests` cho test cấp ứng dụng có sẵn). Surefire mặc định không chạy `*IT`, và pom chưa có failsafe.
- Test đồng thời cho đường ghi nóng đặt cùng package với `*HotPathRepository`.

## Còn mở

- Các mục ghi "(đề xuất)" trong `naming.md`.
