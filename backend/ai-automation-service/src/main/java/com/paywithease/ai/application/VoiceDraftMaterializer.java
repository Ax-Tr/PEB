package com.paywithease.ai.application;

import com.paywithease.ai.infrastructure.VoiceDraft;
import java.util.Map;

public interface VoiceDraftMaterializer {

  MaterializedRecord materialize(VoiceDraft draft, Map<String, Object> reviewedFields);

  record MaterializedRecord(String type, String id) {}
}
