package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.index.FusedEqualIndex.Bucket;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

/**
 * Covers the unified mechanism for equal-bearing joins: a single {@link FusedEqualIndex} whose {@link Bucket}s
 * co-locate matching left and right tuples (an EQUAL match is {@code Objects.equals} = same bucket), built via
 * {@link IndexerFactory#buildFusedEqualIndex()}.
 */
class FusedEqualIndexTest {

    @Test
    void singleEqualBucketSharingAndOps() {
        // equal(gender): pure equal ⇒ the composite key IS the equal key (raw gender), bucket = the equal-key group.
        FusedEqualIndex<UniTuple<String>, UniTuple<String>> fusedEqualIndex =
                new IndexerFactory<>(equalGender()).buildFusedEqualIndex();

        var bucketF = fusedEqualIndex.getOrCreateBucket("F");
        assertThat(fusedEqualIndex.getOrCreateBucket("F")).isSameAs(bucketF); // matching keys ⇒ same bucket
        assertThat(fusedEqualIndex.getBucket("F")).isSameAs(bucketF);
        assertThat(fusedEqualIndex.getOrCreateBucket("M")).isNotSameAs(bucketF);

        var left1 = tuple("L1");
        var right1 = tuple("R1");
        var right2 = tuple("R2");
        var leftEntry = bucketF.putLeft("F", left1);
        bucketF.putRight("F", right1);
        var right2Entry = bucketF.putRight("F", right2);

        assertThat(bucketF.sizeRight("F")).isEqualTo(2);
        assertThat(collectRight(bucketF, "F")).containsExactlyInAnyOrder(right1, right2);
        assertThat(collectLeft(bucketF, "F")).containsExactly(left1);

        bucketF.removeRight("F", right2Entry);
        assertThat(bucketF.sizeRight("F")).isEqualTo(1);
        assertThat(bucketF.isRemovable()).isFalse();

        bucketF.removeLeft("F", leftEntry);
        assertThat(bucketF.isRemovable()).isFalse(); // right1 still present
    }

    @Test
    void emptyBucketCleanup() {
        FusedEqualIndex<UniTuple<String>, UniTuple<String>> fusedEqualIndex =
                new IndexerFactory<>(equalGender()).buildFusedEqualIndex();
        var bucket = fusedEqualIndex.getOrCreateBucket("F");
        var leftEntry = bucket.putLeft("F", tuple("L"));

        fusedEqualIndex.removeBucketIfEmpty("F", bucket); // a live left tuple ⇒ not dropped
        assertThat(fusedEqualIndex.getBucket("F")).isSameAs(bucket);

        bucket.removeLeft("F", leftEntry);
        assertThat(bucket.isRemovable()).isTrue();
        fusedEqualIndex.removeBucketIfEmpty("F", bucket); // both sides empty ⇒ dropped
        assertThat(fusedEqualIndex.getBucket("F")).isNull();
        assertThat(fusedEqualIndex.isEmpty()).isTrue();
    }

    @Test
    void equalPlusEqualKeyedByCompositeKey() {
        // equal(gender) AND equal(age): still pure equal ⇒ keyed by the whole composite key.
        FusedEqualIndex<UniTuple<String>, UniTuple<String>> fusedEqualIndex =
                new IndexerFactory<>(equalGenderEqualAge()).buildFusedEqualIndex();
        var bucket = fusedEqualIndex.getOrCreateBucket(CompositeKey.of("F", 40));
        assertThat(fusedEqualIndex.getOrCreateBucket(CompositeKey.of("F", 40))).isSameAs(bucket); // equal-by-value ⇒ same bucket
        assertThat(fusedEqualIndex.getOrCreateBucket(CompositeKey.of("F", 30))).isNotSameAs(bucket);
        assertThat(fusedEqualIndex.getOrCreateBucket(CompositeKey.of("M", 40))).isNotSameAs(bucket);

        var right = tuple("R");
        bucket.putRight(CompositeKey.of("F", 40), right);
        assertThat(bucket.sizeRight(CompositeKey.of("F", 40))).isEqualTo(1);
        assertThat(collectRight(bucket, CompositeKey.of("F", 40))).containsExactly(right);
    }

