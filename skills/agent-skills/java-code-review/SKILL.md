---
name: java-code-review
description: Review code hoặc PR của T3Tarot theo thứ tự ưu tiên của dự án (đúng đắn nghiệp vụ, bảo mật, kiến trúc, Sonar, hiệu năng, style). Dùng khi cần review thay đổi trước khi merge.
---

# Review code

Nguồn chuẩn: `CLAUDE.md` (quy tắc bắt buộc), `skills/agent-skills/java-code-review/references/conventions.md` (Sonar, DB, test), `skills/agent-skills/new-module/references/architecture.md`.

## Thứ tự soát

1. **Đúng đắn nghiệp vụ:** quy tắc bắt buộc trong `CLAUDE.md`; hạn mức và chuyển trạng thái trong một câu SQL; idempotency; hoàn lượt đúng một lần; `VietnamClock`; tiền không dùng `float`/`double`, làm tròn rõ ràng.
2. **Bảo mật:** `user_id` trong mọi truy vấn theo `reading_id` (IDOR), phân quyền, validate input, rò rỉ dữ liệu trong log và response, secret, replay, rate limit, prompt injection.
3. **Tương thích client:** endpoint dùng được cho website và mobile app; thay đổi không phá app đã phát hành (xoá, đổi tên, đổi nghĩa field, field request mới bắt buộc, đổi mã lỗi); thao tác ghi có idempotency cho app retry. Xem `client-compatibility`.
4. **Kiến trúc:** file đặt đúng chỗ và đúng tên theo `project-structure`; ranh giới module (`ModularityTests`), `common` không chứa nghiệp vụ, không giữ transaction qua lời gọi LLM, controller không gọi repository, `@Transactional` ở use case.
5. **Sonar:** rule hay dính trong `skills/agent-skills/java-code-review/references/conventions.md`; cognitive complexity, chuỗi lặp, PRNG không an toàn, mật khẩu ghi cứng.
6. **Hiệu năng:** N+1, fetch lazy sai, kích thước pool Hikari, virtual thread bị pin bởi `synchronized`, cache sai chỗ.
7. **Style:** không comment trong code, đặt tên, Lombok đúng quy ước (không `@Data` trên entity JPA).
8. **Test:** có test cho nhánh mới, coverage ≥ 80% cho service, controller, utils; không làm xanh giả (xem `CLAUDE.md` mục "Chạy và sửa test").

## Cách báo

Sắp theo mức nghiêm trọng, nhóm theo thứ tự trên. Mỗi phát hiện gồm: vị trí (file:dòng), vấn đề, kịch bản lỗi cụ thể (ví dụ "2 request đồng thời → trừ lượt hai lần"), cách sửa. Không nêu nhận xét chung chung hoặc phát hiện không có kịch bản lỗi.

## Dễ quên

- Đừng kết luận khi chưa đọc phần code liên quan; đánh dấu rõ điều chưa kiểm chứng.
- Phát hiện thuộc phạm vi của PR khác thì ghi là ngoài phạm vi, không trộn vào.
