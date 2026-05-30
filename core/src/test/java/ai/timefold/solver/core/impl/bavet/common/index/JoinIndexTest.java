package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

/**
 * Covers the unified mechanism for equal-bearing joins: a single {@link JoinIndex} whose {@link JoinBucket}s
 * co-locate matching left and right tuples (an EQUAL match is {@code Objects.equals} = same bucket), built via
 * {@link IndexerFactory#buildJoinIndex()}.
 */
class JoinIndexTest {

    @Test
    void singleEqualBucketSharingAndOps() {
        // equal(gender): pure equal ⇒ the composite key IS the equal key (raw gender), bucket = the equal-key group.
        JoinIndex<UniTuple<String>, UniTuple<String>> joinIndex = new IndexerFactory<>(equalGender()).buildJoinIndex();

        var bucketF = joinIndex.getOrCreateBucket("F");
        assertThat(joinIndex.getOrCreateBucket("F")).isSameAs(bucketF); // matching keys ⇒ same bucket
        assertThat(joinIndex.getBucket("F")).isSameAs(bucketF);
        assertThat(joinIndex.getOrCreateBucket("M")).isNotSameAs(bucketF);

        var left1 = tuple("L1");
        var right1 = tuple("R1");
        var right2 = tuple("R2");
        var leftEntry = bucketF.addLeft("F", left1);
        bucketF.addRight("F", right1);
        var right2Entry = bucketF.addRight("F", right2);

        assertThat(bucketF.rightSize("F")).isEqualTo(2);
        assertThat(collectRight(bucketF, "F")).containsExactlyInAnyOrder(right1, right2);
        assertThat(collectLeft(bucketF, "F")).containsExactly(left1);

        bucketF.removeRight("F", right2Entry);
        assertThat(bucketF.rightSize("F")).isEqualTo(1);
        assertThat(bucketF.isEmpty()).isFalse();

        bucketF.removeLeft("F", leftEntry);
        assertThat(bucketF.isEmpty()).isFalse(); // right1 still present
    }

    @Test
    void emptyBucketCleanup() {
        JoinIndex<UniTuple<String>, UniTuple<String>> joinIndex = new IndexerFactory<>(equalGender()).buildJoinIndex();
        var bucket = joinIndex.getOrCreateBucket("F");
        var leftEntry = bucket.addLeft("F", tuple("L"));

        joinIndex.removeBucketIfEmpty("F", bucket); // a live left tuple ⇒ not dropped
        assertThat(joinIndex.getBucket("F")).isSameAs(bucket);

        bucket.removeLeft("F", leftEntry);
        assertThat(bucket.isEmpty()).isTrue();
        joinIndex.removeBucketIfEmpty("F", bucket); // both sides empty ⇒ dropped
        assertThat(joinIndex.getBucket("F")).isNull();
        assertThat(joinIndex.isEmpty()).isTrue();
    }

    @Test
    void equalPlusEqualKeyedByCompositeKey() {
        // equal(gender) AND equal(age): still pure equal ⇒ keyed by the whole composite key.
        JoinIndex<UniTuple<String>, UniTuple<String>> joinIndex =
                new IndexerFactory<>(equalGenderEqualAge()).buildJoinIndex();
        var bucket = joinIndex.getOrCreateBucket(CompositeKey.of("F", 40));
        assertThat(joinIndex.getOrCreateBucket(CompositeKey.of("F", 40))).isSameAs(bucket); // equal-by-value ⇒ same bucket
        assertThat(joinIndex.getOrCreateBucket(CompositeKey.of("F", 30))).isNotSameAs(bucket);
        assertThat(joinIndex.getOrCreateBucket(CompositeKey.of("M", 40))).isNotSameAs(bucket);

        var right = tuple("R");
        bucket.addRight(CompositeKey.of("F", 40), right);
        assertThat(bucket.rightSize(CompositeKey.of("F", 40))).isEqualTo(1);
        assertThat(collectRight(bucket, CompositeKey.of("F", 40))).containsExactly(right);
    }

    @Test
    void equalPlusLessThanSuffixIsPerSideAndFlipped() {
        // equal(gender) AND lessThan(age): bucket keyed by gender only; the right side carries a (flipped) age suffix.
        JoinIndex<UniTuple<String>, UniTuple<String>> joinIndex =
                new IndexerFactory<>(equalGenderLessThanAge()).buildJoinIndex();

        var bucket = joinIndex.getOrCreateBucket(CompositeKey.of("F", 40));
        assertThat(joinIndex.getOrCreateBucket(CompositeKey.of("F", 99))).isSameAs(bucket); // same gender ⇒ same bucket
        assertThat(joinIndex.getOrCreateBucket(CompositeKey.of("M", 40))).isNotSameAs(bucket);

        var rightAge30 = tuple("rightAge30");
        var rightAge50 = tuple("rightAge50");
        bucket.addRight(CompositeKey.of("F", 30), rightAge30);
        bucket.addRight(CompositeKey.of("F", 50), rightAge50);

        // A left tuple ("F", age) matches right tuples with right.age > left.age (left.age < right.age).
        assertThat(collectRight(bucket, CompositeKey.of("F", 40))).containsExactly(rightAge50);
        assertThat(collectRight(bucket, CompositeKey.of("F", 20))).containsExactlyInAnyOrder(rightAge30, rightAge50);
        assertThat(collectRight(bucket, CompositeKey.of("F", 60))).isEmpty();
    }

    @Test
    void hasSuffixAndIsSameBucket() {
        // equal(gender) AND lessThan(age): has a suffix ⇒ different composite keys can share a bucket (same gender).
        JoinIndex<UniTuple<String>, UniTuple<String>> suffixed =
                new IndexerFactory<>(equalGenderLessThanAge()).buildJoinIndex();
        assertThat(suffixed.hasSuffix()).isTrue();
        // Same equal prefix (gender), different suffix (age) ⇒ same bucket: the changed-key update can reuse it.
        assertThat(suffixed.isSameBucket(CompositeKey.of("F", 30), CompositeKey.of("F", 50))).isTrue();
        // Different equal prefix ⇒ different bucket.
        assertThat(suffixed.isSameBucket(CompositeKey.of("F", 30), CompositeKey.of("M", 30))).isFalse();

        // Pure equal: no suffix ⇒ the node never reuses (a changed key is always a different bucket).
        JoinIndex<UniTuple<String>, UniTuple<String>> pureEqual = new IndexerFactory<>(equalGender()).buildJoinIndex();
        assertThat(pureEqual.hasSuffix()).isFalse();
    }

    private static UniTuple<String> tuple(String factA) {
        return UniTuple.of(factA, 0);
    }

    private static List<UniTuple<String>> collectRight(JoinBucket<UniTuple<String>, UniTuple<String>> bucket, Object key) {
        var result = new ArrayList<UniTuple<String>>();
        bucket.forEachRight(key, result::add);
        return result;
    }

    private static List<UniTuple<String>> collectLeft(JoinBucket<UniTuple<String>, UniTuple<String>> bucket, Object key) {
        var result = new ArrayList<UniTuple<String>>();
        bucket.forEachLeft(key, result::add);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static DefaultBiJoiner<TestPerson, TestPerson> equalGender() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender);
    }

    @SuppressWarnings("unchecked")
    private static DefaultBiJoiner<TestPerson, TestPerson> equalGenderEqualAge() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender)
                .and(Joiners.equal(TestPerson::age));
    }

    @SuppressWarnings("unchecked")
    private static DefaultBiJoiner<TestPerson, TestPerson> equalGenderLessThanAge() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender)
                .and(Joiners.lessThan(TestPerson::age));
    }

}
