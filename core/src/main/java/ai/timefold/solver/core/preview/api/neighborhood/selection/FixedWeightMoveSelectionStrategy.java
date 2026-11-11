package ai.timefold.solver.core.preview.api.neighborhood.selection;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.NeighborhoodsConfig;
import ai.timefold.solver.core.impl.solver.random.BiasedRandomChooser;
import ai.timefold.solver.core.impl.solver.random.RandomChooser;
import ai.timefold.solver.core.impl.solver.random.UniformRandomChooser;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chooses each available {@link MoveSource} with a chance proportional to its weight.
 * A move source with weight 0 is never chosen.
 * This is the default {@link MoveSelectionStrategy}; all weights are then 1.
 * <p>
 * To give your own weights, extend this class and override {@link #getWeight(MoveSource, PhaseContext)};
 * your implementation may use {@link NeighborhoodsConfig#getMoveSelectionStrategyCustomProperties() custom properties}.
 * Use {@link MoveSource#id()} to tell the {@link MoveProvider}s apart;
 * give them your own IDs with {@link NeighborhoodBuilder#add(String, MoveProvider)}.
 * The solver reads the weights once per phase run, at the start of the phase.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class FixedWeightMoveSelectionStrategy<Solution_> implements MoveSelectionStrategy<Solution_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixedWeightMoveSelectionStrategy.class);

    private boolean exhaustedInStep = false;
    private @Nullable RandomChooser<MoveSource<Solution_>> chooser = null;

    /**
     * Called once per move source per phase run, in {@link #phaseStarted(PhaseContext)}.
     *
     * @param context valid only during this call
     * @return zero or more; not NaN or infinite; at least one move source must have a weight above zero; by default 1
     */
    protected double getWeight(MoveSource<Solution_> moveSource, PhaseContext<Solution_> context) {
        return 1.0;
    }

    @Override
    public final void phaseStarted(PhaseContext<Solution_> context) {
        var moveSourceList = List.copyOf(context.moveSources());
        var weightArray = new double[moveSourceList.size()];
        var weightTotal = 0.0;
        for (var i = 0; i < moveSourceList.size(); i++) {
            var moveSource = moveSourceList.get(i);
            var weight = getWeight(moveSource, context);
            if (weight < 0.0 || !Double.isFinite(weight)) {
                throw new IllegalArgumentException(
                        "The weight (%s) of the move source (%s) must be zero or more, and finite."
                                .formatted(weight, moveSource.id()));
            }
            weightArray[i] = weight;
            weightTotal += weight;
        }
        if (weightTotal == 0.0) {
            throw new IllegalArgumentException("""
                    All weights of the move sources (%s) are zero.
                    Maybe give at least one move source a weight above zero."""
                    .formatted(moveSourceList.stream().map(MoveSource::id).toList()));
        }
        if (!Double.isFinite(weightTotal)) {
            throw new IllegalArgumentException("""
                    The sum of the weights of the move sources (%s) is not finite.
                    Maybe use smaller weights."""
                    .formatted(moveSourceList.stream().map(MoveSource::id).toList()));
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Move selection strategy ({}) weights: {}.", getClass().getSimpleName(),
                    IntStream.range(0, moveSourceList.size())
                            .mapToObj(i -> moveSourceList.get(i).id() + "=" + weightArray[i])
                            .collect(Collectors.joining(", ")));
        }
        exhaustedInStep = false;
        // The default uses the uniform algorithm of move selector unions, a subclass the biased one.
        chooser = getClass() == FixedWeightMoveSelectionStrategy.class
                ? new UniformRandomChooser<>(moveSourceList, context.random())
                : new BiasedRandomChooser<>(moveSourceList, weightArray, context.random());
    }

    @Override
    public final void stepStarted(StepContext<Solution_> context) {
        // Without exhausted move sources, the live move sources are unchanged.
        if (exhaustedInStep) {
            exhaustedInStep = false;
            chooser.reset();
        }
    }

    @Override
    public final void moveSourceExhausted() {
        exhaustedInStep = true;
        chooser.removeChosen();
    }

    @Override
    public final @Nullable MoveSource<Solution_> nextMoveSource() {
        return chooser.choose();
    }

}
