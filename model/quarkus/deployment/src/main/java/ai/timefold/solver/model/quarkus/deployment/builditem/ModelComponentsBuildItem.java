package ai.timefold.solver.model.quarkus.deployment.builditem;

import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ModelComponentsBuildItem extends SimpleBuildItem {

    private final boolean enterprise;

    private final ClassInfo solverModel;

    private final ClassInfo modelInput;

    private final ClassInfo modelOutput;

    private final ClassInfo modelConfigOverrides;

    private final ClassInfo modelInputMetrics;

    private final ClassInfo modelOutputMetrics;

    private final ClassInfo modelConstraintJustification;

    private final ClassInfo validationIssueSupertype;

    private final ClassInfo modelScoreClass;

    private final ConstraintMetaModel constraintMetaModel;

    public ModelComponentsBuildItem(boolean enterprise, ClassInfo solverModel, ClassInfo modelInput, ClassInfo modelOutput,
            ClassInfo modelConfigOverrides, ClassInfo modelInputMetrics, ClassInfo modelOutputMetrics,
            ClassInfo modelConstraintJustification, ClassInfo validationIssueSupertype, ClassInfo modelScoreClass,
            ConstraintMetaModel constraintMetaModel) {
        this.enterprise = enterprise;
        this.solverModel = solverModel;
        this.modelInput = modelInput;
        this.modelOutput = modelOutput;
        this.modelConfigOverrides = modelConfigOverrides;
        this.modelInputMetrics = modelInputMetrics;
        this.modelOutputMetrics = modelOutputMetrics;
        this.modelConstraintJustification = modelConstraintJustification;
        this.validationIssueSupertype = validationIssueSupertype;
        this.modelScoreClass = modelScoreClass;
        this.constraintMetaModel = constraintMetaModel;
    }

    public boolean isEnterprise() {
        return enterprise;
    }

    public ClassInfo getSolverModel() {
        return solverModel;
    }

    public ClassInfo getModelInput() {
        return modelInput;
    }

    public ClassInfo getModelOutput() {
        return modelOutput;
    }

    public ClassInfo getModelConfigOverrides() {
        return modelConfigOverrides;
    }

    public ClassInfo getModelInputMetrics() {
        return modelInputMetrics;
    }

    public ClassInfo getModelOutputMetrics() {
        return modelOutputMetrics;
    }

    public ClassInfo getModelConstraintJustification() {
        return modelConstraintJustification;
    }

    public ClassInfo getValidationIssueSupertype() {
        return validationIssueSupertype;
    }

    public ClassInfo getModelScoreClass() {
        return modelScoreClass;
    }

    public ConstraintMetaModel getConstraintMetaModel() {
        return constraintMetaModel;
    }

    @Override
    public String toString() {
        return "ModelComponentsBuildItem{" +
                "enterprise=" + enterprise +
                ", solverModel=" + solverModel +
                ", modelInput=" + modelInput +
                ", modelOutput=" + modelOutput +
                ", modelConfigOverrides=" + modelConfigOverrides +
                ", modelInputMetrics=" + modelInputMetrics +
                ", modelOutputMetrics=" + modelOutputMetrics +
                ", modelConstraintJustification=" + modelConstraintJustification +
                ", validationIssue=" + validationIssueSupertype +
                ", modelScoreClass=" + modelScoreClass +
                ", constraintMetaModel=" + constraintMetaModel +
                '}';
    }
}
