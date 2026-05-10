package org.main.dto;

public record DataIntegrityReportDto(
        long matchesTotal,
        long timelinesRawTotal,
        long framesTotal,
        long eventsTotal,
        long matchesWithoutTimelineRaw,
        long timelinesWithoutFrames,
        long timelinesWithoutEvents,
        boolean valid
) {
}