---
name: changelog
description: Sau mỗi task có sửa code, ghi hoặc sửa đè changelog theo feature vào docs/changelog để luôn khớp trạng thái hiện hành. Dùng ở cuối mọi task code, kể cả sửa nhỏ, trước khi báo xong.
---

# Changelog theo feature

Mẫu, ví dụ và quy tắc chi tiết: `skills/agent-skills/changelog/references/changelog.md`. Đặt tên file theo `project-structure`.

Changelog ở đây là **tài liệu mô tả trạng thái hiện hành của feature**, không phải nhật ký nối thêm. Mỗi feature một file `docs/changelog/<feature>.md`; lịch sử do git giữ.

## Các bước

1. **Chốt feature:** tên kebab-case tiếng Anh theo từ điển trong `naming.md` (ví dụ `security-access-control`). Task chạm nhiều feature thì ghi từng feature một file.
2. **Tìm changelog liên quan:** đọc `docs/changelog/README.md`, rồi `grep` trong `docs/changelog/` tên class, endpoint, khoá cấu hình, mã lỗi, bảng vừa đổi. Một file nhắc tới phần vừa đổi là file liên quan, kể cả khi nó thuộc feature khác.
3. **Ghi hoặc sửa đè:**
   - Chưa có: tạo file theo mẫu.
   - Đã có và liên quan: sửa đè các mục sai hoặc thiếu để file mô tả đúng trạng thái sau thay đổi. Không thêm đoạn "cập nhật ngày…", không giữ nội dung đã lỗi thời, không nối thêm.
   - Task xoá hoặc thay thế feature: sửa file cho đúng, hoặc xoá file nếu feature không còn.
4. **Sửa dây chuyền:** các changelog khác nhắc tới phần vừa đổi (tên class, mã lỗi, biến môi trường, endpoint) phải sửa cho khớp trong cùng task.
5. **Cập nhật mục lục** `docs/changelog/README.md`: một dòng mỗi feature (file, mô tả một câu, ngày cập nhật cuối).
6. **Trung thực về kiểm chứng:** mục "Trạng thái kiểm chứng" ghi đúng lệnh đã chạy và kết quả; chưa chạy thì ghi "Chưa chạy" kèm lý do. Không ghi "đã test" khi chưa chạy.
7. **Báo cuối task:** liệt kê file changelog đã tạo hoặc sửa đè, và file nào bị sửa dây chuyền.

## Quy tắc bắt buộc

- Chỉ ghi điều có trong code hoặc tài liệu; không bịa. Giả định ghi rõ là giả định.
- Không đưa vào changelog: secret, token, khoá, PII, nội dung câu hỏi hoặc luận giải, endpoint production. Ví dụ dùng dữ liệu giả.
- Mọi ngày dùng `Asia/Ho_Chi_Minh`, định dạng `YYYY-MM-DD`.
- Thay đổi chạm công thức tiền, lượt, trạng thái reading / followup, migration hay API contract phải nêu rõ ở mục "Ảnh hưởng" và "Rollout".
- Không viết comment hay code mẫu dài; trích tối đa vài dòng để định vị.

## Hỏi trước khi làm

- Gộp hoặc tách feature đang có, hoặc đổi tên file changelog đã có.
- Xoá file changelog.
