package ai.timefold.solver.service.quarkus.deployment.rest;

import ai.timefold.solver.service.definition.api.validation.ModelValidator;
import ai.timefold.solver.service.definition.impl.solver.SolverWorkerFacade;
import ai.timefold.solver.service.definition.impl.validation.ValidationIssueTypeCatalog;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelComponentsBuildItem;

import org.jboss.jandex.DotName;

import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;
import io.quarkus.gizmo.Type.ParameterizedType;

public class DefaultModelResourceTypeInfo implements ModelResourceTypeInfo {

    @Override
    public DotName modelResourceSuperClassName() {
        return DotName.createSimple("ai.timefold.solver.service.rest.impl.AbstractModelAPIResource");
    }

    @Override
    public ParameterizedType modelResourceSuperClassType(ModelComponentsBuildItem modelComponents) {
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
                .addParameterType(Type.classType(SolverWorkerFacade.class))
                .addParameterType(Type.classType(ValidationIssueTypeCatalog.class))
                .build();
    }

    @Override
    public String[] constructorParameterTypes(ModelComponentsBuildItem modelComponents) {
        return new String[] { ModelValidator.class.getCanonicalName(),
                SolverWorkerFacade.class.getCanonicalName(),
                ValidationIssueTypeCatalog.class.getCanonicalName(),
        };
    }
}
