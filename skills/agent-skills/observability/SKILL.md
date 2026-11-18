---
name: observability
description: Thêm metric Micrometer, dựng dashboard Grafana hoặc viết truy vấn Prometheus (PromQL) và Loki (LogQL) cho T3Tarot. Dùng khi cần đo pass/fail, độ trễ, tỉ lệ fallback, chi phí LLM.
---

# Observability

Nguồn chuẩn: `skills/agent-skills/observability/references/observability.md`, `skills/agent-skills/llm-safety-review/references/llm-safety.md`.

## Các bước

1. Xác định câu hỏi cần trả lời (ví dụ tỉ lệ fallback KB tăng không, độ trễ p95 của luận giải).
2. Chọn metric theo danh sách đề xuất: `llm_ttft_seconds`, `llm_duration_seconds`, `llm_tokens_total`, `reading_validation_failures_total`, `reading_fallback_total`, `reading_stopped_crisis_total`, `followup_refund_total`, `quota_rejected_total`.
3. Label chỉ lấy từ tập hữu hạn: `model`, `flow` (daily/spread/followup), `reason`, `layer`, `source`, `type`, `platform` (web/ios/android, đề xuất). Phiên bản app để trong log, không làm label. Không dùng `user_id`, `reading_id`, nội dung câu hỏi làm label.
4. Log là JSON có cấu trúc gửi về Loki. Label Loki chỉ gồm `app`, `env`, `level`, `module`; `requestId`, `reading_id` nằm trong nội dung log. Không log câu hỏi hoặc luận giải. Retention log ≤ 30 ngày.
5. Dashboard mặc định: lưu lượng, tỉ lệ lỗi, độ trễ p50/p95/p99 theo `flow` và `model`, tỉ lệ fallback KB, tỉ lệ bị safety filter chặn, chi phí token mỗi lượt.
6. Cung cấp truy vấn (PromQL hoặc LogQL) và giải thích từng field dùng trong truy vấn.
7. Viết test cho code đo metric (metric được ghi, label đúng).

## Hỏi trước khi làm

- Triển khai dashboard, alert hoặc cấu hình lên môi trường thật.
- Thêm Tempo hoặc tracing (chưa chốt).

## Dễ quên

- Endpoint `/actuator/prometheus` cần cấu hình bảo mật riêng vì Spring Security khoá mặc định.
- Cardinality của label quyết định chi phí Prometheus; mỗi label mới cần lý do.
