package org.main.service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import org.main.persistence.entity.PlatformShard;
import org.main.persistence.entity.RegionRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class IngestLogServiceImpl implements IngestLogService {

    private static final Logger log = LoggerFactory.getLogger(IngestLogServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public IngestLogServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void success(String subjectType, String subjectId, String message) {
        log(
                subjectType,
                subjectId,
                RegionRoute.europe,
                PlatformShard.EUW1,
                "SUCCESS",
                null,
                0,
                message
        );
    }

    @Override
    public void skipped(String subjectType, String subjectId, String message) {
        log(
                subjectType,
                subjectId,
                RegionRoute.europe,
                PlatformShard.EUW1,
                "SKIPPED",
                null,
                0,
                message
        );
    }

    @Override
    public void failed(String subjectType, String subjectId, String message) {
        log(
                subjectType,
                subjectId,
                RegionRoute.europe,
                PlatformShard.EUW1,
                "FAILED",
                null,
                0,
                message
        );
    }

    @Override
    public void failed(String subjectType,
                       String subjectId,
                       Integer httpCode,
                       Integer retries,
                       String message) {
        log(
                subjectType,
                subjectId,
                RegionRoute.europe,
                PlatformShard.EUW1,
                "FAILED",
                httpCode,
                retries,
                message
        );
    }

    @Override
    public void log(String subjectType,
                    String subjectId,
                    RegionRoute region,
                    PlatformShard platform,
                    String status,
                    Integer httpCode,
                    Integer retries,
                    String message) {
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO raw.ingest_log (
                        subject_type,
                        subject_id,
                        region,
                        platform,
                        status,
                        http_code,
                        retries,
                        message,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    trim(subjectType, 100),
                    trim(subjectId, 255),
                    enumName(region),
                    enumName(platform),
                    trim(status, 50),
                    httpCode,
                    retries,
                    trim(message, 2000),
                    Timestamp.from(OffsetDateTime.now().toInstant())
            );
        } catch (Exception ex) {
            log.warn(
                    "Failed to write ingest log: subjectType='{}', subjectId='{}', status='{}'",
                    subjectType,
                    subjectId,
                    status,
                    ex
            );
        }
    }

    private String enumName(Enum<?> value) {
        if (value == null) {
            return null;
        }

        return value.name();
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}