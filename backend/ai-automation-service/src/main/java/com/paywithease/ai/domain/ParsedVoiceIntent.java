package com.paywithease.ai.domain;

import java.util.List;
import java.util.Map;

public record ParsedVoiceIntent(
    VoiceIntent intent,
    Map<String, Object> fields,
    List<String> missingFields,
    double confidence) {}
