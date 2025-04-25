package ai.timefold.solver.core.impl.domain.solution.cloner;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.backlinked.TestdataBacklinkedSolution;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedObject;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testdomain.chained.shadow.TestdataShadowingChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.shadow.TestdataShadowingChainedEntity;
import ai.timefold.solver.core.testdomain.chained.shadow.TestdataShadowingChainedObject;
import ai.timefold.solver.core.testdomain.chained.shadow.TestdataShadowingChainedSolution;
import ai.timefold.solver.core.testdomain.clone.cloneable.PlanningCloneableEntity;
import ai.timefold.solver.core.testdomain.clone.cloneable.PlanningCloneableSolution;
import ai.timefold.solver.core.testdomain.clone.deepcloning.AnnotatedTestdataVariousTypes;
import ai.timefold.solver.core.testdomain.clone.deepcloning.TestdataDeepCloningEntity;
import ai.timefold.solver.core.testdomain.clone.deepcloning.TestdataDeepCloningSolution;
import ai.timefold.solver.core.testdomain.clone.deepcloning.TestdataVariousTypes;
import ai.timefold.solver.core.testdomain.clone.deepcloning.field.TestdataFieldAnnotatedDeepCloningEntity;
import ai.timefold.solver.core.testdomain.clone.deepcloning.field.TestdataFieldAnnotatedDeepCloningSolution;
import ai.timefold.solver.core.testdomain.collection.TestdataArrayBasedEntity;
import ai.timefold.solver.core.testdomain.collection.TestdataArrayBasedSolution;
import ai.timefold.solver.core.testdomain.collection.TestdataEntityCollectionPropertyEntity;
import ai.timefold.solver.core.testdomain.collection.TestdataEntityCollectionPropertySolution;
import ai.timefold.solver.core.testdomain.collection.TestdataSetBasedEntity;
import ai.timefold.solver.core.testdomain.collection.TestdataSetBasedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowExtendedShadowEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowShadowEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow.TestdataExtendedShadowVariable;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedBaseEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.thirdparty.TestdataExtendedThirdPartyEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.thirdparty.TestdataExtendedThirdPartySolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.thirdparty.TestdataThirdPartyEntityPojo;
import ai.timefold.solver.core.testdomain.list.externalized.TestdataListEntityExternalized;
import ai.timefold.solver.core.testdomain.list.externalized.TestdataListSolutionExternalized;
import ai.timefold.solver.core.testdomain.list.externalized.TestdataListValueExternalized;
import ai.timefold.solver.core.testdomain.reflect.accessmodifier.TestdataAccessModifierSolution;
import ai.timefold.solver.core.testdomain.reflect.field.TestdataFieldAnnotatedEntity;
import ai.timefold.solver.core.testdomain.reflect.field.TestdataFieldAnnotatedSolution;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public abstract class AbstractSolutionClonerTest {

    protected abstract <Solution_> SolutionCloner<Solution_> createSolutionCloner(
            SolutionDescriptor<Solution_> solutionDescriptor);

    @Test
    void cloneSolution() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataEntity("a", val1);
        var b = new TestdataEntity("b", val1);
        var c = new TestdataEntity("c", val3);
        var d = new TestdataEntity("d", val3);

        var original = new TestdataSolution("solution");
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        var originalEntityList = Arrays.asList(a, b, c, d);
        original.setEntityList(originalEntityList);

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA = cloneEntityList.get(0);
        var cloneB = cloneEntityList.get(1);
        var cloneC = cloneEntityList.get(2);
        var cloneD = cloneEntityList.get(3);
        assertEntityClone(a, cloneA, "a", "1");
        assertEntityClone(b, cloneB, "b", "1");
        assertEntityClone(c, cloneC, "c", "3");
        assertEntityClone(d, cloneD, "d", "3");

        assertThat(cloneB).isNotSameAs(b);
        b.setValue(val2);
        assertCode("2", b.getValue());
        // Clone remains unchanged
        assertCode("1", cloneB.getValue());
    }

    @Test
    void cloneListVariableSolution() {
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(
                TestdataListSolutionExternalized.class,
                TestdataListEntityExternalized.class);

        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataListValueExternalized("1");
        var val2 = new TestdataListValueExternalized("2");
        var val3 = new TestdataListValueExternalized("3");
        var a = new TestdataListEntityExternalized("a", new ArrayList<>(List.of(val1, val3)));
        var b = new TestdataListEntityExternalized("b", new ArrayList<>(List.of(val2)));

        var original = new TestdataListSolutionExternalized();
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        var originalEntityList = List.of(a, b);
        original.setEntityList(originalEntityList);
        original.setScore(SimpleScore.of(1));

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(2)
                .isNotSameAs(originalEntityList);
        var cloneA = cloneEntityList.get(0);
        var cloneB = cloneEntityList.get(1);
        assertEntityListClone(a, cloneA, "a", List.of("1", "3"));
        assertEntityListClone(b, cloneB, "b", List.of("2"));

        assertThat(cloneA).isNotSameAs(a);
        a.getValueList().remove(val1);
        assertThat(a.getValueList()).hasSize(1);
        assertCode("3", a.getValueList().get(0));
        // Clone remains unchanged
        assertThat(cloneA.getValueList()).hasSize(2);
        assertCode("1", cloneA.getValueList().get(0));
    }

    private boolean isGizmo() {
        return this.getClass().getSimpleName().contains("Gizmo");
    }

    @Test
    void cloneFieldAnnotatedSolution() {
        // can't check cloner class; it doesn't implement any additional interfaces
        Assumptions.assumeFalse(isGizmo(),
                "Gizmo cannot use reflection");

        var solutionDescriptor = TestdataFieldAnnotatedSolution.buildSolutionDescriptor();
        SolutionCloner<TestdataFieldAnnotatedSolution> cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataFieldAnnotatedEntity("a", val1);
        var b = new TestdataFieldAnnotatedEntity("b", val1);
        var c = new TestdataFieldAnnotatedEntity("c", val3);
        var d = new TestdataFieldAnnotatedEntity("d", val3);

        var valueList = Arrays.asList(val1, val2, val3);
        var originalEntityList = Arrays.asList(a, b, c, d);
        var original = new TestdataFieldAnnotatedSolution("solution", valueList, originalEntityList);

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA = cloneEntityList.get(0);
        var cloneB = cloneEntityList.get(1);
        var cloneC = cloneEntityList.get(2);
        var cloneD = cloneEntityList.get(3);
        assertEntityClone(a, cloneA, "a", "1");
        assertEntityClone(b, cloneB, "b", "1");
        assertEntityClone(c, cloneC, "c", "3");
        assertEntityClone(d, cloneD, "d", "3");

        assertThat(cloneB).isNotSameAs(b);
    }

    @Test
    void cloneAccessModifierSolution() {
        // can't check cloner class; it doesn't implement any additional interfaces
        Assumptions.assumeFalse(isGizmo(), "Gizmo cannot use reflection");

        var staticObject = new Object();
        TestdataAccessModifierSolution.setStaticField(staticObject);

        var solutionDescriptor = TestdataAccessModifierSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataEntity("a", val1);
        var b = new TestdataEntity("b", val1);
        var c = new TestdataEntity("c", val3);
        var d = new TestdataEntity("d", val3);

        var original = new TestdataAccessModifierSolution("solution");
        original.setWriteOnlyField("writeHello");
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        var originalEntityList = Arrays.asList(a, b, c, d);
        original.setEntityList(originalEntityList);

        var clone = cloner.cloneSolution(original);

        assertThat(TestdataAccessModifierSolution.getStaticFinalField()).isSameAs("staticFinalFieldValue");
        assertThat(TestdataAccessModifierSolution.getStaticField()).isSameAs(staticObject);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getFinalField()).isEqualTo(original.getFinalField());
        assertThat(clone.getReadOnlyField()).isEqualTo("readHello");
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA = cloneEntityList.get(0);
        var cloneB = cloneEntityList.get(1);
        var cloneC = cloneEntityList.get(2);
        var cloneD = cloneEntityList.get(3);
        assertEntityClone(a, cloneA, "a", "1");
        assertEntityClone(b, cloneB, "b", "1");
        assertEntityClone(c, cloneC, "c", "3");
        assertEntityClone(d, cloneD, "d", "3");

        assertThat(cloneB).isNotSameAs(b);
        b.setValue(val2);
        assertCode("2", b.getValue());
        // Clone remains unchanged
        assertCode("1", cloneB.getValue());
    }

    @Test
    protected void cloneExtendedSolution() {
        // can't check cloner class; it doesn't implement any additional interfaces
        Assumptions.assumeFalse(isGizmo(),
                "Gizmo cannot handle subclasses of the class annotated with @PlanningSolution");

        var solutionDescriptor = TestdataOnlyBaseAnnotatedExtendedSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataOnlyBaseAnnotatedChildEntity("a", val1, null);
        var b = new TestdataOnlyBaseAnnotatedChildEntity("b", val1, "extraObjectOnEntity");
        var c = new TestdataOnlyBaseAnnotatedChildEntity("c", val3);
        var d = new TestdataOnlyBaseAnnotatedChildEntity("d", val3, c);
        c.setExtraObject(d);

        var original = new TestdataOnlyBaseAnnotatedExtendedSolution("solution",
                "extraObjectOnSolution");
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        var originalEntityList = Arrays.asList(a, b, c, d);
        original.setEntityList(originalEntityList);

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getExtraObject()).isEqualTo("extraObjectOnSolution");
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA = cloneEntityList.get(0);
        var cloneB = cloneEntityList.get(1);
        var cloneC = cloneEntityList.get(2);
        var cloneD = cloneEntityList.get(3);
        assertEntityClone(a, cloneA, "a", "1");
        assertThat(cloneA.getExtraObject()).isNull();
        assertEntityClone(b, cloneB, "b", "1");
        assertThat(cloneB.getExtraObject()).isEqualTo("extraObjectOnEntity");
        assertEntityClone(c, cloneC, "c", "3");
        assertThat(cloneC.getExtraObject()).isEqualTo(cloneD);
        assertEntityClone(d, cloneD, "d", "3");
        assertThat(cloneD.getExtraObject()).isEqualTo(cloneC);

        assertThat(cloneB).isNotSameAs(b);
        b.setValue(val2);
        assertCode("2", b.getValue());
        // Clone remains unchanged
        assertCode("1", cloneB.getValue());
    }

    @Test
    void cloneExtendedThirdPartySolution() {
        var solutionDescriptor = TestdataExtendedThirdPartySolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataExtendedThirdPartyEntity("a", val1, null);
        var b = new TestdataExtendedThirdPartyEntity("b", val1, "extraObjectOnEntity");
        var c = new TestdataExtendedThirdPartyEntity("c", val3);
        var d = new TestdataExtendedThirdPartyEntity("d", val3, c);
        c.setExtraObject(d);

        var original = new TestdataExtendedThirdPartySolution("solution",
                "extraObjectOnSolution");
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        List<TestdataThirdPartyEntityPojo> originalEntityList = Arrays.asList(a, b, c, d);
        original.setEntityList(originalEntityList);

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getExtraObject()).isEqualTo("extraObjectOnSolution");
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA = (TestdataExtendedThirdPartyEntity) cloneEntityList.get(0);
        var cloneB = (TestdataExtendedThirdPartyEntity) cloneEntityList.get(1);
        var cloneC = (TestdataExtendedThirdPartyEntity) cloneEntityList.get(2);
        var cloneD = (TestdataExtendedThirdPartyEntity) cloneEntityList.get(3);
        assertEntityClone(a, cloneA, "a", "1");
        assertThat(cloneA.getExtraObject()).isNull();
        assertEntityClone(b, cloneB, "b", "1");
        assertThat(cloneB.getExtraObject()).isEqualTo("extraObjectOnEntity");
        assertEntityClone(c, cloneC, "c", "3");
        assertThat(cloneC.getExtraObject()).isEqualTo(cloneD);
        assertEntityClone(d, cloneD, "d", "3");
        assertThat(cloneD.getExtraObject()).isEqualTo(cloneC);

        assertThat(cloneB).isNotSameAs(b);
        b.setValue(val2);
        assertCode("2", b.getValue());
        // Clone remains unchanged
        assertCode("1", cloneB.getValue());
    }

    private void assertEntityClone(TestdataOnlyBaseAnnotatedBaseEntity originalEntity,
            TestdataOnlyBaseAnnotatedBaseEntity cloneEntity,
            String entityCode, String valueCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertCode(valueCode, cloneEntity.getValue());
    }

    private void assertEntityClone(TestdataEntity originalEntity, TestdataEntity cloneEntity,
            String entityCode, String valueCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertCode(valueCode, cloneEntity.getValue());
    }

    private void assertEntityClone(TestdataFieldAnnotatedEntity originalEntity, TestdataFieldAnnotatedEntity cloneEntity,
            String entityCode, String valueCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertCode(valueCode, cloneEntity.getValue());
    }

    private void assertEntityClone(TestdataThirdPartyEntityPojo originalEntity,
            TestdataThirdPartyEntityPojo cloneEntity, String entityCode, String valueCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertCode(valueCode, cloneEntity.getValue());
    }

    private void assertEntityListClone(TestdataListEntityExternalized originalEntity,
            TestdataListEntityExternalized cloneEntity, String entityCode, List<String> valueCodeList) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertThat(cloneEntity.getValueList()).isNotSameAs(originalEntity.getValueList());
        assertCode(entityCode, cloneEntity);
        assertThat(cloneEntity.getValueList()).hasSameSizeAs(valueCodeList);
        assertThat(cloneEntity.getValueList()).containsExactlyElementsOf(originalEntity.getValueList());
        assertThat(cloneEntity.getValueList()).zipSatisfy(valueCodeList,
                (value, code) -> assertThat(value.getCode()).isEqualTo(code));
    }

    @Test
    void cloneChainedSolution() {
        var solutionDescriptor = TestdataChainedSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var a0 = new TestdataChainedAnchor("a0");
        var a1 = new TestdataChainedEntity("a1", a0);
        var a2 = new TestdataChainedEntity("a2", a1);
        var a3 = new TestdataChainedEntity("a3", a2);

        var b0 = new TestdataChainedAnchor("b0");
        var b1 = new TestdataChainedEntity("b1", b0);

        var original = new TestdataChainedSolution("solution");
        var anchorList = Arrays.asList(a0, b0);
        original.setChainedAnchorList(anchorList);
        var originalEntityList = Arrays.asList(a1, a2, a3, b1);
        original.setChainedEntityList(originalEntityList);

        var clone = cloner.cloneSolution(original);
        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getChainedAnchorList()).isSameAs(anchorList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getChainedEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA1 = cloneEntityList.get(0);
        var cloneA2 = cloneEntityList.get(1);
        var cloneA3 = cloneEntityList.get(2);
        var cloneB1 = cloneEntityList.get(3);
        assertChainedEntityClone(a1, cloneA1, "a1", a0);
        assertChainedEntityClone(a2, cloneA2, "a2", cloneA1);
        assertChainedEntityClone(a3, cloneA3, "a3", cloneA2);
        assertChainedEntityClone(b1, cloneB1, "b1", b0);

        a3.setChainedObject(b1);
        assertCode("b1", a3.getChainedObject());
        // Clone remains unchanged
        assertCode("a2", cloneA3.getChainedObject());
    }

    private void assertChainedEntityClone(TestdataChainedEntity originalEntity, TestdataChainedEntity cloneEntity,
            String entityCode, TestdataChainedObject value) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertThat(cloneEntity.getChainedObject()).isSameAs(value);
    }

    @Test
    void cloneShadowChainedSolution() {
        var solutionDescriptor = TestdataShadowingChainedSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var a0 = new TestdataShadowingChainedAnchor("a0");
        var a1 = new TestdataShadowingChainedEntity("a1", a0);
        var a2 = new TestdataShadowingChainedEntity("a2", a1);
        var a3 = new TestdataShadowingChainedEntity("a3", a2);

        var b0 = new TestdataShadowingChainedAnchor("b0");
        var b1 = new TestdataShadowingChainedEntity("b1", b0);

        a0.setNextEntity(a1);
        a1.setNextEntity(a2);
        a2.setNextEntity(a3);
        a3.setNextEntity(null);

        b0.setNextEntity(b1);
        b1.setNextEntity(null);

        var original = new TestdataShadowingChainedSolution("solution");
        var originalAnchorList = Arrays.asList(a0, b0);
        original.setChainedAnchorList(originalAnchorList);
        var originalEntityList = Arrays.asList(a1, a2, a3, b1);
        original.setChainedEntityList(originalEntityList);

        var clone = cloner.cloneSolution(original);
        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneAnchorList = clone.getChainedAnchorList();
        assertThat(cloneAnchorList)
                .hasSize(2)
                .isNotSameAs(originalAnchorList);
        var cloneA0 = cloneAnchorList.get(0);
        var cloneB0 = cloneAnchorList.get(1);

        var cloneEntityList = clone.getChainedEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA1 = cloneEntityList.get(0);
        var cloneA2 = cloneEntityList.get(1);
        var cloneA3 = cloneEntityList.get(2);
        var cloneB1 = cloneEntityList.get(3);
        assertChainedShadowingAnchorClone(a0, cloneA0, "a0", cloneA1);
        assertChainedShadowingEntityClone(a1, cloneA1, "a1", cloneA0, cloneA2);
        assertChainedShadowingEntityClone(a2, cloneA2, "a2", cloneA1, cloneA3);
        assertChainedShadowingEntityClone(a3, cloneA3, "a3", cloneA2, null);
        assertChainedShadowingAnchorClone(b0, cloneB0, "b0", cloneB1);
        assertChainedShadowingEntityClone(b1, cloneB1, "b1", cloneB0, null);

        a3.setChainedObject(b1);
        assertCode("b1", a3.getChainedObject());
        // Clone remains unchanged.
        assertCode("a2", cloneA3.getChainedObject());
    }

    private void assertChainedShadowingAnchorClone(TestdataShadowingChainedAnchor originalEntity,
            TestdataShadowingChainedAnchor cloneEntity,
            String entityCode, TestdataShadowingChainedEntity next) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertThat(cloneEntity.getNextEntity()).isSameAs(next);
    }

    private void assertChainedShadowingEntityClone(TestdataShadowingChainedEntity originalEntity,
            TestdataShadowingChainedEntity cloneEntity,
            String entityCode, TestdataShadowingChainedObject value, TestdataShadowingChainedEntity next) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertThat(cloneEntity.getChainedObject()).isSameAs(value);
        assertThat(cloneEntity.getNextEntity()).isSameAs(next);
    }

    @Test
    void cloneSetBasedSolution() {
        var solutionDescriptor = TestdataSetBasedSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataSetBasedEntity("a", val1);
        var b = new TestdataSetBasedEntity("b", val1);
        var c = new TestdataSetBasedEntity("c", val3);
        var d = new TestdataSetBasedEntity("d", val3);

        var original = new TestdataSetBasedSolution("solution");
        var valueSet = new TreeSet<TestdataValue>((a1, b1) -> {
            return b1.getCode().compareTo(a1.getCode()); // Reverse alphabetic
        });
        valueSet.addAll(Arrays.asList(val1, val2, val3));
        original.setValueSet(valueSet);
        Comparator<TestdataSetBasedEntity> entityComparator = (a1, b1) -> {
            return b1.getCode().compareTo(a1.getCode()); // Reverse alphabetic
        };
        var originalEntitySet = new TreeSet<>(entityComparator);
        originalEntitySet.addAll(Arrays.asList(a, b, c, d));
        original.setEntitySet(originalEntitySet);

        var clone = cloner.cloneSolution(original);
        assertThat(clone).isNotSameAs(original);
        assertThat(clone.getValueSet()).isSameAs(valueSet);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntitySet = clone.getEntitySet();
        assertThat(cloneEntitySet)
                .isInstanceOf(SortedSet.class)
                .isNotSameAs(originalEntitySet);
        assertThat(((SortedSet<?>) cloneEntitySet).comparator()).isSameAs(entityComparator);
        assertCode("solution", clone);
        assertThat(cloneEntitySet).hasSize(4);
        var it = cloneEntitySet.iterator();
        // Reverse order because they got sorted
        var cloneD = it.next();
        var cloneC = it.next();
        var cloneB = it.next();
        var cloneA = it.next();
        assertSetBasedEntityClone(a, cloneA, "a", "1");
        assertSetBasedEntityClone(b, cloneB, "b", "1");
        assertSetBasedEntityClone(c, cloneC, "c", "3");
        assertSetBasedEntityClone(d, cloneD, "d", "3");

        b.setValue(val2);
        assertCode("2", b.getValue());
        // Clone remains unchanged
        assertCode("1", cloneB.getValue());
    }

    private void assertSetBasedEntityClone(TestdataSetBasedEntity originalEntity, TestdataSetBasedEntity cloneEntity,
            String entityCode, String valueCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertCode(valueCode, cloneEntity.getValue());
    }

    @Test
    void cloneEntityCollectionPropertySolution() {
        var solutionDescriptor = TestdataEntityCollectionPropertySolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataEntityCollectionPropertyEntity("a", val1);
        var b = new TestdataEntityCollectionPropertyEntity("b", val1);
        var c = new TestdataEntityCollectionPropertyEntity("c", val3);
        a.setEntityList(Arrays.asList(b, c));
        a.setEntitySet(new HashSet<>(Arrays.asList(b, c)));
        a.setStringToEntityMap(new HashMap<>());
        a.getStringToEntityMap().put("b", b);
        a.getStringToEntityMap().put("c", c);
        a.setEntityToStringMap(new HashMap<>());
        a.getEntityToStringMap().put(b, "b");
        a.getEntityToStringMap().put(c, "c");
        a.setStringToEntityListMap(new HashMap<>());
        a.getStringToEntityListMap().put("bc", Arrays.asList(b, c));

        b.setEntityList(Collections.emptyList());
        b.setEntitySet(new HashSet<>());
        b.setStringToEntityMap(new HashMap<>());
        b.setEntityToStringMap(null);
        b.setStringToEntityListMap(null);

        c.setEntityList(Arrays.asList(a, c));
        c.setEntitySet(new HashSet<>(Arrays.asList(a, c)));
        c.setStringToEntityMap(new HashMap<>());
        c.getStringToEntityMap().put("a", a);
        c.getStringToEntityMap().put("c", c);
        c.setEntityToStringMap(null);
        c.setStringToEntityListMap(null);

        var original = new TestdataEntityCollectionPropertySolution("solution");
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        var originalEntityList = Arrays.asList(a, b, c);
        original.setEntityList(originalEntityList);

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(3)
                .isNotSameAs(originalEntityList);
        var cloneA = cloneEntityList.get(0);
        var cloneB = cloneEntityList.get(1);
        var cloneC = cloneEntityList.get(2);

        assertEntityCollectionPropertyEntityClone(a, cloneA, "a", "1");
        assertThat(cloneA.getEntityList()).isNotSameAs(a.getEntityList());
        assertThat(cloneA.getEntityList()).hasSize(2);
        assertThat(cloneA.getEntityList().get(0)).isSameAs(cloneB);
        assertThat(cloneA.getEntityList().get(1)).isSameAs(cloneC);
        assertThat(cloneA.getEntitySet()).isNotSameAs(a.getEntitySet());
        assertThat(cloneA.getEntitySet()).hasSize(2);
        assertThat(cloneA.getStringToEntityMap()).isNotSameAs(a.getStringToEntityMap());
        assertThat(cloneA.getStringToEntityMap()).hasSize(2);
        assertThat(cloneA.getStringToEntityMap().get("b")).isSameAs(cloneB);
        assertThat(cloneA.getStringToEntityMap().get("c")).isSameAs(cloneC);
        assertThat(cloneA.getEntityToStringMap()).isNotSameAs(a.getEntityToStringMap());
        assertThat(cloneA.getEntityToStringMap()).hasSize(2);
        assertThat(cloneA.getEntityToStringMap()).containsEntry(cloneB, "b");
        assertThat(cloneA.getEntityToStringMap()).containsEntry(cloneC, "c");

        assertThat(cloneA.getStringToEntityListMap()).isNotSameAs(a.getStringToEntityListMap());
        assertThat(cloneA.getStringToEntityListMap()).hasSize(1);
        var entityListOfMap = cloneA.getStringToEntityListMap().get("bc");
        assertThat(entityListOfMap).hasSize(2);
        assertThat(entityListOfMap.get(0)).isSameAs(cloneB);
        assertThat(entityListOfMap.get(1)).isSameAs(cloneC);

        assertEntityCollectionPropertyEntityClone(b, cloneB, "b", "1");
        assertThat(cloneB.getEntityList()).isEmpty();
        assertThat(cloneB.getEntitySet()).isEmpty();
        assertThat(cloneB.getStringToEntityMap()).isEmpty();
        assertThat(cloneB.getEntityToStringMap()).isNull();
        assertThat(cloneB.getStringToEntityListMap()).isNull();

        assertEntityCollectionPropertyEntityClone(c, cloneC, "c", "3");
        assertThat(cloneC.getEntityList()).hasSize(2);
        assertThat(cloneC.getEntityList().get(0)).isSameAs(cloneA);
        assertThat(cloneC.getEntityList().get(1)).isSameAs(cloneC);
        assertThat(cloneC.getEntitySet()).hasSize(2);
        assertThat(cloneC.getStringToEntityMap()).hasSize(2);
        assertThat(cloneC.getStringToEntityMap().get("a")).isSameAs(cloneA);
        assertThat(cloneC.getStringToEntityMap().get("c")).isSameAs(cloneC);
        assertThat(cloneC.getEntityToStringMap()).isNull();
        assertThat(cloneC.getStringToEntityListMap()).isNull();
    }

    private void assertEntityCollectionPropertyEntityClone(TestdataEntityCollectionPropertyEntity originalEntity,
            TestdataEntityCollectionPropertyEntity cloneEntity, String entityCode, String valueCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertCode(valueCode, cloneEntity.getValue());
    }

    @Test
    void cloneEntityArrayPropertySolution() {
        var solutionDescriptor = TestdataArrayBasedSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataArrayBasedEntity("a", val1);
        var b = new TestdataArrayBasedEntity("b", val1);
        var c = new TestdataArrayBasedEntity("c", val3);
        a.setEntities(new TestdataArrayBasedEntity[] { b, c });

        b.setEntities(new TestdataArrayBasedEntity[] {});

        c.setEntities(new TestdataArrayBasedEntity[] { a, c });

        var original = new TestdataArrayBasedSolution("solution");
        var values = new TestdataValue[] { val1, val2, val3 };
        original.setValues(values);
        var originalEntities = new TestdataArrayBasedEntity[] { a, b, c };
        original.setEntities(originalEntities);

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getValues()).isSameAs(values);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntities = clone.getEntities();
        assertThat(cloneEntities)
                .hasSize(3)
                .isNotSameAs(originalEntities);
        TestdataArrayBasedEntity cloneA = cloneEntities[0];
        TestdataArrayBasedEntity cloneB = cloneEntities[1];
        TestdataArrayBasedEntity cloneC = cloneEntities[2];

        assertEntityArrayPropertyEntityClone(a, cloneA, "a", "1");
        assertThat(cloneA.getEntities()).isNotSameAs(a.getEntities());
        assertThat(cloneA.getEntities()).hasSize(2);
        assertThat(cloneA.getEntities()[0]).isSameAs(cloneB);
        assertThat(cloneA.getEntities()[1]).isSameAs(cloneC);

        assertEntityArrayPropertyEntityClone(b, cloneB, "b", "1");
        assertThat(cloneB.getEntities()).isEmpty();

        assertEntityArrayPropertyEntityClone(c, cloneC, "c", "3");
        assertThat(cloneC.getEntities()).hasSize(2);
        assertThat(cloneC.getEntities()[0]).isSameAs(cloneA);
        assertThat(cloneC.getEntities()[1]).isSameAs(cloneC);
    }

    private void assertEntityArrayPropertyEntityClone(TestdataArrayBasedEntity originalEntity,
            TestdataArrayBasedEntity cloneEntity, String entityCode, String valueCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertCode(valueCode, cloneEntity.getValue());
    }

    @Test
    void deepPlanningClone() {
        var solutionDescriptor = TestdataDeepCloningSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataDeepCloningEntity("a", val1);
        a.setUnannotatedCopiedTestdataVariousTypes(new TestdataVariousTypes());
        a.setUnannotatedClonedTestdataVariousTypes(new TestdataVariousTypes());
        a.setAnnotatedTestdataVariousTypes(new AnnotatedTestdataVariousTypes());
        a.setAnnotatedClonedTestdataVariousTypes(new AnnotatedTestdataVariousTypes());
        a.setSameValueAsUnannotatedClonedTestdataVariousTypes(a.getUnannotatedClonedTestdataVariousTypes());
        var aShadowVariableList = Arrays.asList("shadow a1", "shadow a2");
        a.setShadowVariableList(aShadowVariableList);
        var b = new TestdataDeepCloningEntity("b", val1);
        var bShadowVariableMap = new HashMap<String, String>();
        bShadowVariableMap.put("shadow key b1", "shadow value b1");
        bShadowVariableMap.put("shadow key b2", "shadow value b2");
        b.setShadowVariableMap(bShadowVariableMap);
        var c = new TestdataDeepCloningEntity("c", val3);
        var cShadowVariableList = Arrays.asList("shadow c1", "shadow c2");
        c.setShadowVariableList(cShadowVariableList);
        var d = new TestdataDeepCloningEntity("d", val3);

        var original = new TestdataDeepCloningSolution("solution");
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        var originalEntityList = Arrays.asList(a, b, c, d);
        original.setEntityList(originalEntityList);
        var generalShadowVariableList = Arrays.asList("shadow g1", "shadow g2");
        original.setGeneralShadowVariableList(generalShadowVariableList);

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA = cloneEntityList.get(0);
        assertDeepCloningEntityClone(a, cloneA, "a");
        var cloneB = cloneEntityList.get(1);
        assertDeepCloningEntityClone(b, cloneB, "b");
        var cloneC = cloneEntityList.get(2);
        assertDeepCloningEntityClone(c, cloneC, "c");
        var cloneD = cloneEntityList.get(3);
        assertDeepCloningEntityClone(d, cloneD, "d");

        var cloneGeneralShadowVariableList = clone.getGeneralShadowVariableList();
        assertThat(cloneGeneralShadowVariableList)
                .hasSize(2)
                .isNotSameAs(generalShadowVariableList);
        assertThat(cloneGeneralShadowVariableList.get(0)).isSameAs(generalShadowVariableList.get(0));
        assertThat(cloneGeneralShadowVariableList.get(1)).isEqualTo(generalShadowVariableList.get(1));

        b.setValue(val2);
        assertCode("2", b.getValue());
        // Clone remains unchanged
        assertCode("1", cloneB.getValue());

        b.getShadowVariableMap().put("shadow key b1", "other shadow value b1");
        assertThat(b.getShadowVariableMap()).containsEntry("shadow key b1", "other shadow value b1");
        // Clone remains unchanged
        assertThat(cloneB.getShadowVariableMap()).containsEntry("shadow key b1", "shadow value b1");

        // Assert that all the various types have been treated properly.
        assertThat(cloneA.getUnannotatedCopiedTestdataVariousTypes())
                .isSameAs(a.getUnannotatedCopiedTestdataVariousTypes());
        assertThat(cloneA.getSameValueAsUnannotatedClonedTestdataVariousTypes())
                .isSameAs(a.getSameValueAsUnannotatedClonedTestdataVariousTypes());

        var originalUnannotatedTypes = a.getUnannotatedClonedTestdataVariousTypes();
        var clonedUnannotatedTypes = cloneA.getUnannotatedClonedTestdataVariousTypes();
        assertThat(clonedUnannotatedTypes).isNotSameAs(originalUnannotatedTypes);
        assertTestdataVariousTypes(originalUnannotatedTypes, clonedUnannotatedTypes);

        var originalAnnotatedTypes = a.getAnnotatedTestdataVariousTypes();
        var clonedAnnotatedTypes = cloneA.getAnnotatedTestdataVariousTypes();
        assertThat(clonedAnnotatedTypes).isNotSameAs(originalAnnotatedTypes);
        assertTestdataVariousTypes(originalAnnotatedTypes, clonedAnnotatedTypes);

        var originalAnnotatedClonedTypes = a.getAnnotatedClonedTestdataVariousTypes();
        var clonedAnnotatedClonedTypes = cloneA.getAnnotatedClonedTestdataVariousTypes();
        assertSoftly(softly -> {
            softly.assertThat(clonedAnnotatedClonedTypes).isNotSameAs(originalAnnotatedClonedTypes);
            softly.assertThat(clonedAnnotatedClonedTypes).isInstanceOf(AnnotatedTestdataVariousTypes.class);
        });
        assertTestdataVariousTypes(originalAnnotatedClonedTypes, clonedAnnotatedClonedTypes);
    }

    private void assertTestdataVariousTypes(TestdataVariousTypes original, TestdataVariousTypes cloned) {
        assertSoftly(softly -> {
            softly.assertThat(cloned.booleanValue).isEqualTo(original.booleanValue);
            softly.assertThat(cloned.byteValue).isEqualTo(original.byteValue);
            softly.assertThat(cloned.charValue).isEqualTo(original.charValue);
            softly.assertThat(cloned.shortValue).isEqualTo(original.shortValue);
            softly.assertThat(cloned.intValue).isEqualTo(original.intValue);
            softly.assertThat(cloned.longValue).isEqualTo(original.longValue);
            softly.assertThat(cloned.floatValue).isEqualTo(original.floatValue);
            softly.assertThat(cloned.doubleValue).isEqualTo(original.doubleValue);
        });
        // Ensure reference types are copied, not cloned.
        assertSoftly(softly -> {
            softly.assertThat(cloned.booleanRef).isSameAs(original.booleanRef);
            softly.assertThat(cloned.byteRef).isSameAs(original.byteRef);
            softly.assertThat(cloned.charRef).isSameAs(original.charRef);
            softly.assertThat(cloned.shortRef).isSameAs(original.shortRef);
            softly.assertThat(cloned.intRef).isSameAs(original.intRef);
            softly.assertThat(cloned.longRef).isSameAs(original.longRef);
            softly.assertThat(cloned.floatRef).isSameAs(original.floatRef);
            softly.assertThat(cloned.doubleRef).isSameAs(original.doubleRef);
            softly.assertThat(cloned.bigInteger).isSameAs(original.bigInteger);
            softly.assertThat(cloned.bigDecimal).isSameAs(original.bigDecimal);
            softly.assertThat(cloned.uuidRef).isSameAs(original.uuidRef);
            softly.assertThat(cloned.stringRef).isSameAs(original.stringRef);
            softly.assertThat(cloned.nonClonedRecord).isSameAs(original.nonClonedRecord);
        });
        // Ensure that the rest is cloned properly too.
        assertSoftly(softly -> {
            softly.assertThat(cloned.deepClonedListRef).isNotSameAs(original.deepClonedListRef);
            softly.assertThat(cloned.deepClonedListRef)
                    .first()
                    .isSameAs(original.deepClonedListRef.get(0));
            softly.assertThat(cloned.extraDeepClonedObject).isNotSameAs(original.extraDeepClonedObject);
            softly.assertThat(cloned.extraDeepClonedObject.id).isSameAs(original.extraDeepClonedObject.id);
        });
        assertSoftly(softly -> {
            softly.assertThat(cloned.shallowClonedListRef).isSameAs(original.shallowClonedListRef);
            softly.assertThat(cloned.shallowClonedListRef)
                    .first()
                    .isSameAs(original.shallowClonedListRef.get(0));
        });
    }

    private void assertDeepCloningEntityClone(TestdataDeepCloningEntity originalEntity, TestdataDeepCloningEntity cloneEntity,
            String entityCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertThat(cloneEntity.getValue()).isSameAs(originalEntity.getValue());

        var originalShadowVariableList = originalEntity.getShadowVariableList();
        var cloneShadowVariableList = cloneEntity.getShadowVariableList();
        if (originalShadowVariableList == null) {
            assertThat(cloneShadowVariableList).isNull();
        } else {
            assertThat(cloneShadowVariableList).isNotSameAs(originalShadowVariableList);
            assertThat(cloneShadowVariableList).hasSameSizeAs(originalShadowVariableList);
            for (int i = 0; i < originalShadowVariableList.size(); i++) {
                assertThat(cloneShadowVariableList.get(i)).isSameAs(originalShadowVariableList.get(i));
            }
        }

        var originalShadowVariableMap = originalEntity.getShadowVariableMap();
        var cloneShadowVariableMap = cloneEntity.getShadowVariableMap();
        if (originalShadowVariableMap == null) {
            assertThat(cloneShadowVariableMap).isNull();
        } else {
            assertThat(cloneShadowVariableMap).isNotSameAs(originalShadowVariableMap);
            assertThat(cloneShadowVariableMap).hasSameSizeAs(originalShadowVariableMap);
            for (String key : originalShadowVariableMap.keySet()) {
                assertThat(cloneShadowVariableMap.get(key)).isSameAs(originalShadowVariableMap.get(key));
            }
        }
    }

    @Test
    void fieldAnnotatedDeepPlanningClone() {
        var solutionDescriptor = TestdataFieldAnnotatedDeepCloningSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataFieldAnnotatedDeepCloningEntity("a", val1);
        var aShadowVariableList = Arrays.asList("shadow a1", "shadow a2");
        a.setShadowVariableList(aShadowVariableList);
        var stringListToStringMap = new LinkedHashMap<List<String>, String>();
        stringListToStringMap.put(null, "a");
        stringListToStringMap.put(List.of("b"), null);
        stringListToStringMap.put(Arrays.asList("c", null), null);
        a.setStringListToStringMap(stringListToStringMap);

        var b = new TestdataFieldAnnotatedDeepCloningEntity("b", val1);
        var bShadowVariableMap = new HashMap<String, String>();
        bShadowVariableMap.put("shadow key b1", "shadow value b1");
        bShadowVariableMap.put("shadow key b2", "shadow value b2");
        b.setShadowVariableMap(bShadowVariableMap);
        var stringToStringListMap = new LinkedHashMap<String, List<String>>();
        stringToStringListMap.put("a", null);
        stringToStringListMap.put(null, List.of("b"));
        stringToStringListMap.put("c", Arrays.asList("d", null));
        b.setStringToStringListMap(stringToStringListMap);

        var c = new TestdataFieldAnnotatedDeepCloningEntity("c", val3);
        var cShadowVariableList = Arrays.asList("shadow c1", "shadow c2");
        c.setShadowVariableList(cShadowVariableList);
        var d = new TestdataFieldAnnotatedDeepCloningEntity("d", val3);

        var original = new TestdataFieldAnnotatedDeepCloningSolution("solution");
        var valueList = Arrays.asList(val1, val2, val3);
        original.setValueList(valueList);
        var originalEntityList = Arrays.asList(a, b, c, d);
        original.setEntityList(originalEntityList);
        var generalShadowVariableList = Arrays.asList("shadow g1", "shadow g2");
        original.setGeneralShadowVariableList(generalShadowVariableList);

        var clone = cloner.cloneSolution(original);

        assertThat(clone).isNotSameAs(original);
        assertCode("solution", clone);
        assertThat(clone.getValueList()).isSameAs(valueList);
        assertThat(clone.getScore()).isEqualTo(original.getScore());

        var cloneEntityList = clone.getEntityList();
        assertThat(cloneEntityList)
                .hasSize(4)
                .isNotSameAs(originalEntityList);
        var cloneA = cloneEntityList.get(0);
        assertDeepCloningEntityClone(a, cloneA, "a");
        var cloneB = cloneEntityList.get(1);
        assertDeepCloningEntityClone(b, cloneB, "b");
        var cloneC = cloneEntityList.get(2);
        assertDeepCloningEntityClone(c, cloneC, "c");
        var cloneD = cloneEntityList.get(3);
        assertDeepCloningEntityClone(d, cloneD, "d");

        var cloneGeneralShadowVariableList = clone.getGeneralShadowVariableList();
        assertThat(cloneGeneralShadowVariableList)
                .hasSize(2)
                .isNotSameAs(generalShadowVariableList);
        assertThat(cloneGeneralShadowVariableList.get(0)).isSameAs(generalShadowVariableList.get(0));
        assertThat(cloneGeneralShadowVariableList.get(1)).isEqualTo(generalShadowVariableList.get(1));

        b.setValue(val2);
        assertCode("2", b.getValue());
        // Clone remains unchanged
        assertCode("1", cloneB.getValue());

        b.getShadowVariableMap().put("shadow key b1", "other shadow value b1");
        assertThat(b.getShadowVariableMap()).containsEntry("shadow key b1", "other shadow value b1");
        // Clone remains unchanged
        assertThat(cloneB.getShadowVariableMap()).containsEntry("shadow key b1", "shadow value b1");
    }

    @Test
    void supportsEntityToSolutionBacklinking() {
        var entityCount = 2;
        var cloner = createSolutionCloner(TestdataBacklinkedSolution.buildSolutionDescriptor());
        var solution = TestdataBacklinkedSolution.generateSolution(2, entityCount);

        var clonedSolution = cloner.cloneSolution(solution);
        assertThat(clonedSolution).isNotSameAs(solution);
        for (var i = 0; i < entityCount; i++) {
            var originalEntity = solution.getEntityList().get(i);
            var clonedEntity = clonedSolution.getEntityList().get(i);
            assertSoftly(softly -> {
                softly.assertThat(clonedEntity).isNotSameAs(originalEntity);
                softly.assertThat(clonedEntity.getSolution()).isSameAs(clonedSolution);
            });
        }
    }

    private void assertDeepCloningEntityClone(TestdataFieldAnnotatedDeepCloningEntity originalEntity,
            TestdataFieldAnnotatedDeepCloningEntity cloneEntity,
            String entityCode) {
        assertThat(cloneEntity).isNotSameAs(originalEntity);
        assertCode(entityCode, originalEntity);
        assertCode(entityCode, cloneEntity);
        assertThat(cloneEntity.getValue()).isSameAs(originalEntity.getValue());

        var originalShadowVariableList = originalEntity.getShadowVariableList();
        var cloneShadowVariableList = cloneEntity.getShadowVariableList();
        if (originalShadowVariableList == null) {
            assertThat(cloneShadowVariableList).isNull();
        } else {
            assertThat(cloneShadowVariableList).isNotSameAs(originalShadowVariableList);
            assertThat(cloneShadowVariableList).hasSameSizeAs(originalShadowVariableList);
            for (var i = 0; i < originalShadowVariableList.size(); i++) {
                assertThat(cloneShadowVariableList.get(i)).isSameAs(originalShadowVariableList.get(i));
            }
        }

        var originalShadowVariableMap = originalEntity.getShadowVariableMap();
        var cloneShadowVariableMap = cloneEntity.getShadowVariableMap();
        if (originalShadowVariableMap == null) {
            assertThat(cloneShadowVariableMap).isNull();
        } else {
            assertThat(cloneShadowVariableMap).isNotSameAs(originalShadowVariableMap);
            assertThat(cloneShadowVariableMap).hasSameSizeAs(originalShadowVariableMap);
            for (var key : originalShadowVariableMap.keySet()) {
                assertThat(cloneShadowVariableMap.get(key)).isSameAs(originalShadowVariableMap.get(key));
            }
        }

        var originalStringListToStringMap = originalEntity.getStringListToStringMap();
        var cloneStringListToStringMap = cloneEntity.getStringListToStringMap();
        if (originalStringListToStringMap == null) {
            assertThat(cloneStringListToStringMap).isNull();
        } else {
            assertThat(cloneStringListToStringMap).isNotSameAs(originalStringListToStringMap);
            assertThat(cloneStringListToStringMap).hasSameSizeAs(originalStringListToStringMap);
            for (var key : originalStringListToStringMap.keySet()) {
                assertThat(cloneStringListToStringMap.get(key)).isSameAs(originalStringListToStringMap.get(key));
            }
        }

        var originalStringToStringListMap = originalEntity.getStringToStringListMap();
        var cloneStringToStringListMap = cloneEntity.getStringToStringListMap();
        if (originalStringToStringListMap == null) {
            assertThat(cloneStringToStringListMap).isNull();
        } else {
            assertThat(cloneStringToStringListMap).isNotSameAs(originalStringToStringListMap);
            assertThat(cloneStringToStringListMap).hasSameSizeAs(originalStringToStringListMap);
            for (var key : originalStringToStringListMap.keySet()) {
                assertThat(cloneStringToStringListMap).containsEntry(key, originalStringToStringListMap.get(key));
            }
        }

    }

    @Test
    void cloneExtendedShadowEntities() {
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(TestdataExtendedShadowSolution.class,
                TestdataExtendedShadowEntity.class, TestdataExtendedShadowShadowEntity.class);
        var cloner = createSolutionCloner(solutionDescriptor);

        var entity0 = new TestdataExtendedShadowEntity(0);
        entity0.myPlanningVariable = new TestdataExtendedShadowVariable(0);
        var shadowEntity = new TestdataExtendedShadowExtendedShadowEntity(entity0);

        var original = new TestdataExtendedShadowSolution(shadowEntity);
        var clone = cloner.cloneSolution(original);

        assertThat(clone.shadowEntityList)
                .hasSize(1)
                .isNotSameAs(original.shadowEntityList)
                .first()
                .isNotNull();

        assertThat(clone.shadowEntityList.get(0))
                .isNotSameAs(original.shadowEntityList.get(0));
    }

    @Test
    void clonePlanningCloneableItems() {
        var solutionDescriptor = PlanningCloneableSolution.buildSolutionDescriptor();
        var cloner = createSolutionCloner(solutionDescriptor);

        var entityA = new PlanningCloneableEntity("A");
        var entityB = new PlanningCloneableEntity("B");
        var entityC = new PlanningCloneableEntity("C");

        var original = new PlanningCloneableSolution(List.of(entityA, entityB, entityC));
        var clone = cloner.cloneSolution(original);

        assertThat(clone.entityList)
                .hasSize(3)
                .isNotSameAs(original.entityList)
                .first()
                .isNotNull();

        assertThat(clone.entityList.get(0))
                .isNotSameAs(original.entityList.get(0))
                .hasFieldOrPropertyWithValue("code", "A");

        assertThat(clone.entityList.get(1))
                .isNotSameAs(original.entityList.get(1))
                .hasFieldOrPropertyWithValue("code", "B");

        assertThat(clone.entityList.get(2))
                .isNotSameAs(original.entityList.get(2))
                .hasFieldOrPropertyWithValue("code", "C");

        assertThat(clone.codeToEntity)
                .hasSize(3)
                .isNotSameAs(original.codeToEntity);

        assertThat(clone.codeToEntity.get("A"))
                .isSameAs(clone.entityList.get(0));

        assertThat(clone.codeToEntity.get("B"))
                .isSameAs(clone.entityList.get(1));

        assertThat(clone.codeToEntity.get("C"))
                .isSameAs(clone.entityList.get(2));
    }

}
