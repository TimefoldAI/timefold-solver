package ai.timefold.solver.core.impl.score.director;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.variable.listener.support.VariableListenerSupport;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchTotal;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class CorruptionAnalyzer<Solution_, Score_ extends Score<Score_>> {

    private static final int CONSTRAINT_MATCH_DISPLAY_LIMIT = 8;

    private final InnerScoreDirector<Solution_, Score_> scoreDirector;

    public CorruptionAnalyzer(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector);
    }

    /**
     * @param uncorruptedScoreDirector never null
     * @param predicted true if the score was predicted and might have been calculated on another thread
     * @return never null
     */
    public String analyzeScore(InnerScoreDirector<Solution_, Score_> uncorruptedScoreDirector, boolean predicted) {
        if (!scoreDirector.getConstraintMatchPolicy().isEnabled()
                || !uncorruptedScoreDirector.getConstraintMatchPolicy().isEnabled()) {
            return """
                    Score corruption analysis could not be generated because either corrupted constraintMatchPolicy (%s) \
                    or uncorrupted constraintMatchPolicy (%s) is %s.
                    Check your score constraints manually.""".formatted(scoreDirector.getConstraintMatchPolicy(),
                    uncorruptedScoreDirector.getConstraintMatchPolicy(), ConstraintMatchPolicy.DISABLED);
        }

        var constraintMatchTotalMap = scoreDirector.getConstraintMatchTotalMap();
        var corruptedMap = createConstraintMatchMap(constraintMatchTotalMap.values());
        var uncorruptedConstraintMatchTotalMap = uncorruptedScoreDirector.getConstraintMatchTotalMap();
        var uncorruptedMap = createConstraintMatchMap(uncorruptedConstraintMatchTotalMap.values());

        var excessSet = new LinkedHashSet<ConstraintMatch<Score_>>();
        var missingSet = new LinkedHashSet<ConstraintMatch<Score_>>();

        uncorruptedMap.forEach((key, uncorruptedMatches) -> {
            var corruptedMatches = corruptedMap.getOrDefault(key, Collections.emptySet());
            if (corruptedMatches.isEmpty()) {
                missingSet.addAll(uncorruptedMatches);
                return;
            }
            updateExcessAndMissingConstraintMatches(uncorruptedMatches, corruptedMatches, excessSet, missingSet);
        });

        corruptedMap.forEach((key, corruptedMatches) -> {
            var uncorruptedMatches = uncorruptedMap.getOrDefault(key, Collections.emptySet());
            if (uncorruptedMatches.isEmpty()) {
                excessSet.addAll(corruptedMatches);
                return;
            }
            updateExcessAndMissingConstraintMatches(uncorruptedMatches, corruptedMatches, excessSet, missingSet);
        });

        var analysis = new StringBuilder();
        analysis.append("""
                Score corruption analysis:
                """);
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
                analysis.append("  Impossible state. Maybe this is a bug in the scoreDirector (%s).".formatted(getClass()));
            }
        }
        return analysis.toString();
    }

    private static <Score_ extends Score<Score_>> Map<Object, Set<ConstraintMatch<Score_>>>
            createConstraintMatchMap(Collection<ConstraintMatchTotal<Score_>> constraintMatchTotals) {
        var constraintMatchMap =
                LinkedHashMap.<Object, Set<ConstraintMatch<Score_>>> newLinkedHashMap(constraintMatchTotals.size() * 16);
        for (var constraintMatchTotal : constraintMatchTotals) {
            var constraintId = constraintMatchTotal.getConstraintRef();
            for (var constraintMatch : constraintMatchTotal.getConstraintMatchSet()) {
                var keyStream = Stream.builder().add(constraintId);
                var justification = constraintMatch.getJustification();
                keyStream.add(justification);
                // And now we store the reference to the constraint match.
                // Constraint Streams with indistinct tuples may produce two different match instances for the same key.
                var key = keyStream.add(constraintMatch.getScore()).build().collect(Collectors.toList());
                var added = constraintMatchMap.computeIfAbsent(key, k -> new LinkedHashSet<>(0)).add(constraintMatch);
                if (!added) {
                    throw new IllegalStateException(
                            "Score corruption because the constraintMatch (%s) was added twice for constraintMatchTotal (%s) without removal."
                                    .formatted(constraintMatch, constraintMatchTotal));
                }
            }
        }
        return constraintMatchMap;
    }

    private static <Score_ extends Score<Score_>> void updateExcessAndMissingConstraintMatches(
            Set<ConstraintMatch<Score_>> uncorruptedSet, Set<ConstraintMatch<Score_>> corruptedSet,
            Set<ConstraintMatch<Score_>> excessSet, Set<ConstraintMatch<Score_>> missingSet) {
        var uncorruptedMatchCount = (long) uncorruptedSet.size();
        var corruptedMatchCount = (long) corruptedSet.size();
        /*
         * The corrupted and uncorrupted sets contain 1+ constraint matches which are the same.
         * (= They have the same constraint, same justifications and the same score.)
         * This is perfectly fine and happens when a constraint stream produces duplicate tuples.
         *
         * It is expected that the number of these matches would be the same between the two sets.
         * When it is not, it is a sign of score corruption.
         * In that case, for visualization purposes, we need to take the excess and/or missing constraint matches,
         * and print them to the user.
         * It does not matter which ones we pick, because they are all the same.
         * So we just use the limit() below to pick the first ones.
         */
        if (corruptedMatchCount > uncorruptedMatchCount) {
            corruptedSet.stream().limit(corruptedMatchCount - uncorruptedMatchCount).forEach(excessSet::add);
        } else if (corruptedMatchCount < uncorruptedMatchCount) {
            uncorruptedSet.stream().limit(uncorruptedMatchCount - corruptedMatchCount).forEach(missingSet::add);
        }
    }

    private static <Score_ extends Score<Score_>> void appendAnalysis(StringBuilder analysis, String workingLabel,
            String suffix, Set<ConstraintMatch<Score_>> matches) {
        if (matches.isEmpty()) {
            analysis.append("""
                      The %s scoreDirector has no ConstraintMatch(es) which %s.
                    """.formatted(workingLabel, suffix));
        } else {
            analysis.append("""
                      The %s scoreDirector has %s ConstraintMatch(es) which %s:
                    """.formatted(workingLabel, matches.size(), suffix));
            matches.stream().sorted().limit(CONSTRAINT_MATCH_DISPLAY_LIMIT).forEach(match -> analysis.append("""
                        %s
                    """.formatted(match)));
            if (matches.size() >= CONSTRAINT_MATCH_DISPLAY_LIMIT) {
                analysis.append("""
                            ... %s more
                        """.formatted(matches.size() - CONSTRAINT_MATCH_DISPLAY_LIMIT));
            }
        }
    }

    /**
     * @param predicted true if the score was predicted and might have been calculated on another thread
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public String analyzeShadowVariables(boolean predicted) {
        var violationMessage =
                ((VariableListenerSupport<Solution_>) scoreDirector.getSupplyManager()).createShadowVariablesViolationMessage();
        var workingLabel = predicted ? "working" : "corrupted";
        if (violationMessage == null) {
            return """
                    Shadow variable corruption in the %s scoreDirector:
                      None""".formatted(workingLabel);
        }
        return """
                Shadow variable corruption in the %s scoreDirector:
                %s
                  Maybe there is a bug in the updater of those shadow variable(s).""".formatted(workingLabel, violationMessage);
    }

}
