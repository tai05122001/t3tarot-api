---
name: client-compatibility
description: Kiểm tra endpoint mới hoặc sửa đổi có chạy đúng cho cả website và mobile app không - tương thích với app phiên bản cũ, xác thực theo nền tảng, mạng chập chờn, header client. Dùng trong workflow tạo hoặc sửa endpoint và khi review API.
---

# Tương thích client: website và mobile app

Nguồn chuẩn: `skills/agent-skills/client-compatibility/references/client-platforms.md`.

Khác biệt cốt lõi: website deploy cùng lúc với backend, còn app đã cài trên máy người dùng có thể ở phiên bản cũ nhiều tháng. Khi mobile dùng endpoint, mọi thay đổi phải giả định app cũ vẫn đang gọi.

## Các bước

1. **Xác định client** dùng endpoint: website, mobile app (iOS, Android) hay cả hai. Chưa rõ thì hỏi, không đoán.
2. **Một hợp đồng chung** cho mọi client. Không trả cấu trúc khác nhau theo nền tảng nếu chưa được duyệt.
3. **Mobile có dùng:** kiểm tương thích ngược theo bảng "được làm / phá vỡ" trong reference. Thay đổi phá vỡ với app đã phát hành luôn là mức ảnh hưởng Cao.
4. **Mạng chập chờn:** mọi thao tác ghi có `Idempotency-Key` (app retry khi mất mạng); SSE nối lại được và phát lại từ DB; danh sách dài có phân trang; không trả dữ liệu thừa, ảnh dùng URL.
5. **Xác thực và bảo mật theo nền tảng:** web cần CORS với origin cụ thể và chống CSRF nếu dùng cookie; app dùng token. Cơ chế đăng nhập chưa chốt, ghi rõ là giả định.
6. **Header client và metric:** đọc nền tảng và phiên bản app nếu đã chốt header (xem "Đề xuất chưa chốt" trong reference); không tin header này cho phân quyền.
7. **Xuất bảng ảnh hưởng theo client:**

| Client | Ảnh hưởng | Phiên bản app tối thiểu | Việc phía client |
| --- | --- | --- | --- |
| Website | | không áp dụng | |
| iOS | | | |
| Android | | | |

## Hỏi trước khi làm

- Thay đổi phá vỡ tương thích với app đã phát hành.
- Thêm phiên bản API mới, cơ chế bắt buộc cập nhật app, hoặc tách API riêng cho từng client.
- Đổi cơ chế xác thực của một nền tảng.

## Dễ quên

- Thời gian trả về theo ISO-8601 có offset. Ngày của daily card do server quyết theo `Asia/Ho_Chi_Minh`, không lấy giờ trên máy người dùng.
- Client hiển thị lỗi theo `code` (`error.<nhóm>.<lý do>`) trong `ProblemDetail` và tự map ra câu chữ; server không trả thông báo. Đổi tên hoặc xoá một mã lỗi là phá tương thích với app đã phát hành.
- Thêm giá trị enum mới vào field đang có có thể làm app cũ lỗi nếu app không xử lý giá trị lạ.
