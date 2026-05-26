package ai.timefold.solver.model.rest.impl;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelInput;
import ai.timefold.solver.model.definition.api.data.DemoDataGenerator;
import ai.timefold.solver.model.definition.api.data.DemoMetaData;
import ai.timefold.solver.model.definition.api.domain.ModelRequest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public abstract class AbstractDemoDataResource<ModelInput_ extends ModelInput, ModelConfigurationOverrides_ extends ModelConfigOverrides> {
    private final DemoDataGenerator demoDataGenerator;

    public AbstractDemoDataResource() {
        // required for byte code generator
        this.demoDataGenerator = null;
    }

    @Inject
    public AbstractDemoDataResource(DemoDataGenerator demoDataGenerator) {
        this.demoDataGenerator = demoDataGenerator;
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "List of available demo datasets") })
    @Operation(summary = "List of available demo datasets.")
    @GET
    public List<DemoMetaData> list() {
        return demoDataGenerator.demoMetaData();
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case the given demo data does not exist",
                    content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "Demo data as a dataset") })
    @Operation(summary = "Get the demo dataset with the given identifier.")
    @GET
    @Path("/{demoDataId}")
    public ModelRequest<ModelInput_, ModelConfigurationOverrides_>
            getDemoDataRequest(@PathParam("demoDataId") @Parameter(
                    description = "ID of the demo dataset from the list of available datasets",
                    required = true) String demoDataId) {

        return (ModelRequest<ModelInput_, ModelConfigurationOverrides_>) demoDataGenerator.generateDemoData(demoDataId)
                .modelRequest();
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case the given demo data does not exist",
                    content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "200", description = "Demo data as a dataset") })
    @Operation(summary = "Get the demo dataset with the given identifier as model input only.")
    @GET
    @Path("/{demoDataId}/input")
    public ModelInput_
            getDemoDataModelInput(@PathParam("demoDataId") @Parameter(
                    description = "ID of the demo dataset from the list of available datasets",
                    required = true) String demoDataId) {

        return (ModelInput_) demoDataGenerator.generateDemoData(demoDataId).modelRequest().modelInput();
    }
}
