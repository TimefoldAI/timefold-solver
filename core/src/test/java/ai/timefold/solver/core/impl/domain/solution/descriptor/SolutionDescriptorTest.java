package ai.timefold.solver.core.impl.domain.solution.descriptor;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.impl.testdata.domain.collection.TestdataArrayBasedSolution;
import ai.timefold.solver.core.impl.testdata.domain.collection.TestdataSetBasedSolution;
import ai.timefold.solver.core.impl.testdata.domain.extended.TestdataAnnotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.extended.TestdataUnannotatedExtendedEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.reflect.generic.TestdataGenericEntity;
import ai.timefold.solver.core.impl.testdata.domain.reflect.generic.TestdataGenericSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.TestdataNoProblemFactPropertySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.TestdataProblemFactPropertySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.TestdataReadMethodProblemFactCollectionPropertySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.TestdataWildcardSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.autodiscover.TestdataAutoDiscoverFieldOverrideSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.autodiscover.TestdataAutoDiscoverFieldSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.autodiscover.TestdataAutoDiscoverGetterOverrideSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.autodiscover.TestdataAutoDiscoverGetterSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.autodiscover.TestdataAutoDiscoverUnannotatedEntitySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.autodiscover.TestdataExtendedAutoDiscoverGetterSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.invalid.TestdataDuplicatePlanningEntityCollectionPropertySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.invalid.TestdataDuplicatePlanningScorePropertySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.invalid.TestdataDuplicateProblemFactCollectionPropertySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.invalid.TestdataMissingScorePropertySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.invalid.TestdataProblemFactCollectionPropertyWithArgumentSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.invalid.TestdataProblemFactIsPlanningEntityCollectionPropertySolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.invalid.TestdataUnknownFactTypeSolution;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.invalid.TestdataUnsupportedWildcardSolution;
import ai.timefold.solver.core.impl.testdata.domain.valuerange.TestdataValueRangeEntity;
import ai.timefold.solver.core.impl.testdata.domain.valuerange.TestdataValueRangeSolution;
import ai.timefold.solver.core.impl.testdata.domain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.impl.testdata.domain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.impl.testdata.util.CodeAssertableArrayList;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;
import ai.timefold.solver.core.impl.util.MathUtils;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

class SolutionDescriptorTest {

    // ************************************************************************
    // Problem fact and planning entity properties
    // ************************************************************************

    @Test
    void problemFactProperty() {
        SolutionDescriptor<TestdataProblemFactPropertySolution> solutionDescriptor = TestdataProblemFactPropertySolution
                .buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("extraObject");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("valueList",
                "otherProblemFactList");
    }

    @Test
    void readMethodProblemFactCollectionProperty() {
        SolutionDescriptor<TestdataReadMethodProblemFactCollectionPropertySolution> solutionDescriptor =
                TestdataReadMethodProblemFactCollectionPropertySolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("valueList",
                "createProblemFacts");
    }

    @Test
    void problemFactCollectionPropertyWithArgument() {
        assertThatIllegalStateException().isThrownBy(
                TestdataProblemFactCollectionPropertyWithArgumentSolution::buildSolutionDescriptor);
    }

    @Test
    void duplicateProblemFactCollectionProperty() {
        assertThatIllegalStateException().isThrownBy(
                TestdataDuplicateProblemFactCollectionPropertySolution::buildSolutionDescriptor);
    }

    @Test
    void duplicatePlanningEntityCollectionProperty() {
        assertThatIllegalStateException().isThrownBy(
                TestdataDuplicatePlanningEntityCollectionPropertySolution::buildSolutionDescriptor);
    }

    @Test
    void duplicatePlanningScorePropertyProperty() {
        assertThatIllegalStateException().isThrownBy(
                TestdataDuplicatePlanningScorePropertySolution::buildSolutionDescriptor);
    }

    @Test
    void missingPlanningScorePropertyProperty() {
        assertThatIllegalStateException().isThrownBy(
                TestdataMissingScorePropertySolution::buildSolutionDescriptor);
    }