    @Test
    void equalPlusLessThanSuffixIsPerSideAndFlipped() {
        // equal(gender) AND lessThan(age): bucket keyed by gender only; the right side carries a (flipped) age suffix.
        FusedEqualIndex<UniTuple<String>, UniTuple<String>> fusedEqualIndex =
                new IndexerFactory<>(equalGenderLessThanAge()).buildFusedEqualIndex();

        var bucket = fusedEqualIndex.getOrCreateBucket(CompositeKey.of("F", 40));
        assertThat(fusedEqualIndex.getOrCreateBucket(CompositeKey.of("F", 99))).isSameAs(bucket); // same gender ⇒ same bucket
        assertThat(fusedEqualIndex.getOrCreateBucket(CompositeKey.of("M", 40))).isNotSameAs(bucket);

        var rightAge30 = tuple("rightAge30");
        var rightAge50 = tuple("rightAge50");
        bucket.putRight(CompositeKey.of("F", 30), rightAge30);
        bucket.putRight(CompositeKey.of("F", 50), rightAge50);

        // A left tuple ("F", age) matches right tuples with right.age > left.age (left.age < right.age).
        assertThat(collectRight(bucket, CompositeKey.of("F", 40))).containsExactly(rightAge50);
        assertThat(collectRight(bucket, CompositeKey.of("F", 20))).containsExactlyInAnyOrder(rightAge30, rightAge50);
        assertThat(collectRight(bucket, CompositeKey.of("F", 60))).isEmpty();
    }

    @Test
    void hasSuffixAndIsSameBucket() {
        // equal(gender) AND lessThan(age): has a suffix ⇒ different composite keys can share a bucket (same gender).
        FusedEqualIndex<UniTuple<String>, UniTuple<String>> suffixed =
                new IndexerFactory<>(equalGenderLessThanAge()).buildFusedEqualIndex();
        assertThat(suffixed.hasSuffix()).isTrue();
        // Same equal prefix (gender), different suffix (age) ⇒ same bucket: the changed-key update can reuse it.
        assertThat(suffixed.isSameBucket(CompositeKey.of("F", 30), CompositeKey.of("F", 50))).isTrue();
        // Different equal prefix ⇒ different bucket.
        assertThat(suffixed.isSameBucket(CompositeKey.of("F", 30), CompositeKey.of("M", 30))).isFalse();

        // Pure equal: no suffix ⇒ the node never reuses (a changed key is always a different bucket).
        FusedEqualIndex<UniTuple<String>, UniTuple<String>> pureEqual =
                new IndexerFactory<>(equalGender()).buildFusedEqualIndex();
        assertThat(pureEqual.hasSuffix()).isFalse();
    }

    @Test
    void lazyAllocation_leftOnlyKey_rightNotAllocated() {
        var rightInitCount = new AtomicInteger(0);
        var index = new FusedEqualIndex<String, String>(
                new SingleKeyUnpacker<>(), // identity: pure-equal, single-component key
                false, // hasSuffix
                LinkedListLeafIndexer::new,
                () -> {
                    rightInitCount.incrementAndGet();
                    return new LinkedListLeafIndexer<>();
                });

        // right downstream must NOT be allocated on bucket creation
        var bucket = index.getOrCreateBucket("X");
        assertThat(rightInitCount.get())
                .as("right downstream must not be allocated on bucket creation")
                .isZero();

        // still not allocated after putting a left element
        var leftEntry = bucket.putLeft("X", "leftTuple");
        assertThat(rightInitCount.get())
                .as("right downstream must not be allocated when only left is used")
                .isZero();

        // forEachRight and sizeRight must be no-ops when right was never put
        var matchCount = new AtomicInteger(0);
        bucket.forEachRight("X", ignored -> matchCount.incrementAndGet());
        assertThat(matchCount.get()).isZero();
        assertThat(bucket.sizeRight("X")).isZero();
        assertThat(rightInitCount.get())
                .as("iteration/size on uninitialised right must not allocate")
                .isZero();

        // isRemovable: left non-empty → false; after remove left → true (right is null = empty)
        assertThat(bucket.isRemovable()).isFalse();
        bucket.removeLeft("X", leftEntry);
        assertThat(bucket.isRemovable())
                .as("left empty + right null must be removable")
                .isTrue();

        // right IS allocated on first putRight
        bucket.putRight("X", "rightTuple");
        assertThat(rightInitCount.get())
                .as("right downstream must be allocated on first putRight")
                .isEqualTo(1);
    }

    private static UniTuple<String> tuple(String factA) {
        return UniTuple.of(factA, 0);
    }

    private static List<UniTuple<String>> collectRight(Bucket<UniTuple<String>, UniTuple<String>> bucket, Object key) {
        var result = new ArrayList<UniTuple<String>>();
        bucket.forEachRight(key, result::add);
        return result;
    }

    private static List<UniTuple<String>> collectLeft(Bucket<UniTuple<String>, UniTuple<String>> bucket, Object key) {
        var result = new ArrayList<UniTuple<String>>();
        bucket.forEachLeft(key, result::add);
        return result;
    }

    private static DefaultBiJoiner<TestPerson, TestPerson> equalGender() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender);
    }

    private static DefaultBiJoiner<TestPerson, TestPerson> equalGenderEqualAge() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender)
                .and(Joiners.equal(TestPerson::age));
    }

    private static DefaultBiJoiner<TestPerson, TestPerson> equalGenderLessThanAge() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender)
                .and(Joiners.lessThan(TestPerson::age));
    }

}
