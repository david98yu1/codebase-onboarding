package com.onboarding.model;

import java.util.List;

public record QAResponse(
    String answer,
    List<String> sources
) {}
