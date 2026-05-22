package ai.timefold.solver.model.testmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import ai.timefold.solver.model.rest.definition.api.OperationId;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.openapi.api.SmallRyeOpenAPI;

@QuarkusTest
public class OpenApiTest {

    @ConfigProperty(name = "model.api.version")
    String modelApiVersion;

    @Test
    public void testOperationHasPriorityQueryParam() throws IOException {
        Path openApiFilePath = Paths.get("target/timefold/timefold-test-model_" + modelApiVersion + "/openapi/service.json");

        if (!Files.exists(openApiFilePath)) {
            fail("OpenAPI file not found at: " + openApiFilePath.toAbsolutePath());
        }

        OpenAPI openAPI;
        try (InputStream inputStream = Files.newInputStream(openApiFilePath)) {
            openAPI = SmallRyeOpenAPI.builder().withCustomStaticFile(() -> inputStream).build().model();
        }

        List<String> operationsId = List.of(OperationId.SCHEDULE, OperationId.SOLVE_DATASET, OperationId.FROM_INPUT,
                OperationId.FROM_PATCH);
        operationsId.forEach(operationId -> assertOperation(openAPI, operationId));

        // verify that paths start with api version configured
        openAPI.getPaths().getPathItems().forEach((k, v) -> assertThat(k).startsWith("/" + modelApiVersion));
    }

    private void assertOperation(OpenAPI openAPI, String operationId) {
        Optional<Operation> operation = findOperationById(openAPI, operationId);

        assertTrue(operation.isPresent(),
                "Could not find any operation with operationId: '" + operationId + "' in the OpenAPI document.");

        Optional<Parameter> parameter = operation.stream().flatMap(op -> op.getParameters().stream())
                .filter(param -> "priority".equals(param.getName()) && Parameter.In.QUERY.equals(param.getIn()))
                .findFirst();

        assertTrue(parameter.isPresent(),
                "The operation '" + operationId + "' exists, but it does not have a query parameter named 'priority'.");
    }

    private Optional<Operation> findOperationById(OpenAPI openAPI, String operationId) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null) {
            return Optional.empty();
        }

        return openAPI.getPaths().getPathItems().values().stream().map(PathItem::getPOST)
                .filter(op -> op != null && op.getOperationId().equals(operationId))
                .findFirst();
    }

}
