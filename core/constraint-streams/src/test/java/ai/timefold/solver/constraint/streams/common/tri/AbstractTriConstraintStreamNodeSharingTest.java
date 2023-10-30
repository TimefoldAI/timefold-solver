package ai.timefold.solver.constraint.streams.common.tri;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Predicate;

import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamTest;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamNodeSharingTest;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

public abstract class AbstractTriConstraintStreamNodeSharingTest extends AbstractConstraintStreamTest implements
        ConstraintStreamNodeSharingTest {

    private ConstraintFactory constraintFactory;
    private TriConstraintStream<TestdataEntity, TestdataEntity, TestdataEntity> baseStream;

    protected AbstractTriConstraintStreamNodeSharingTest(
            ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    @BeforeEach
    public void setup() {
        constraintFactory = buildConstraintFactory(TestdataSolution.buildSolutionDescriptor());
        baseStream = constraintFactory.forEach(TestdataEntity.class)
                .join(TestdataEntity.class)
                .join(TestdataEntity.class);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************
    @Override
    @TestTemplate
    public void differentParentSameFilter() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter2 = (a, b, c) -> false;

        assertThat(baseStream.filter(filter1))
                .isNotSameAs(baseStream.filter(filter2)
                        .filter(filter1));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFilter() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter2 = (a, b, c) -> false;

        assertThat(baseStream.filter(filter1))
                .isNotSameAs(baseStream.filter(filter2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFilter() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;

        assertThat(baseStream.filter(filter1))
                .isSameAs(baseStream.filter(filter1));
    }

    // ************************************************************************
    // Join
    // ************************************************************************
    @Override
    @TestTemplate
    public void differentLeftParentJoin() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;

        assertThat(baseStream.join(TestdataValue.class))
                .isNotSameAs(baseStream.filter(filter1).join(TestdataValue.class));
    }

    @Override
    @TestTemplate
    public void differentRightParentJoin() {
        Predicate<TestdataValue> filter1 = a -> true;

        assertThat(baseStream.join(TestdataValue.class))
                .isNotSameAs(baseStream.join(constraintFactory
                        .forEach(TestdataValue.class)
                        .filter(filter1)));
    }

    @Override
    @TestTemplate
    public void sameParentsDifferentIndexerJoin() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataValue> indexedJoiner1 =
                Joiners.equal((a, b, c) -> a.getCode(), TestdataValue::getCode);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataValue> indexedJoiner2 =
                Joiners.equal((a, b, c) -> b.getCode(), TestdataValue::getCode);

        assertThat(baseStream.join(TestdataValue.class, indexedJoiner1))
                .isNotSameAs(baseStream.join(TestdataValue.class, indexedJoiner2));
    }

    @Override
    @TestTemplate
    public void sameParentsDifferentFilteringJoin() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataValue> filteringJoiner1 =
                Joiners.filtering((a, b, c, d) -> false);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataValue> filteringJoiner2 =
                Joiners.filtering((a, b, c, d) -> true);

        assertThat(baseStream.join(TestdataValue.class, filteringJoiner1))
                .isNotSameAs(baseStream.join(TestdataValue.class, filteringJoiner2));
    }

    @Override
    @TestTemplate
    public void sameParentsJoin() {
        assertThat(baseStream.join(TestdataValue.class))
                .isSameAs(baseStream.join(TestdataValue.class));
    }

    @Override
    @TestTemplate
    public void sameParentsSameIndexerJoin() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataValue> indexedJoiner =
                Joiners.equal((a, b, c) -> a.getCode(), TestdataValue::getCode);

        assertThat(baseStream.join(TestdataValue.class, indexedJoiner))
                .isSameAs(baseStream.join(TestdataValue.class, indexedJoiner));
    }

    @Override
    @TestTemplate
    public void sameParentsSameFilteringJoin() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataValue> filteringJoiner =
                Joiners.filtering((a, b, c, d) -> true);

        assertThat(baseStream.join(TestdataValue.class, filteringJoiner))
                .isSameAs(baseStream.join(TestdataValue.class, filteringJoiner));
    }

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    // TODO

    // ************************************************************************
    // Group by
    // ************************************************************************

    // TODO

    // ************************************************************************
    // Map/expand/flatten/distinct/concat
    // ************************************************************************

    // TODO Map/expand/flatten

    @Override
    @TestTemplate
    public void differentParentDistinct() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;

        assertThat(baseStream.distinct())
                .isNotSameAs(baseStream.filter(filter1).distinct());
    }

    @Override
    @TestTemplate
    public void sameParentDistinct() {
        assertThat(baseStream.distinct())
                .isSameAs(baseStream.distinct());
    }

    @Override
    @TestTemplate
    public void differentFirstSourceConcat() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> sourceFilter = (a, b, c) -> true;
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> false;

        assertThat(baseStream
                .concat(baseStream.filter(filter1)))
                .isNotSameAs(baseStream.filter(sourceFilter)
                        .concat(baseStream.filter(filter1)));
    }

    @Override
    @TestTemplate
    public void differentSecondSourceConcat() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> sourceFilter = (a, b, c) -> true;
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> false;

        assertThat(baseStream
                .filter(filter1)
                .concat(baseStream))
                .isNotSameAs(baseStream
                        .filter(filter1)
                        .concat(baseStream.filter(sourceFilter)));
    }

    @Override
    @TestTemplate
    public void sameSourcesConcat() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;

        assertThat(baseStream
                .concat(baseStream.filter(filter1)))
                .isSameAs(baseStream.concat(baseStream.filter(filter1)));
    }
}
