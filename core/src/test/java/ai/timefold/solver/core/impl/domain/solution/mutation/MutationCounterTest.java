package ai.timefold.solver.core.impl.domain.solution.mutation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;

import org.junit.jupiter.api.Test;

class MutationCounterTest {

    @Test
    void countMutationsNone() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var mutationCounter = new MutationCounter<>(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var valueList = Arrays.asList(val1, val2, val3);

        var a_a = new TestdataEntity("a", val1);
        var a_b = new TestdataEntity("b", val1);
        var a_c = new TestdataEntity("c", val3);
        var a_d = new TestdataEntity("d", val3);
        var aEntityList = Arrays.asList(a_a, a_b, a_c, a_d);

        var a = new TestdataSolution("solution");
        a.setValueList(valueList);
        a.setEntityList(aEntityList);

        var b_a = new TestdataEntity("a", val1);
        var b_b = new TestdataEntity("b", val1);
        var b_c = new TestdataEntity("c", val3);
        var b_d = new TestdataEntity("d", val3);
        var bEntityList = Arrays.asList(b_a, b_b, b_c, b_d);

        var b = new TestdataSolution("solution");
        b.setValueList(valueList);
        b.setEntityList(bEntityList);

        assertThat(mutationCounter.countMutations(a, b)).isZero();
    }

    @Test
    void countMutationsSome() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var mutationCounter = new MutationCounter<>(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var valueList = Arrays.asList(val1, val2, val3);

        var a_a = new TestdataEntity("a", val1);
        var a_b = new TestdataEntity("b", val1);
        var a_c = new TestdataEntity("c", val3);
        var a_d = new TestdataEntity("d", val3);
        var aEntityList = Arrays.asList(a_a, a_b, a_c, a_d);

        var a = new TestdataSolution("solution");
        a.setValueList(valueList);
        a.setEntityList(aEntityList);

        var b_a = new TestdataEntity("a", val3); // Mutated
        var b_b = new TestdataEntity("b", val1);
        var b_c = new TestdataEntity("c", val3);
        var b_d = new TestdataEntity("d", val2); // Mutated
        var bEntityList = Arrays.asList(b_a, b_b, b_c, b_d);

        var b = new TestdataSolution("solution");
        b.setValueList(valueList);
        b.setEntityList(bEntityList);

        assertThat(mutationCounter.countMutations(a, b)).isEqualTo(2);
    }

    @Test
    void countMutationsAll() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var mutationCounter = new MutationCounter<>(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var valueList = Arrays.asList(val1, val2, val3);

        var a_a = new TestdataEntity("a", val1);
        var a_b = new TestdataEntity("b", val1);
        var a_c = new TestdataEntity("c", val3);
        var a_d = new TestdataEntity("d", val3);
        var aEntityList = Arrays.asList(a_a, a_b, a_c, a_d);

        var a = new TestdataSolution("solution");
        a.setValueList(valueList);
        a.setEntityList(aEntityList);

        var b_a = new TestdataEntity("a", val2); // Mutated
        var b_b = new TestdataEntity("b", val2); // Mutated
        var b_c = new TestdataEntity("c", val2); // Mutated
        var b_d = new TestdataEntity("d", val2); // Mutated
        var bEntityList = Arrays.asList(b_a, b_b, b_c, b_d);

        var b = new TestdataSolution("solution");
        b.setValueList(valueList);
        b.setEntityList(bEntityList);

        assertThat(mutationCounter.countMutations(a, b)).isEqualTo(4);
    }

    @Test
    void countMutationsOnExtendedEntities() {
        var solutionDescriptor = TestdataBothAnnotatedExtendedSolution.buildSolutionDescriptor();
        var mutationCounter = new MutationCounter<>(solutionDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var valueList = Arrays.asList(val1, val2);

        var entityListSize = 3;
        var subEntityListSize = 7;
        var rawEntityListSize = 17;

        var a = TestdataBothAnnotatedExtendedSolution.generateSolution(entityListSize, subEntityListSize, rawEntityListSize);
        a.setValueList(valueList);
        a.getEntity().setValue(val1);
        a.getSubEntity().setValue(val1);
        a.getSubEntity().setSubValue(val1);
        a.getEntityList().forEach(e -> e.setValue(val1));
        a.getSubEntityList().forEach(e -> {
            e.setValue(val1);
            e.setSubValue(val1);
        });
        for (Object o : a.getRawEntityList()) {
            ((TestdataBothAnnotatedChildEntity) o).setValue(val1);
            ((TestdataBothAnnotatedChildEntity) o).setSubValue(val1);
        }

        var b = TestdataBothAnnotatedExtendedSolution.generateSolution(entityListSize, subEntityListSize, rawEntityListSize);
        b.setValueList(valueList);
        b.getEntity().setValue(val2);
        b.getSubEntity().setValue(val2);
        b.getSubEntity().setSubValue(val2);
        b.getEntityList().forEach(e -> e.setValue(val2));
        b.getSubEntityList().forEach(e -> {
            e.setValue(val2);
            e.setSubValue(val2);
        });
        for (Object o : b.getRawEntityList()) {
            ((TestdataBothAnnotatedChildEntity) o).setValue(val2);
            ((TestdataBothAnnotatedChildEntity) o).setSubValue(val2);
        }
        var baseDescriptorCount = entityListSize + subEntityListSize + rawEntityListSize + 2;
        var childDescriptorCount = subEntityListSize * 2 + rawEntityListSize * 2 + 2;
        assertThat(mutationCounter.countMutations(a, b)).isEqualTo(baseDescriptorCount + childDescriptorCount);
    }
}
