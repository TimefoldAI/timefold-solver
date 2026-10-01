package ai.timefold.solver.service.definition.api.validation;

import java.util.List;
import java.util.UUID;

public interface Validator<Entity_> {

    List<String> validate(UUID tenantId, String id, String version, String operation, String configurationId,
            Entity_ entity);
}
