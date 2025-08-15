package ai.timefold.solver.core.impl.domain.policy;

import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
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
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.score.descriptor.ScoreDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.CompositeValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromSolutionPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DescriptorPolicy {
    private Map<String, SolutionCloner> generatedSolutionClonerMap = new LinkedHashMap<>();
    private final Map<String, MemberAccessor> fromSolutionValueRangeProviderMap = new LinkedHashMap<>();
    private final Set<MemberAccessor> anonymousFromSolutionValueRangeProviderSet = new LinkedHashSet<>();
    private final Map<String, MemberAccessor> fromEntityValueRangeProviderMap = new LinkedHashMap<>();
    private final Set<MemberAccessor> anonymousFromEntityValueRangeProviderSet = new LinkedHashSet<>();
    private DomainAccessType domainAccessType = DomainAccessType.REFLECTION;
    private Set<PreviewFeature> enabledPreviewFeatureSet = EnumSet.noneOf(PreviewFeature.class);
    @Nullable
    private MemberAccessorFactory memberAccessorFactory;
    private int entityDescriptorCount = 0;
    private int valueRangeDescriptorCount = 0;

    public <Solution_> EntityDescriptor<Solution_> buildEntityDescriptor(SolutionDescriptor<Solution_> solutionDescriptor,
            Class<?> entityClass) {
        var entityDescriptor = new EntityDescriptor<>(entityDescriptorCount++, solutionDescriptor, entityClass);
        solutionDescriptor.addEntityDescriptor(entityDescriptor);
        return entityDescriptor;
    }

    public <Score_ extends Score<Score_>> ScoreDescriptor<Score_> buildScoreDescriptor(Member member, Class<?> solutionClass) {
        MemberAccessor scoreMemberAccessor = buildScoreMemberAccessor(member);
        Class<Score_> scoreType = extractScoreType(scoreMemberAccessor, solutionClass);
        PlanningScore annotation = extractPlanningScoreAnnotation(scoreMemberAccessor);
        ScoreDefinition<Score_> scoreDefinition =
                buildScoreDefinition(solutionClass, scoreMemberAccessor, scoreType, annotation);
        return new ScoreDescriptor<>(scoreMemberAccessor, scoreDefinition);
    }

    public <Solution_> CompositeValueRangeDescriptor<Solution_> buildCompositeValueRangeDescriptor(
            GenuineVariableDescriptor<Solution_> variableDescriptor,
            List<ValueRangeDescriptor<Solution_>> childValueRangeDescriptorList) {
        return new CompositeValueRangeDescriptor<>(valueRangeDescriptorCount++, variableDescriptor,
                childValueRangeDescriptorList);
    }

    public <Solution_> FromSolutionPropertyValueRangeDescriptor<Solution_> buildFromSolutionPropertyValueRangeDescriptor(
            GenuineVariableDescriptor<Solution_> variableDescriptor, MemberAccessor valueRangeProviderMemberAccessor) {
        return new FromSolutionPropertyValueRangeDescriptor<>(valueRangeDescriptorCount++, variableDescriptor,
                valueRangeProviderMemberAccessor);
    }

    public <Solution_> FromEntityPropertyValueRangeDescriptor<Solution_> buildFromEntityPropertyValueRangeDescriptor(
            GenuineVariableDescriptor<Solution_> variableDescriptor, MemberAccessor valueRangeProviderMemberAccessor) {
        return new FromEntityPropertyValueRangeDescriptor<>(valueRangeDescriptorCount++, variableDescriptor,
                valueRangeProviderMemberAccessor);
    }

    @SuppressWarnings("unchecked")
    private static <Score_ extends Score<Score_>> Class<Score_> extractScoreType(MemberAccessor scoreMemberAccessor,
            Class<?> solutionClass) {
        Class<?> memberType = scoreMemberAccessor.getType();
        if (!Score.class.isAssignableFrom(memberType)) {
            throw new IllegalStateException(
                    "The solutionClass (%s) has a @%s annotated member (%s) that does not return a subtype of Score."
                            .formatted(solutionClass, PlanningScore.class.getSimpleName(), scoreMemberAccessor));
        }
        if (memberType == Score.class) {
            throw new IllegalStateException(
                    """
                            The solutionClass (%s) has a @%s annotated member (%s) that doesn't return a non-abstract %s class.
                            Maybe make it return %s or another specific %s implementation."""
                            .formatted(solutionClass, PlanningScore.class.getSimpleName(), scoreMemberAccessor,
                                    Score.class.getSimpleName(), HardSoftScore.class.getSimpleName(),
                                    Score.class.getSimpleName()));
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
                throw new IllegalArgumentException(
                        "The solutionClass (%s) has a @%s annotated member (%s) that has a scoreDefinition (%s) that must not have a bendableHardLevelsSize (%d) or a bendableSoftLevelsSize (%d)."
                                .formatted(solutionClass, PlanningScore.class.getSimpleName(), scoreMemberAccessor,
                                        scoreDefinitionClass, bendableHardLevelsSize, bendableSoftLevelsSize));
            }
            return ConfigUtils.newInstance(() -> scoreMemberAccessor + " with @" + PlanningScore.class.getSimpleName(),
                    "scoreDefinitionClass", scoreDefinitionClass);
        }
        if (!IBendableScore.class.isAssignableFrom(scoreType)) {
            if (bendableHardLevelsSize != PlanningScore.NO_LEVEL_SIZE
                    || bendableSoftLevelsSize != PlanningScore.NO_LEVEL_SIZE) {
                throw new IllegalArgumentException(
                        "The solutionClass (%s) has a @%s annotated member (%s) that returns a scoreType (%s) that must not have a bendableHardLevelsSize (%d) or a bendableSoftLevelsSize (%d)."
                                .formatted(solutionClass, PlanningScore.class.getSimpleName(), scoreMemberAccessor, scoreType,
                                        bendableHardLevelsSize, bendableSoftLevelsSize));
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
                throw new IllegalArgumentException(
                        """
                                The solutionClass (%s) has a @%s annotated member (%s) that returns a scoreType (%s) that is not recognized as a default %s implementation.
                                  If you intend to use a custom implementation, maybe set a scoreDefinition in the @%s annotation."""
                                .formatted(solutionClass, PlanningScore.class.getSimpleName(), scoreMemberAccessor, scoreType,
                                        Score.class.getSimpleName(), PlanningScore.class.getSimpleName()));
            }
        } else {
            if (bendableHardLevelsSize == PlanningScore.NO_LEVEL_SIZE
                    || bendableSoftLevelsSize == PlanningScore.NO_LEVEL_SIZE) {
                throw new IllegalArgumentException(
                        "The solutionClass (%s) has a @%s annotated member (%s) that returns a scoreType (%s) that must have a bendableHardLevelsSize (%d) and a bendableSoftLevelsSize (%d)."
                                .formatted(solutionClass, PlanningScore.class.getSimpleName(), scoreMemberAccessor, scoreType,
                                        bendableHardLevelsSize, bendableSoftLevelsSize));
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
                throw new IllegalArgumentException(
                        """
                                The solutionClass (%s) has a @%s annotated member (%s) that returns a bendable scoreType (%s) that is not recognized as a default %s implementation.
                                  If you intend to use a custom implementation, maybe set a scoreDefinition in the annotation."""
                                .formatted(solutionClass, PlanningScore.class.getSimpleName(), scoreMemberAccessor, scoreType,
                                        Score.class.getSimpleName()));
            }
        }
    }

    public MemberAccessor buildScoreMemberAccessor(Member member) {
        return getMemberAccessorFactory().buildAndCacheMemberAccessor(
                member,
                FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PlanningScore.class,
                getDomainAccessType());
    }

    public void addFromSolutionValueRangeProvider(MemberAccessor memberAccessor) {
        String id = extractValueRangeProviderId(memberAccessor);
        if (id == null) {
            anonymousFromSolutionValueRangeProviderSet.add(memberAccessor);
        } else {
            fromSolutionValueRangeProviderMap.put(id, memberAccessor);
        }
    }

    public boolean isFromSolutionValueRangeProvider(MemberAccessor memberAccessor) {
        return fromSolutionValueRangeProviderMap.containsValue(memberAccessor)
                || anonymousFromSolutionValueRangeProviderSet.contains(memberAccessor);
    }

    public boolean hasFromSolutionValueRangeProvider(String id) {
        return fromSolutionValueRangeProviderMap.containsKey(id);
    }

    public MemberAccessor getFromSolutionValueRangeProvider(String id) {
        return fromSolutionValueRangeProviderMap.get(id);
    }

    public Set<MemberAccessor> getAnonymousFromSolutionValueRangeProviderSet() {
        return anonymousFromSolutionValueRangeProviderSet;
    }

    public void addFromEntityValueRangeProvider(MemberAccessor memberAccessor) {
        String id = extractValueRangeProviderId(memberAccessor);
        if (id == null) {
            anonymousFromEntityValueRangeProviderSet.add(memberAccessor);
        } else {
            fromEntityValueRangeProviderMap.put(id, memberAccessor);
        }
    }

    public boolean isFromEntityValueRangeProvider(MemberAccessor memberAccessor) {
        return fromEntityValueRangeProviderMap.containsValue(memberAccessor)
                || anonymousFromEntityValueRangeProviderSet.contains(memberAccessor);
    }

    public boolean hasFromEntityValueRangeProvider(String id) {
        return fromEntityValueRangeProviderMap.containsKey(id);
    }

    public Set<MemberAccessor> getAnonymousFromEntityValueRangeProviderSet() {
        return anonymousFromEntityValueRangeProviderSet;
    }

    public DomainAccessType getDomainAccessType() {
        return domainAccessType;
    }

    public void setDomainAccessType(DomainAccessType domainAccessType) {
        this.domainAccessType = domainAccessType;
    }

    public Set<PreviewFeature> getEnabledPreviewFeatureSet() {
        return enabledPreviewFeatureSet;
    }

    public void setEnabledPreviewFeatureSet(Set<PreviewFeature> enabledPreviewFeatureSet) {
        this.enabledPreviewFeatureSet = enabledPreviewFeatureSet;
    }

    /**
     * @return never null
     */
    public Map<String, SolutionCloner> getGeneratedSolutionClonerMap() {
        return generatedSolutionClonerMap;
    }

    public void setGeneratedSolutionClonerMap(Map<String, SolutionCloner> generatedSolutionClonerMap) {
        this.generatedSolutionClonerMap = generatedSolutionClonerMap;
    }

    public MemberAccessorFactory getMemberAccessorFactory() {
        return memberAccessorFactory;
    }

    public void setMemberAccessorFactory(MemberAccessorFactory memberAccessorFactory) {
        this.memberAccessorFactory = memberAccessorFactory;
    }

    public MemberAccessor getFromEntityValueRangeProvider(String id) {
        return fromEntityValueRangeProviderMap.get(id);
    }

    public boolean isPreviewFeatureEnabled(PreviewFeature previewFeature) {
        return enabledPreviewFeatureSet.contains(previewFeature);
    }

    private @Nullable String extractValueRangeProviderId(MemberAccessor memberAccessor) {
        ValueRangeProvider annotation = memberAccessor.getAnnotation(ValueRangeProvider.class);
        String id = annotation.id();
        if (id == null || id.isEmpty()) {
            return null;
        }
        validateUniqueValueRangeProviderId(id, memberAccessor);
        return id;
    }

    private void validateUniqueValueRangeProviderId(String id, MemberAccessor memberAccessor) {
        MemberAccessor duplicate = fromSolutionValueRangeProviderMap.get(id);
        if (duplicate != null) {
            throw new IllegalStateException("2 members (%s, %s) with a @%s annotation must not have the same id (%s)."
                    .formatted(duplicate, memberAccessor, ValueRangeProvider.class.getSimpleName(), id));
        }
        duplicate = fromEntityValueRangeProviderMap.get(id);
        if (duplicate != null) {
            throw new IllegalStateException("2 members (%s, %s) with a @%s annotation must not have the same id (%s)."
                    .formatted(duplicate, memberAccessor, ValueRangeProvider.class.getSimpleName(), id));
        }
    }

    public Collection<String> getValueRangeProviderIds() {
        List<String> valueRangeProviderIds = new ArrayList<>(
                fromSolutionValueRangeProviderMap.size() + fromEntityValueRangeProviderMap.size());
        valueRangeProviderIds.addAll(fromSolutionValueRangeProviderMap.keySet());
        valueRangeProviderIds.addAll(fromEntityValueRangeProviderMap.keySet());
        return valueRangeProviderIds;
    }

}
