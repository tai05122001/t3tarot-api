# Đường ghi nóng

Đường ghi nóng là mọi câu ghi quyết định hạn mức, lá bài, trạng thái hoặc hoàn lượt. Chúng viết bằng `JdbcClient` với SQL có điều kiện; Hibernate chỉ dùng cho CRUD thường. Nguồn: ERD mục 8, đã chạy thử với request song song thật trên PostgreSQL 16. Nếu ERD đổi thì cập nhật file này.

## Vì sao không dùng Hibernate ở đây

- Đọc–sửa–ghi: 2 request cùng `findById` thấy `followup_count = 2`, cùng ghi 3, cùng gọi LLM, vượt hạn mức.
- Mất cập nhật sau native query: entity trong persistence context còn giá trị cũ, một `save()` khác flush ghi đè bộ đếm.
- Không lỗi, không exception; chỉ lộ ra khi đối soát số liệu.

## Quy tắc

1. Chỉ class `*HotPathRepository` trong module sở hữu bảng được ghi các cột nóng, bằng `JdbcClient`.
2. Mỗi quyết định là một câu lệnh duy nhất; điều kiện nằm trong `WHERE`. Kết quả đọc từ số dòng cập nhật hoặc `RETURNING`, không kiểm trước rồi ghi sau.
3. Chuyển trạng thái = compare-and-set: `UPDATE ... WHERE status = :cũ`.
4. Luôn kèm `user_id` trong điều kiện (chặn IDOR). Bài của người khác trả 404, không trả 403.
5. 0 dòng là kết quả nghiệp vụ hợp lệ ("Sắp có", "đã có lá", "hết lượt"), không ném exception chung.
6. Entity Hibernate của `Reading` không map setter cho `followup_count`, `status`, `interpret_attempts`, `lease_*`; các cột này dùng `@Column(updatable = false)`.
7. Hạn mức (3 câu, N lượt) lấy từ cấu hình, không viết cứng trong SQL.
8. Không giữ transaction qua lời gọi LLM.

## Các câu lệnh

### Q1 — Daily: tạo nếu chưa có, rồi đọc lại

```sql
INSERT INTO reading (id, user_id, kind, spread_type, topic, draw_date, status,
                     reversal_mode, deck_id, content_version_id, question_expires_at, drawn_at)
VALUES (:id, :uid, 'DAILY', 'ONE_CARD', 'GENERAL', :vn_today, 'DRAWN',
        :reversal_mode, :deck_id, :cv_id, :expires_at, now())
ON CONFLICT (user_id, draw_date) WHERE kind = 'DAILY' DO NOTHING
RETURNING id;
```

0 dòng nghĩa là đã có lá hôm nay: đọc lại, không bốc lại. `:vn_today` luôn lấy từ `VietnamClock.today()`.

### Q2 — Quota tuần: kiểm và tăng

```sql
INSERT INTO spread_quota (user_id, week_start, used)
VALUES (:uid, :week_start, 1)
ON CONFLICT (user_id, week_start)
DO UPDATE SET used = spread_quota.used + 1, updated_at = now()
WHERE spread_quota.used < :n
RETURNING used;
```

Chạy cùng transaction với tạo reading. Nếu `:n = 0` nhánh INSERT vẫn cho qua một lượt, nên API phải chặn trước. Chỉ đúng với tuần lịch (Thứ 2 00:00 giờ VN); chưa chốt.

### Q3 — Hỏi tiếp: trừ lượt

```sql
UPDATE reading
SET followup_count = followup_count + 1, updated_at = now()
WHERE id = :rid AND user_id = :uid AND status = 'COMPLETED' AND followup_count < 3
RETURNING followup_count;
```

Cùng transaction với `INSERT INTO followup (..., status = 'PENDING')`. Retry cùng `idempotency_key` vi phạm `UNIQUE (reading_id, idempotency_key)` làm rollback cả transaction, nên không tốn thêm lượt; xử lý lỗi này bằng cách trả lại câu trả lời đã có.

### Q4 — Hoàn lượt: đúng một lần

