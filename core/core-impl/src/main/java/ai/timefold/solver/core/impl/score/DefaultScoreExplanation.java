package ai.timefold.solver.core.impl.score;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class DefaultScoreExplanation<Solution_, Score_ extends Score<Score_>>
        implements ScoreExplanation<Solution_, Score_> {

    private static final int DEFAULT_SCORE_EXPLANATION_INDICTMENT_LIMIT = 5;
    private static final int DEFAULT_SCORE_EXPLANATION_CONSTRAINT_MATCH_LIMIT = 2;

    private final Solution_ solution;
    private final Score_ score;
    private final Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap;
    private final List<ConstraintJustification> constraintJustificationList;
    private final Map<Object, Indictment<Score_>> indictmentMap;
    private final AtomicReference<String> summary = new AtomicReference<>(); // Will be calculated lazily.

    public static <Score_ extends Score<Score_>> String explainScore(Score_ workingScore,
            Collection<ConstraintMatchTotal<Score_>> constraintMatchTotalCollection,
            Collection<Indictment<Score_>> indictmentCollection) {
        return explainScore(workingScore, constraintMatchTotalCollection, indictmentCollection,
                DEFAULT_SCORE_EXPLANATION_INDICTMENT_LIMIT, DEFAULT_SCORE_EXPLANATION_CONSTRAINT_MATCH_LIMIT);
    }

    public static <Score_ extends Score<Score_>> String explainScore(Score_ workingScore,
            Collection<ConstraintMatchTotal<Score_>> constraintMatchTotalCollection,
            Collection<Indictment<Score_>> indictmentCollection, int indictmentLimit, int constraintMatchLimit) {
        StringBuilder scoreExplanation =
                new StringBuilder((constraintMatchTotalCollection.size() + 4 + 2 * indictmentLimit) * 80);
        scoreExplanation.append("Explanation of score (").append(workingScore).append("):\n");
        scoreExplanation.append("    Constraint match totals:\n");
        Comparator<ConstraintMatchTotal<Score_>> constraintMatchTotalComparator = comparing(ConstraintMatchTotal::getScore);
        Comparator<ConstraintMatch<Score_>> constraintMatchComparator = comparing(ConstraintMatch::getScore);
        constraintMatchTotalCollection.stream()
                .sorted(constraintMatchTotalComparator)
                .forEach(constraintMatchTotal -> {
                    Set<ConstraintMatch<Score_>> constraintMatchSet = constraintMatchTotal.getConstraintMatchSet();
                    scoreExplanation
                            .append("        ").append(constraintMatchTotal.getScore().toShortString())
                            .append(": constraint (").append(constraintMatchTotal.getConstraintRef().constraintName())
                            .append(") has ").append(constraintMatchSet.size()).append(" matches:\n");
                    constraintMatchSet.stream()
                            .sorted(constraintMatchComparator)
                            .limit(constraintMatchLimit)
                            .forEach(constraintMatch -> scoreExplanation
                                    .append("            ")
                                    .append(constraintMatch.getScore().toShortString())
                                    .append(": justifications (")
                                    .append(constraintMatch.getJustification().toString())
                                    .append(")\n"));
                    if (constraintMatchSet.size() > constraintMatchLimit) {
                        scoreExplanation.append("            ...\n");
                    }
                });

        int indictmentCount = indictmentCollection.size();
        if (indictmentLimit < indictmentCount) {
            scoreExplanation.append("    Indictments (top ").append(indictmentLimit)
                    .append(" of ").append(indictmentCount).append("):\n");
        } else {
            scoreExplanation.append("    Indictments:\n");
        }
        Comparator<Indictment<Score_>> indictmentComparator = comparing(Indictment::getScore);
        Comparator<ConstraintMatch<Score_>> constraintMatchScoreComparator = comparing(ConstraintMatch::getScore);
        indictmentCollection.stream()
                .sorted(indictmentComparator)
                .limit(indictmentLimit)
                .forEach(indictment -> {
                    Set<ConstraintMatch<Score_>> constraintMatchSet = indictment.getConstraintMatchSet();
                    scoreExplanation
                            .append("        ").append(indictment.getScore().toShortString())
                            .append(": indicted object (").append(indictment.getIndictedObject().toString())
                            .append(") has ").append(constraintMatchSet.size()).append(" matches:\n");
                    constraintMatchSet.stream()
                            .sorted(constraintMatchScoreComparator)
                            .limit(constraintMatchLimit)
                            .forEach(constraintMatch -> scoreExplanation
                                    .append("            ").append(constraintMatch.getScore().toShortString())
                                    .append(": constraint (").append(constraintMatch.getConstraintRef().constraintName())
                                    .append(")\n"));
                    if (constraintMatchSet.size() > constraintMatchLimit) {
                        scoreExplanation.append("            ...\n");
                    }
                });
        if (indictmentCount > indictmentLimit) {
            scoreExplanation.append("        ...\n");
        }
        return scoreExplanation.toString();
    }

    public DefaultScoreExplanation(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        this(scoreDirector.getWorkingSolution(), scoreDirector.calculateScore(), scoreDirector.getConstraintMatchTotalMap(),
                scoreDirector.getIndictmentMap());
    }

    public DefaultScoreExplanation(Solution_ solution, Score_ score,
            Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        this.solution = solution;
        this.score = score;
        this.constraintMatchTotalMap = constraintMatchTotalMap;
        List<ConstraintJustification> workingConstraintJustificationList = new ArrayList<>();
        for (ConstraintMatchTotal<Score_> constraintMatchTotal : constraintMatchTotalMap.values()) {
            for (ConstraintMatch<Score_> constraintMatch : constraintMatchTotal.getConstraintMatchSet()) {
                ConstraintJustification justification = constraintMatch.getJustification();
                workingConstraintJustificationList.add(justification);
            }
        }
        this.constraintJustificationList = workingConstraintJustificationList;
        this.indictmentMap = indictmentMap;
    }

    @Override
    public Solution_ getSolution() {
        return solution;
    }

    @Override
    public Score_ getScore() {
        return score;
    }

    @Override
    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        return constraintMatchTotalMap;
    }

    @Override
    public List<ConstraintJustification> getJustificationList() {
        return constraintJustificationList;
    }

    @Override
    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        return indictmentMap;
    }

    @Override
    public String getSummary() {
        return summary.updateAndGet(currentSummary -> Objects.requireNonNullElseGet(currentSummary,
                () -> explainScore(score, constraintMatchTotalMap.values(), indictmentMap.values())));
    }

    @Override
    public String toString() {
        return getSummary(); // So that this class can be used in strings directly.
    }
}
