package org.main.service;

import org.main.persistence.entity.PlatformShard;
import org.main.persistence.entity.RegionRoute;

public interface IngestLogService {

    void success(String subjectType, String subjectId, String message);

    void skipped(String subjectType, String subjectId, String message);

    void failed(String subjectType, String subjectId, String message);

    void failed(String subjectType, String subjectId, Integer httpCode, Integer retries, String message);

    void log(String subjectType,
             String subjectId,
             RegionRoute region,
             PlatformShard platform,
             String status,
             Integer httpCode,
             Integer retries,
             String message);
}