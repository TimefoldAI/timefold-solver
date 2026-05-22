package ai.timefold.solver.model.definition.api.data;

import java.util.List;

public record DemoMetaData(String id, String shortDescription, String longDescription, List<String> tags,
        List<DemoDataConfigEntry> config) {

    public DemoMetaData {
    }

    public DemoMetaData(String id) {
        this(id, null, null, List.of(), List.of());
    }

    public DemoMetaData(String id, String shortDescription) {
        this(id, shortDescription, null, List.of(), List.of());
    }
}