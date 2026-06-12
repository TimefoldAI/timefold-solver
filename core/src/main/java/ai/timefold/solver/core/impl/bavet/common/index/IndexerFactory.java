package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.AbstractJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.penta.joiner.DefaultPentaJoiner;
import ai.timefold.solver.core.impl.bavet.quad.joiner.DefaultQuadJoiner;
import ai.timefold.solver.core.impl.bavet.tri.joiner.DefaultTriJoiner;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.DefaultBiNeighborhoodsJoiner;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

/**
 * {@link Indexer Indexers} form a parent-child hierarchy,
 * each child has exactly one parent.
 * {@link LeafIndexer} is always at the bottom of the hierarchy,
 * never a parent unless it is the only indexer.
 * Parent indexers delegate to their children,
 * until they reach the ultimate {@link LeafIndexer}.
 * <p>
 * Example 1: EQUAL+LESS_THAN joiner will become EqualIndexer -> ComparisonIndexer -> NoneIndexer.
 * <p>
 * Indexers have an id, which is the position of the indexer in the chain.
 * Top-most indexer has id 0, and the id increases as we go down the hierarchy.
 * Each {@link Tuple tuple} is assigned an
 * {@link CompositeKey} instance,
 * which determines its location in the index.
 * {@link CompositeKey} instances are built from
 * {@link AbstractJoiner joiners}
 * using methods such as {@link #buildUniLeftKeysExtractor()} and {@link #buildRightKeysExtractor()}.
 * Each {@link CompositeKey#get(int) index keyFunction} has an
 * id,
 * and this id matches the id of the indexer;
 * each keyFunction in {@link CompositeKey} is associated with a
 * single indexer.
 * <p>
 * Comparison joiners result in a single indexer each.
 * The comber reorders joiners equal-first, so all equal joiners form a single leading run;
 * that run is merged into a single (top-most) indexer.
 * In that case,
 * a composite keyFunction is created of type {@link Pair}, {@link Triple},
 * {@link Quadruple} or {@link IndexerKey},
 * based on the length of the equal prefix (number of leading equal joiners).
 *
 * <ul>
 * <li>Example 2: For an EQUAL+LESS_THAN joiner,
 * there are two indexers in the chain with keyFunction length of 1 each.</li>
 * <li>Example 3: For an EQUAL+EQUAL+LESS_THAN joiner,
 * there are still two indexers,
 * but the first (equal) indexer's keyFunction length is 2.</li>
 * <li>Example 4: For an EQUAL+EQUAL+LESS_THAN+GREATER_THAN joiner,
 * there are three indexers in the chain,
 * and the first (equal) one's keyFunction length is 2.</li>
 * </ul>
 *
 * @param <Right_>
 */
public final class IndexerFactory<Right_> {

    private final AbstractJoiner<Right_> joiner;
    private final boolean requiresRandomAccess; // Neighborhoods with enumerating joiners require random access.
    /**
     * The number of leading {@link JoinerType#EQUAL} joiners. The comber reorders joiners equal-first
     * (see {@code reorderedEqualsFirst()}), so all equal joiners form a single run at the front; this is
     * its length (0 if the joiner starts with a non-equal joiner). The equal run is merged into one indexer
     * level (the composite key); every remaining joiner becomes its own single-key level.
     */
    private final int equalPrefixLength;

    public IndexerFactory(AbstractJoiner<Right_> joiner) {
        this.joiner = joiner;
        // TODO Code encapsulation: remove the field requiresRandomAccess and call joiner.requireRandomAccess() instead?
        // TODO It also impacts the flip(). Is requiresRandomAccess a good name?
        this.requiresRandomAccess = joiner instanceof DefaultBiNeighborhoodsJoiner<?, Right_>;
        var joinerCount = joiner.getJoinerCount();
        var prefix = 0;
        while (prefix < joinerCount && joiner.getJoinerType(prefix) == JoinerType.EQUAL) {
            prefix++;
        }
        this.equalPrefixLength = prefix;
    }

    /**
     * The end-exclusive boundary of each indexer level, top-to-bottom: the merged equal-prefix level
     * (length {@link #equalPrefixLength}, when {@code >= 1}) followed by one single-joiner level each.
     * Empty when the joiner has no joiners. For {@code [EQUAL, EQUAL, LESS_THAN]} this is {@code [2, 3]}
     * (one merged level spanning indices 0..1, then a single level at index 2).
     */
    private int[] levelEndIndices() {
        var joinerCount = joiner.getJoinerCount();
        if (joinerCount == 0) {
            return new int[0];
        }
        var firstEndExclusive = equalPrefixLength == 0 ? 1 : equalPrefixLength;
        var levelCount = joinerCount - firstEndExclusive + 1;
        var endIndices = new int[levelCount];
        for (var i = 0; i < levelCount; i++) {
            endIndices[i] = firstEndExclusive + i;
        }
        return endIndices;
    }

