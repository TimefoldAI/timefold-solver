package ai.timefold.solver.core.impl.domain.solution.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.AbstractValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.collection.TestdataArrayBasedSolution;
import ai.timefold.solver.core.testdomain.collection.TestdataSetBasedSolution;
import ai.timefold.solver.core.testdomain.immutable.enumeration.TestdataEnumSolution;
import ai.timefold.solver.core.testdomain.immutable.record.TestdataRecordSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.invalid.badfactcollection.TestdataBadFactCollectionSolution;
import ai.timefold.solver.core.testdomain.invalid.constraintweightoverrides.TestdataInvalidConstraintWeightOverridesSolution;
import ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact.TestdataEntityAnnotatedAsProblemFactArraySolution;
import ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact.TestdataEntityAnnotatedAsProblemFactCollectionSolution;
import ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact.TestdataEntityAnnotatedAsProblemFactSolution;
import ai.timefold.solver.core.testdomain.invalid.nosolution.TestdataNoSolution;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySolution;
import ai.timefold.solver.core.testdomain.reflect.generic.TestdataGenericEntity;
import ai.timefold.solver.core.testdomain.reflect.generic.TestdataGenericSolution;
import ai.timefold.solver.core.testdomain.shadow.missing.TestdataDeclarativeMissingSupplierSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.TestdataNoProblemFactPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.TestdataProblemFactPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.TestdataReadMethodProblemFactCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.TestdataWildcardSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataDuplicatePlanningEntityCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataDuplicatePlanningScorePropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataDuplicateProblemFactCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataMissingScorePropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataProblemFactCollectionPropertyWithArgumentSolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataProblemFactIsPlanningEntityCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.solutionproperties.invalid.TestdataUnsupportedWildcardSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

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
        assertThatIllegalArgumentException().isThrownBy(
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
    void testBadFactCollection() {
        assertThatCode(TestdataBadFactCollectionSolution::buildSolutionDescriptor)
                .hasMessageContaining("that does not return a Collection or an array.");
    }

    @Test
    void missingDeclarativeSupplierName() {
        assertThatCode(TestdataDeclarativeMissingSupplierSolution::buildSolutionDescriptor)
                .hasMessageContainingAll("@ShadowVariable (endTime)",
                        "supplierName (calculateEndTime) that does not exist",
                        "inside its declaring class (ai.timefold.solver.core.testdomain.shadow.missing.TestdataDeclarativeMissingSupplierValue).",
                        "Maybe you included a parameter which is not a planning solution (ai.timefold.solver.core.testdomain.shadow.missing.TestdataDeclarativeMissingSupplierSolution)",
                        "Maybe you misspelled the supplierName name?");
    }

    @Test
    void testOrdinalId() {
        var solutionDescriptor = TestdataMixedMultiEntitySolution.buildSolutionDescriptor();
        assertThat(solutionDescriptor.getEntityDescriptors().stream().map(EntityDescriptor::getOrdinal))
                .hasSameElementsAs(List.of(0, 1));
        var allIds = new ArrayList<Integer>();
        assertThat(solutionDescriptor.getValueRangeDescriptorCount()).isEqualTo(3);
        allIds.addAll(solutionDescriptor.getBasicVariableDescriptorList().stream()
                .map(BasicVariableDescriptor::getValueRangeDescriptor).mapToInt(AbstractValueRangeDescriptor::getOrdinal)
                .boxed()
                .toList());
        allIds.add(solutionDescriptor.getListVariableDescriptor().getValueRangeDescriptor().getOrdinal());
        assertThat(allIds).containsExactlyInAnyOrder(0, 1, 2);
    }
}
