package vn.t3tarot.api.identity.shared.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public final class SqlTimestamps {

    private SqlTimestamps() {
    }

    public static Timestamp of(Instant instant) {
        return Timestamp.from(instant);
    }

    public static Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
