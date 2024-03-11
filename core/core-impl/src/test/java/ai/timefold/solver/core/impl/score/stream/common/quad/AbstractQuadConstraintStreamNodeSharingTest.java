package ai.timefold.solver.core.impl.score.stream.common.quad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamNodeSharingTest;
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
    public void ifExistsIncludingUnassignedDifferentParent() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExistsIncludingUnassigned(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsIncludingUnassignedDifferentParent() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExistsIncludingUnassigned(TestdataEntity.class));
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
    public void ifExistsIncludingUnassignedSameParentSameIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c, d) -> a, e -> e);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentSameFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d, e) -> a == b);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentSameIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c, d) -> a, e -> e);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentSameFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d, e) -> a == b);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner));
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
    public void ifExistsIncludingUnassignedSameParentDifferentIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c, d) -> a, e -> e);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c, d) -> b, e -> e);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentDifferentFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d, e) -> a == b);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d, e) -> a != b);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentDifferentIndexer() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c, d) -> a, e -> e);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c, d) -> b, e -> e);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentDifferentFilter() {
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d, e) -> a == b);
        PentaJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d, e) -> a != b);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    @TestTemplate
    public void differentParentGroupBy() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper =
                (a, b, c, d) -> a;

        assertThat(baseStream.groupBy(keyMapper))
                .isNotSameAs(baseStream.filter(filter1).groupBy(keyMapper));
    }

    @Override
    @TestTemplate
    public void differentKeyMapperGroupBy() {
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper1 =
                (a, b, c, d) -> a;
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper2 =
                (a, b, c, d) -> b;

        assertThat(baseStream.groupBy(keyMapper1))
                .isNotSameAs(baseStream.groupBy(keyMapper2));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentCollectorGroupBy() {
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper =
                (a, b, c, d) -> a;

        assertThat(baseStream.groupBy(ConstraintCollectors.toList(keyMapper)))
                .isNotSameAs(baseStream.groupBy(ConstraintCollectors.countDistinct(keyMapper)));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentCollectorFunctionGroupBy() {
        ToIntQuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> sumFunction1 = (a, b, c, d) -> 0;
        ToIntQuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> sumFunction2 = (a, b, c, d) -> 0;

        assertThat(baseStream.groupBy(ConstraintCollectors.sum(sumFunction1)))
                .isNotSameAs(baseStream.groupBy(ConstraintCollectors.sum(sumFunction2)));
    }

    @Override
    @TestTemplate
    public void sameParentSameKeyMapperGroupBy() {
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper =
                (a, b, c, d) -> a;

        assertThat(baseStream.groupBy(keyMapper))
                .isSameAs(baseStream.groupBy(keyMapper));
    }

    @Override
    @TestTemplate
    public void sameParentSameCollectorGroupBy() {
        ToIntQuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> sumFunction = (a, b, c, d) -> 0;

        assertThat(baseStream.groupBy(ConstraintCollectors.sum(sumFunction)))
                .isSameAs(baseStream.groupBy(ConstraintCollectors.sum(sumFunction)));
    }

    // ************************************************************************
    // Map/expand/flatten/distinct/concat
    // ************************************************************************

    @Override
    @TestTemplate
    public void differentParentSameFunctionMap() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> mapper = (a, b, c, d) -> a;

        assertThat(baseStream.map(mapper))
                .isNotSameAs(baseStream.filter(filter1).map(mapper));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFunctionMap() {
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> mapper1 =
                (a, b, c, d) -> a;
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> mapper2 =
                (a, b, c, d) -> b;

        assertThat(baseStream.map(mapper1))
                .isNotSameAs(baseStream.map(mapper2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFunctionMap() {
        QuadFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> mapper = (a, b, c, d) -> a;

        assertThat(baseStream.map(mapper))
                .isSameAs(baseStream.map(mapper));
    }

    @Override
    @TestTemplate
    public void differentParentSameFunctionFlattenLast() {
        QuadPredicate<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c, d) -> true;
        Function<TestdataEntity, Iterable<TestdataEntity>> flattener = a -> Collections.emptyList();

        assertThat(baseStream.flattenLast(flattener))
                .isNotSameAs(baseStream.filter(filter1).flattenLast(flattener));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFunctionFlattenLast() {
        Function<TestdataEntity, Iterable<TestdataEntity>> flattener1 = a -> Collections.emptyList();
        Function<TestdataEntity, Iterable<TestdataEntity>> flattener2 = a -> Collections.emptySet();

        assertThat(baseStream.flattenLast(flattener1))
                .isNotSameAs(baseStream.flattenLast(flattener2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFunctionFlattenLast() {
        Function<TestdataEntity, Iterable<TestdataEntity>> flattener = a -> Collections.emptyList();

        assertThat(baseStream.flattenLast(flattener))
                .isSameAs(baseStream.flattenLast(flattener));
    }

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
