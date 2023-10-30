package ai.timefold.solver.constraint.streams.common.quad;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamTest;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamNodeSharingTest;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

public abstract class AbstractQuadConstraintStreamNodeSharingTest extends AbstractConstraintStreamTest implements
        ConstraintStreamNodeSharingTest {

    private ConstraintFactory constraintFactory;
    private QuadConstraintStream<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> baseStream;

    protected AbstractQuadConstraintStreamNodeSharingTest(
            ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    @BeforeEach
    public void setup() {
        constraintFactory = buildConstraintFactory(TestdataSolution.buildSolutionDescriptor());
        baseStream = constraintFactory.forEach(TestdataEntity.class)
                .join(TestdataEntity.class)
                .join(TestdataEntity.class)
                .join(TestdataEntity.class);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************
    @Override
    @TestTemplate
    public void differentParentSameFilter() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter2 = (a, b, c, d) -> false;

        assertThat(baseStream.filter(filter1))
                .isNotSameAs(baseStream.filter(filter2)
                        .filter(filter1));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFilter() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter2 = (a, b, c, d) -> false;

        assertThat(baseStream.filter(filter1))
                .isNotSameAs(baseStream.filter(filter2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFilter() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

        assertThat(baseStream.filter(filter1))
                .isSameAs(baseStream.filter(filter1));
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
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

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
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> sourceFilter = (a, b, c, d) -> true;
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> false;

        assertThat(baseStream
                .concat(baseStream.filter(filter1)))
                .isNotSameAs(baseStream.filter(sourceFilter)
                        .concat(baseStream.filter(filter1)));
    }

    @Override
    @TestTemplate
    public void differentSecondSourceConcat() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> sourceFilter = (a, b, c, d) -> true;
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> false;

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
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

        assertThat(baseStream
                .concat(baseStream.filter(filter1)))
                .isSameAs(baseStream.concat(baseStream.filter(filter1)));
    }
}
