package ai.timefold.solver.core.impl.score.director;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.change.ProblemChangeDirector;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.lookup.LookUpManager;
import ai.timefold.solver.core.impl.domain.solution.ConstraintWeightSupplier;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.support.VariableListenerSupport;
import ai.timefold.solver.core.impl.domain.variable.listener.support.violation.SolutionTracker;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.exception.CloningCorruptionException;
import ai.timefold.solver.core.impl.solver.exception.ScoreCorruptionException;
import ai.timefold.solver.core.impl.solver.exception.UndoScoreCorruptionException;
import ai.timefold.solver.core.impl.solver.exception.VariableCorruptionException;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link ScoreDirector}.
 * <p>
 * Implementation note: Extending classes should follow these guidelines:
 * <ul>
 * <li>before* method: last statement should be a call to the super method</li>
 * <li>after* method: first statement should be a call to the super method</li>
 * </ul>
 *
 * @see ScoreDirector
 */
public abstract class AbstractScoreDirector<Solution_, Score_ extends Score<Score_>, Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_>>
        implements InnerScoreDirector<Solution_, Score_>, Cloneable {

    private static final int CONSTRAINT_MATCH_DISPLAY_LIMIT = 8;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final boolean lookUpEnabled;
    private final LookUpManager lookUpManager;
    protected final ConstraintMatchPolicy constraintMatchPolicy;
    private final boolean expectShadowVariablesInCorrectState;
    protected final Factory_ scoreDirectorFactory;
    private final VariableDescriptorCache<Solution_> variableDescriptorCache;
    protected final VariableListenerSupport<Solution_> variableListenerSupport;

    private long workingEntityListRevision = 0L;
    private int workingGenuineEntityCount = 0;
    private boolean allChangesWillBeUndoneBeforeStepEnds = false;
    private long calculationCount = 0L;
    protected Solution_ workingSolution;
    private int workingInitScore = 0;
    private String undoMoveText;

    // Null when tracking disabled
    private final boolean trackingWorkingSolution;
    private final SolutionTracker<Solution_> solutionTracker;
    private final MoveDirector<Solution_> moveDirector = new MoveDirector<>(this);

    // Null when no list variable
    private final ListVariableStateSupply<Solution_> listVariableStateSupply;

    protected AbstractScoreDirector(Factory_ scoreDirectorFactory, boolean lookUpEnabled,
            ConstraintMatchPolicy constraintMatchPolicy, boolean expectShadowVariablesInCorrectState) {
        var solutionDescriptor = scoreDirectorFactory.getSolutionDescriptor();
        this.lookUpEnabled = lookUpEnabled;
        this.lookUpManager = lookUpEnabled
                ? new LookUpManager(solutionDescriptor.getLookUpStrategyResolver())
                : null;
        this.constraintMatchPolicy = constraintMatchPolicy;
        this.expectShadowVariablesInCorrectState = expectShadowVariablesInCorrectState;
        this.scoreDirectorFactory = scoreDirectorFactory;
        this.variableDescriptorCache = new VariableDescriptorCache<>(solutionDescriptor);
        this.variableListenerSupport = VariableListenerSupport.create(this);
        this.variableListenerSupport.linkVariableListeners();
        if (scoreDirectorFactory.isTrackingWorkingSolution()) {
            this.solutionTracker = new SolutionTracker<>(getSolutionDescriptor(),
                    getSupplyManager());
            this.trackingWorkingSolution = true;
        } else {
            this.solutionTracker = null;
            this.trackingWorkingSolution = false;
        }
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor == null) {
            this.listVariableStateSupply = null;
        } else {
            this.listVariableStateSupply = getSupplyManager().demand(listVariableDescriptor.getStateDemand());
        }
    }

    @Override
    public final ConstraintMatchPolicy getConstraintMatchPolicy() {
        return constraintMatchPolicy;
    }

    @Override
    public Factory_ getScoreDirectorFactory() {
        return scoreDirectorFactory;
    }

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return scoreDirectorFactory.getSolutionDescriptor();
    }

    @Override
    public ScoreDefinition<Score_> getScoreDefinition() {
        return scoreDirectorFactory.getScoreDefinition();
    }

    @Override
    public VariableDescriptorCache<Solution_> getVariableDescriptorCache() {
        return variableDescriptorCache;
    }

    @Override
    public ListVariableStateSupply<Solution_> getListVariableStateSupply(ListVariableDescriptor<Solution_> variableDescriptor) {
        var originalListVariableDescriptor = getSolutionDescriptor().getListVariableDescriptor();
        if (variableDescriptor != originalListVariableDescriptor) {
            throw new IllegalStateException(
                    "The variableDescriptor (%s) is not the same as the solution's variableDescriptor (%s)."
                            .formatted(variableDescriptor, originalListVariableDescriptor));
        }
        return Objects.requireNonNull(listVariableStateSupply);
    }

    @Override
    public boolean expectShadowVariablesInCorrectState() {
        return expectShadowVariablesInCorrectState;
    }

    @Override
    public @NonNull Solution_ getWorkingSolution() {
        return workingSolution;
    }

    protected int getWorkingInitScore() {
        return workingInitScore;
    }

    @Override
    public long getWorkingEntityListRevision() {
        return workingEntityListRevision;
    }

    @Override
    public int getWorkingGenuineEntityCount() {
        return workingGenuineEntityCount;
    }

    @Override
    public void setAllChangesWillBeUndoneBeforeStepEnds(boolean allChangesWillBeUndoneBeforeStepEnds) {
        this.allChangesWillBeUndoneBeforeStepEnds = allChangesWillBeUndoneBeforeStepEnds;
    }

    @Override
    public long getCalculationCount() {
        return calculationCount;
    }

    @Override
    public void resetCalculationCount() {
        this.calculationCount = 0L;
    }

    @Override
    public void incrementCalculationCount() {
        this.calculationCount++;
    }

    @Override
    public SupplyManager getSupplyManager() {
        return variableListenerSupport;
    }

    @Override
    public MoveDirector<Solution_> getMoveDirector() {
        return moveDirector;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    /**
     * Note: resetting the working solution does NOT substitute the calls to before/after methods of
     * the {@link ProblemChangeDirector} during {@link ProblemChange problem changes},
     * as these calls are propagated to {@link VariableListener variable listeners},
     * which update shadow variables in the {@link PlanningSolution working solution} to keep it consistent.
     */
    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        this.workingSolution = requireNonNull(workingSolution);
        var solutionDescriptor = getSolutionDescriptor();

        /*
         * Both problem facts and entities need to be asserted,
         * which requires iterating over all of them,
         * possibly many thousands of objects.
         * Providing the init score and genuine entity count requires another pass over the entities.
         * The following code does all of those operations in a single pass.
         */
        Consumer<Object> visitor = null;
        if (lookUpEnabled) {
            lookUpManager.reset();
            visitor = lookUpManager::addWorkingObject;
            // This visits all the problem facts, applying the visitor.
            solutionDescriptor.visitAllProblemFacts(workingSolution, visitor);
        }
        // This visits all the entities, applying the visitor if non-null.
        Consumer<Object> entityValidator = entity -> scoreDirectorFactory.validateEntity(this, entity);
        visitor = visitor == null ? entityValidator : visitor.andThen(entityValidator);
        var initializationStatistics = solutionDescriptor.computeInitializationStatistics(workingSolution, visitor);
        setWorkingEntityListDirty();

        workingInitScore =
                -(initializationStatistics.unassignedValueCount() + initializationStatistics.uninitializedVariableCount());
        assertInitScoreZeroOrLess();
        workingGenuineEntityCount = initializationStatistics.genuineEntityCount();
        variableListenerSupport.resetWorkingSolution();
    }

    private void assertInitScoreZeroOrLess() {
        if (workingInitScore > 0) {
            throw new IllegalStateException("""
                    workingInitScore > 0 (%d).
                    Maybe a custom move is removing more entities than were ever added?
                    """.formatted(workingInitScore));
        }
    }

    @Override
    public Score_ doAndProcessMove(Move<Solution_> move, boolean assertMoveScoreFromScratch, Consumer<Score_> moveProcessor) {
        if (trackingWorkingSolution) {
            solutionTracker.setBeforeMoveSolution(workingSolution);
        }
        try (var ephemeralMoveDirector = moveDirector.ephemeral()) {
            move.execute(ephemeralMoveDirector);
            Score_ score = calculateScore();
            if (assertMoveScoreFromScratch) {
                undoMoveText = "Undo(" + move + ")";
                if (trackingWorkingSolution) {
                    solutionTracker.setAfterMoveSolution(workingSolution);
                }
                assertWorkingScoreFromScratch(score, move);
            }
            if (moveProcessor != null) {
                moveProcessor.accept(score);
            }
            return score;
        }
    }

    @Override
    public boolean isWorkingEntityListDirty(long expectedWorkingEntityListRevision) {
        return workingEntityListRevision != expectedWorkingEntityListRevision;
    }

    protected void setWorkingEntityListDirty() {
        workingEntityListRevision++;
    }

    @Override
    public Solution_ cloneSolution(Solution_ originalSolution) {
        SolutionDescriptor<Solution_> solutionDescriptor = getSolutionDescriptor();
        Score_ originalScore = solutionDescriptor.getScore(originalSolution);
        Solution_ cloneSolution = solutionDescriptor.getSolutionCloner().cloneSolution(originalSolution);
        Score_ cloneScore = solutionDescriptor.getScore(cloneSolution);
        if (scoreDirectorFactory.isAssertClonedSolution()) {
            if (!Objects.equals(originalScore, cloneScore)) {
                throw new CloningCorruptionException("""
                        Cloning corruption: the original's score (%s) is different from the clone's score (%s).
                        Check the %s."""
                        .formatted(originalScore, cloneScore, SolutionCloner.class.getSimpleName()));
            }
            Map<Object, Object> originalEntityMap = new IdentityHashMap<>();
            solutionDescriptor.visitAllEntities(originalSolution,
                    originalEntity -> originalEntityMap.put(originalEntity, null));
            solutionDescriptor.visitAllEntities(cloneSolution, cloneEntity -> {
                if (originalEntityMap.containsKey(cloneEntity)) {
                    throw new CloningCorruptionException("""
                            Cloning corruption: the same entity (%s) is present in both the original and the clone.
                            So when a planning variable in the original solution changes, the cloned solution will change too.
                            Check the %s."""
                            .formatted(cloneEntity, SolutionCloner.class.getSimpleName()));
                }
            });
        }
        return cloneSolution;
    }

    @Override
    public void triggerVariableListeners() {
        variableListenerSupport.triggerVariableListenersInNotificationQueues();
    }

    @Override
    public void forceTriggerVariableListeners() {
        variableListenerSupport.forceTriggerAllVariableListeners(getWorkingSolution());
    }

    protected void setCalculatedScore(Score_ score) {
        getSolutionDescriptor().setScore(workingSolution, score);
        calculationCount++;
    }

    /**
     * @deprecated Unused, but kept for backward compatibility.
     */
    @Deprecated(forRemoval = true, since = "1.14.0")
    @Override
    public AbstractScoreDirector<Solution_, Score_, Factory_> clone() {
        throw new UnsupportedOperationException("Cloning score directors is not supported.");
    }

    @Override
    public InnerScoreDirector<Solution_, Score_> createChildThreadScoreDirector(ChildThreadType childThreadType) {
        if (childThreadType == ChildThreadType.PART_THREAD) {
            var childThreadScoreDirector = (AbstractScoreDirector<Solution_, Score_, Factory_>) scoreDirectorFactory
                    .buildDerivedScoreDirector(lookUpEnabled, constraintMatchPolicy);
            // ScoreCalculationCountTermination takes into account previous phases
            // but the calculationCount of partitions is maxed, not summed.
            childThreadScoreDirector.calculationCount = calculationCount;
            return childThreadScoreDirector;
        } else if (childThreadType == ChildThreadType.MOVE_THREAD) {
            var childThreadScoreDirector = (AbstractScoreDirector<Solution_, Score_, Factory_>) scoreDirectorFactory
                    .buildDerivedScoreDirector(true, constraintMatchPolicy);
            childThreadScoreDirector.setWorkingSolution(cloneWorkingSolution());
            return childThreadScoreDirector;
        } else {
            throw new IllegalStateException("The childThreadType (" + childThreadType + ") is not implemented.");
        }
    }

    @Override
    public void close() {
        workingSolution = null;
        workingInitScore = 0;
        if (lookUpEnabled) {
            lookUpManager.reset();
        }
        if (listVariableStateSupply != null) {
            getSupplyManager().cancel(listVariableStateSupply.getSourceVariableDescriptor().getStateDemand());
        }
        variableListenerSupport.close();
    }

    // ************************************************************************
    // Entity/variable add/change/remove methods
    // ************************************************************************

    public void beforeEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        variableListenerSupport.beforeEntityAdded(entityDescriptor, entity);
    }

    public void afterEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        workingInitScore -= entityDescriptor.countUninitializedVariables(entity);
        if (entityDescriptor.isGenuine()) {
            workingGenuineEntityCount++;
        }
        if (lookUpEnabled) {
            lookUpManager.addWorkingObject(entity);
        }
        if (!allChangesWillBeUndoneBeforeStepEnds) {
            setWorkingEntityListDirty();
        }
    }

    @Override
    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        if (variableDescriptor.isGenuineAndUninitialized(entity)) {
            workingInitScore++;
        }
        assertInitScoreZeroOrLess();
        variableListenerSupport.beforeVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        if (variableDescriptor.isGenuineAndUninitialized(entity)) {
            workingInitScore--;
        }
    }

    @Override
    public void changeVariableFacade(VariableDescriptor<Solution_> variableDescriptor, Object entity, Object newValue) {
        beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void beforeListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        // Do nothing
    }

    @Override
    public void afterListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        if (!variableDescriptor.allowsUnassignedValues()) { // Unassigned elements don't count towards the initScore here.
            workingInitScore++;
            assertInitScoreZeroOrLess();
        }
    }

    @Override
    public void beforeListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        // Do nothing
    }

    @Override
    public void afterListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        if (!variableDescriptor.allowsUnassignedValues()) { // Unassigned elements don't count towards the initScore here.
            workingInitScore--;
        }
        variableListenerSupport.afterElementUnassigned(variableDescriptor, element);
    }

    @Override
    public void beforeListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        // Pinning is implemented in generic moves, but custom moves need to take it into account as well.
        // This fail-fast exists to detect situations where pinned things are being moved, in case of user error.
        if (variableDescriptor.isElementPinned(getWorkingSolution(), entity, fromIndex)) {
            throw new IllegalStateException(
                    """
                            Attempting to change list variable (%s) on an entity (%s) in range [%d, %d), which is partially or entirely pinned.
                            This is most likely a bug in a move.
                            Maybe you are using an improperly implemented custom move?"""
                            .formatted(variableDescriptor, entity, fromIndex, toIndex));
        }
        variableListenerSupport.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor,
            Object entity, int fromIndex, int toIndex) {
        variableListenerSupport.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    public void beforeEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        workingInitScore += entityDescriptor.countUninitializedVariables(entity);
        assertInitScoreZeroOrLess();
        variableListenerSupport.beforeEntityRemoved(entityDescriptor, entity);
    }

    public void afterEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        if (entityDescriptor.isGenuine()) {
            workingGenuineEntityCount--;
        }
        if (lookUpEnabled) {
            lookUpManager.removeWorkingObject(entity);
        }
        if (!allChangesWillBeUndoneBeforeStepEnds) {
            setWorkingEntityListDirty();
        }
    }

    // ************************************************************************
    // Problem fact add/change/remove methods
    // ************************************************************************

    @Override
    public void beforeProblemFactAdded(Object problemFact) {
        // Do nothing
    }

    @Override
    public void afterProblemFactAdded(Object problemFact) {
        if (lookUpEnabled) {
            lookUpManager.addWorkingObject(problemFact);
        }
        variableListenerSupport.resetWorkingSolution(); // TODO do not nuke the variable listeners
    }

    @Override
    public void beforeProblemPropertyChanged(Object problemFactOrEntity) {
        // Do nothing
    }

    @Override
    public void afterProblemPropertyChanged(Object problemFactOrEntity) {
        if (isConstraintConfiguration(problemFactOrEntity)) {
            setWorkingSolution(workingSolution); // Nuke everything and recalculate, constraint weights have changed.
        } else {
            variableListenerSupport.resetWorkingSolution(); // TODO do not nuke the variable listeners
        }
    }

    @Override
    public void beforeProblemFactRemoved(Object problemFact) {
        if (isConstraintConfiguration(problemFact)) {
            throw new IllegalStateException("Attempted to remove constraint configuration (" + problemFact +
                    ") from solution (" + workingSolution + ").\n" +
                    "Maybe use before/afterProblemPropertyChanged(...) instead.");
        }
    }

    @Override
    public void afterProblemFactRemoved(Object problemFact) {
        if (lookUpEnabled) {
            lookUpManager.removeWorkingObject(problemFact);
        }
        variableListenerSupport.resetWorkingSolution(); // TODO do not nuke the variable listeners
    }

    @Override
    public <E> @Nullable E lookUpWorkingObject(@Nullable E externalObject) {
        if (!lookUpEnabled) {
            throw new IllegalStateException("When lookUpEnabled (" + lookUpEnabled
                    + ") is disabled in the constructor, this method should not be called.");
        }
        return lookUpManager.lookUpWorkingObject(externalObject);
    }

    @Override
    public <E> @Nullable E lookUpWorkingObjectOrReturnNull(@Nullable E externalObject) {
        if (!lookUpEnabled) {
            throw new IllegalStateException("When lookUpEnabled (" + lookUpEnabled
                    + ") is disabled in the constructor, this method should not be called.");
        }
        return lookUpManager.lookUpWorkingObjectOrReturnNull(externalObject);
    }

    // ************************************************************************
    // Assert methods
    // ************************************************************************

    @Override
    public void assertExpectedWorkingScore(Score_ expectedWorkingScore, Object completedAction) {
        Score_ workingScore = calculateScore();
        if (!expectedWorkingScore.equals(workingScore)) {
            throw new ScoreCorruptionException("""
                    Score corruption (%s): the expectedWorkingScore (%s) is not the workingScore (%s) \
                    after completedAction (%s)."""
                    .formatted(expectedWorkingScore.subtract(workingScore).toShortString(),
                            expectedWorkingScore, workingScore, completedAction));
        }
    }

    @Override
    public void assertShadowVariablesAreNotStale(Score_ expectedWorkingScore, Object completedAction) {
        String violationMessage = variableListenerSupport.createShadowVariablesViolationMessage();
        if (violationMessage != null) {
            throw new VariableCorruptionException("""
                    %s corruption after completedAction (%s):
                    %s"""
                    .formatted(VariableListener.class.getSimpleName(), completedAction, violationMessage));
        }

        Score_ workingScore = calculateScore();
        if (!expectedWorkingScore.equals(workingScore)) {
            assertWorkingScoreFromScratch(workingScore,
                    "assertShadowVariablesAreNotStale(" + expectedWorkingScore + ", " + completedAction + ")");
            throw new VariableCorruptionException("""
                    Impossible %s corruption (%s): the expectedWorkingScore (%s) is not the workingScore (%s) \
                    after all %s were triggered without changes to the genuine variables after completedAction (%s).
                    All the shadow variable values are still the same, so this is impossible.
                    Maybe run with %s if you haven't already, to fail earlier."""
                    .formatted(VariableListener.class.getSimpleName(),
                            expectedWorkingScore.subtract(workingScore).toShortString(),
                            expectedWorkingScore, workingScore, VariableListener.class.getSimpleName(), completedAction,
                            EnvironmentMode.TRACKED_FULL_ASSERT));
        }
    }

    /**
     * @param predicted true if the score was predicted and might have been calculated on another thread
     * @return never null
     */
    protected String buildShadowVariableAnalysis(boolean predicted) {
        String violationMessage = variableListenerSupport.createShadowVariablesViolationMessage();
        String workingLabel = predicted ? "working" : "corrupted";
        if (violationMessage == null) {
            return """
                    Shadow variable corruption in the %s scoreDirector:
                      None"""
                    .formatted(workingLabel);
        }
        return """
                Shadow variable corruption in the %s scoreDirector:
                %s
                  Maybe there is a bug in the %s of those shadow variable(s)."""
                .formatted(workingLabel, violationMessage, VariableListener.class.getSimpleName());
    }

    @Override
    public void assertWorkingScoreFromScratch(Score_ workingScore, Object completedAction) {
        assertScoreFromScratch(workingScore, completedAction, false);
    }

    @Override
    public void assertPredictedScoreFromScratch(Score_ workingScore, Object completedAction) {
        assertScoreFromScratch(workingScore, completedAction, true);
    }

    private void assertScoreFromScratch(Score_ score, Object completedAction, boolean predicted) {
        InnerScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory = scoreDirectorFactory
                .getAssertionScoreDirectorFactory();
        if (assertionScoreDirectorFactory == null) {
            assertionScoreDirectorFactory = scoreDirectorFactory;
        }
        try (var uncorruptedScoreDirector =
                assertionScoreDirectorFactory.buildDerivedScoreDirector(false, ConstraintMatchPolicy.ENABLED)) {
            uncorruptedScoreDirector.setWorkingSolution(workingSolution);
            Score_ uncorruptedScore = uncorruptedScoreDirector.calculateScore();
            if (!score.equals(uncorruptedScore)) {
                String scoreCorruptionAnalysis = buildScoreCorruptionAnalysis(uncorruptedScoreDirector, predicted);
                String shadowVariableAnalysis = buildShadowVariableAnalysis(predicted);
                throw new ScoreCorruptionException("""
                        Score corruption (%s): the %s (%s) is not the uncorruptedScore (%s) after completedAction (%s):
                        %s
                        %s"""
                        .formatted(score.subtract(uncorruptedScore).toShortString(),
                                predicted ? "predictedScore" : "workingScore", score, uncorruptedScore, completedAction,
                                scoreCorruptionAnalysis, shadowVariableAnalysis));
            }
        }
    }

    @Override
    public void assertExpectedUndoMoveScore(Move<Solution_> move, Score_ beforeMoveScore, SolverLifecyclePoint executionPoint) {
        Score_ undoScore = calculateScore();
        if (!undoScore.equals(beforeMoveScore)) {
            logger.trace("        Corruption detected. Diagnosing...");

            if (trackingWorkingSolution) {
                solutionTracker.setAfterUndoSolution(workingSolution);
            }
            // Precondition: assert that there are probably no corrupted constraints
            assertWorkingScoreFromScratch(undoScore, undoMoveText);
            // Precondition: assert that shadow variables aren't stale after doing the undoMove
            assertShadowVariablesAreNotStale(undoScore, undoMoveText);
            String corruptionDiagnosis = "";
            if (trackingWorkingSolution) {
                // Recalculate all shadow variables from scratch.
                // We cannot set all shadow variables to null, since some variable listeners
                // may expect them to be non-null.
                // Instead, we just simulate a change to all genuine variables.
                variableListenerSupport.forceTriggerAllVariableListeners(workingSolution);
                solutionTracker.setUndoFromScratchSolution(workingSolution);

                // Also calculate from scratch for the before solution, since it might
                // have been corrupted but was only detected now
                solutionTracker.restoreBeforeSolution();
                variableListenerSupport.forceTriggerAllVariableListeners(workingSolution);
                solutionTracker.setBeforeFromScratchSolution(workingSolution);

                corruptionDiagnosis = solutionTracker.buildScoreCorruptionMessage();
            }
            String scoreDifference = undoScore.subtract(beforeMoveScore).toShortString();
            String corruptionMessage = """
                    UndoMove corruption (%s):
                       the beforeMoveScore (%s) is not the undoScore (%s),
                       which is the uncorruptedScore (%s) of the workingSolution.

                    Corruption diagnosis:
                    %s

                    1) Enable EnvironmentMode %s (if you haven't already)
                       to fail-faster in case of a score corruption or variable listener corruption.
                       Let the solver run until it reaches the same point in its lifecycle (%s),
                       even though it may take a very long time.
                       If the solver throws an exception before reaching that point,
                       there may be yet another problem that needs to be fixed.

                    2) If you use custom moves, check the Move.createUndoMove(...) method of the custom move class (%s).
                       The move (%s) might have a corrupted undoMove (%s).

                    3) If you use custom %ss,
                       check them for shadow variables that are used by score constraints
                       that could cause the scoreDifference (%s)."""
                    .formatted(scoreDifference, beforeMoveScore, undoScore, undoScore,
                            corruptionDiagnosis,
                            EnvironmentMode.TRACKED_FULL_ASSERT, executionPoint,
                            move.getClass().getSimpleName(), move, undoMoveText,
                            VariableListener.class.getSimpleName(), scoreDifference);

            if (trackingWorkingSolution) {
                throw new UndoScoreCorruptionException(corruptionMessage,
                        solutionTracker.getBeforeMoveSolution(),
                        solutionTracker.getAfterMoveSolution(),
                        solutionTracker.getAfterUndoSolution());
            } else {
                throw new ScoreCorruptionException(corruptionMessage);
            }
        }
    }

    /**
     * @param uncorruptedScoreDirector never null
     * @param predicted true if the score was predicted and might have been calculated on another thread
     * @return never null
     */
    protected String buildScoreCorruptionAnalysis(InnerScoreDirector<Solution_, Score_> uncorruptedScoreDirector,
            boolean predicted) {
        if (!getConstraintMatchPolicy().isEnabled() || !uncorruptedScoreDirector.getConstraintMatchPolicy().isEnabled()) {
            return """
                    Score corruption analysis could not be generated because either corrupted constraintMatchPolicy (%s) \
                    or uncorrupted constraintMatchPolicy (%s) is %s.
                      Check your score constraints manually."""
                    .formatted(constraintMatchPolicy, uncorruptedScoreDirector.getConstraintMatchPolicy(),
                            ConstraintMatchPolicy.DISABLED);
        }

        var corruptedAnalysis = buildScoreAnalysis(ScoreAnalysisFetchPolicy.FETCH_ALL, ScoreAnalysisMode.SCORE_CORRUPTION);
        var uncorruptedAnalysis = uncorruptedScoreDirector.buildScoreAnalysis(ScoreAnalysisFetchPolicy.FETCH_ALL,
                ScoreAnalysisMode.SCORE_CORRUPTION);

        var excessSet = new LinkedHashSet<MatchAnalysis<Score_>>();
        var missingSet = new LinkedHashSet<MatchAnalysis<Score_>>();

        uncorruptedAnalysis.constraintMap().forEach((constraintRef, uncorruptedConstraintAnalysis) -> {
            var uncorruptedConstraintMatches = emptyMatchAnalysisIfNull(uncorruptedConstraintAnalysis);
            var corruptedConstraintMatches = emptyMatchAnalysisIfNull(corruptedAnalysis.constraintMap()
                    .get(constraintRef));
            if (corruptedConstraintMatches.isEmpty()) {
                missingSet.addAll(uncorruptedConstraintMatches);
            } else {
                updateExcessAndMissingConstraintMatches(uncorruptedConstraintMatches, corruptedConstraintMatches, excessSet,
                        missingSet);
            }
        });

        corruptedAnalysis.constraintMap().forEach((constraintRef, corruptedConstraintAnalysis) -> {
            var corruptedConstraintMatches = emptyMatchAnalysisIfNull(corruptedConstraintAnalysis);
            var uncorruptedConstraintMatches = emptyMatchAnalysisIfNull(uncorruptedAnalysis.constraintMap()
                    .get(constraintRef));
            if (uncorruptedConstraintMatches.isEmpty()) {
                excessSet.addAll(corruptedConstraintMatches);
            } else {
                updateExcessAndMissingConstraintMatches(uncorruptedConstraintMatches, corruptedConstraintMatches, excessSet,
                        missingSet);
            }
        });

        var analysis = new StringBuilder();
        analysis.append("Score corruption analysis:\n");
        // If predicted, the score calculation might have happened on another thread, so a different ScoreDirector
        // so there is no guarantee that the working ScoreDirector is the corrupted ScoreDirector
        var workingLabel = predicted ? "working" : "corrupted";
        appendAnalysis(analysis, workingLabel, "should not be there", excessSet);
        appendAnalysis(analysis, workingLabel, "are missing", missingSet);
        if (!missingSet.isEmpty() || !excessSet.isEmpty()) {
            analysis.append("""
                      Maybe there is a bug in the score constraints of those ConstraintMatch(s).
                      Maybe a score constraint doesn't select all the entities it depends on,
                        but discovers some transitively through a reference from the selected entity.
                        This corrupts incremental score calculation,
                        because the constraint is not re-evaluated if the transitively discovered entity changes.
                    """.stripTrailing());
        } else {
            if (predicted) {
                analysis.append("""
                          If multi-threaded solving is active:
                            - the working scoreDirector is probably not the corrupted scoreDirector.
                            - maybe the rebase() method of the move is bugged.
                            - maybe a VariableListener affected the moveThread's workingSolution after doing and undoing a move,
                              but this didn't happen here on the solverThread, so we can't detect it.
                        """.stripTrailing());
            } else {
                analysis.append("  Impossible state. Maybe this is a bug in the scoreDirector (%s)."
                        .formatted(getClass()));
            }
        }
        return analysis.toString();
    }

    private static <Score_ extends Score<Score_>> List<MatchAnalysis<Score_>>
            emptyMatchAnalysisIfNull(ConstraintAnalysis<Score_> constraintAnalysis) {
        if (constraintAnalysis == null) {
            return Collections.emptyList();
        }
        return Objects.requireNonNullElse(constraintAnalysis.matches(), Collections.emptyList());
    }

    private void appendAnalysis(StringBuilder analysis, String workingLabel, String suffix,
            Set<MatchAnalysis<Score_>> matches) {
        if (matches.isEmpty()) {
            analysis.append("""
                      The %s scoreDirector has no ConstraintMatch(es) which %s.
                    """.formatted(workingLabel, suffix));
        } else {
            analysis.append("""
                      The %s scoreDirector has %s ConstraintMatch(es) which %s:
                    """.formatted(workingLabel, matches.size(), suffix));
            matches.stream().sorted().limit(CONSTRAINT_MATCH_DISPLAY_LIMIT)
                    .forEach(match -> analysis.append("""
                                %s/%s=%s
                            """.formatted(match.constraintRef().constraintId(), match.justification(), match.score())));
            if (matches.size() >= CONSTRAINT_MATCH_DISPLAY_LIMIT) {
                analysis.append("""
                            ... %s more
                        """.formatted(matches.size() - CONSTRAINT_MATCH_DISPLAY_LIMIT));
            }
        }
    }

    private void updateExcessAndMissingConstraintMatches(List<MatchAnalysis<Score_>> uncorruptedList,
            List<MatchAnalysis<Score_>> corruptedList, Set<MatchAnalysis<Score_>> excessSet,
            Set<MatchAnalysis<Score_>> missingSet) {
        iterateAndAddIfFound(corruptedList, uncorruptedList, excessSet);
        iterateAndAddIfFound(uncorruptedList, corruptedList, missingSet);
    }

    private void iterateAndAddIfFound(List<MatchAnalysis<Score_>> referenceList, List<MatchAnalysis<Score_>> lookupList,
            Set<MatchAnalysis<Score_>> targetSet) {
        if (referenceList.isEmpty()) {
            return;
        }
        var lookupSet = new LinkedHashSet<>(lookupList); // Guaranteed to not contain duplicates anyway.
        for (var reference : referenceList) {
            if (!lookupSet.contains(reference)) {
                targetSet.add(reference);
            }
        }
    }

    protected boolean isConstraintConfiguration(Object problemFactOrEntity) {
        SolutionDescriptor<Solution_> solutionDescriptor = scoreDirectorFactory.getSolutionDescriptor();
        ConstraintWeightSupplier<Solution_, Score_> constraintWeightSupplier = solutionDescriptor.getConstraintWeightSupplier();
        if (constraintWeightSupplier == null) {
            return false;
        }
        return constraintWeightSupplier.getProblemFactClass()
                .isInstance(problemFactOrEntity);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + calculationCount + ")";
    }

}