    public AbstractJoiner<Right_> getJoiner() {
        return joiner;
    }

    public boolean hasJoiners() {
        return joiner.getJoinerCount() > 0;
    }

    public <A> UniKeysExtractor<A> buildUniLeftKeysExtractor() {
        return buildUniKeysExtractor(getMappingExtractor());
    }

    private <A> IntFunction<Function<A, Object>> getMappingExtractor() {
        if (joiner instanceof DefaultBiJoiner<?, Right_> castJoiner) {
            return i -> (Function<A, Object>) castJoiner.getLeftMapping(i);
        } else if (joiner instanceof DefaultBiNeighborhoodsJoiner<?, Right_> castJoiner) {
            return i -> (Function<A, Object>) castJoiner.getLeftMapping(i);
        } else {
            throw new IllegalStateException("Impossible state: The joiner (%s) is neither %s nor %s."
                    .formatted(joiner.getClass(), DefaultBiJoiner.class, DefaultBiNeighborhoodsJoiner.class));
        }
    }

    @SuppressWarnings("unchecked")
    private <A> UniKeysExtractor<A> buildUniKeysExtractor(IntFunction<Function<A, Object>> mappingExtractor) {
        var joinerCount = joiner.getJoinerCount();
        if (joinerCount == 0) {
            return tuple -> CompositeKey.none();
        } else if (joinerCount == 1) {
            return toKeysExtractor(mappingExtractor.apply(0));
        }
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<Function<A, Object>>();
        for (var endIndexExclusive : levelEndIndices()) {
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            var levelStart = startIndexInclusive;
            // The leading EQUAL run is merged into a single composite keyFunction; every other level has length 1.
            Function<A, Object> keyFunction = switch (keyFunctionLength) {
                case 1 -> mappingExtractor.apply(levelStart);
                case 2 -> {
                    var mapping1 = mappingExtractor.apply(levelStart);
                    var mapping2 = mappingExtractor.apply(levelStart + 1);
                    yield a -> new Pair<>(mapping1.apply(a), mapping2.apply(a));
                }
                case 3 -> {
                    var mapping1 = mappingExtractor.apply(levelStart);
                    var mapping2 = mappingExtractor.apply(levelStart + 1);
                    var mapping3 = mappingExtractor.apply(levelStart + 2);
                    yield a -> new Triple<>(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a));
                }
                case 4 -> {
                    var mapping1 = mappingExtractor.apply(levelStart);
                    var mapping2 = mappingExtractor.apply(levelStart + 1);
                    var mapping3 = mappingExtractor.apply(levelStart + 2);
                    var mapping4 = mappingExtractor.apply(levelStart + 3);
                    yield a -> new Quadruple<>(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a), mapping4.apply(a));
                }
                default -> {
                    Function<A, Object>[] mappings = new Function[keyFunctionLength];
                    for (var i = 0; i < keyFunctionLength; i++) {
                        mappings[i] = mappingExtractor.apply(levelStart + i);
                    }
                    yield toCompositeKeyFunction(mappings);
                }
            };
            keyFunctionList.add(keyFunction);
            startIndexInclusive = endIndexExclusive;
        }
        return toKeysExtractor(keyFunctionList);
    }

    @SafeVarargs
    private static <A> Function<A, Object> toCompositeKeyFunction(Function<A, Object>... mappings) {
        return a -> {
            var mappingCount = mappings.length;
            var result = new Object[mappingCount];
            for (var i = 0; i < mappingCount; i++) {
                result[i] = mappings[i].apply(a);
            }
            return new IndexerKey(result);
        };
    }

    private static <A> UniKeysExtractor<A> toKeysExtractor(Function<A, Object> keyFunction) {
        return tuple -> CompositeKey.of(keyFunction.apply(tuple.getA()));
    }

    private static <A> UniKeysExtractor<A> toKeysExtractor(List<Function<A, Object>> keyFunctionList) {
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionCount) {
            case 1 -> toKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield tuple -> {
                    var a = tuple.getA();
                    return CompositeKey.of(keyFunction1.apply(a), keyFunction2.apply(a));
                };
            }
            default -> tuple -> {
                var a = tuple.getA();
                var arr = new Object[keyFunctionCount];
                for (var i = 0; i < keyFunctionCount; i++) {
                    arr[i] = keyFunctionList.get(i).apply(a);
                }
                return CompositeKey.ofMany(arr);
            };
        };
    }

    @SuppressWarnings("unchecked")
    public <A, B> BiKeysExtractor<A, B> buildBiLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultTriJoiner<A, B, Right_>) joiner;
        if (joinerCount == 0) {
            return tuple -> CompositeKey.none();
        } else if (joinerCount == 1) {
            return toKeysExtractor(castJoiner.getLeftMapping(0));
        }
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<BiFunction<A, B, Object>>();
        for (var endIndexExclusive : levelEndIndices()) {
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            var levelStart = startIndexInclusive;
            // The leading EQUAL run is merged into a single composite keyFunction; every other level has length 1.
            BiFunction<A, B, Object> keyFunction = switch (keyFunctionLength) {
                case 1 -> castJoiner.getLeftMapping(levelStart);
                case 2 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    yield (a, b) -> new Pair<>(mapping1.apply(a, b), mapping2.apply(a, b));
                }
                case 3 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    var mapping3 = castJoiner.getLeftMapping(levelStart + 2);
                    yield (a, b) -> new Triple<>(mapping1.apply(a, b), mapping2.apply(a, b), mapping3.apply(a, b));
                }
                case 4 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    var mapping3 = castJoiner.getLeftMapping(levelStart + 2);
                    var mapping4 = castJoiner.getLeftMapping(levelStart + 3);
                    yield (a, b) -> new Quadruple<>(mapping1.apply(a, b), mapping2.apply(a, b), mapping3.apply(a, b),
                            mapping4.apply(a, b));
                }
                default -> {
                    BiFunction<A, B, Object>[] mappings = new BiFunction[keyFunctionLength];
                    for (var i = 0; i < keyFunctionLength; i++) {
                        mappings[i] = castJoiner.getLeftMapping(levelStart + i);
                    }
                    yield (a, b) -> {
                        var mappingCount = mappings.length;
                        var result = new Object[mappingCount];
                        for (var i = 0; i < mappingCount; i++) {
                            result[i] = mappings[i].apply(a, b);
                        }
                        return new IndexerKey(result);
                    };
                }
            };
            keyFunctionList.add(keyFunction);
            startIndexInclusive = endIndexExclusive;
        }
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionCount) {
            case 1 -> toKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield tuple -> {
                    var a = tuple.getA();
                    var b = tuple.getB();
                    return CompositeKey.of(keyFunction1.apply(a, b), keyFunction2.apply(a, b));
                };
            }
            default -> tuple -> {
                var a = tuple.getA();
                var b = tuple.getB();
                var arr = new Object[keyFunctionCount];
                for (var i = 0; i < keyFunctionCount; i++) {
                    arr[i] = keyFunctionList.get(i).apply(a, b);
                }
                return CompositeKey.ofMany(arr);
            };
        };
    }

    private static <A, B> BiKeysExtractor<A, B> toKeysExtractor(BiFunction<A, B, Object> keyFunction) {
        return tuple -> CompositeKey.of(keyFunction.apply(tuple.getA(), tuple.getB()));
    }

    @SuppressWarnings("unchecked")
    public <A, B, C> TriKeysExtractor<A, B, C> buildTriLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultQuadJoiner<A, B, C, Right_>) joiner;
        if (joinerCount == 0) {
            return tuple -> CompositeKey.none();
        } else if (joinerCount == 1) {
            return toKeysExtractor(castJoiner.getLeftMapping(0));
        }
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<TriFunction<A, B, C, Object>>();
        for (var endIndexExclusive : levelEndIndices()) {
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            var levelStart = startIndexInclusive;
            // The leading EQUAL run is merged into a single composite keyFunction; every other level has length 1.
            TriFunction<A, B, C, Object> keyFunction = switch (keyFunctionLength) {
                case 1 -> castJoiner.getLeftMapping(levelStart);
                case 2 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    yield (a, b, c) -> new Pair<>(mapping1.apply(a, b, c), mapping2.apply(a, b, c));
                }
                case 3 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    var mapping3 = castJoiner.getLeftMapping(levelStart + 2);
                    yield (a, b, c) -> new Triple<>(mapping1.apply(a, b, c), mapping2.apply(a, b, c), mapping3.apply(a, b, c));
                }
                case 4 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    var mapping3 = castJoiner.getLeftMapping(levelStart + 2);
                    var mapping4 = castJoiner.getLeftMapping(levelStart + 3);
                    yield (a, b, c) -> new Quadruple<>(mapping1.apply(a, b, c), mapping2.apply(a, b, c),
                            mapping3.apply(a, b, c), mapping4.apply(a, b, c));
                }
                default -> {
                    TriFunction<A, B, C, Object>[] mappings = new TriFunction[keyFunctionLength];
                    for (var i = 0; i < keyFunctionLength; i++) {
                        mappings[i] = castJoiner.getLeftMapping(levelStart + i);
                    }
                    yield (a, b, c) -> {
                        var mappingCount = mappings.length;
                        var result = new Object[mappingCount];
                        for (var i = 0; i < mappingCount; i++) {
                            result[i] = mappings[i].apply(a, b, c);
                        }
                        return new IndexerKey(result);
                    };
                }
            };
            keyFunctionList.add(keyFunction);
            startIndexInclusive = endIndexExclusive;
        }
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionCount) {
            case 1 -> toKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield tuple -> {
                    var a = tuple.getA();
                    var b = tuple.getB();
                    var c = tuple.getC();
                    return CompositeKey.of(keyFunction1.apply(a, b, c), keyFunction2.apply(a, b, c));
                };
            }
            default -> tuple -> {
                var a = tuple.getA();
                var b = tuple.getB();
                var c = tuple.getC();
                var arr = new Object[keyFunctionCount];
                for (var i = 0; i < keyFunctionCount; i++) {
                    arr[i] = keyFunctionList.get(i).apply(a, b, c);
                }
                return CompositeKey.ofMany(arr);
            };
        };
    }

    private static <A, B, C> TriKeysExtractor<A, B, C> toKeysExtractor(TriFunction<A, B, C, Object> keyFunction) {
        return tuple -> CompositeKey.of(keyFunction.apply(tuple.getA(), tuple.getB(), tuple.getC()));
    }

    @SuppressWarnings("unchecked")
    public <A, B, C, D> QuadKeysExtractor<A, B, C, D> buildQuadLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultPentaJoiner<A, B, C, D, Right_>) joiner;
        if (joinerCount == 0) {
            return tuple -> CompositeKey.none();
        } else if (joinerCount == 1) {
            return toKeysExtractor(castJoiner.getLeftMapping(0));
        }
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<QuadFunction<A, B, C, D, Object>>();
        for (var endIndexExclusive : levelEndIndices()) {
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            var levelStart = startIndexInclusive;
            // The leading EQUAL run is merged into a single composite keyFunction; every other level has length 1.
            QuadFunction<A, B, C, D, Object> keyFunction = switch (keyFunctionLength) {
                case 1 -> castJoiner.getLeftMapping(levelStart);
                case 2 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    yield (a, b, c, d) -> new Pair<>(mapping1.apply(a, b, c, d), mapping2.apply(a, b, c, d));
                }
                case 3 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    var mapping3 = castJoiner.getLeftMapping(levelStart + 2);
                    yield (a, b, c, d) -> new Triple<>(mapping1.apply(a, b, c, d), mapping2.apply(a, b, c, d),
                            mapping3.apply(a, b, c, d));
                }
                case 4 -> {
                    var mapping1 = castJoiner.getLeftMapping(levelStart);
                    var mapping2 = castJoiner.getLeftMapping(levelStart + 1);
                    var mapping3 = castJoiner.getLeftMapping(levelStart + 2);
                    var mapping4 = castJoiner.getLeftMapping(levelStart + 3);
                    yield (a, b, c, d) -> new Quadruple<>(mapping1.apply(a, b, c, d), mapping2.apply(a, b, c, d),
                            mapping3.apply(a, b, c, d), mapping4.apply(a, b, c, d));
                }
                default -> {
                    QuadFunction<A, B, C, D, Object>[] mappings = new QuadFunction[keyFunctionLength];
                    for (var i = 0; i < keyFunctionLength; i++) {
                        mappings[i] = castJoiner.getLeftMapping(levelStart + i);
                    }
                    yield (a, b, c, d) -> {
                        var mappingCount = mappings.length;
                        var result = new Object[mappingCount];
                        for (var i = 0; i < mappingCount; i++) {
                            result[i] = mappings[i].apply(a, b, c, d);
                        }
                        return new IndexerKey(result);
                    };
                }
            };
            keyFunctionList.add(keyFunction);
            startIndexInclusive = endIndexExclusive;
        }
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionList.size()) {
            case 1 -> toKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield tuple -> {
                    var a = tuple.getA();
                    var b = tuple.getB();
                    var c = tuple.getC();
                    var d = tuple.getD();
                    return CompositeKey.of(keyFunction1.apply(a, b, c, d), keyFunction2.apply(a, b, c, d));
                };
            }
            default -> tuple -> {
                var a = tuple.getA();
                var b = tuple.getB();
                var c = tuple.getC();
                var d = tuple.getD();
                var arr = new Object[keyFunctionCount];
                for (var i = 0; i < keyFunctionCount; i++) {
                    arr[i] = keyFunctionList.get(i).apply(a, b, c, d);
                }
                return CompositeKey.ofMany(arr);
            };
        };
    }

    private static <A, B, C, D> QuadKeysExtractor<A, B, C, D> toKeysExtractor(QuadFunction<A, B, C, D, Object> keyFunction) {
        return tuple -> CompositeKey.of(keyFunction.apply(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD()));
    }

    public UniKeysExtractor<Right_> buildRightKeysExtractor() {
        return buildUniKeysExtractor(joiner::getRightMapping);
    }

    public <T> Indexer<T> buildIndexer(boolean isLeftBridge) {
        Supplier<Indexer<T>> backendSupplier = RandomAccessLeafIndexer::new;
        if (!hasJoiners()) { // NoneJoiner results in a bare backend (NoneIndexer).
            return backendSupplier.get();
        }
        return buildIndexerChain(isLeftBridge, 0, backendSupplier).get();
    }

    /**
     * Builds the indexer chain for levels {@code [fromLevelInclusive, last]}, children-first,
     * and returns a supplier of its top indexer.
     * Level ids (hence the {@link KeyUnpacker#composite(int)} ids)
     * are anchored to the FULL chain;
     * {@code fromLevelInclusive} only bounds where the build stops.
     * Therefore a suffix built with {@code fromLevelInclusive == 1} still uses
     * {@code KeyUnpacker.composite(1)} for its top level and never takes the
     * {@code indexPropertyId == 0} leaf-fuse / single-key shortcut
     * (that would hand it the whole composite key instead of component 1).
     * <p>
     * {@code buildIndexer(isLeftBridge)} is {@code buildIndexerChain(isLeftBridge, 0, backendSupplier)};
     * {@link #buildFusedEqualIndex()} uses {@code fromLevelInclusive == 1} to build the per-side suffix sub-chains.
     */
    private <T> Supplier<Indexer<T>> buildIndexerChain(boolean isLeftBridge, int fromLevelInclusive,
            Supplier<Indexer<T>> backendSupplier) {
        // Build children-first, so iterate the levels bottom-up (the leaf is the highest indexPropertyId).
        // A level's joiner type is the type of any joiner it spans; the merged equal prefix (level 0) is EQUAL.
        var endIndices = levelEndIndices();
        var downstreamIndexerSupplier = backendSupplier;
        for (var indexPropertyId = endIndices.length - 1; indexPropertyId >= fromLevelInclusive; indexPropertyId--) {
            var joinerType = joiner.getJoinerType(endIndices[indexPropertyId] - 1);
            if (downstreamIndexerSupplier == backendSupplier && indexPropertyId == 0) {
                // Leaf-most level whose index key equals the whole composite key: no KeyUnpacker indirection.
                if (joinerType == JoinerType.EQUAL) {
                    // Fuse the leaf-most equal indexer with its backend.
                    downstreamIndexerSupplier = () -> new EqualIndexer<>(KeyUnpacker.single(), RandomAccessLeafIndexer::new);
                } else {
                    KeyUnpacker<?> keyUnpacker = KeyUnpacker.single();
                    downstreamIndexerSupplier = () -> buildIndexerPart(isLeftBridge, joinerType, keyUnpacker, backendSupplier);
                }
            } else {
                KeyUnpacker<?> keyUnpacker = KeyUnpacker.composite(indexPropertyId);
                var actualDownstreamIndexerSupplier = downstreamIndexerSupplier;
                downstreamIndexerSupplier =
                        () -> buildIndexerPart(isLeftBridge, joinerType, keyUnpacker, actualDownstreamIndexerSupplier);
            }
        }
        return downstreamIndexerSupplier;
    }

    /**
     * Whether this join/ifExists can use a unified {@link FusedEqualIndex} instead of two parallel indexers.
     * Eligible iff the joiner has a leading EQUAL run
     * (always true iff it has any equal, since the comber reorders equal-first)
     * and random access is not required.
     */
    public boolean isFusedEqualIndexEligible() {
        return equalPrefixLength >= 1 && !requiresRandomAccess;
    }

    /**
     * Builds a unified {@link FusedEqualIndex} for an {@link #isFusedEqualIndexEligible() eligible} join/ifExists.
     * The top map is keyed by the equal prefix;
     * each bucket holds the per-side downstream for the remaining comparison/containing suffix
     * (built right-flipped on the right side),
     * or a bare tuple-list backend when the join is pure-equal.
     *
     * @param <L> the left element type (a left tuple, or {@code ExistsCounter} for ifExists)
     * @param <R> the right element type (a right {@code UniTuple})
     */
    public <L, R> FusedEqualIndex<L, R> buildFusedEqualIndex() {
        var joinerCount = joiner.getJoinerCount();
        // Pure-equal ⇒ the composite key IS the equal key (KeyUnpacker.single());
        // otherwise it is component 0.
        KeyUnpacker<Object> topEqualKeyUnpacker =
                equalPrefixLength == joinerCount ? KeyUnpacker.single() : KeyUnpacker.composite(0);
        if (equalPrefixLength == joinerCount) {
            // Pure equal: the per-side downstream is just the tuple list; the bucket is the equal-key group.
            return new FusedEqualIndex<>(topEqualKeyUnpacker, false, RandomAccessLeafIndexer::new,
                    RandomAccessLeafIndexer::new);
        } else {
            // Equal prefix + suffix: build the per-side suffix sub-chain (the right side flips comparisons).
            var leftDownstreamSupplier = this.<L> buildIndexerChain(true, 1, RandomAccessLeafIndexer::new);
            var rightDownstreamSupplier = this.<R> buildIndexerChain(false, 1, RandomAccessLeafIndexer::new);
            return new FusedEqualIndex<>(topEqualKeyUnpacker, true, leftDownstreamSupplier, rightDownstreamSupplier);
        }
    }

    private <T> Indexer<T> buildIndexerPart(boolean isLeftBridge, JoinerType joinerType, KeyUnpacker<?> keyUnpacker,
            Supplier<Indexer<T>> downstreamIndexerSupplier) {
        // Note that if creating indexer for a right bridge node, the joiner type has to be flipped.
        // (<A, B> becomes <B, A>.)
        // This does not apply if random access is required,
        // because in that case we create a right bridge only,
        // and we query it from the left.
        // TODO Does the requiresRandomAccess check make sense?
        //      Shouldn't a right bridge always flip, even if there is no left bridge?
        if (!isLeftBridge && !requiresRandomAccess) {
            joinerType = joinerType.flip();
        }
        return switch (joinerType) {
            case EQUAL -> new EqualIndexer<>(keyUnpacker, downstreamIndexerSupplier);
            case LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                new ComparisonIndexer<>(joinerType, keyUnpacker, downstreamIndexerSupplier);
            case CONTAINING -> new ContainingIndexer<>(keyUnpacker, downstreamIndexerSupplier);
            case CONTAINED_IN -> new ContainedInIndexer<>(keyUnpacker, downstreamIndexerSupplier);
            case CONTAINING_ANY_OF -> new ContainingAnyOfIndexer<>(keyUnpacker, downstreamIndexerSupplier);
        };
    }

    /**
     * Represents a function which extracts index keys from a tuple.
     *
     * @param <Tuple_>
     */
    @FunctionalInterface
    public interface KeysExtractor<Tuple_ extends Tuple> extends Function<Tuple_, Object> {
    }

    @FunctionalInterface
    public interface UniKeysExtractor<A> extends KeysExtractor<UniTuple<A>> {
    }

    @FunctionalInterface
    public interface BiKeysExtractor<A, B> extends KeysExtractor<BiTuple<A, B>> {
    }

    @FunctionalInterface
    public interface TriKeysExtractor<A, B, C> extends KeysExtractor<TriTuple<A, B, C>> {
    }

    @FunctionalInterface
    public interface QuadKeysExtractor<A, B, C, D> extends KeysExtractor<QuadTuple<A, B, C, D>> {
    }

}
