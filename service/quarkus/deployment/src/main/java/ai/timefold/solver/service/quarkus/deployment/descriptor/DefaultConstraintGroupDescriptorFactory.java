package ai.timefold.solver.service.quarkus.deployment.descriptor;

import java.util.List;

import ai.timefold.solver.service.definition.internal.descriptor.ConstraintDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ConstraintGroupDescriptor;

/**
 * Creates a default {@link ConstraintGroupDescriptor} that contains all constraints with no group defined.
 */
public final class DefaultConstraintGroupDescriptorFactory {

    final static String DEFAULT_GROUP_ID = "default-constraint-group";
    final static String DEFAULT_GROUP_NAME = "default";
    final static String DEFAULT_GROUP_DESCRIPTION = "Contains constraints with no constraint group defined";

    private DefaultConstraintGroupDescriptorFactory() {
        throw new UnsupportedOperationException();
    }

    public static ConstraintGroupDescriptor create(List<ConstraintDescriptor> constraintDescriptors) {
        return new ConstraintGroupDescriptor(DEFAULT_GROUP_ID, DEFAULT_GROUP_NAME, DEFAULT_GROUP_DESCRIPTION, null,
                constraintDescriptors.toArray(new ConstraintDescriptor[0]), null);
    };
}
