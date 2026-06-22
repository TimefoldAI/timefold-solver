package ai.timefold.solver.core.impl.solver.random;

import java.util.random.RandomGenerator;

public record MockRandomSource(RandomGenerator source) implements RandomSource {
    @Override
    public RandomGenerator moveUsage() {
        return source;
    }

    @Override
    public RandomGenerator acceptorUsage() {
        return source;
    }
}
