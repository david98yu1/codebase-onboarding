package com.onboarding.model;

import java.util.Map;

public record OverviewResponse(
    String repoId,
    String repoOverview,
    Map<String, String> directorySummaries,
    Map<String, String> fileSummaries
) {}
