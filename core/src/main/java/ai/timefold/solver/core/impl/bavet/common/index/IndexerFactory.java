package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.AbstractJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.penta.joiner.DefaultPentaJoiner;
import ai.timefold.solver.core.impl.bavet.quad.joiner.DefaultQuadJoiner;
import ai.timefold.solver.core.impl.bavet.tri.joiner.DefaultTriJoiner;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

/**
 * {@link Indexer Indexers} form a parent-child hierarchy,
 * each child has exactly one parent.
 * {@link NoneIndexer} is always at the bottom of the hierarchy,
 * never a parent unless it is the only indexer.
 * Parent indexers delegate to their children,
 * until they reach the ultimate {@link NoneIndexer}.
 * <p>
 * Example 1: EQUAL+LESS_THAN joiner will become EqualsIndexer -> ComparisonIndexer -> NoneIndexer.
 * <p>
 * Indexers have an id, which is the position of the indexer in the chain.
 * Top-most indexer has id 0, and the id increases as we go down the hierarchy.
 * Each {@link AbstractTuple tuple} is assigned an
 * {@link IndexKeys} instance,
 * which determines its location in the index.
 * {@link IndexKeys} instances are built from
 * {@link AbstractJoiner joiners}
 * using methods such as {@link #buildUniLeftKeysExtractor()} and {@link #buildRightKeysExtractor()}.
 * Each {@link IndexKeys#get(int) index keyFunction} has an
 * id,
 * and this id matches the id of the indexer;
 * each keyFunction in {@link IndexKeys} is associated with a
 * single indexer.
 * <p>
 * Comparison joiners result in a single indexer each,
 * whereas equal joiners will be merged into a single indexer if they are consecutive.
 * In the latter case,
 * a composite keyFunction is created of type {@link Pair}, {@link TriTuple},
 * {@link Quadruple} or {@link IndexerKey},
 * based on the length of the composite keyFunction (number of equals joiners in sequence).
 *
 * <ul>
 * <li>Example 2: For an EQUAL+LESS_THAN joiner,
 * there are two indexers in the chain with keyFunction length of 1 each.</li>
 * <li>Example 3: For an LESS_THAN+EQUAL+EQUAL joiner,
 * there are still two indexers,
 * but the second indexer's keyFunction length is 2.</li>
 * <li>Example 4: For an LESS_THAN+EQUAL+EQUAL+LESS_THAN joiner,
 * there are three indexers in the chain,
 * and the middle one's keyFunction length is 2.</li>
 * </ul>
 *
 * @param <Right_>
 */
public final class IndexerFactory<Right_> {

    private final AbstractJoiner<Right_> joiner;
    private final NavigableMap<Integer, JoinerType> joinerTypeMap;

    public IndexerFactory(AbstractJoiner<Right_> joiner) {
        this.joiner = joiner;
        var joinerCount = joiner.getJoinerCount();
        if (joinerCount < 2) {
            joinerTypeMap = null;
        } else {
            joinerTypeMap = new TreeMap<>();
            for (var i = 1; i <= joinerCount; i++) {
                var joinerType = i < joinerCount ? joiner.getJoinerType(i) : null;
                var previousJoinerType = joiner.getJoinerType(i - 1);
                if (joinerType != JoinerType.EQUAL || previousJoinerType != joinerType) {
                    /*
                     * Equal joiner is building a composite key with preceding equal joiner(s).
                     * Does not apply to joiners other than equal; for those, each indexer has its own simple key.
                     */
                    joinerTypeMap.put(i, previousJoinerType);
                }
            }
        }
    }

    public boolean hasJoiners() {
        return joiner.getJoinerCount() > 0;
    }

    public <A> UniKeysExtractor<A> buildUniLeftKeysExtractor() {
        var castJoiner = (DefaultBiJoiner<A, Right_>) joiner;
        return buildUniKeysExtractor(castJoiner::getLeftMapping);
    }

