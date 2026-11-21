# Quy ước changelog

## Vị trí và tên

- `docs/changelog/<feature>.md`, một feature một file; mục lục ở `docs/changelog/README.md`.
- `<feature>`: kebab-case tiếng Anh, theo từ điển thuật ngữ trong `.claude/skills/project-structure/references/naming.md`. Không dùng ngày, số phiên bản, tên người.
- Feature là một khả năng người dùng hoặc team nhận ra được (`security-access-control`, `daily-card`), không phải một commit hay một class.

## Nguyên tắc sửa đè

- File luôn mô tả trạng thái hiện hành. Đọc file là biết feature đang chạy thế nào, không phải đọc lại từng lần đổi.
- Thay đổi chạm feature đã có: sửa đè đúng mục, xoá nội dung lỗi thời. Lịch sử xem bằng `git log -- docs/changelog/<feature>.md`.
- Mục "Cập nhật lần cuối" là ngày task sửa file gần nhất.
- Khi một thay đổi làm sai file khác (đổi tên class, mã lỗi, biến môi trường, đường dẫn endpoint), sửa luôn file đó.

## Mẫu

```markdown
# <Tên feature>

Cập nhật lần cuối: YYYY-MM-DD · Trạng thái: Đang phát triển | Đã phát hành | Ngừng dùng

## Tóm tắt

Một đến hai câu: feature làm gì, hiện ở mức nào.

## Thay đổi gì

Nhóm theo thành phần (module, endpoint, cấu hình). Mỗi dòng một thay đổi, nêu tên class hoặc khoá cụ thể.

## Vì sao

Lý do và quyết định liên quan, kèm giả định chưa xác minh (đánh dấu "Giả định").

## Ảnh hưởng

- API / contract: endpoint, field, mã lỗi bị thêm hoặc đổi; có phá tương thích app cũ không.
- Cấu hình / biến môi trường / cờ.
- Database: migration, bảng, cột (hoặc "Không").
- Client: website, iOS, Android cần làm gì, phiên bản app tối thiểu.
- Tiền, hạn mức, state machine: có chạm không.

## QA cần test

Danh sách kịch bản: happy path, biên, lỗi, đồng thời, bảo mật (IDOR, replay), tương thích app cũ.

## Rollout

Thứ tự triển khai, thay đổi hạ tầng (probe, port, secret), cách lùi.

## Trạng thái kiểm chứng

Lệnh đã chạy và kết quả; phần chưa chạy kèm lý do.

## File chính

Đường dẫn chính để định vị, tối đa khoảng mười dòng.

## Liên quan

Link tới changelog khác và tài liệu gốc.
```

Mục không áp dụng thì ghi "Không", không xoá mục, để người đọc thấy đã cân nhắc.

## Mục lục `docs/changelog/README.md`

Bảng `Feature | Mô tả | Cập nhật lần cuối`, sắp theo tên feature. Thêm, sửa hoặc xoá dòng cùng lúc với file.

## Danh sách kiểm trước khi báo xong

- [ ] Feature có file changelog, mô tả đúng code hiện tại.
- [ ] Đã `grep` các tên vừa đổi trong `docs/changelog/`; file khác nhắc tới đã sửa.
- [ ] Mục lục khớp.
- [ ] "Trạng thái kiểm chứng" đúng với những gì đã chạy.
- [ ] Không có secret, PII, nội dung câu hỏi.
