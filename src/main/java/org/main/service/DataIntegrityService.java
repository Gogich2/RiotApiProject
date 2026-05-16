package org.main.service;

import org.main.dto.DataIntegrityReportDto;
import org.main.dto.RankRepairResultDto;

public interface DataIntegrityService {

    DataIntegrityReportDto check();

    DataIntegrityReportDto repairMissingTimelines(int limit);

    RankRepairResultDto repairMissingRanks(int limit);
}