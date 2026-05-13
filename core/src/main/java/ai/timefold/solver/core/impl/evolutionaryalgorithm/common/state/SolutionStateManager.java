package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.EvolutionaryDecider;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * Base contract for defining solution state managers used by the {@link EvolutionaryDecider evolutionary decider} to save and
 * restore states.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 * @param <State_> the solution state type
 */
@NullMarked
public interface SolutionStateManager<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>> {

    State_ saveSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector, boolean saveAssigned);

    State_ saveSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector, Individual<Solution_, Score_> individual);

    void restoreSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector, State_ stateToRestore);

}
