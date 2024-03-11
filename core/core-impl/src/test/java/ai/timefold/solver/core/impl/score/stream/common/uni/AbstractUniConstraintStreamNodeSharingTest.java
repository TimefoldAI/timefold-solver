package ai.timefold.solver.core.impl.score.stream.common.uni;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamNodeSharingTest;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

public abstract class AbstractUniConstraintStreamNodeSharingTest extends AbstractConstraintStreamTest implements
        ConstraintStreamNodeSharingTest {

    private ConstraintFactory constraintFactory;
    private UniConstraintStream<TestdataEntity> baseStream;

    protected AbstractUniConstraintStreamNodeSharingTest(
            ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    @BeforeEach
    public void setup() {
        constraintFactory = buildConstraintFactory(TestdataSolution.buildSolutionDescriptor());
        baseStream = constraintFactory.forEach(TestdataEntity.class);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************
    @Override
    @TestTemplate
    public void differentParentSameFilter() {
        Predicate<TestdataEntity> filter1 = a -> true;
        Predicate<TestdataEntity> filter2 = a -> false;

        assertThat(baseStream.filter(filter1))
                .isNotSameAs(baseStream.filter(filter2)
                        .filter(filter1));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFilter() {
        Predicate<TestdataEntity> filter1 = a -> true;
        Predicate<TestdataEntity> filter2 = a -> false;

        assertThat(baseStream.filter(filter1))
                .isNotSameAs(baseStream.filter(filter2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFilter() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.filter(filter1))
                .isSameAs(baseStream.filter(filter1));
    }

    // ************************************************************************
    // Join
    // ************************************************************************
    @Override
    @TestTemplate
    public void differentLeftParentJoin() {
        Predicate<TestdataEntity> filter1 = a -> true;

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
        BiJoiner<TestdataEntity, TestdataValue> indexedJoiner1 = Joiners.equal(TestdataEntity::getCode, TestdataValue::getCode);
        BiJoiner<TestdataEntity, TestdataValue> indexedJoiner2 =
                Joiners.equal(TestdataEntity::toString, TestdataValue::toString);

        assertThat(baseStream.join(TestdataValue.class, indexedJoiner1))
                .isNotSameAs(baseStream.join(TestdataValue.class, indexedJoiner2));
    }

    @Override
    @TestTemplate
    public void sameParentsDifferentFilteringJoin() {
        BiJoiner<TestdataEntity, TestdataValue> filteringJoiner1 = Joiners.filtering((a, b) -> false);
        BiJoiner<TestdataEntity, TestdataValue> filteringJoiner2 = Joiners.filtering((a, b) -> true);

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
        BiJoiner<TestdataEntity, TestdataValue> indexedJoiner = Joiners.equal(TestdataEntity::getCode, TestdataValue::getCode);

        assertThat(baseStream.join(TestdataValue.class, indexedJoiner))
                .isSameAs(baseStream.join(TestdataValue.class, indexedJoiner));
    }

    @Override
    @TestTemplate
    public void sameParentsSameFilteringJoin() {
        BiJoiner<TestdataEntity, TestdataValue> filteringJoiner = Joiners.filtering((a, b) -> true);

        assertThat(baseStream.join(TestdataValue.class, filteringJoiner))
                .isSameAs(baseStream.join(TestdataValue.class, filteringJoiner));
    }

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    @Override
    @TestTemplate
    public void ifExistsOtherDifferentParent() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.ifExistsOther(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExistsOther(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    public void ifExistsOtherSameParentDifferentIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.equal();
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.equal(TestdataEntity::getCode);

        assertThat(baseStream.ifExistsOther(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsOther(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsOtherSameParentDifferentFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b) -> a == b);
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b) -> a != b);

        assertThat(baseStream.ifExistsOther(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsOther(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsOtherSameParentSameIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.equal();

        assertThat(baseStream.ifExistsOther(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsOther(TestdataEntity.class, joiner));
    }

    // Cannot test same filter since ifExistsOther will create a new filtering predicate

    @Override
    @TestTemplate
    public void ifNotExistsOtherDifferentParent() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.ifExistsOther(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExistsOther(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    public void ifNotExistsOtherSameParentDifferentIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.equal();
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.equal(TestdataEntity::getCode);

        assertThat(baseStream.ifNotExistsOther(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsOther(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsOtherSameParentDifferentFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b) -> a == b);
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b) -> a != b);

        assertThat(baseStream.ifNotExistsOther(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsOther(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsOtherSameParentSameIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.equal();

        assertThat(baseStream.ifNotExistsOther(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsOther(TestdataEntity.class, joiner));
    }

    // Cannot test same filter since ifExistsOther will create a new filtering predicate

    @Override
    @TestTemplate
    public void ifExistsOtherIncludingUnassignedDifferentParent() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.ifExistsOtherIncludingUnassigned(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExistsOtherIncludingUnassigned(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    public void ifExistsOtherIncludingUnassignedSameParentDifferentIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.equal();
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.equal(TestdataEntity::getCode);

        assertThat(baseStream.ifExistsOtherIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsOtherIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsOtherIncludingUnassignedSameParentDifferentFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b) -> a == b);
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b) -> a != b);

        assertThat(baseStream.ifExistsOtherIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsOtherIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsOtherIncludingUnassignedSameParentSameIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.equal();

        assertThat(baseStream.ifExistsOtherIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsOtherIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsOtherIncludingUnassignedDifferentParent() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.ifNotExistsOtherIncludingUnassigned(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExistsOtherIncludingUnassigned(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    public void ifNotExistsOtherIncludingUnassignedSameParentDifferentIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.equal();
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.equal(TestdataEntity::getCode);

        assertThat(baseStream.ifNotExistsOtherIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsOtherIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsOtherIncludingUnassignedSameParentDifferentFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b) -> a == b);
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b) -> a != b);

        assertThat(baseStream.ifNotExistsOtherIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsOtherIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsOtherIncludingUnassignedSameParentSameIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.equal();

        assertThat(baseStream.ifNotExistsOtherIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsOtherIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifExistsDifferentParent() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.ifExists(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExists(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsDifferentParent() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.ifNotExists(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExists(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifExistsIncludingUnassignedDifferentParent() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExistsIncludingUnassigned(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsIncludingUnassignedDifferentParent() {
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExistsIncludingUnassigned(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentSameIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.equal();

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentSameFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.filtering((a, b) -> a == b);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentSameIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.equal();

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentSameFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.filtering((a, b) -> a == b);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentSameIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.equal();

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentSameFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.filtering((a, b) -> a == b);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentSameIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.equal();

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentSameFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner = Joiners.filtering((a, b) -> a == b);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentDifferentIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.equal();
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.equal(TestdataEntity::getCode);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentDifferentFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b) -> a == b);
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b) -> a != b);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentDifferentIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.equal();
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.equal(TestdataEntity::getCode);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentDifferentFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b) -> a == b);
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b) -> a != b);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentDifferentIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.equal();
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.equal(TestdataEntity::getCode);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingUnassignedSameParentDifferentFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b) -> a == b);
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b) -> a != b);

        assertThat(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentDifferentIndexer() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.equal();
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.equal(TestdataEntity::getCode);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingUnassignedSameParentDifferentFilter() {
        BiJoiner<TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b) -> a == b);
        BiJoiner<TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b) -> a != b);

        assertThat(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingUnassigned(TestdataEntity.class, joiner2));
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    @TestTemplate
    public void differentParentGroupBy() {
        Predicate<TestdataEntity> filter1 = a -> true;
        Function<TestdataEntity, TestdataEntity> keyMapper = a -> a;

        assertThat(baseStream.groupBy(keyMapper))
                .isNotSameAs(baseStream.filter(filter1).groupBy(keyMapper));
    }

    @Override
    @TestTemplate
    public void differentKeyMapperGroupBy() {
        Function<TestdataEntity, Integer> keyMapper1 = a -> 0;
        Function<TestdataEntity, Integer> keyMapper2 = a -> 1;

        assertThat(baseStream.groupBy(keyMapper1))
                .isNotSameAs(baseStream.groupBy(keyMapper2));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentCollectorGroupBy() {
        assertThat(baseStream.groupBy(ConstraintCollectors.count()))
                .isNotSameAs(baseStream.groupBy(ConstraintCollectors.countDistinct()));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentCollectorFunctionGroupBy() {
        ToIntFunction<TestdataEntity> sumFunction1 = a -> 0;
        ToIntFunction<TestdataEntity> sumFunction2 = a -> 1;

        assertThat(baseStream.groupBy(ConstraintCollectors.sum(sumFunction1)))
                .isNotSameAs(baseStream.groupBy(ConstraintCollectors.sum(sumFunction2)));
    }

    @Override
    @TestTemplate
    public void sameParentSameKeyMapperGroupBy() {
        Function<TestdataEntity, Integer> keyMapper = a -> 0;

        assertThat(baseStream.groupBy(keyMapper))
                .isSameAs(baseStream.groupBy(keyMapper));
    }

    @Override
    @TestTemplate
    public void sameParentSameCollectorGroupBy() {
        ToIntFunction<TestdataEntity> sumFunction = a -> 0;

        assertThat(baseStream.groupBy(ConstraintCollectors.sum(sumFunction)))
                .isSameAs(baseStream.groupBy(ConstraintCollectors.sum(sumFunction)));
    }

    // ************************************************************************
    // Map/expand/flatten/distinct/concat
    // ************************************************************************

    @Override
    @TestTemplate
    public void differentParentSameFunctionExpand() {
        Predicate<TestdataEntity> filter1 = a -> true;
        Function<TestdataEntity, TestdataEntity> expander = a -> a;

        assertThat(baseStream.expand(expander))
                .isNotSameAs(baseStream.filter(filter1).expand(expander));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFunctionExpand() {
        Function<TestdataEntity, TestdataEntity> expander1 = a -> a;
        Function<TestdataEntity, TestdataEntity> expander2 = a -> null;

        assertThat(baseStream.expand(expander1))
                .isNotSameAs(baseStream.expand(expander2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFunctionExpand() {
        Function<TestdataEntity, TestdataEntity> expander = a -> a;

        assertThat(baseStream.expand(expander))
                .isSameAs(baseStream.expand(expander));
    }

    @Override
    @TestTemplate
    public void differentParentSameFunctionMap() {
        Predicate<TestdataEntity> filter1 = a -> true;
        Function<TestdataEntity, TestdataEntity> mapper = a -> a;

        assertThat(baseStream.map(mapper))
                .isNotSameAs(baseStream.filter(filter1).map(mapper));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFunctionMap() {
        Function<TestdataEntity, TestdataEntity> mapper1 = a -> a;
        Function<TestdataEntity, TestdataEntity> mapper2 = a -> null;

        assertThat(baseStream.map(mapper1))
                .isNotSameAs(baseStream.map(mapper2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFunctionMap() {
        Function<TestdataEntity, TestdataEntity> mapper = a -> a;

        assertThat(baseStream.map(mapper))
                .isSameAs(baseStream.map(mapper));
    }

    @Override
    @TestTemplate
    public void differentParentSameFunctionFlattenLast() {
        Predicate<TestdataEntity> filter1 = a -> true;
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
        Predicate<TestdataEntity> filter1 = a -> true;

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
        Predicate<TestdataEntity> sourceFilter = Objects::nonNull;
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream
                .concat(baseStream.filter(filter1)))
                .isNotSameAs(baseStream.filter(sourceFilter)
                        .concat(baseStream.filter(filter1)));
    }

    @Override
    @TestTemplate
    public void differentSecondSourceConcat() {
        Predicate<TestdataEntity> sourceFilter = Objects::nonNull;
        Predicate<TestdataEntity> filter1 = a -> true;

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
        Predicate<TestdataEntity> filter1 = a -> true;

        assertThat(baseStream
                .concat(baseStream.filter(filter1)))
                .isSameAs(baseStream.concat(baseStream.filter(filter1)));
    }
}
