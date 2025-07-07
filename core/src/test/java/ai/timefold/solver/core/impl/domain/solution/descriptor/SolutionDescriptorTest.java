package ai.timefold.solver.core.impl.domain.solution.descriptor;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testdomain.collection.TestdataArrayBasedSolution;
import ai.timefold.solver.core.testdomain.collection.TestdataSetBasedSolution;
import ai.timefold.solver.core.testdomain.declarative.missing.TestdataDeclarativeMissingSupplierSolution;
import ai.timefold.solver.core.testdomain.immutable.enumeration.TestdataEnumSolution;
import ai.timefold.solver.core.testdomain.immutable.record.TestdataRecordSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.invalid.badconfiguration.TestdataBadConfigurationSolution;
import ai.timefold.solver.core.testdomain.invalid.badfactcollection.TestdataBadFactCollectionSolution;
import ai.timefold.solver.core.testdomain.invalid.constraintconfiguration.TestdataInvalidConfigurationSolution;
import ai.timefold.solver.core.testdomain.invalid.constraintweightoverrides.TestdataInvalidConstraintWeightOverridesSolution;
import ai.timefold.solver.core.testdomain.invalid.duplicateweightoverrides.TestdataDuplicateWeightConfigurationSolution;
import ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact.TestdataEntityAnnotatedAsProblemFactArraySolution;
import ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact.TestdataEntityAnnotatedAsProblemFactCollectionSolution;
import ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact.TestdataEntityAnnotatedAsProblemFactSolution;
import ai.timefold.solver.core.testdomain.invalid.multivar.TestdataInvalidMultiVarSolution;
import ai.timefold.solver.core.testdomain.invalid.nosolution.TestdataNoSolution;
import ai.timefold.solver.core.testdomain.invalid.variablemap.TestdataMapConfigurationSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.reflect.generic.TestdataGenericEntity;
import ai.timefold.solver.core.testdomain.reflect.generic.TestdataGenericSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.TestdataNoProblemFactPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.TestdataProblemFactPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.TestdataReadMethodProblemFactCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.TestdataWildcardSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.autodiscover.TestdataAutoDiscoverFieldOverrideSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.autodiscover.TestdataAutoDiscoverFieldSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.autodiscover.TestdataAutoDiscoverGetterOverrideSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.autodiscover.TestdataAutoDiscoverGetterSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.autodiscover.TestdataAutoDiscoverUnannotatedEntitySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.autodiscover.TestdataExtendedAutoDiscoverGetterSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataDuplicatePlanningEntityCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataDuplicatePlanningScorePropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataDuplicateProblemFactCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataMissingScorePropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataProblemFactCollectionPropertyWithArgumentSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataProblemFactIsPlanningEntityCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataUnknownFactTypeSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataUnsupportedWildcardSolution;
import ai.timefold.solver.core.testdomain.valuerange.TestdataValueRangeEntity;
import ai.timefold.solver.core.testdomain.valuerange.TestdataValueRangeSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testutil.CodeAssertableArrayList;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

class SolutionDescriptorTest {

    // ************************************************************************
    // Problem fact and planning entity properties
    // ************************************************************************

    @Test
    void problemFactProperty() {
        var solutionDescriptor = TestdataProblemFactPropertySolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("extraObject");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("valueList",
                "otherProblemFactList");
    }

    @Test
    void readMethodProblemFactCollectionProperty() {
        var solutionDescriptor = TestdataReadMethodProblemFactCollectionPropertySolution.buildSolutionDescriptor();
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
    void planningEntityIsProblemFactProperty() {
        assertThatIllegalStateException().isThrownBy(TestdataEntityAnnotatedAsProblemFactSolution::buildSolutionDescriptor);
    }

    @Test
    void planningEntityIsProblemFactCollectionProperty() {
        assertThatIllegalStateException()
                .isThrownBy(TestdataEntityAnnotatedAsProblemFactCollectionSolution::buildSolutionDescriptor);
    }

    @Test
    void planningEntityIsProblemFactArrayProperty() {
        assertThatIllegalStateException()
                .isThrownBy(TestdataEntityAnnotatedAsProblemFactArraySolution::buildSolutionDescriptor);
    }

    @Test
    void wildcardProblemFactAndEntityProperties() {
        var solutionDescriptor = TestdataWildcardSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("extendsValueList",
                "supersValueList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("extendsEntityList");
    }

    @Test
    void wildcardSupersEntityListProperty() {
        var solverFactory =
                PlannerTestUtils.buildSolverFactory(TestdataUnsupportedWildcardSolution.class, TestdataEntity.class);
        var solver = solverFactory.buildSolver();
        var solution = new TestdataUnsupportedWildcardSolution();
        solution.setValueList(Arrays.asList(new TestdataValue("v1")));
        solution.setSupersEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataValue("v2")));
        // TODO Ideally, this already fails fast on buildSolverFactory
        assertThatIllegalArgumentException().isThrownBy(() -> solver.solve(solution));
    }

