package ai.timefold.solver.constraint.streams.common.bi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamTest;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.ConstraintStreamNodeSharingTest;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

public abstract class AbstractBiConstraintStreamNodeSharingTest extends AbstractConstraintStreamTest implements
        ConstraintStreamNodeSharingTest {

    private ConstraintFactory constraintFactory;
    private BiConstraintStream<TestdataEntity, TestdataEntity> baseStream;

    protected AbstractBiConstraintStreamNodeSharingTest(
            ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    @BeforeEach
    public void setup() {
        constraintFactory = buildConstraintFactory(TestdataSolution.buildSolutionDescriptor());
        baseStream = constraintFactory.forEach(TestdataEntity.class).join(TestdataEntity.class);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************
    @Override
    @TestTemplate
    public void differentParentSameFilter() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;
        BiPredicate<TestdataEntity, TestdataEntity> filter2 = (a, b) -> false;

        assertThat(baseStream.filter(filter1))
                .isNotSameAs(baseStream.filter(filter2)
                        .filter(filter1));
    }

    @Override
    @TestTemplate
    public void sameParentDifferentFilter() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;
        BiPredicate<TestdataEntity, TestdataEntity> filter2 = (a, b) -> false;

        assertThat(baseStream.filter(filter1))
                .isNotSameAs(baseStream.filter(filter2));
    }

    @Override
    @TestTemplate
    public void sameParentSameFilter() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

        assertThat(baseStream.filter(filter1))
                .isSameAs(baseStream.filter(filter1));
    }

    // ************************************************************************
    // Join
    // ************************************************************************
    @Override
    @TestTemplate
    public void differentLeftParentJoin() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

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
        TriJoiner<TestdataEntity, TestdataEntity, TestdataValue> indexedJoiner1 =
                Joiners.equal((a, b) -> a.getCode(), TestdataValue::getCode);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataValue> indexedJoiner2 =
                Joiners.equal((a, b) -> b.getCode(), TestdataValue::getCode);

        assertThat(baseStream.join(TestdataValue.class, indexedJoiner1))
                .isNotSameAs(baseStream.join(TestdataValue.class, indexedJoiner2));
    }

    @Override
    @TestTemplate
    public void sameParentsDifferentFilteringJoin() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataValue> filteringJoiner1 = Joiners.filtering((a, b, c) -> false);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataValue> filteringJoiner2 = Joiners.filtering((a, b, c) -> true);

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
        TriJoiner<TestdataEntity, TestdataEntity, TestdataValue> indexedJoiner =
                Joiners.equal((a, b) -> a.getCode(), TestdataValue::getCode);

        assertThat(baseStream.join(TestdataValue.class, indexedJoiner))
                .isSameAs(baseStream.join(TestdataValue.class, indexedJoiner));
    }

    @Override
    @TestTemplate
    public void sameParentsSameFilteringJoin() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataValue> filteringJoiner = Joiners.filtering((a, b, c) -> true);

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
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

        assertThat(baseStream.ifExists(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExists(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsDifferentParent() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

        assertThat(baseStream.ifNotExists(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExists(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifExistsIncludingNullVarsDifferentParent() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifExistsIncludingNullVars(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    @SuppressWarnings("unchecked")
    public void ifNotExistsIncludingNullVarsDifferentParent() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class))
                .isNotSameAs(baseStream.filter(filter1).ifNotExistsIncludingNullVars(TestdataEntity.class));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentSameIndexer() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner = Joiners.equal((a, b) -> a, c -> c);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentSameFilter() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner = Joiners.filtering((a, b, c) -> a == b);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentSameIndexer() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner = Joiners.equal((a, b) -> a, c -> c);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentSameFilter() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner = Joiners.filtering((a, b, c) -> a == b);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingNullVarsSameParentSameIndexer() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner = Joiners.equal((a, b) -> a, c -> c);

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingNullVarsSameParentSameFilter() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner = Joiners.filtering((a, b, c) -> a == b);

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingNullVarsSameParentSameIndexer() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner = Joiners.equal((a, b) -> a, c -> c);

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingNullVarsSameParentSameFilter() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner = Joiners.filtering((a, b, c) -> a == b);

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner))
                .isSameAs(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentDifferentIndexer() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner1 = Joiners.equal((a, b) -> a, c -> c);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner2 = Joiners.equal((a, b) -> b, c -> c);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsSameParentDifferentFilter() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b, c) -> a == b);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b, c) -> a != b);

        assertThat(baseStream.ifExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentDifferentIndexer() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner1 = Joiners.equal((a, b) -> a, c -> c);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner2 = Joiners.equal((a, b) -> b, c -> c);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsSameParentDifferentFilter() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b, c) -> a == b);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b, c) -> a != b);

        assertThat(baseStream.ifNotExists(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExists(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingNullVarsSameParentDifferentIndexer() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner1 = Joiners.equal((a, b) -> a, c -> c);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner2 = Joiners.equal((a, b) -> b, c -> c);

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifExistsIncludingNullVarsSameParentDifferentFilter() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b, c) -> a == b);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b, c) -> a != b);

        assertThat(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifExistsIncludingNullVars(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingNullVarsSameParentDifferentIndexer() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner1 = Joiners.equal((a, b) -> a, c -> c);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner2 = Joiners.equal((a, b) -> b, c -> c);

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner2));
    }

    @Override
    @TestTemplate
    public void ifNotExistsIncludingNullVarsSameParentDifferentFilter() {
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner1 = Joiners.filtering((a, b, c) -> a == b);
        TriJoiner<TestdataEntity, TestdataEntity, TestdataEntity> joiner2 = Joiners.filtering((a, b, c) -> a != b);

        assertThat(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner1))
                .isNotSameAs(baseStream.ifNotExistsIncludingNullVars(TestdataEntity.class, joiner2));
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    public void differentParentGroupBy() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;
        BiFunction<TestdataEntity, TestdataEntity, TestdataEntity> keyMapper = (a, b) -> a;

        assertThat(baseStream.groupBy(keyMapper))
                .isNotSameAs(baseStream.filter(filter1).groupBy(keyMapper));
    }

    @Override
    public void differentKeyMapperGroupBy() {
        BiFunction<TestdataEntity, TestdataEntity, TestdataEntity> keyMapper1 = (a, b) -> a;
        BiFunction<TestdataEntity, TestdataEntity, TestdataEntity> keyMapper2 = (a, b) -> b;

        assertThat(baseStream.groupBy(keyMapper1))
                .isNotSameAs(baseStream.groupBy(keyMapper2));
    }

    @Override
    public void sameParentDifferentCollectorGroupBy() {
        BiFunction<TestdataEntity, TestdataEntity, TestdataEntity> keyMapper = (a, b) -> a;

        assertThat(baseStream.groupBy(ConstraintCollectors.toList(keyMapper)))
                .isNotSameAs(baseStream.groupBy(ConstraintCollectors.countDistinct(keyMapper)));
    }

    @Override
    public void sameParentDifferentCollectorFunctionGroupBy() {
        ToIntBiFunction<TestdataEntity, TestdataEntity> sumFunction1 = (a, b) -> 0;
        ToIntBiFunction<TestdataEntity, TestdataEntity> sumFunction2 = (a, b) -> 1;

        assertThat(baseStream.groupBy(ConstraintCollectors.sum(sumFunction1)))
                .isNotSameAs(baseStream.groupBy(ConstraintCollectors.sum(sumFunction2)));
    }

    @Override
    public void sameParentSameKeyMapperGroupBy() {
        BiFunction<TestdataEntity, TestdataEntity, Integer> keyMapper = (a, b) -> 0;

        assertThat(baseStream.groupBy(keyMapper))
                .isSameAs(baseStream.groupBy(keyMapper));
    }

    @Override
    public void sameParentSameCollectorGroupBy() {
        ToIntBiFunction<TestdataEntity, TestdataEntity> sumFunction = (a, b) -> 0;

        assertThat(baseStream.groupBy(ConstraintCollectors.sum(sumFunction)))
                .isSameAs(baseStream.groupBy(ConstraintCollectors.sum(sumFunction)));
    }

    // ************************************************************************
    // Map/expand/flatten/distinct/concat
    // ************************************************************************

    // TODO Map/expand/flatten

    @Override
    @TestTemplate
    public void differentParentDistinct() {
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

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
        BiPredicate<TestdataEntity, TestdataEntity> sourceFilter = (a, b) -> a != b;
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

        assertThat(baseStream
                .concat(baseStream.filter(filter1)))
                .isNotSameAs(baseStream.filter(sourceFilter)
                        .concat(baseStream.filter(filter1)));
    }

    @Override
    @TestTemplate
    public void differentSecondSourceConcat() {
        BiPredicate<TestdataEntity, TestdataEntity> sourceFilter = (a, b) -> a != b;
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

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
        BiPredicate<TestdataEntity, TestdataEntity> filter1 = (a, b) -> true;

        assertThat(baseStream
                .concat(baseStream.filter(filter1)))
                .isSameAs(baseStream.concat(baseStream.filter(filter1)));
    }
}
