package ai.timefold.solver.core.impl.domain.score.descriptor;

import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER;

import java.lang.reflect.Member;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.score.buildin.BendableBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.BendableLongScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.BendableScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardMediumSoftBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardMediumSoftLongScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardMediumSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardSoftBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardSoftLongScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.HardSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.SimpleBigDecimalScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.SimpleLongScoreDefinition;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

public class ScoreDescriptor<Score_ extends Score<Score_>> {

    // Used to obtain default @PlanningScore attribute values from a score member that was auto-discovered,
    // as if it had an empty @PlanningScore annotation on it.
    @PlanningScore
    private static final Object PLANNING_SCORE = new Object();

    private final MemberAccessor scoreMemberAccessor;
    private final ScoreDefinition<Score_> scoreDefinition;

    private ScoreDescriptor(MemberAccessor scoreMemberAccessor, ScoreDefinition<Score_> scoreDefinition) {
        this.scoreMemberAccessor = scoreMemberAccessor;
        this.scoreDefinition = scoreDefinition;
    }

    public static <Score_ extends Score<Score_>> ScoreDescriptor<Score_> buildScoreDescriptor(DescriptorPolicy descriptorPolicy,
            Member member, Class<?> solutionClass) {
        MemberAccessor scoreMemberAccessor = buildScoreMemberAccessor(descriptorPolicy, member);
        Class<Score_> scoreType = extractScoreType(scoreMemberAccessor, solutionClass);
        PlanningScore annotation = extractPlanningScoreAnnotation(scoreMemberAccessor);
        ScoreDefinition<Score_> scoreDefinition =
                buildScoreDefinition(solutionClass, scoreMemberAccessor, scoreType, annotation);
        return new ScoreDescriptor<>(scoreMemberAccessor, scoreDefinition);
    }

    private static MemberAccessor buildScoreMemberAccessor(DescriptorPolicy descriptorPolicy, Member member) {
        return descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(
                member,
                FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PlanningScore.class,
                descriptorPolicy.getDomainAccessType());
    }