    @Test
    void noProblemFactPropertyWithEasyScoreCalculation() {
        var solverFactory =
                PlannerTestUtils.buildSolverFactory(TestdataNoProblemFactPropertySolution.class, TestdataEntity.class);
        assertThatCode(solverFactory::buildSolver)
                .doesNotThrowAnyException();
    }

    @Test
    void extended() {
        var solutionDescriptor = TestdataBothAnnotatedExtendedSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("valueList",
                "subValueList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("entity", "subEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList", "subEntityList",
                "rawEntityList", "entityList", "objectEntityList");
    }

    @Test
    void setProperties() {
        var solutionDescriptor = TestdataSetBasedSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("valueSet");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entitySet");
    }

    @Test
    void arrayProperties() {
        var solutionDescriptor = TestdataArrayBasedSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("values");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entities");
    }

    @Test
    void generic() {
        var solutionDescriptor = TestdataGenericSolution.buildSolutionDescriptor();

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
        var solutionDescriptor = TestdataAutoDiscoverFieldSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getScoreDefinition()).isInstanceOf(SimpleScoreDefinition.class);
        assertThat(solutionDescriptor.getScoreDefinition().getScoreClass()).isEqualTo(SimpleScore.class);
        assertThat(solutionDescriptor.getConstraintConfigurationMemberAccessor().getName())
                .isEqualTo("constraintConfiguration");
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("constraintConfiguration",
                "singleProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        var singleProblemFact = new TestdataObject("p1");
        var valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        var entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        var otherEntity = new TestdataEntity("otherE1");
        var solution = new TestdataAutoDiscoverFieldSolution("s1", singleProblemFact, valueList, entityList, otherEntity);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution), "otherE1", "p1", "e1", "e2",
                "v1", "v2");
    }

    @Test
    void autoDiscoverGetters() {
        var solutionDescriptor = TestdataAutoDiscoverGetterSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getConstraintConfigurationMemberAccessor().getName())
                .isEqualTo("constraintConfiguration");
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("constraintConfiguration",
                "singleProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        var singleProblemFact = new TestdataObject("p1");
        var valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        var entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        var otherEntity = new TestdataEntity("otherE1");
        var solution = new TestdataAutoDiscoverGetterSolution("s1", singleProblemFact, valueList, entityList, otherEntity);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution), "otherE1", "p1", "e1", "e2",
                "v1", "v2");
    }

    @Test
    void autoDiscoverFieldsFactCollectionOverriddenToSingleProperty() {
        var solutionDescriptor = TestdataAutoDiscoverFieldOverrideSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("singleProblemFact",
                "listProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        var singleProblemFact = new TestdataObject("p1");
        var valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        var entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        var otherEntity = new TestdataEntity("otherE1");
        var listFact = new CodeAssertableArrayList<>("list1", Arrays.asList("x", "y"));
        var solution = new TestdataAutoDiscoverFieldOverrideSolution("s1", singleProblemFact, valueList, entityList,
                otherEntity, listFact);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution),
                "otherE1", "list1", "p1", "e1", "e2", "v1", "v2");
    }

    @Test
    void autoDiscoverGettersFactCollectionOverriddenToSingleProperty() {
        var solutionDescriptor = TestdataAutoDiscoverGetterOverrideSolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("singleProblemFact",
                "listProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        var singleProblemFact = new TestdataObject("p1");
        var valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        var entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        var otherEntity = new TestdataEntity("otherE1");
        var listFact = new CodeAssertableArrayList<>("list1", Arrays.asList("x", "y"));
        var solution = new TestdataAutoDiscoverGetterOverrideSolution("s1", singleProblemFact, valueList, entityList,
                otherEntity, listFact);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution),
                "otherE1", "list1", "p1", "e1", "e2", "v1", "v2");
    }

    @Test
    void autoDiscoverUnannotatedEntitySubclass() {
        var solutionDescriptor = TestdataAutoDiscoverUnannotatedEntitySolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("singleProblemFact");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).containsOnlyKeys("problemFactList");
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        var singleProblemFact = new TestdataObject("p1");
        var valueList = Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"));
        var entityList = Arrays.asList(
                new TestdataOnlyBaseAnnotatedChildEntity("u1"),
                new TestdataOnlyBaseAnnotatedChildEntity("u2"));
        var otherEntity = new TestdataOnlyBaseAnnotatedChildEntity("otherU1");
        var solution =
                new TestdataAutoDiscoverUnannotatedEntitySolution("s1", singleProblemFact, valueList, entityList, otherEntity);

        assertAllCodesOfCollection(solutionDescriptor.getAllEntitiesAndProblemFacts(solution), "otherU1", "p1", "u1", "u2",
                "v1", "v2");
    }

    @Test
    void autoDiscoverGettersOverriddenInSubclass() {
        var solutionDescriptor = TestdataExtendedAutoDiscoverGetterSolution.buildSubclassSolutionDescriptor();
        assertThat(solutionDescriptor.getConstraintConfigurationMemberAccessor().getName())
                .isEqualTo("constraintConfiguration");
        assertThat(solutionDescriptor.getProblemFactMemberAccessorMap()).containsOnlyKeys("constraintConfiguration",
                "singleProblemFact", "problemFactList");
        assertThat(solutionDescriptor.getProblemFactCollectionMemberAccessorMap()).isEmpty();
        assertThat(solutionDescriptor.getEntityMemberAccessorMap()).containsOnlyKeys("otherEntity");
        assertThat(solutionDescriptor.getEntityCollectionMemberAccessorMap()).containsOnlyKeys("entityList");

        var singleProblemFact = new TestdataObject("p1");
        var listAsSingleProblemFact =
                new CodeAssertableArrayList<>("f1", Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        var entityList = Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"));
        var otherEntity = new TestdataEntity("otherE1");
        var solution = new TestdataExtendedAutoDiscoverGetterSolution("s1", singleProblemFact, listAsSingleProblemFact,
                entityList, otherEntity);

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

        var valueList = solution.getEntityList().get(0).getValueList();
        var unassignedValueCount = valueList.size();
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
        var valueCount = 10;
        var entityCount = 20;
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var solution = TestdataSolution.generateSolution(valueCount, entityCount);
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
        var valueCount = 27;
        var entityCount = 27;
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var solution = TestdataSolution.generateSolution(valueCount, entityCount);
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
        final var entityCount = 1L;
        final var valueCount = 3L;
        final var variableCount = 8L;
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
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
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
    void problemScaleSingleEntityProvidingSingleValueRange() {
        var solutionDescriptor = TestdataEntityProvidingSolution.buildSolutionDescriptor();
        var solution = new TestdataEntityProvidingSolution("Solution");
        var v1 = new TestdataValue("1");
        solution.setEntityList(List.of(
                new TestdataEntityProvidingEntity("A",
                        List.of(v1))));
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(1L);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(1L);

            // Add 1 to the value range sizes, since the value range allows unassigned
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(2L);
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(2L);
            softly.assertThat(solutionDescriptor.getProblemScale(solution))
                    .isCloseTo(Math.log10(2), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void problemScaleChained() {
        var anchorCount = 20;
        var entityCount = 500;
        var solutionDescriptor = TestdataChainedSolution.buildSolutionDescriptor();
        var solution = generateChainedSolution(anchorCount, entityCount);
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
        var solution = new TestdataChainedSolution("test solution");
        var anchorList = IntStream.range(0, anchorCount)
                .mapToObj(Integer::toString)
                .map(TestdataChainedAnchor::new).toList();
        solution.setChainedAnchorList(anchorList);
        var entityList = IntStream.range(0, entityCount)
                .mapToObj(Integer::toString)
                .map(TestdataChainedEntity::new).toList();
        solution.setChainedEntityList(entityList);
        solution.setUnchainedValueList(Collections.singletonList(new TestdataValue("v")));
        return solution;
    }

    @Test
    void problemScaleList() {
        var valueCount = 500;
        var entityCount = 20;
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var solution = TestdataListSolution.generateUninitializedSolution(valueCount, entityCount);
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
    void problemScaleSingleEntityWithAssignedValues() {
        var valueCount = 1;
        var entityCount = 1;
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var solution = TestdataListSolution.generateUninitializedSolution(valueCount, entityCount);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(valueCount);
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(valueCount);
            softly.assertThat(solutionDescriptor.getProblemScale(solution)).isEqualTo(0.0);
        });
    }

    @Test
    void problemScaleSingleEntityWithUnassignedValues() {
        var valueCount = 1;
        var entityCount = 1;
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(valueCount, entityCount);
        assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor.getGenuineEntityCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getGenuineVariableCount(solution)).isEqualTo(entityCount);
            softly.assertThat(solutionDescriptor.getMaximumValueRangeSize(solution)).isEqualTo(valueCount);
            softly.assertThat(solutionDescriptor.getApproximateValueCount(solution)).isEqualTo(valueCount);
            softly.assertThat(solutionDescriptor.getProblemScale(solution))
                    .isCloseTo(Math.log10(2), Percentage.withPercentage(1.0));
        });
    }

    @Test
    void assertProblemScaleListIsApproximatelyProblemScaleChained() {
        var valueCount = 500;
        var entityCount = 20;
        var solutionDescriptorList = TestdataListSolution.buildSolutionDescriptor();
        var listSolution = TestdataListSolution.generateUninitializedSolution(valueCount, entityCount);
        var listPowerExponent = solutionDescriptorList.getProblemScale(listSolution);
        var solutionDescriptorChained = TestdataChainedSolution.buildSolutionDescriptor();
        var solutionChained = generateChainedSolution(entityCount, valueCount);
        var chainedPowerExponent = solutionDescriptorChained.getProblemScale(solutionChained);
        // Since they are using different bases in calculation, some difference is expected,
        // but the numbers should be relatively (i.e. ~1%) close.
        assertThat(Math.pow(10, listPowerExponent))
                .isCloseTo(Math.pow(10, chainedPowerExponent), Percentage.withPercentage(1));
    }

    @Test
    void testImmutableClass() {
        assertThatCode(TestdataRecordSolution::buildSolutionDescriptor)
                .hasMessageContaining("cannot be a record as it needs to be mutable.");
        assertThatCode(TestdataEnumSolution::buildSolutionDescriptor)
                .hasMessageContaining("cannot be an enum as it needs to be mutable.");
    }

    @Test
    void testMultipleConstraintWeights() {
        assertThatCode(TestdataInvalidConstraintWeightOverridesSolution::buildSolutionDescriptor)
                .hasMessageContaining("has more than one field")
                .hasMessageContaining(
                        "of type interface ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides");
    }

    @Test
    void testNoSolution() {
        assertThatCode(TestdataNoSolution::buildSolutionDescriptor)
                .hasMessageContaining("is not annotated with @PlanningSolution but defines annotated members");
    }

    @Test
    void testInvalidConfiguration() {
        assertThatCode(TestdataInvalidConfigurationSolution::buildSolutionDescriptor)
                .hasMessageContaining("The autoDiscoverMemberType ")
                .hasMessageContaining("cannot accept a member")
                .hasMessageContaining("with an elementType")
                .hasMessageContaining("that has a @ConstraintConfiguration annotation.");
    }

    @Test
    void testConfigurationMap() {
        assertThatCode(TestdataMapConfigurationSolution::buildSolutionDescriptor)
                .hasMessageContaining("The autoDiscoverMemberType ")
                .hasMessageContaining("does not yet support the member")
                .hasMessageContaining("which is an implementation of Map.");
    }

    @Test
    void testDuplicateConfigurationWeights() {
        assertThatCode(TestdataDuplicateWeightConfigurationSolution::buildSolutionDescriptor)
                .hasMessageContaining(
                        "has both a ConstraintWeightOverrides member and a ConstraintConfigurationProvider-annotated member")
                .hasMessageContaining(
                        "ConstraintConfigurationProvider is deprecated, please remove it from your codebase and keep ConstraintWeightOverrides only");
    }

    @Test
    void testBadConfiguration() {
        assertThatCode(TestdataBadConfigurationSolution::buildSolutionDescriptor)
                .hasMessageContaining("The solutionClass")
                .hasMessageContaining("has a @ConstraintConfigurationProvider annotated member")
                .hasMessageContaining("that does not return a class")
                .hasMessageContaining("that has a ConstraintConfiguration annotation.");
    }

    @Test
    void testBadFactCollection() {
        assertThatCode(TestdataBadFactCollectionSolution::buildSolutionDescriptor)
                .hasMessageContaining("that does not return a Collection or an array.");
    }

    @Test
    void testBadChainedAndListModel() {
        assertThatCode(TestdataInvalidMultiVarSolution::buildSolutionDescriptor)
                .hasMessageContaining("Combining chained variables")
                .hasMessageContaining("with list variables")
                .hasMessageContaining("on a single planning entity")
                .hasMessageContaining("is not supported");
    }

    @Test
    void missingDeclarativeSupplierMethod() {
        assertThatCode(TestdataDeclarativeMissingSupplierSolution::buildSolutionDescriptor)
                .hasMessageContainingAll("@ShadowVariable (endTime)",
                        "supplierMethod (calculateEndTime) that does not exist",
                        "inside its declaring class (ai.timefold.solver.core.testdomain.declarative.missing.TestdataDeclarativeMissingSupplierValue).",
                        "Maybe you misspelled the supplierMethod name?");
    }
}
