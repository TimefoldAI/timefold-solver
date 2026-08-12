package ai.timefold.solver.core.impl.neighborhood.bias;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

/**
 * One entity ({@code "b0"}, {@code "b1"}, ...) per bucket in {@code bucketSizeList}, each paired
 * only with its own bucket's values ({@code "b0-0"}, {@code "b0-1"}, ..., matched by an indexing
 * {@code equal} on the code prefix). A bucket size of 0 gives its entity zero matching values,
 * which is how {@link LeftRetirementBiasIT} builds an unreachable ("dead") left without a separate
 * fixture: liveness here just means a non-empty bucket.
 */
final class BucketedFixture {

    final PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable =
            TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class).basicVariable("value",
                    TestdataValue.class);
    final List<TestdataEntity> entityList = new ArrayList<>();
    final List<TestdataValue> valueList = new ArrayList<>();
    final TestdataSolution solution;

    BucketedFixture(List<Integer> bucketSizeList) {
        for (var bucketIndex = 0; bucketIndex < bucketSizeList.size(); bucketIndex++) {
            entityList.add(new TestdataEntity("b" + bucketIndex));
            for (var i = 0; i < bucketSizeList.get(bucketIndex); i++) {
                valueList.add(new TestdataValue("b" + bucketIndex + "-" + i));
            }
        }
        solution = new TestdataSolution("solution");
        solution.setEntityList(entityList);
        solution.setValueList(valueList);
    }

    BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> joiner() {
        return NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(TestdataEntity::getCode,
                value -> value.getCode().split("-")[0]);
    }

    BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> filteringJoiner() {
        return NeighborhoodsJoiners.<TestdataSolution, TestdataEntity, TestdataValue> filtering(
                (solutionView, entity, value) -> value.getCode().startsWith(entity.getCode() + "-"));
    }

    /**
     * Every code-only-in-this-bucket move; empty for a bucket with size 0.
     */
    Set<Move<TestdataSolution>> movesFor(int bucketIndex) {
        var entity = entityList.get(bucketIndex);
        var moveSet = new HashSet<Move<TestdataSolution>>();
        for (var value : valueList) {
            if (value.getCode().startsWith(entity.getCode() + "-")) {
                moveSet.add(Moves.change(variable, entity, value));
            }
        }
        return moveSet;
    }

    int bucketSize(int bucketIndex) {
        return movesFor(bucketIndex).size();
    }

}