    @Test
    void problemFactIsPlanningEntityCollectionProperty() {
        assertThatIllegalStateException().isThrownBy(
                TestdataProblemFactIsPlanningEntityCollectionPropertySolution::buildSolutionDescriptor);
    }

    @Test
    void wildcardProblemFactAndEntityProperties() {
        SolutionDescriptor<TestdataWildcardSolution> solutionDescriptor = TestdataWildcardSolution
                .buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("extendsValueList",
                "supersValueList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("extendsEntityList");
    }

    @Test
    void wildcardSupersEntityListProperty() {
        SolverFactory<TestdataUnsupportedWildcardSolution> solverFactory = PlannerTestUtils.buildSolverFactory(
                TestdataUnsupportedWildcardSolution.class, TestdataEntity.class);
        Solver<TestdataUnsupportedWildcardSolution> solver = solverFactory.buildSolver();
        TestdataUnsupportedWildcardSolution solution = new TestdataUnsupportedWildcardSolution();
        solution.setValueList(Arrays.asList(new TestdataValue("v1")));
        solution.setSupersEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataValue("v2")));
        // TODO Ideally, this already fails fast on buildSolverFactory
        assertThatIllegalArgumentException().isThrownBy(() -> solver.solve(solution));
    }

    @Test
    void noProblemFactPropertyWithEasyScoreCalculation() {
        SolverFactory<TestdataNoProblemFactPropertySolution> solverFactory = PlannerTestUtils.buildSolverFactory(
                TestdataNoProblemFactPropertySolution.class, TestdataEntity.class);
        solverFactory.buildSolver();
    }

    @Test
    void extended() {
        SolutionDescriptor<TestdataAnnotatedExtendedSolution> solutionDescriptor = TestdataAnnotatedExtendedSolution
                .buildExtendedSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("valueList",
                "subValueList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList", "subEntityList");
    }

    @Test
    void setProperties() {
        SolutionDescriptor<TestdataSetBasedSolution> solutionDescriptor = TestdataSetBasedSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("valueSet");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entitySet");
    }

    @Test
    void arrayProperties() {
        SolutionDescriptor<TestdataArrayBasedSolution> solutionDescriptor = TestdataArrayBasedSolution
                .buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("values");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entities");
    }

    @Test
    void generic() {
        SolutionDescriptor<TestdataGenericSolution> solutionDescriptor = TestdataGenericSolution.buildSolutionDescriptor();

        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("valueList",
                "complexGenericValueList", "subTypeValueList");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        assertThat(solutionDescriptor.findEntityDescriptor(TestdataGenericEntity.class).getVariableDescriptorMap())
                .containsOnlyKeys("value", "subTypeValue", "complexGenericValue");
    }

    // ************************************************************************
    // Autodiscovery
    // ************************************************************************

    @Test
    void autoDiscoverProblemFactCollectionPropertyElementTypeUnknown() {
        assertThatIllegalArgumentException().isThrownBy(TestdataUnknownFactTypeSolution::buildSolutionDescriptor);
    }

    @Test
    void autoDiscoverFields() {
        SolutionDescriptor<TestdataAutoDiscoverFieldSolution> solutionDescriptor = TestdataAutoDiscoverFieldSolution
                .buildSolutionDescriptor();
        assertThat(solutionDescriptor.getScoreDefinition()).isInstanceOf(SimpleScoreDefinition.class);
        assertThat(solutionDescriptor.getScoreDefinition().getScoreClass()).isEqualTo(SimpleScore.class);
        assertThat(solutionDescriptor.getConstraintConfigurationMemberAccessor().getName())
                .isEqualTo("constraintConfiguration");
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("constraintConfiguration",
                "singleProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        TestdataObject singleProblemFact = new TestdataObject("p1");
        List<TestdataValue> valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        List<TestdataEntity> entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        TestdataEntity otherEntity = new TestdataEntity("otherE1");
        TestdataAutoDiscoverFieldSolution solution = new TestdataAutoDiscoverFieldSolution(
                "s1", singleProblemFact, valueList, entityList, otherEntity);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution), "otherE1", "p1", "e1", "e2",
                "v1", "v2");
    }

    @Test
    void autoDiscoverGetters() {
        SolutionDescriptor<TestdataAutoDiscoverGetterSolution> solutionDescriptor = TestdataAutoDiscoverGetterSolution
                .buildSolutionDescriptor();
        assertThat(solutionDescriptor.getConstraintConfigurationMemberAccessor().getName())
                .isEqualTo("constraintConfiguration");
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("constraintConfiguration",
                "singleProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        TestdataObject singleProblemFact = new TestdataObject("p1");
        List<TestdataValue> valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        List<TestdataEntity> entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        TestdataEntity otherEntity = new TestdataEntity("otherE1");
        TestdataAutoDiscoverGetterSolution solution = new TestdataAutoDiscoverGetterSolution(
                "s1", singleProblemFact, valueList, entityList, otherEntity);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution), "otherE1", "p1", "e1", "e2",
                "v1", "v2");
    }

    @Test
    void autoDiscoverFieldsFactCollectionOverriddenToSingleProperty() {
        SolutionDescriptor<TestdataAutoDiscoverFieldOverrideSolution> solutionDescriptor =
                TestdataAutoDiscoverFieldOverrideSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("singleProblemFact",
                "listProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        TestdataObject singleProblemFact = new TestdataObject("p1");
        List<TestdataValue> valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        List<TestdataEntity> entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        TestdataEntity otherEntity = new TestdataEntity("otherE1");
        List<String> listFact = new CodeAssertableArrayList<>("list1", Arrays.asList("x", "y"));
        TestdataAutoDiscoverFieldOverrideSolution solution = new TestdataAutoDiscoverFieldOverrideSolution(
                "s1", singleProblemFact, valueList, entityList, otherEntity, listFact);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution),
                "otherE1", "list1", "p1", "e1", "e2", "v1", "v2");
    }

    @Test
    void autoDiscoverGettersFactCollectionOverriddenToSingleProperty() {
        SolutionDescriptor<TestdataAutoDiscoverGetterOverrideSolution> solutionDescriptor =
                TestdataAutoDiscoverGetterOverrideSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("singleProblemFact",
                "listProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        TestdataObject singleProblemFact = new TestdataObject("p1");
        List<TestdataValue> valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        List<TestdataEntity> entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        TestdataEntity otherEntity = new TestdataEntity("otherE1");
        List<String> listFact = new CodeAssertableArrayList<>("list1", Arrays.asList("x", "y"));
        TestdataAutoDiscoverGetterOverrideSolution solution = new TestdataAutoDiscoverGetterOverrideSolution(
                "s1", singleProblemFact, valueList, entityList, otherEntity, listFact);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution),
                "otherE1", "list1", "p1", "e1", "e2", "v1", "v2");
    }

    @Test
    void autoDiscoverUnannotatedEntitySubclass() {
        SolutionDescriptor<TestdataAutoDiscoverUnannotatedEntitySolution> solutionDescriptor =
                TestdataAutoDiscoverUnannotatedEntitySolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("singleProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        TestdataObject singleProblemFact = new TestdataObject("p1");
        List<TestdataValue> valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        List<TestdataUnannotatedExtendedEntity> entityList = Arrays.asList(
                new TestdataUnannotatedExtendedEntity("u1"),
                new TestdataUnannotatedExtendedEntity("u2"));
        TestdataUnannotatedExtendedEntity otherEntity = new TestdataUnannotatedExtendedEntity("otherU1");
        TestdataAutoDiscoverUnannotatedEntitySolution solution = new TestdataAutoDiscoverUnannotatedEntitySolution(
                "s1", singleProblemFact, valueList, entityList, otherEntity);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution), "otherU1", "p1", "u1", "u2",
                "v1", "v2");
    }

    @Test
    void autoDiscoverGettersOverriddenInSubclass() {
        SolutionDescriptor<TestdataExtendedAutoDiscoverGetterSolution> solutionDescriptor =
                TestdataExtendedAutoDiscoverGetterSolution.buildSubclassSolutionDescriptor();
        assertThat(solutionDescriptor.getConstraintConfigurationMemberAccessor().getName())
                .isEqualTo("constraintConfiguration");
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("constraintConfiguration",
                "singleProblemFact", "problemFactList");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        TestdataObject singleProblemFact = new TestdataObject("p1");
        List<TestdataValue> listAsSingleProblemFact = new CodeAssertableArrayList<>(
                "f1", Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        List<TestdataEntity> entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        TestdataEntity otherEntity = new TestdataEntity("otherE1");
        TestdataExtendedAutoDiscoverGetterSolution solution = new TestdataExtendedAutoDiscoverGetterSolution(
                "s1", singleProblemFact, listAsSingleProblemFact, entityList, otherEntity);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution), "otherE1", "f1", "p1", "e1",
                "e2");
    }

    @Test
    void countEntities() {
        var valueCount = 10;
        var entityCount = 3;
        var solution = TestdataListSolution.generateInitializedSolution(valueCount, entityCount);
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();

        var initializationStats = solutionDescriptor.computeInitializationStatistics(solution);
        assertThat(initializationStats.genuineEntityCount()).isEqualTo(entityCount);
        assertThat(initializationStats.shadowEntityCount()).isEqualTo(valueCount);
    }

    @Test
    void countUninitializedVariables() {
        var valueCount = 10;
        var entityCount = 3;
        var solution = TestdataSolution.generateSolution(valueCount, entityCount);
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();

        var initializationStats = solutionDescriptor.computeInitializationStatistics(solution);
        assertThat(initializationStats.uninitializedVariableCount()).isZero();

        solution.getEntityList().get(0).setValue(null);
        initializationStats = solutionDescriptor.computeInitializationStatistics(solution);
        assertThat(initializationStats.uninitializedVariableCount()).isOne();

        solution.getEntityList().forEach(entity -> entity.setValue(null));
        initializationStats = solutionDescriptor.computeInitializationStatistics(solution);
        assertThat(initializationStats.uninitializedVariableCount()).isEqualTo(entityCount);
    }

    @Test
    void countUnassignedValues() {
        var valueCount = 10;
        var entityCount = 3;
        var solution = TestdataListSolution.generateInitializedSolution(valueCount, entityCount);
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();

        var initializationStats = solutionDescriptor.computeInitializationStatistics(solution);
        assertThat(initializationStats.unassignedValueCount()).isZero();

        List<TestdataListValue> valueList = solution.getEntityList().get(0).getValueList();
        int unassignedValueCount = valueList.size();
        assertThat(valueList).hasSizeGreaterThan(10 / 3);
        valueList.forEach(value -> {
            value.setEntity(null);
            value.setIndex(null);
        });
        valueList.clear();

        initializationStats = solutionDescriptor.computeInitializationStatistics(solution);
        assertThat(initializationStats.unassignedValueCount()).isEqualTo(unassignedValueCount);
    }

    @Test
    void problemScaleBasic() {
        int valueCount = 10;
        int entityCount = 20;
        SolutionDescriptor<TestdataSolution> solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        TestdataSolution solution = TestdataSolution.generateSolution(valueCount, entityCount);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(valueCount);
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(valueCount);
            softly.assertThat(solutionDescriptor.getProblemScale(solution))
                    .isEqualTo(20.0);
        });
    }

    @Test
    void emptyProblemScale() {
        int valueCount = 27;
        int entityCount = 27;
        SolutionDescriptor<TestdataSolution> solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        TestdataSolution solution = TestdataSolution.generateSolution(valueCount, entityCount);
        solution.getValueList().clear();
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(0);
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(0);
            softly.assertThat(solutionDescriptor.getProblemScale(solution))
                    .isEqualTo(0);
        });
    }

    @Test
    void problemScaleMultipleValueRanges() {
        var solutionDescriptor = TestdataValueRangeSolution.buildSolutionDescriptor();
        var solution = new TestdataValueRangeSolution("Solution");
        solution.setEntityList(List.of(new TestdataValueRangeEntity("A")));
        final long entityCount = 1L;
        final long valueCount = 3L;
        final long variableCount = 8L;
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount * variableCount);
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(3L);
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(variableCount * valueCount);
            softly.assertThat(solutionDescriptor.getProblemScale(solution))
                    .isCloseTo(Math.log10(Math.pow(valueCount, variableCount)), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void problemScaleEntityProvidingValueRange() {
        var solutionDescriptor = TestdataEntityProvidingSolution.buildSolutionDescriptor();
        var solution = new TestdataEntityProvidingSolution("Solution");
        TestdataValue v1 = new TestdataValue("1");
        TestdataValue v2 = new TestdataValue("2");
        solution.setEntityList(List.of(
                new TestdataEntityProvidingEntity("A",
                        List.of(v1, v2)),
                new TestdataEntityProvidingEntity("B",
                        List.of(v1, v2, new TestdataValue("3")))));
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(2L);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(2L);

            // Add 1 to the value range sizes, since the value range allows unassigned
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(4L);
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(3L + 4L);
            softly.assertThat(solutionDescriptor.getProblemScale(solution))
                    .isCloseTo(Math.log10(3 * 4), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void problemScaleChained() {
        int anchorCount = 20;
        int entityCount = 500;
        SolutionDescriptor<TestdataChainedSolution> solutionDescriptor = TestdataChainedSolution.buildSolutionDescriptor();
        TestdataChainedSolution solution = generateChainedSolution(anchorCount, entityCount);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount * 2);
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(entityCount + anchorCount);
            // 1 unchained value is inside the solution
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(entityCount + anchorCount + 1);
            softly.assertThat(solutionDescriptor.getProblemScale(solution))
                    .isCloseTo(MathUtils.getPossibleArrangementsScaledApproximateLog(MathUtils.LOG_PRECISION, 10, 500, 20)
                            / (double) MathUtils.LOG_PRECISION, Percentage.withPercentage(1.0));
        });
    }

    static TestdataChainedSolution generateChainedSolution(int anchorCount, int entityCount) {
        TestdataChainedSolution solution = new TestdataChainedSolution("test solution");
        List<TestdataChainedAnchor> anchorList = IntStream.range(0, anchorCount)
                .mapToObj(Integer::toString)
                .map(TestdataChainedAnchor::new).collect(Collectors.toList());
        solution.setChainedAnchorList(anchorList);
        List<TestdataChainedEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(Integer::toString)
                .map(TestdataChainedEntity::new).collect(Collectors.toList());
        solution.setChainedEntityList(entityList);
        solution.setUnchainedValueList(Collections.singletonList(new TestdataValue("v")));
        return solution;
    }

    @Test
    void problemScaleList() {
        int valueCount = 500;
        int entityCount = 20;
        SolutionDescriptor<TestdataListSolution> solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        TestdataListSolution solution = TestdataListSolution.generateUninitializedSolution(valueCount, entityCount);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(valueCount);
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(valueCount);
            softly.assertThat(solutionDescriptor.getProblemScale(solution))
                    .isCloseTo(MathUtils.getPossibleArrangementsScaledApproximateLog(MathUtils.LOG_PRECISION, 10, 500, 20)
                            / (double) MathUtils.LOG_PRECISION, Percentage.withPercentage(1.0));
        });
    }

    @Test
    void assertProblemScaleListIsApproximatelyProblemScaleChained() {
        int valueCount = 500;
        int entityCount = 20;
        SolutionDescriptor<TestdataListSolution> solutionDescriptorList = TestdataListSolution.buildSolutionDescriptor();
        TestdataListSolution listSolution = TestdataListSolution.generateUninitializedSolution(valueCount, entityCount);
        double listPowerExponent = solutionDescriptorList.getProblemScale(listSolution);
        SolutionDescriptor<TestdataChainedSolution> solutionDescriptorChained =
                TestdataChainedSolution.buildSolutionDescriptor();
        TestdataChainedSolution solutionChained = generateChainedSolution(entityCount, valueCount);
        double chainedPowerExponent = solutionDescriptorChained.getProblemScale(solutionChained);
        // Since they are using different bases in calculation, some difference is expected,
        // but the numbers should be relatively (i.e. ~1%) close.
        assertThat(Math.pow(10, listPowerExponent))
                .isCloseTo(Math.pow(10, chainedPowerExponent), Percentage.withPercentage(1));
    }
}
