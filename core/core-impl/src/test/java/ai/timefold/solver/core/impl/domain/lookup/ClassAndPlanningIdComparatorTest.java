package ai.timefold.solver.core.impl.domain.lookup;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCompareToOrder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntityGroup;

import org.junit.jupiter.api.Test;

class ClassAndPlanningIdComparatorTest {
    private final Comparator<Object> comparator =
            new ClassAndPlanningIdComparator(new MemberAccessorFactory(), DomainAccessType.REFLECTION, false);

    @Test
    void comparesDifferentClassesByClassName() {
        assertCompareToOrder(comparator, 1d, 1);
    }

    @Test
    void comparesSameComparableClassesByNaturalOrder() {
        assertCompareToOrder(comparator, 1, 2, 3);
    }

    @Test
    void comparesSameUnComparableClassesByPlanningId() {
        TestdataLavishEntityGroup group = new TestdataLavishEntityGroup();
        TestdataLavishEntity firstEntity = new TestdataLavishEntity("a", group);
        TestdataLavishEntity secondEntity = new TestdataLavishEntity("b", group);
        TestdataLavishEntity thirdEntity = new TestdataLavishEntity("c", group);
        assertCompareToOrder(comparator, firstEntity, secondEntity, thirdEntity);
    }

    @Test
    void treatesSameUnComparableClassesWithoutPlanningIdAsEqual() {
        Object firstObject = new ClassAndPlanningIdComparator(new MemberAccessorFactory(), DomainAccessType.REFLECTION, false);
        Object secondObject = new ClassAndPlanningIdComparator(new MemberAccessorFactory(), DomainAccessType.REFLECTION, false);
        int result = comparator.compare(firstObject, secondObject);
        assertThat(result).isEqualTo(0);
    }

}
