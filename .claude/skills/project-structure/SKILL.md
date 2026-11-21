---
name: project-structure
description: Quyết định file mới (Java, test, migration, cấu hình, tài liệu, skill) đặt ở thư mục, package nào và đặt tên theo chuẩn của repo T3Tarot API, để đọc và tra cứu (tracking) dễ. Dùng mỗi khi sắp tạo, đổi tên hoặc di chuyển file, class, method, bảng, endpoint, metric.
---

# Đặt file đúng chỗ, đặt tên đúng chuẩn

Hai reference trong skill này:

- `.claude/skills/project-structure/references/structure.md`: cây thư mục, file loại nào đặt ở đâu, cây quyết định chọn package con, phạm vi truy cập.
- `.claude/skills/project-structure/references/naming.md`: năm nguyên tắc đặt tên, từ điển thuật ngữ, chuẩn tên cho class, method, biến, test, bảng, migration, API, cấu hình, metric, branch, commit; kèm ví dụ đúng và sai.

Kiến trúc module: `.claude/skills/new-module/references/architecture.md`.

Mục đích: nhìn đường dẫn và tên là biết file thuộc module nào, làm vai trò gì; `grep` một khái niệm (ví dụ `followup`) ra đủ API, class, bảng, metric, test.

## Các bước

1. **Xác định loại file:** Java chạy thật, test, migration, cấu hình, tài liệu hay skill. Mỗi loại có một chỗ duy nhất (bảng "Loại file và vị trí" trong `structure.md`).
2. **Java: tìm module sở hữu** theo bảng module trong `architecture.md`.
   - Thuộc module có sẵn: đặt trong module đó.
   - Không mang nghiệp vụ (util, mã lỗi chung, cấu hình kỹ thuật): đặt trong `common`, theo mục "Đặt file trong `common`" của `structure.md` (chọn nhóm, hằng số nào vào `constant`, khi nào tách package con).
   - Có nghiệp vụ nhưng chưa có module: dừng, dùng skill `new-module`.
3. **Java trong module nghiệp vụ: chọn feature rồi thư mục vai trò** theo mục "Cấu trúc trong module" trong `structure.md`: `<module>/<feature>/<controller|dto/request|dto/response|service|repository|entity|enumeration|policy|client|config>/`, phần dùng chung ở `<module>/shared/`. Không khớp feature nào thì dừng và hỏi. Trong `common` không dùng luật này.
4. **Tra từ điển thuật ngữ** trong `naming.md`. Khái niệm đã có thì dùng đúng chữ đó (ví dụ `followup`, không `followUp`, không `question`). Khái niệm mới thì đề xuất thêm một dòng vào từ điển trước khi đặt tên.
5. **Đặt tên** theo bảng tương ứng trong `naming.md`: hậu tố vai trò cho class, động từ cho method, hậu tố kiểu dữ liệu cho biến (`At`, `Date`, `Vnd`, `MicroUsd`, `Count`), `<kết quả>When<điều kiện>` cho method test, `V{n}__{động_từ}_{đối_tượng}.sql` cho migration, danh từ số nhiều kebab-case cho đường dẫn API.
6. **Tự kiểm tên** trước khi tạo:
   - Có hậu tố hoặc dạng nói rõ vai trò không?
   - Có viết tắt ngoài danh sách cho phép không?
   - Có số thứ tự, ngày, tên người, `V2`, `New`, `Impl`, `Helper`, `Util` không?
   - Có trùng tên hoặc trùng vai trò với file đã có không? Có rồi thì sửa file đó, không tạo bản thứ hai.
7. **Test:** cùng package với class được test, trong `src/test/java`, tên `<Class>Test` (đường nóng: `<Class>ConcurrencyTest`).
8. **Báo trước khi tạo:** liệt kê từng file sắp tạo với đường dẫn đầy đủ và lý do một dòng.
9. **Báo sau khi tạo:** bảng tracking `File | Module | Lớp | Vai trò | Thuộc tính năng` (mẫu trong `naming.md`), đưa vào Session Feature Summary. Có commit thì gợi ý message theo chuẩn commit trong `naming.md`.

## Hỏi trước khi làm

- Tạo thư mục mới ở gốc repo, Maven module mới, hoặc module nghiệp vụ mới (dùng `new-module`).
- Thêm file vào `common` khi không chắc nó có mang nghiệp vụ hay không.
- Tạo nhóm mới trong `common`, tách package con mới trong một nhóm, hoặc chuyển file giữa các nhóm và package con.
- Thêm khái niệm mới vào từ điển thuật ngữ, hoặc dùng một tên khác với từ điển.
- Đổi tên file, class, bảng, endpoint đang có (đổi tên endpoint hoặc field là đổi API contract; xem `client-compatibility`).
- Tạo file cấu hình theo profile (`application-*.yml`) hoặc migration (dùng `flyway-migration`).

## Cấm

- Package tên theo loại class: `util`, `helper`, `misc`, `base`, `dto`, `config`, `impl`, `model`, ở bất kỳ cấp nào (module, nhóm trong `common`, package con).
- Tiền tố `I` cho interface; hậu tố `Impl`, `Manager`, `Helper`, `Util`, `Processor`, `V2`, `New`.
- Đặt class nghiệp vụ trong `common` (kể cả mã lỗi riêng của module vào `common/constant`); controller ngoài `<feature>/controller`; package theo tầng (`domain`, `infra`, `usecase`, `web`) ngay dưới module; feature gọi thẳng use case, repository, entity của feature khác (chỉ qua Facade và dto); module khác import package con của module này.
- Test tên `*IT` (surefire không chạy) hoặc tên không mô tả hành vi (`test1`, `shouldWork`).
- Ghi quy tắc mới vào `docs/`; `docs/` chỉ chứa tài liệu gốc và `docs/changelog/`, quy tắc nằm trong `references/` của skill.