    @SuppressWarnings("unchecked")
    private <A> UniKeysExtractor<A> buildUniKeysExtractor(IntFunction<Function<A, Object>> mappingExtractor) {
        var joinerCount = joiner.getJoinerCount();
        if (joinerCount == 0) {
            return tuple -> IndexKeys.none();
        } else if (joinerCount == 1) {
            return toKeysExtractor(mappingExtractor.apply(0));
        }
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<Function<A, Object>>();
        for (var entry : joinerTypeMap.entrySet()) {
            var endIndexExclusive = entry.getKey();
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            // Consecutive EQUAL joiners are merged into a single composite keyFunction.
            Function<A, Object> keyFunction = switch (keyFunctionLength) {
                case 1 -> mappingExtractor.apply(startIndexInclusive);
                case 2 -> {
                    var mapping1 = mappingExtractor.apply(startIndexInclusive);
                    var mapping2 = mappingExtractor.apply(startIndexInclusive + 1);
                    yield a -> new Pair<>(mapping1.apply(a), mapping2.apply(a));
                }
                case 3 -> {
                    var mapping1 = mappingExtractor.apply(startIndexInclusive);
                    var mapping2 = mappingExtractor.apply(startIndexInclusive + 1);
                    var mapping3 = mappingExtractor.apply(startIndexInclusive + 2);
                    yield a -> new Triple<>(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a));
                }
                case 4 -> {
                    var mapping1 = mappingExtractor.apply(startIndexInclusive);
                    var mapping2 = mappingExtractor.apply(startIndexInclusive + 1);
                    var mapping3 = mappingExtractor.apply(startIndexInclusive + 2);
                    var mapping4 = mappingExtractor.apply(startIndexInclusive + 3);
                    yield a -> new Quadruple<>(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a),
                            mapping4.apply(a));
                }
                default -> {
                    Function<A, Object>[] mappings = new Function[joinerCount];
                    for (var i = 0; i < joinerCount; i++) {
                        var mapping = mappingExtractor.apply(i);
                        mappings[i] = mapping;
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
        return tuple -> {
            var a = tuple.factA;
            return IndexKeys.of(keyFunction.apply(a));
        };
    }

    private static <A> UniKeysExtractor<A> toKeysExtractor(List<Function<A, Object>> keyFunctionList) {
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionCount) {
            case 1 -> toKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield tuple -> {
                    var a = tuple.factA;
                    return IndexKeys.of(keyFunction1.apply(a), keyFunction2.apply(a));
                };
            }
            default -> tuple -> {
                var a = tuple.factA;
                var arr = new Object[keyFunctionCount];
                for (var i = 0; i < keyFunctionCount; i++) {
                    arr[i] = keyFunctionList.get(i).apply(a);
                }
                return IndexKeys.ofMany(arr);
            };
        };
    }

