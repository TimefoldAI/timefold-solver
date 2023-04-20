package ai.timefold.solver.examples.common.score;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;

/**
 * @see ConstraintProviderTest
 */
@TestInstance(PER_CLASS)
@DisplayNameGeneration(SimplifiedTestNameGenerator.class)
public abstract class AbstractConstraintProviderTest<ConstraintProvider_ extends ConstraintProvider, Solution_> {

    private final ConstraintVerifier<ConstraintProvider_, Solution_> bavetConstraintVerifier = createConstraintVerifier()
            .withConstraintStreamImplType(ConstraintStreamImplType.BAVET);

    protected abstract ConstraintVerifier<ConstraintProvider_, Solution_> createConstraintVerifier();

    protected final Stream<? extends Arguments> getBavetConstraintVerifierImpl() {
        return Stream.of(
                arguments(named("BAVET", bavetConstraintVerifier)));
    }
}