```sql
WITH failed AS (
    UPDATE followup
    SET status = 'FAILED', fail_reason = :reason, failed_at = now()
    WHERE id = :fid AND status = 'PENDING'
    RETURNING reading_id
)
UPDATE reading r
SET followup_count = r.followup_count - 1, updated_at = now()
FROM failed f
WHERE r.id = f.reading_id
RETURNING r.followup_count;
```

Dùng cho cả lỗi LLM lẫn sweeper (`fail_reason = 'STALE'`). Cùng mẫu này sẽ dùng lại cho hoàn xu sau MVP.

### Q5 — Chuyển trạng thái có điều kiện

```sql
UPDATE reading
SET status = 'INTERPRETING', interpret_attempts = interpret_attempts + 1, updated_at = now()
WHERE id = :rid AND user_id = :uid AND status IN ('DRAWN', 'FAILED') AND interpret_attempts < 2
RETURNING interpret_attempts;
```

### Q6 — Purge hằng ngày (idempotent, theo lô)

```sql
WITH due AS (
    SELECT id FROM reading
    WHERE content_purged_at IS NULL AND question_expires_at <= now()
    ORDER BY question_expires_at
    LIMIT 1000
    FOR UPDATE SKIP LOCKED
), del_fc AS (
    DELETE FROM followup_content fc USING followup f, due
    WHERE fc.followup_id = f.id AND f.reading_id = due.id
), del_rc AS (
    DELETE FROM reading_content rc USING due WHERE rc.reading_id = due.id
)
UPDATE reading r SET content_purged_at = now(), updated_at = now()
FROM due WHERE r.id = due.id;
```

### Q7 — Chiếm lease sinh luận giải (đề xuất, cần thêm cột)

Cần thêm `lease_owner varchar`, `lease_until timestamptz` vào `reading`.

```sql
UPDATE reading
SET status = 'INTERPRETING', lease_owner = :worker,
    lease_until = now() + interval '90 seconds',
    interpret_attempts = interpret_attempts + 1, updated_at = now()
WHERE id = :rid AND user_id = :uid AND interpret_attempts < 2
  AND (status = 'DRAWN'
       OR (status IN ('INTERPRETING', 'FAILED') AND lease_until < now()))
RETURNING id;
```

Hai tab cùng mở stream thì chỉ một bên chiếm được lease và gọi LLM; bên kia chỉ nghe. Khi 0 dòng mà reading chưa xong, nối vào luồng phát lại hoặc rơi về fallback KB.

### Sweeper

- Followup `PENDING` quá X phút → Q4 với `fail_reason = 'STALE'`.
- Reading `INTERPRETING` quá hạn lease → để Q7 tự chiếm lại, hoặc chuyển `FAILED`.
- Dùng ShedLock khi chạy từ 2 pod trở lên (chưa thêm vào pom).

## Mẫu code

```java
@Repository
class ReadingHotPathRepository {

    private final JdbcClient jdbcClient;

    ReadingHotPathRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    boolean claimFollowup(UUID readingId, UUID userId) {
        return jdbcClient.sql("""
                UPDATE reading
                SET followup_count = followup_count + 1, updated_at = now()
                WHERE id = :readingId AND user_id = :userId
                  AND status = 'COMPLETED' AND followup_count < :limit
                """)
                .param("readingId", readingId)
                .param("userId", userId)
                .param("limit", FollowupPolicy.MAX_PER_READING)
                .update() == 1;
    }
}
```

## Mẫu test đồng thời

Mỗi câu lệnh nóng cần một test chạy song song thật trên Postgres (Testcontainers), mỗi request một connection.

```java
@Test
void onlyOneClaimSucceedsWhenFiveRequestsRaceAtCountTwo() throws Exception {
    int threads = 5;
    try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                return repository.claimFollowup(readingId, userId);
            }));
        }
        ready.await();
        start.countDown();
        long successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successes++;
            }
        }
        assertThat(successes).isEqualTo(1);
    }
}
```

Kịch bản bắt buộc (theo spec): 20 request daily cùng user cùng ngày → 1 reading; 15 request trải với N = 3 → 3 thành công, `used = 3`; 5 câu hỏi tiếp ở `followup_count = 2` → 1 thành công; user B hỏi tiếp bài của user A → 0 dòng; 5 request hoàn lượt cho 1 followup → hoàn đúng 1 lần; job purge chạy 2 lần → lần 2 không làm gì.
