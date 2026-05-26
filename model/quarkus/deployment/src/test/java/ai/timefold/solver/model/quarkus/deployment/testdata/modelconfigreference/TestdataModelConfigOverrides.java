package ai.timefold.solver.model.quarkus.deployment.testdata.modelconfigreference;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.domain.ConstraintReference;
import ai.timefold.solver.model.definition.internal.descriptor.ParameterKind;

public class TestdataModelConfigOverrides implements ModelConfigOverrides {

    public static final long DEFAULT_WEIGHT_ONE = 1L;

    @ConstraintReference(TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_NAME)
    private long constraintWeightByDefault = DEFAULT_WEIGHT_ONE;

    @ConstraintReference(value = TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_NAME, kind = ParameterKind.PARAMETER)
    private long parameter = 10L;

    @ConstraintReference(value = TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_NAME, kind = ParameterKind.WEIGHT)
    private long constraintWeightByAnnotation = DEFAULT_WEIGHT_ONE;

    // Getters added due to serialization in tests when the extension is created (@RegisterExtension)

    public long getConstraintWeightByDefault() {
        return constraintWeightByDefault;
    }

    public long getParameter() {
        return parameter;
    }

    public long getConstraintWeightByAnnotation() {
        return constraintWeightByAnnotation;
    }
}
