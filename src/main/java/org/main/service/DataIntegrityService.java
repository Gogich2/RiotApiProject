package org.main.service;

import org.main.dto.DataIntegrityReportDto;

public interface DataIntegrityService {

    DataIntegrityReportDto check();

    DataIntegrityReportDto repairMissingTimelines(int limit);
}