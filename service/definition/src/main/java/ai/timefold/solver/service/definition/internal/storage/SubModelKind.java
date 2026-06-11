package ai.timefold.solver.service.definition.internal.storage;

import java.util.stream.Stream;

public enum SubModelKind {

    // IMPORTANT: Keep the ids in sync with resource names (paths) in the REST api
    MODEL_INPUT("input"),
    MODEL_INPUT_SOLVED("input-solved"),
    CONFIG("config"),
    UNPROCESSED_CONFIG("unprocessed-config"),
    METADATA("run"), // "run" kept for backward compatibility so that we don't lose the stored data
    VALIDATION_RESULT("validation-result"),
    INPUT_METRICS("input-metrics"),
    KPIS("kpis"),
    LOGS("logs"),
    SCORE_ANALYSIS("score-analysis"),
    SCORE_ANALYSIS_WITH_JUSTIFICATIONS("score-analysis-with-justifications"),
    WAYPOINTS("waypoints"),
    PATCH_REQUEST("patch-request");

    private String id;

    SubModelKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static SubModelKind valueOfId(String id) {

        return Stream.of(values())
                .filter(item -> item.id().equals(id)).findAny()
                .orElseThrow(() -> new IllegalStateException("Unsupported SubModelKind (%s)".formatted(id)));
    }
}
