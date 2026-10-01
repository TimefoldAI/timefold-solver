package ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates;

import jakarta.ws.rs.Path;

import ai.timefold.solver.service.rest.api.ModelRest;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Testdata REST")
@Path("/v1/testdata")
public interface TestdataRest extends ModelRest {
}
