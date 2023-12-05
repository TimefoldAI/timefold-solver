package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

public class AllVariablesAssertTest {

    @Test
    void testSnapshotBasicVariables() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var variableDescriptor =
                solutionDescriptor.getEntityDescriptorStrict(TestdataEntity.class).getGenuineVariableDescriptor("value");

        var workingSolution = TestdataSolution.generateSolution(3, 3);
        VariableSnapshotTotal<TestdataSolution> snapshot = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        workingSolution.getEntityList().get(0).setValue(null);
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(0));

        var entity1Snapshot =
                snapshot.getVariableSnapshot(new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(0)));
        var entity2Snapshot =
                snapshot.getVariableSnapshot(new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(1)));
        var entity3Snapshot =
                snapshot.getVariableSnapshot(new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(2)));

        assertThat(entity1Snapshot.getVariableDescriptor()).isEqualTo(variableDescriptor);
        assertThat(entity2Snapshot.getVariableDescriptor()).isEqualTo(variableDescriptor);
        assertThat(entity3Snapshot.getVariableDescriptor()).isEqualTo(variableDescriptor);

        assertThat(entity1Snapshot.getEntity()).isEqualTo(workingSolution.getEntityList().get(0));
        assertThat(entity2Snapshot.getEntity()).isEqualTo(workingSolution.getEntityList().get(1));
        assertThat(entity3Snapshot.getEntity()).isEqualTo(workingSolution.getEntityList().get(2));

        assertThat(entity1Snapshot.getValue()).isEqualTo(workingSolution.getValueList().get(0));
        assertThat(entity2Snapshot.getValue()).isEqualTo(workingSolution.getValueList().get(1));
        assertThat(entity3Snapshot.getValue()).isEqualTo(workingSolution.getValueList().get(2));
    }

    private void setValueList(TestdataListEntity entity, TestdataListValue... values) {
        List<TestdataListValue> valueList = entity.getValueList();
        valueList.clear();
        valueList.addAll(List.of(values));
    }

    @Test
    void testSnapshotListVariables() {
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var variableDescriptor =
                solutionDescriptor.getEntityDescriptorStrict(TestdataListEntity.class)
                        .getGenuineVariableDescriptor("valueList");

        var workingSolution = TestdataListSolution.generateInitializedSolution(9, 3);
        VariableSnapshotTotal<TestdataListSolution> snapshot = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        setValueList(workingSolution.getEntityList().get(0),
                workingSolution.getValueList().get(0),
                workingSolution.getValueList().get(1));
        setValueList(workingSolution.getEntityList().get(1),
                workingSolution.getValueList().get(2),
                workingSolution.getValueList().get(3),
                workingSolution.getValueList().get(4));
        setValueList(workingSolution.getEntityList().get(2),
                workingSolution.getValueList().get(5),
                workingSolution.getValueList().get(6),
                workingSolution.getValueList().get(7),
                workingSolution.getValueList().get(8));

        var entity1Snapshot =
                snapshot.getVariableSnapshot(new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(0)));
        var entity2Snapshot =
                snapshot.getVariableSnapshot(new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(1)));
        var entity3Snapshot =
                snapshot.getVariableSnapshot(new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(2)));

        assertThat(entity1Snapshot.getVariableDescriptor()).isEqualTo(variableDescriptor);
        assertThat(entity2Snapshot.getVariableDescriptor()).isEqualTo(variableDescriptor);
        assertThat(entity3Snapshot.getVariableDescriptor()).isEqualTo(variableDescriptor);

        assertThat(entity1Snapshot.getEntity()).isEqualTo(workingSolution.getEntityList().get(0));
        assertThat(entity2Snapshot.getEntity()).isEqualTo(workingSolution.getEntityList().get(1));
        assertThat(entity3Snapshot.getEntity()).isEqualTo(workingSolution.getEntityList().get(2));

        assertThat(entity1Snapshot.getValue()).isEqualTo(List.of(
                workingSolution.getValueList().get(0),
                workingSolution.getValueList().get(3),
                workingSolution.getValueList().get(6)));
        assertThat(entity2Snapshot.getValue()).isEqualTo(List.of(
                workingSolution.getValueList().get(1),
                workingSolution.getValueList().get(4),
                workingSolution.getValueList().get(7)));
        assertThat(entity3Snapshot.getValue()).isEqualTo(List.of(
                workingSolution.getValueList().get(2),
                workingSolution.getValueList().get(5),
                workingSolution.getValueList().get(8)));
    }

    @Test
    void testSnapshotDiffBasicVariables() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var variableDescriptor =
                solutionDescriptor.getEntityDescriptorStrict(TestdataEntity.class).getGenuineVariableDescriptor("value");

        var workingSolution = TestdataSolution.generateSolution(3, 3);
        VariableSnapshotTotal<TestdataSolution> before = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        workingSolution.getEntityList().get(0).setValue(null);
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(0));

        VariableSnapshotTotal<TestdataSolution> after = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        var diff = after.changedVariablesFrom(before);

        assertThat(diff).containsExactlyInAnyOrder(
                new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(0)),
                new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(1)));
    }

    @Test
    void testSnapshotDiffListVariables() {
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var variableDescriptor =
                solutionDescriptor.getEntityDescriptorStrict(TestdataListEntity.class)
                        .getGenuineVariableDescriptor("valueList");

        var workingSolution = TestdataListSolution.generateInitializedSolution(9, 3);
        VariableSnapshotTotal<TestdataListSolution> before = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        setValueList(workingSolution.getEntityList().get(0),
                workingSolution.getValueList().get(0),
                workingSolution.getValueList().get(3));
        setValueList(workingSolution.getEntityList().get(1),
                workingSolution.getValueList().get(1),
                workingSolution.getValueList().get(4),
                workingSolution.getValueList().get(6),
                workingSolution.getValueList().get(7));

        VariableSnapshotTotal<TestdataListSolution> after = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        var diff = after.changedVariablesFrom(before);

        assertThat(diff).containsExactlyInAnyOrder(
                new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(0)),
                new VariableId<>(variableDescriptor, workingSolution.getEntityList().get(1)));
    }
}
