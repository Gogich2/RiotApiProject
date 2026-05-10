package org.main.controller;

import java.util.Map;
import org.main.service.staticdata.DataDragonSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticDataController {

    private final DataDragonSyncService dataDragonSyncService;

    public StaticDataController(DataDragonSyncService dataDragonSyncService) {
        this.dataDragonSyncService = dataDragonSyncService;
    }

    @PostMapping("/api/static/sync")
    public Map<String, Object> syncStaticData() {
        String version = dataDragonSyncService.syncLatestVersion();

        return Map.of(
                "status", "synced",
                "version", version
        );
    }
}