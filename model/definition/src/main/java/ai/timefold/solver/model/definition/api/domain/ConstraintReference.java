package ai.timefold.solver.model.definition.api.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ai.timefold.solver.model.definition.internal.descriptor.ModelConfigDescriptor;
import ai.timefold.solver.model.definition.internal.descriptor.ParameterKind;

/**
 * Annotates a field in a {@link ModelConfigDescriptor} to reference a constraint.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConstraintReference {

    String KIND_FIELD = "kind";

    /**
     * The constraint name as defined in the {@link ai.timefold.solver.core.api.score.stream.ConstraintProvider} implementation.
     */
    String value();

    ParameterKind kind() default ParameterKind.WEIGHT;
}
