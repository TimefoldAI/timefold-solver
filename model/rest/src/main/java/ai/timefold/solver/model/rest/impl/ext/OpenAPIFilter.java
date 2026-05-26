package ai.timefold.solver.model.rest.impl.ext;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Operation;

public class OpenAPIFilter implements OASFilter {

    @Override
    public Operation filterOperation(Operation operation) {

        Config config = ConfigProvider.getConfig();

        Optional<String> summary =
                config.getOptionalValue("timefold.rest." + operation.getOperationId() + ".summary", String.class);

        if (summary.isPresent()) {
            operation.setSummary(summary.get());
        }

        return operation;
    }

}
