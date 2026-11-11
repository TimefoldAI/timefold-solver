package ai.timefold.solver.core.impl.move.builtin;

import java.util.List;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;

/**
 * A pillar bundled with its per-variable {@link SampleValueRanges}, computed once when the {@code .map(...)} step
 * settles for a dirty group, not once per candidate probe - the row is stable, and reused,
 * for as long as the pillar's group is unchanged, which can span many local search steps, unlike a cache field
 * owned by a move iterator that gets rebuilt every step.
 */
public record PillarWithRanges<Entity_>(Sample<Entity_> pillar, List<SampleValueRanges<Object>> rangesPerVariable) {
}