    @SuppressWarnings("unchecked")
    public <A, B> BiKeysExtractor<A, B> buildBiLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultTriJoiner<A, B, Right_>) joiner;
        if (joinerCount == 0) {
            return tuple -> IndexKeys.none();
        } else if (joinerCount == 1) {
            return toKeysExtractor(castJoiner.getLeftMapping(0));
        }
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<BiFunction<A, B, Object>>();
        for (var entry : joinerTypeMap.entrySet()) {
            var endIndexExclusive = entry.getKey();
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            // Consecutive EQUAL joiners are merged into a single composite keyFunction.
            BiFunction<A, B, Object> keyFunction = switch (keyFunctionLength) {
                case 1 -> castJoiner.getLeftMapping(startIndexInclusive);
                case 2 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    yield (a, b) -> new Pair<>(mapping1.apply(a, b), mapping2.apply(a, b));
                }
                case 3 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    var mapping3 = castJoiner.getLeftMapping(startIndexInclusive + 2);
                    yield (a, b) -> new Triple<>(mapping1.apply(a, b), mapping2.apply(a, b), mapping3.apply(a, b));
                }
                case 4 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    var mapping3 = castJoiner.getLeftMapping(startIndexInclusive + 2);
                    var mapping4 = castJoiner.getLeftMapping(startIndexInclusive + 3);
                    yield (a, b) -> new Quadruple<>(mapping1.apply(a, b), mapping2.apply(a, b), mapping3.apply(a, b),
                            mapping4.apply(a, b));
                }
                default -> {
                    BiFunction<A, B, Object>[] mappings = new BiFunction[joinerCount];
                    for (var i = 0; i < joinerCount; i++) {
                        var mapping = castJoiner.getLeftMapping(i);
                        mappings[i] = mapping;
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
                    var a = tuple.factA;
                    var b = tuple.factB;
                    return IndexKeys.of(keyFunction1.apply(a, b), keyFunction2.apply(a, b));
                };
            }
            default -> tuple -> {
                var a = tuple.factA;
                var b = tuple.factB;
                var arr = new Object[keyFunctionCount];
                for (var i = 0; i < keyFunctionCount; i++) {
                    arr[i] = keyFunctionList.get(i).apply(a, b);
                }
                return IndexKeys.ofMany(arr);
            };
        };
    }

    private static <A, B> BiKeysExtractor<A, B> toKeysExtractor(BiFunction<A, B, Object> keyFunction) {
        return tuple -> {
            var a = tuple.factA;
            var b = tuple.factB;
            return IndexKeys.of(keyFunction.apply(a, b));
        };
    }

    @SuppressWarnings("unchecked")
    public <A, B, C> TriKeysExtractor<A, B, C> buildTriLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultQuadJoiner<A, B, C, Right_>) joiner;
        if (joinerCount == 0) {
            return tuple -> IndexKeys.none();
        } else if (joinerCount == 1) {
            return toKeysExtractor(castJoiner.getLeftMapping(0));
        }
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<TriFunction<A, B, C, Object>>();
        for (var entry : joinerTypeMap.entrySet()) {
            var endIndexExclusive = entry.getKey();
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            // Consecutive EQUAL joiners are merged into a single composite keyFunction.
            TriFunction<A, B, C, Object> keyFunction = switch (keyFunctionLength) {
                case 1 -> castJoiner.getLeftMapping(startIndexInclusive);
                case 2 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    yield (a, b, c) -> new Pair<>(mapping1.apply(a, b, c), mapping2.apply(a, b, c));
                }
                case 3 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    var mapping3 = castJoiner.getLeftMapping(startIndexInclusive + 2);
                    yield (a, b, c) -> new Triple<>(mapping1.apply(a, b, c), mapping2.apply(a, b, c),
                            mapping3.apply(a, b, c));
                }
                case 4 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    var mapping3 = castJoiner.getLeftMapping(startIndexInclusive + 2);
                    var mapping4 = castJoiner.getLeftMapping(startIndexInclusive + 3);
                    yield (a, b, c) -> new Quadruple<>(mapping1.apply(a, b, c), mapping2.apply(a, b, c),
                            mapping3.apply(a, b, c), mapping4.apply(a, b, c));
                }
                default -> {
                    TriFunction<A, B, C, Object>[] mappings = new TriFunction[joinerCount];
                    for (var i = 0; i < joinerCount; i++) {
                        var mapping = castJoiner.getLeftMapping(i);
                        mappings[i] = mapping;
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
                    var a = tuple.factA;
                    var b = tuple.factB;
                    var c = tuple.factC;
                    return IndexKeys.of(keyFunction1.apply(a, b, c), keyFunction2.apply(a, b, c));
                };
            }
            default -> tuple -> {
                var a = tuple.factA;
                var b = tuple.factB;
                var c = tuple.factC;
                var arr = new Object[keyFunctionCount];
                for (var i = 0; i < keyFunctionCount; i++) {
                    arr[i] = keyFunctionList.get(i).apply(a, b, c);
                }
                return IndexKeys.ofMany(arr);
            };
        };
    }

    private static <A, B, C> TriKeysExtractor<A, B, C> toKeysExtractor(TriFunction<A, B, C, Object> keyFunction) {
        return tuple -> {
            var a = tuple.factA;
            var b = tuple.factB;
            var c = tuple.factC;
            return IndexKeys.of(keyFunction.apply(a, b, c));
        };
    }

    @SuppressWarnings("unchecked")
    public <A, B, C, D> QuadKeysExtractor<A, B, C, D> buildQuadLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultPentaJoiner<A, B, C, D, Right_>) joiner;
        if (joinerCount == 0) {
            return tuple -> IndexKeys.none();
        } else if (joinerCount == 1) {
            return toKeysExtractor(castJoiner.getLeftMapping(0));
        }
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<QuadFunction<A, B, C, D, Object>>();
        for (var entry : joinerTypeMap.entrySet()) {
            var endIndexExclusive = entry.getKey();
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            // Consecutive EQUAL joiners are merged into a single composite keyFunction.
            QuadFunction<A, B, C, D, Object> keyFunction = switch (keyFunctionLength) {
                case 1 -> castJoiner.getLeftMapping(startIndexInclusive);
                case 2 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    yield (a, b, c, d) -> new Pair<>(mapping1.apply(a, b, c, d), mapping2.apply(a, b, c, d));
                }
                case 3 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    var mapping3 = castJoiner.getLeftMapping(startIndexInclusive + 2);
                    yield (a, b, c, d) -> new Triple<>(mapping1.apply(a, b, c, d), mapping2.apply(a, b, c, d),
                            mapping3.apply(a, b, c, d));
                }
                case 4 -> {
                    var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                    var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                    var mapping3 = castJoiner.getLeftMapping(startIndexInclusive + 2);
                    var mapping4 = castJoiner.getLeftMapping(startIndexInclusive + 3);
                    yield (a, b, c, d) -> new Quadruple<>(mapping1.apply(a, b, c, d), mapping2.apply(a, b, c, d),
                            mapping3.apply(a, b, c, d), mapping4.apply(a, b, c, d));
                }
                default -> {
                    QuadFunction<A, B, C, D, Object>[] mappings = new QuadFunction[joinerCount];
                    for (var i = 0; i < joinerCount; i++) {
                        var mapping = castJoiner.getLeftMapping(i);
                        mappings[i] = mapping;
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
                    var a = tuple.factA;
                    var b = tuple.factB;
                    var c = tuple.factC;
                    var d = tuple.factD;
                    return IndexKeys.of(keyFunction1.apply(a, b, c, d), keyFunction2.apply(a, b, c, d));
                };
            }
            default -> tuple -> {
                var a = tuple.factA;
                var b = tuple.factB;
                var c = tuple.factC;
                var d = tuple.factD;
                var arr = new Object[keyFunctionCount];
                for (var i = 0; i < keyFunctionCount; i++) {
                    arr[i] = keyFunctionList.get(i).apply(a, b, c, d);
                }
                return IndexKeys.ofMany(arr);
            };
        };
    }

    private static <A, B, C, D> QuadKeysExtractor<A, B, C, D> toKeysExtractor(QuadFunction<A, B, C, D, Object> keyFunction) {
        return tuple -> {
            var a = tuple.factA;
            var b = tuple.factB;
            var c = tuple.factC;
            var d = tuple.factD;
            return IndexKeys.of(keyFunction.apply(a, b, c, d));
        };
    }

    public UniKeysExtractor<Right_> buildRightKeysExtractor() {
        return buildUniKeysExtractor(joiner::getRightMapping);
    }

    public <T> Indexer<T> buildIndexer(boolean isLeftBridge) {
        /*
         * Note that if creating indexer for a right bridge node, the joiner type has to be flipped.
         * (<A, B> becomes <B, A>.)
         */
        if (!hasJoiners()) { // NoneJoiner results in NoneIndexer.
            return new NoneIndexer<>();
        } else if (joiner.getJoinerCount() == 1) { // Single joiner maps directly to EqualsIndexer or ComparisonIndexer.
            var joinerType = joiner.getJoinerType(0);
            if (joinerType == JoinerType.EQUAL) {
                return new EqualsIndexer<>();
            } else {
                return new ComparisonIndexer<>(isLeftBridge ? joinerType : joinerType.flip());
            }
        }
        // The following code builds the children first, so it needs to iterate over the joiners in reverse order.
        var descendingJoinerTypeMap = joinerTypeMap.descendingMap();
        Supplier<Indexer<T>> noneIndexerSupplier = NoneIndexer::new;
        Supplier<Indexer<T>> downstreamIndexerSupplier = noneIndexerSupplier;
        var indexPropertyId = descendingJoinerTypeMap.size() - 1;
        for (var entry : descendingJoinerTypeMap.entrySet()) {
            var joinerType = entry.getValue();
            if (downstreamIndexerSupplier == noneIndexerSupplier && indexPropertyId == 0) {
                if (joinerType == JoinerType.EQUAL) {
                    downstreamIndexerSupplier = EqualsIndexer::new;
                } else {
                    var actualJoinerType = isLeftBridge ? joinerType : joinerType.flip();
                    downstreamIndexerSupplier = () -> new ComparisonIndexer<>(actualJoinerType);
                }
            } else {
                var actualDownstreamIndexerSupplier = downstreamIndexerSupplier;
                var effectivelyFinalIndexPropertyId = indexPropertyId;
                if (joinerType == JoinerType.EQUAL) {
                    downstreamIndexerSupplier =
                            () -> new EqualsIndexer<>(effectivelyFinalIndexPropertyId, actualDownstreamIndexerSupplier);
                } else {
                    var actualJoinerType = isLeftBridge ? joinerType : joinerType.flip();
                    downstreamIndexerSupplier = () -> new ComparisonIndexer<>(actualJoinerType, effectivelyFinalIndexPropertyId,
                            actualDownstreamIndexerSupplier);
                }
            }
            indexPropertyId--;
        }
        return downstreamIndexerSupplier.get();
    }

    /**
     * Represents a function which extracts index keys from a tuple.
     *
     * @param <Tuple_>
     */
    @FunctionalInterface
    public interface KeysExtractor<Tuple_ extends AbstractTuple> extends Function<Tuple_, Object> {
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
