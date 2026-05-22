package ai.timefold.solver.model.quarkus.deployment.testdata.modelconfigreference;

import jakarta.ws.rs.Path;

import ai.timefold.solver.model.rest.api.ModelRest;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Testdata REST")
@Path("/v1/testdata")
public interface TestdataRest extends ModelRest {
}
