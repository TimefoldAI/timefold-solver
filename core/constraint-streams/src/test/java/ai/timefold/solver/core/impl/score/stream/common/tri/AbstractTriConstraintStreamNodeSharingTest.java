package ai.timefold.solver.core.impl.score.stream.common.tri;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamNodeSharingTest;
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

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifExistsDifferentParent() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;

        assertThat(baseStream.ifExists(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExists(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsDifferentParent() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;

        assertThat(baseStream.ifNotExists(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExists(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifExistsIncludingUnassignedDifferentParent() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExistsIncludingUnassigned(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsIncludingUnassignedDifferentParent() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExistsIncludingUnassigned(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentSameIndexer() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c) -> a, d -> d);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentSameFilter() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d) -> a == b);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentSameIndexer() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c) -> a, d -> d);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentSameFilter() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d) -> a == b);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentSameIndexer() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c) -> a, d -> d);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentSameFilter() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d) -> a == b);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentSameIndexer() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.equal((a, b, c) -> a, d -> d);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentSameFilter() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner =
                Joiners.filtering((a, b, c, d) -> a == b);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentDifferentIndexer() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c) -> a, d -> d);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c) -> b, d -> d);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentDifferentFilter() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d) -> a == b);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d) -> a != b);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentDifferentIndexer() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c) -> a, d -> d);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c) -> b, d -> d);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentDifferentFilter() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d) -> a == b);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d) -> a != b);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentDifferentIndexer() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c) -> a, d -> d);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c) -> b, d -> d);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentDifferentFilter() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d) -> a == b);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d) -> a != b);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentDifferentIndexer() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.equal((a, b, c) -> a, d -> d);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.equal((a, b, c) -> b, d -> d);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentDifferentFilter() {
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner1 =
                Joiners.filtering((a, b, c, d) -> a == b);
        QuadJoiner<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> joiner2 =
                Joiners.filtering((a, b, c, d) -> a != b);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    @TestTemplate
    public void differentParentGroupBy() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper = (a, b, c) -> a;

        assertThat(baseStream.groupBy(keyMapper))
                .isNotSameAs(baseStream.filter(filter1).groupBy(keyMapper));
    }

    @Override
    @TestTemplate
    public void differentKeyMapperGroupBy() {
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper1 = (a, b, c) -> a;
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper2 = (a, b, c) -> b;

        assertThat(baseStream.groupBy(keyMapper1))
                .isNotSameAs(baseStream.groupBy(keyMapper2));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentCollectorGroupBy() {
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper = (a, b, c) -> a;

        assertThat(baseStream.groupBy(ConstraintCollectors.toList(keyMapper)))
                .isNotSameAs(baseStream.groupBy(ConstraintCollectors.countDistinct(keyMapper)));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentCollectorFunctionGroupBy() {
        ToIntTriFunction<TestdataEntity, TestdataEntity, TestdataEntity> sumFunction1 = (a, b, c) -> 0;
        ToIntTriFunction<TestdataEntity, TestdataEntity, TestdataEntity> sumFunction2 = (a, b, c) -> 1;

        assertThat(baseStream.groupBy(ConstraintCollectors.sum(sumFunction1)))
                .isNotSameAs(baseStream.groupBy(ConstraintCollectors.sum(sumFunction2)));
    }

    @Override
    @TestTemplate
    public void sameParentSameKeyMapperGroupBy() {
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> keyMapper = (a, b, c) -> a;

        assertThat(baseStream.groupBy(keyMapper))
                .isSameAs(baseStream.groupBy(keyMapper));
    }

    @Override
    @TestTemplate
    public void sameParentSameCollectorGroupBy() {
        ToIntTriFunction<TestdataEntity, TestdataEntity, TestdataEntity> sumFunction = (a, b, c) -> 0;

        assertThat(baseStream.groupBy(ConstraintCollectors.sum(sumFunction)))
                .isSameAs(baseStream.groupBy(ConstraintCollectors.sum(sumFunction)));
    }

    // ************************************************************************
    // Map/expand/flatten/distinct/concat
    // ************************************************************************

    @Override
    @TestTemplate
    public void differentParentSameFunctionExpand() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> expander = (a, b, c) -> a;

        assertThat(baseStream.expand(expander))
                .isNotSameAs(baseStream.filter(filter1).expand(expander));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFunctionExpand() {
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> expander1 = (a, b, c) -> a;
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> expander2 = (a, b, c) -> b;

        assertThat(baseStream.expand(expander1))
                .isNotSameAs(baseStream.expand(expander2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFunctionExpand() {
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> expander = (a, b, c) -> a;

        assertThat(baseStream.expand(expander))
                .isSameAs(baseStream.expand(expander));
    }

    @Override
    @TestTemplate
    public void differentParentSameFunctionMap() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> mapper = (a, b, c) -> a;

        assertThat(baseStream.map(mapper))
                .isNotSameAs(baseStream.filter(filter1).map(mapper));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFunctionMap() {
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> mapper1 = (a, b, c) -> a;
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> mapper2 = (a, b, c) -> a;

        assertThat(baseStream.map(mapper1))
                .isNotSameAs(baseStream.map(mapper2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFunctionMap() {
        TriFunction<TestdataEntity, TestdataEntity, TestdataEntity, TestdataEntity> mapper = (a, b, c) -> a;

        assertThat(baseStream.map(mapper))
                .isSameAs(baseStream.map(mapper));
    }

    @Override
    @TestTemplate
    public void differentParentSameFunctionFlattenLast() {
        TriPredicate<TestdataEntity, TestdataEntity, TestdataEntity> filter1 = (a, b, c) -> true;
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
