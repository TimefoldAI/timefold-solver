package ai.timefold.solver.core.api.solver.change;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.impl.heuristic.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * A ProblemChange represents a change in one or more {@link PlanningEntity planning entities} or problem facts
 * of a {@link PlanningSolution}.
 * <p>
 * The {@link Solver} checks the presence of waiting problem changes after every {@link Move} evaluation.
 * If there are waiting problem changes, the {@link Solver}:
 * <ol>
 * <li>clones the last {@link PlanningSolution best solution}
 * and sets the clone as the new {@link PlanningSolution working solution}</li>
 * <li>applies every problem change keeping the order in which problem changes have been submitted;
 * after every problem change, {@link VariableListener variable listeners} are triggered
 * <li>calculates the score and makes the {@link PlanningSolution updated working solution}
 * the new {@link PlanningSolution best solution};
 * note that this {@link PlanningSolution solution} is not published via the {@link BestSolutionChangedEvent},
 * as it hasn't been initialized yet</li>
 * <li>restarts solving to fill potential uninitialized {@link PlanningEntity planning entities}</li>
 * </ol>
 * <p>
 * From the above, it follows that the solver will require some time
 * to restart solving and produce the next best solution.
 * For that reason, it is recommended to submit problem changes in batches, rather than one by one.
 * If there is not enough time between problem changes,
 * the solver may not have enough time to produce a new best solution
 * and the barrage of problem changes will have effectively caused the optimization process to stop.
 * It is impossible to say with certainty how much time is needed between problem changes,
 * as it depends on the problem size, complexity, and the speed of the {@link Score} calculation.
 * But in general, problem changes should be separated by at least a few seconds, if not minutes.
 * <p>
 * Note that the {@link Solver} clones a {@link PlanningSolution} at will.
 * Any change must be done on the problem facts and planning entities referenced by the {@link PlanningSolution}.
 * <p>
 * An example implementation, based on the Cloud balancing problem, looks as follows:
 *
 * <pre>
 * {@code
 * public class DeleteComputerProblemChange implements ProblemChange<CloudBalance> {
 *
 *     private final CloudComputer computer;
 *
 *     public DeleteComputerProblemChange(CloudComputer computer) {
 *         this.computer = computer;
 *     }
 *
 *     {@literal @Override}
 *     public void doChange(CloudBalance cloudBalance, ProblemChangeDirector problemChangeDirector) {
 *         CloudComputer workingComputer = problemChangeDirector.lookUpWorkingObjectOrFail(computer);
 *         // First remove the problem fact from all planning entities that use it
 *         for (CloudProcess process : cloudBalance.getProcessList()) {
 *             if (process.getComputer() == workingComputer) {
 *                 problemChangeDirector.changeVariable(process, "computer",
 *                         workingProcess -> workingProcess.setComputer(null));
 *             }
 *         }
 *         // A SolutionCloner does not clone problem fact lists (such as computerList), only entity lists.
 *         // Shallow clone the computerList so only the working solution is affected.
 *         ArrayList<CloudComputer> computerList = new ArrayList<>(cloudBalance.getComputerList());
 *         cloudBalance.setComputerList(computerList);
 *         // Remove the problem fact itself
 *         problemChangeDirector.removeProblemFact(workingComputer, computerList::remove);
 *     }
 * }
 * }
 * </pre>
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@FunctionalInterface
@NullMarked
public interface ProblemChange<Solution_> {

    /**
     * Do the change on the {@link PlanningSolution}.
     * Every modification to the {@link PlanningSolution} must be done via the {@link ProblemChangeDirector},
     * otherwise the {@link Score} calculation will be corrupted.
     *
     * @param workingSolution the {@link PlanningSolution working solution} which contains the problem facts
     *        (and {@link PlanningEntity planning entities}) to change
     * @param problemChangeDirector {@link ProblemChangeDirector} to perform the change through
     */
    void doChange(Solution_ workingSolution, ProblemChangeDirector problemChangeDirector);
}