    @SuppressWarnings("unchecked")
    private static <Score_ extends Score<Score_>> Class<Score_> extractScoreType(MemberAccessor scoreMemberAccessor,
            Class<?> solutionClass) {
        Class<?> memberType = scoreMemberAccessor.getType();
        if (!Score.class.isAssignableFrom(memberType)) {
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") has a @" + PlanningScore.class.getSimpleName()
                    + " annotated member (" + scoreMemberAccessor + ") that does not return a subtype of Score.");
        }
        if (memberType == Score.class) {
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") has a @" + PlanningScore.class.getSimpleName()
                    + " annotated member (" + scoreMemberAccessor
                    + ") that doesn't return a non-abstract " + Score.class.getSimpleName() + " class.\n"
                    + "Maybe make it return " + HardSoftScore.class.getSimpleName()
                    + " or another specific " + Score.class.getSimpleName() + " implementation.");
        }
        return (Class<Score_>) memberType;
    }

    private static PlanningScore extractPlanningScoreAnnotation(MemberAccessor scoreMemberAccessor) {
        PlanningScore annotation = scoreMemberAccessor.getAnnotation(PlanningScore.class);
        if (annotation != null) {
            return annotation;
        }
        // The member was auto-discovered.
        try {
            return ScoreDescriptor.class.getDeclaredField("PLANNING_SCORE").getAnnotation(PlanningScore.class);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Impossible situation: the field (PLANNING_SCORE) must exist.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <Score_ extends Score<Score_>, ScoreDefinition_ extends ScoreDefinition<Score_>> ScoreDefinition_
            buildScoreDefinition(Class<?> solutionClass,
                    MemberAccessor scoreMemberAccessor, Class<Score_> scoreType, PlanningScore annotation) {
        Class<ScoreDefinition_> scoreDefinitionClass = (Class<ScoreDefinition_>) annotation.scoreDefinitionClass();
        int bendableHardLevelsSize = annotation.bendableHardLevelsSize();
        int bendableSoftLevelsSize = annotation.bendableSoftLevelsSize();
        if (!Objects.equals(scoreDefinitionClass, PlanningScore.NullScoreDefinition.class)) {
            if (bendableHardLevelsSize != PlanningScore.NO_LEVEL_SIZE
                    || bendableSoftLevelsSize != PlanningScore.NO_LEVEL_SIZE) {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a @" + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that has a scoreDefinition (" + scoreDefinitionClass
                        + ") that must not have a bendableHardLevelsSize (" + bendableHardLevelsSize
                        + ") or a bendableSoftLevelsSize (" + bendableSoftLevelsSize + ").");
            }
            return ConfigUtils.newInstance(() -> scoreMemberAccessor + " with @" + PlanningScore.class.getSimpleName(),
                    "scoreDefinitionClass", scoreDefinitionClass);
        }
        if (!IBendableScore.class.isAssignableFrom(scoreType)) {
            if (bendableHardLevelsSize != PlanningScore.NO_LEVEL_SIZE
                    || bendableSoftLevelsSize != PlanningScore.NO_LEVEL_SIZE) {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a @" + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that returns a scoreType (" + scoreType
                        + ") that must not have a bendableHardLevelsSize (" + bendableHardLevelsSize
                        + ") or a bendableSoftLevelsSize (" + bendableSoftLevelsSize + ").");
            }
            if (scoreType.equals(SimpleScore.class)) {
                return (ScoreDefinition_) new SimpleScoreDefinition();
            } else if (scoreType.equals(SimpleLongScore.class)) {
                return (ScoreDefinition_) new SimpleLongScoreDefinition();
            } else if (scoreType.equals(SimpleBigDecimalScore.class)) {
                return (ScoreDefinition_) new SimpleBigDecimalScoreDefinition();
            } else if (scoreType.equals(HardSoftScore.class)) {
                return (ScoreDefinition_) new HardSoftScoreDefinition();
            } else if (scoreType.equals(HardSoftLongScore.class)) {
                return (ScoreDefinition_) new HardSoftLongScoreDefinition();
            } else if (scoreType.equals(HardSoftBigDecimalScore.class)) {
                return (ScoreDefinition_) new HardSoftBigDecimalScoreDefinition();
            } else if (scoreType.equals(HardMediumSoftScore.class)) {
                return (ScoreDefinition_) new HardMediumSoftScoreDefinition();
            } else if (scoreType.equals(HardMediumSoftLongScore.class)) {
                return (ScoreDefinition_) new HardMediumSoftLongScoreDefinition();
            } else if (scoreType.equals(HardMediumSoftBigDecimalScore.class)) {
                return (ScoreDefinition_) new HardMediumSoftBigDecimalScoreDefinition();
            } else {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a @" + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that returns a scoreType (" + scoreType
                        + ") that is not recognized as a default " + Score.class.getSimpleName() + " implementation.\n"
                        + "  If you intend to use a custom implementation,"
                        + " maybe set a scoreDefinition in the @" + PlanningScore.class.getSimpleName()
                        + " annotation.");
            }
        } else {
            if (bendableHardLevelsSize == PlanningScore.NO_LEVEL_SIZE
                    || bendableSoftLevelsSize == PlanningScore.NO_LEVEL_SIZE) {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a @" + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that returns a scoreType (" + scoreType
                        + ") that must have a bendableHardLevelsSize (" + bendableHardLevelsSize
                        + ") and a bendableSoftLevelsSize (" + bendableSoftLevelsSize + ").");
            }
            if (scoreType.equals(BendableScore.class)) {
                return (ScoreDefinition_) new BendableScoreDefinition(bendableHardLevelsSize, bendableSoftLevelsSize);
            } else if (scoreType.equals(BendableLongScore.class)) {
                return (ScoreDefinition_) new BendableLongScoreDefinition(bendableHardLevelsSize,
                        bendableSoftLevelsSize);
            } else if (scoreType.equals(BendableBigDecimalScore.class)) {
                return (ScoreDefinition_) new BendableBigDecimalScoreDefinition(bendableHardLevelsSize,
                        bendableSoftLevelsSize);
            } else {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a @" + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that returns a bendable scoreType (" + scoreType
                        + ") that is not recognized as a default " + Score.class.getSimpleName() + " implementation.\n"
                        + "  If you intend to use a custom implementation,"
                        + " maybe set a scoreDefinition in the annotation.");
            }
        }
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
        MemberAccessor memberAccessor = buildScoreMemberAccessor(descriptorPolicy, member);
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
