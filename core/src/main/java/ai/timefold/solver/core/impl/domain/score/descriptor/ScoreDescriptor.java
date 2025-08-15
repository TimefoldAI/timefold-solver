package ai.timefold.solver.core.impl.domain.score.descriptor;

import java.lang.reflect.Member;

import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

public final class ScoreDescriptor<Score_ extends Score<Score_>> {

    // Used to obtain default @PlanningScore attribute values from a score member that was auto-discovered,
    // as if it had an empty @PlanningScore annotation on it.
    @PlanningScore
    private static final Object PLANNING_SCORE = new Object();

    private final MemberAccessor scoreMemberAccessor;
    private final ScoreDefinition<Score_> scoreDefinition;

    public ScoreDescriptor(MemberAccessor scoreMemberAccessor, ScoreDefinition<Score_> scoreDefinition) {
        this.scoreMemberAccessor = scoreMemberAccessor;
        this.scoreDefinition = scoreDefinition;
    }

    public ScoreDefinition<Score_> getScoreDefinition() {
        return scoreDefinition;
    }

    public Class<Score_> getScoreClass() {
        return scoreDefinition.getScoreClass();
    }

    @SuppressWarnings("unchecked")
    public Score_ getScore(Object solution) {
        return (Score_) scoreMemberAccessor.executeGetter(solution);
    }

    public void setScore(Object solution, Score_ score) {
        scoreMemberAccessor.executeSetter(solution, score);
    }

    public void failFastOnDuplicateMember(DescriptorPolicy descriptorPolicy, Member member, Class<?> solutionClass) {
        MemberAccessor memberAccessor = descriptorPolicy.buildScoreMemberAccessor(member);
        // A solution class cannot have more than one score field or bean property (name check), and the @PlanningScore
        // annotation cannot appear on both the score field and its getter (member accessor class check).
        if (!scoreMemberAccessor.getName().equals(memberAccessor.getName())
                || !scoreMemberAccessor.getClass().equals(memberAccessor.getClass())) {
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") has a @" + PlanningScore.class.getSimpleName()
                    + " annotated member (" + memberAccessor
                    + ") that is duplicated by another member (" + scoreMemberAccessor + ").\n"
                    + "Maybe the annotation is defined on both the field and its getter.");
        }
    }
}
