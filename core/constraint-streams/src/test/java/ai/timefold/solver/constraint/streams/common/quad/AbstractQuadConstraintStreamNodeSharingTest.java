package ai.timefold.solver.constraint.streams.common.quad;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamTest;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamNodeSharingTest;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
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

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifExistsDifferentParent() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

        assertThat(baseStream.ifExists(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExists(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsDifferentParent() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

        assertThat(baseStream.ifNotExists(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExists(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifExistsIncludingNullVarsDifferentParent() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExistsIncludingNullVars(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsIncludingNullVarsDifferentParent() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExistsIncludingNullVars(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentSameIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c, d) -> a, e -> e);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentSameFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d, e) -> a == b);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentSameIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c, d) -> a, e -> e);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentSameFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d, e) -> a == b);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingNullVarsSameParentSameIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c, d) -> a, e -> e);

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingNullVarsSameParentSameFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d, e) -> a == b);

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingNullVarsSameParentSameIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c, d) -> a, e -> e);

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingNullVarsSameParentSameFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d, e) -> a == b);

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentDifferentIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c, d) -> a, e -> e);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c, d) -> b, e -> e);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentDifferentFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d, e) -> a == b);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d, e) -> a != b);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentDifferentIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c, d) -> a, e -> e);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c, d) -> b, e -> e);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentDifferentFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d, e) -> a == b);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d, e) -> a != b);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingNullVarsSameParentDifferentIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c, d) -> a, e -> e);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c, d) -> b, e -> e);

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingNullVarsSameParentDifferentFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d, e) -> a == b);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d, e) -> a != b);

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingNullVarsSameParentDifferentIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c, d) -> a, e -> e);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c, d) -> b, e -> e);

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingNullVarsSameParentDifferentFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d, e) -> a == b);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d, e) -> a != b);

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner2));
    }

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
