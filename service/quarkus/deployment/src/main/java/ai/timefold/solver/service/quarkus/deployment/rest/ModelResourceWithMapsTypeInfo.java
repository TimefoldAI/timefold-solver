package ai.timefold.solver.service.quarkus.deployment.rest;

import java.util.Map;

import ai.timefold.solver.service.definition.api.validation.ModelValidator;
import ai.timefold.solver.service.definition.impl.validation.ValidationIssueTypeCatalog;
import ai.timefold.solver.service.definition.internal.events.DatasetCreatedEvent;
import ai.timefold.solver.service.definition.internal.events.DatasetValidateComputeCommand;
import ai.timefold.solver.service.definition.internal.events.SolveStartCommand;
import ai.timefold.solver.service.definition.internal.events.SolveTerminateCommand;
import ai.timefold.solver.service.definition.internal.events.SolverChannels;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelComponentsBuildItem;

import org.jboss.jandex.DotName;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;

public class ModelResourceWithMapsTypeInfo implements ModelResourceTypeInfo {

    @Override
    public DotName modelResourceSuperClassName() {
        return DotName.createSimple("ai.timefold.solver.service.maps.service.rest.impl.AbstractMapsModelAPIResource");
    }

    @Override
    public Type.ParameterizedType modelResourceSuperClassType(ModelComponentsBuildItem modelComponents) {
        return Type.parameterizedType(Type.classType(modelResourceSuperClassName()),
                Type.classType(modelComponents.getModelInput().name()),
                Type.classType(modelComponents.getModelOutput().name()),
                Type.classType(modelComponents.getModelConfigOverrides().name()),
                Type.classType(modelComponents.getModelScoreClass().name()),
                Type.classType(modelComponents.getModelInputMetrics().name()),
                Type.classType(modelComponents.getModelOutputMetrics().name()),
                Type.classType(modelComponents.getModelConstraintJustification().name()),
                Type.classType(modelComponents.getValidationIssueSupertype().name()));
    }

    @Override
    public String constructorSignature(ModelComponentsBuildItem modelComponents) {
        return SignatureBuilder.forMethod()
                .addParameterType(Type.parameterizedType(Type.classType(ModelValidator.class),
                        Type.classType(modelComponents.getModelInput().name()),
                        Type.classType(modelComponents.getModelConfigOverrides().name())))
                .addParameterType(Type.parameterizedType(Type.classType(AbstractStorageService.class),
                        Type.classType(modelComponents.getModelInput().name()),
                        Type.classType(modelComponents.getModelConfigOverrides().name()),
                        Type.classType(modelComponents.getModelInputMetrics().name()),
                        Type.classType(modelComponents.getModelOutputMetrics().name()),
                        Type.classType(modelComponents.getModelOutput().name()),
                        Type.classType(modelComponents.getModelScoreClass().name()),
                        Type.classType(modelComponents.getModelConstraintJustification().name())))
                .addParameterType(
                        Type.parameterizedType(Type.classType(EMITTER_CLASS_NAME),
                                Type.classType(DatasetCreatedEvent.class)))
                .addParameterType(
                        Type.parameterizedType(Type.classType(EMITTER_CLASS_NAME),
                                Type.classType(DatasetValidateComputeCommand.class)))
                .addParameterType(
                        Type.parameterizedType(Type.classType(EMITTER_CLASS_NAME),
                                Type.classType(SolveStartCommand.class)))
                .addParameterType(
                        Type.parameterizedType(Type.classType(MUTINY_EMITTER_CLASS_NAME),
                                Type.classType(SolveTerminateCommand.class)))
                .addParameterType(Type.classType(ObjectMapper.class))
                .addParameterType(Type.classType(ValidationIssueTypeCatalog.class))
                .addParameterType(Type.classType(WAYPOINT_SERVICE.toString()))
                .build();
    }

    @Override
    public String[] constructorParameterTypes(ModelComponentsBuildItem modelComponents) {
        return new String[] { ModelValidator.class.getCanonicalName(),
                AbstractStorageService.class.getCanonicalName(),
                EMITTER_CLASS_NAME,
                EMITTER_CLASS_NAME,
                EMITTER_CLASS_NAME,
                MUTINY_EMITTER_CLASS_NAME,
                ObjectMapper.class.getCanonicalName(),
                ValidationIssueTypeCatalog.class.getCanonicalName(),
                WAYPOINT_SERVICE.toString()
        };
    }

    @Override
    public Map<String, Integer> channelConstructorParameterIndices() {
        return Map.of(
                SolverChannels.DATASET_CREATED, 2,
                SolverChannels.DATASET_VALIDATE_COMPUTE, 3,
                SolverChannels.START, 4,
                SolverChannels.TERMINATE, 5);
    }
}
