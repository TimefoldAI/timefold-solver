package ai.timefold.solver.core.impl.testutil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.condition.DisabledIf;

/**
 * Used to disable tests that take advantage of features which are not available in the Red Hat build of Timefold.
 * <p>
 * Case in point: timefold-solver-constraint-streams-bavet module may not be built at all,
 * and all tests that expect its presence need to be disabled.
 */
@Retention(RetentionPolicy.RUNTIME)
// Implemented in this roundabout way because @DisabledIfSystemProperty has issues with null properties.
@DisabledIf("ai.timefold.solver.core.impl.testutil.DisabledInProductizationCheck#isProductized")
public @interface DisabledInProductization {

    // TODO remove this when Bavet is productized

}
