# Observability

- Metrics qua Micrometer → Prometheus. Metric đề xuất: `llm_ttft_seconds`, `llm_duration_seconds`, `llm_tokens_total`, `reading_validation_failures_total`, `reading_fallback_total`, `reading_stopped_crisis_total`, `followup_refund_total`, `quota_rejected_total`.
- Label chỉ gồm tập hữu hạn: `model`, `flow` (daily/spread/followup), `reason`, `layer`, `source`, `type`, `platform` (web/ios/android, đề xuất). Phiên bản app để trong log, không làm label. Không dùng `user_id`, `reading_id`, nội dung câu hỏi làm label (cardinality bùng nổ và lộ PII).
- Log JSON có cấu trúc gửi về Loki. Label Loki chỉ gồm `app`, `env`, `level`, `module`; `reading_id`, `user_id`, `requestId` nằm trong nội dung log.
- Retention log ≤ 30 ngày, khớp khung 7–30 ngày của dữ liệu người dùng.
- Dashboard mặc định: lưu lượng, tỉ lệ lỗi, độ trễ p50/p95/p99 theo `flow` và `model`, tỉ lệ fallback KB, tỉ lệ bị safety filter chặn, chi phí token mỗi lượt.
- Actuator mở `health`, `info`, `prometheus`; endpoint `prometheus` cần cấu hình bảo mật riêng vì Spring Security đang khoá mặc định.
