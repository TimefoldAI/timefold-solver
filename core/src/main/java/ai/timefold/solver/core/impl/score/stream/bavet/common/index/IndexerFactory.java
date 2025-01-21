package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.JoinerType;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.common.AbstractJoiner;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;

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
        return buildUniKeysExtractor(value -> UniMappingFunction.of(joiner, value));
    }

    private <A> UniKeysExtractor<A> buildUniKeysExtractor(IntFunction<UniMappingFunction<A>> mappingExtractor) {
        var joinerCount = joiner.getJoinerCount();
        if (joinerCount == 0) {
            return (tuple, oldKeys) -> IndexKeys.none();
        } else if (joinerCount == 1) {
            return buildUniKeysExtractor(new UniKeyFunction<>(mappingExtractor.apply(0)));
        }
        var keyFunctions = extractKeyFunctions(mappingExtractor, UniKeyFunction::new);
        return buildUniKeysExtractor(keyFunctions);
    }

    private <MappingFunction_, KeyFunction_ extends KeyFunction>
            List<KeyFunction_> extractKeyFunctions(IntFunction<MappingFunction_> mappingExtractor,
                    MappingToKeyFunction<MappingFunction_, KeyFunction_> constructor) {
        var joinerCount = joiner.getJoinerCount();
        var startIndexInclusive = 0;
        var keyFunctionList = new ArrayList<KeyFunction_>();
        for (var entry : joinerTypeMap.entrySet()) {
            var endIndexExclusive = entry.getKey();
            var keyFunctionLength = endIndexExclusive - startIndexInclusive;
            // Consecutive EQUAL joiners are merged into a single composite keyFunction.
            var keyFunctions = switch (keyFunctionLength) {
                case 1 -> Collections.singletonList(mappingExtractor.apply(startIndexInclusive));
                case 2 -> List.of(mappingExtractor.apply(startIndexInclusive),
                        mappingExtractor.apply(startIndexInclusive + 1));
                case 3 -> List.of(mappingExtractor.apply(startIndexInclusive),
                        mappingExtractor.apply(startIndexInclusive + 1),
                        mappingExtractor.apply(startIndexInclusive + 2));
                case 4 -> List.of(mappingExtractor.apply(startIndexInclusive),
                        mappingExtractor.apply(startIndexInclusive + 1),
                        mappingExtractor.apply(startIndexInclusive + 2),
                        mappingExtractor.apply(startIndexInclusive + 3));
                default -> {
                    var result = new ArrayList<MappingFunction_>(keyFunctionLength);
                    for (var i = 0; i < joinerCount; i++) {
                        var mapping = mappingExtractor.apply(i);
                        result.add(mapping);
                    }
                    yield result;
                }
            };
            keyFunctionList.add(constructor.apply(keyFunctionList.size(), keyFunctions));
            startIndexInclusive = endIndexExclusive;
        }
        return keyFunctionList;
    }

    @FunctionalInterface
    private interface MappingToKeyFunction<MappingFunction_, KeyFunction_ extends KeyFunction> {

        KeyFunction_ apply(int keyId, List<MappingFunction_> mappingFunctions);

    }

    public <A, B> BiKeysExtractor<A, B> buildBiLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        if (joinerCount == 0) {
            return (tuple, oldKeys) -> IndexKeys.none();
        } else if (joinerCount == 1) {
            return buildBiKeysExtractor(new BiKeyFunction<>(BiMappingFunction.of(joiner, 0)));
        }
        var keyFunctions = extractKeyFunctions(
                value -> BiMappingFunction.<A, B> of(joiner, value),
                BiKeyFunction::new);
        return buildBiKeysExtractor(keyFunctions);
    }

    public <A, B, C> TriKeysExtractor<A, B, C> buildTriLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        if (joinerCount == 0) {
            return (tuple, oldKeys) -> IndexKeys.none();
        } else if (joinerCount == 1) {
            return buildTriKeysExtractor(new TriKeyFunction<>(TriMappingFunction.of(joiner, 0)));
        }
        var keyFunctions = extractKeyFunctions(
                value -> TriMappingFunction.<A, B, C> of(joiner, value),
                TriKeyFunction::new);
        return buildTriKeysExtractor(keyFunctions);
    }

    public <A, B, C, D> QuadKeysExtractor<A, B, C, D> buildQuadLeftKeysExtractor() {
        var joinerCount = joiner.getJoinerCount();
        if (joinerCount == 0) {
            return (tuple, oldKeys) -> IndexKeys.none();
        } else if (joinerCount == 1) {
            return buildQuadKeysExtractor(new QuadKeyFunction<>(QuadMappingFunction.of(joiner, 0)));
        }
        var keyFunctions = extractKeyFunctions(
                (value) -> QuadMappingFunction.<A, B, C, D> of(joiner, value),
                QuadKeyFunction::new);
        return buildQuadKeysExtractor(keyFunctions);
    }

    public UniKeysExtractor<Right_> buildRightKeysExtractor() {
        return buildUniKeysExtractor(value -> {
            var mapping = joiner.getRightMapping(value);
            return mapping::apply;
        });
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

    private static <A> UniKeysExtractor<A> buildUniKeysExtractor(UniKeyFunction<A> keyFunction) {
        return (tuple, oldKeys) -> IndexKeys.of(keyFunction.apply(tuple.factA, oldKeys));
    }

    private static <A> UniKeysExtractor<A> buildUniKeysExtractor(List<UniKeyFunction<A>> keyFunctionList) {
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionCount) {
            case 1 -> buildUniKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield (tuple, oldKeys) -> {
                    var a = tuple.factA;
                    var key1 = keyFunction1.apply(a, oldKeys);
                    var key2 = keyFunction2.apply(a, oldKeys);
                    return IndexKeys.of(key1, key2, oldKeys);
                };
            }
            default -> (tuple, oldKeys) -> {
                var a = tuple.factA;
                var arr = new Object[keyFunctionCount];
                if (oldKeys == null) {
                    for (var i = 0; i < keyFunctionCount; i++) {
                        arr[i] = keyFunctionList.get(i).apply(a, null);
                    }
                } else {
                    var oldIndexKeys = ((ManyIndexKeys) oldKeys).properties();
                    var sameOldIndexKeys = true;
                    for (var i = 0; i < keyFunctionCount; i++) {
                        var oldIndexKey = oldIndexKeys[i];
                        var key = keyFunctionList.get(i).apply(a, oldIndexKey);
                        if (sameOldIndexKeys) {
                            sameOldIndexKeys = Objects.equals(key, oldIndexKey);
                        }
                        arr[i] = key;
                    }
                    if (sameOldIndexKeys) {
                        return oldKeys;
                    }
                }
                return IndexKeys.ofMany(arr);
            };
        };
    }

    private static <A, B> BiKeysExtractor<A, B> buildBiKeysExtractor(BiKeyFunction<A, B> keyFunction) {
        return (tuple, oldKeys) -> IndexKeys.of(keyFunction.apply(tuple.factA, tuple.factB, oldKeys));
    }

    private static <A, B> BiKeysExtractor<A, B> buildBiKeysExtractor(List<BiKeyFunction<A, B>> keyFunctionList) {
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionCount) {
            case 1 -> buildBiKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield (tuple, oldKeys) -> {
                    var a = tuple.factA;
                    var b = tuple.factB;
                    var key1 = keyFunction1.apply(a, b, oldKeys);
                    var key2 = keyFunction2.apply(a, b, oldKeys);
                    return IndexKeys.of(key1, key2, oldKeys);
                };
            }
            default -> (tuple, oldKeys) -> {
                var a = tuple.factA;
                var b = tuple.factB;
                var arr = new Object[keyFunctionCount];
                if (oldKeys == null) {
                    for (var i = 0; i < keyFunctionCount; i++) {
                        arr[i] = keyFunctionList.get(i).apply(a, b, null);
                    }
                } else {
                    var oldIndexKeys = ((ManyIndexKeys) oldKeys).properties();
                    var sameOldIndexKeys = true;
                    for (var i = 0; i < keyFunctionCount; i++) {
                        var oldIndexKey = oldIndexKeys[i];
                        var key = keyFunctionList.get(i).apply(a, b, oldIndexKey);
                        if (sameOldIndexKeys) {
                            sameOldIndexKeys = Objects.equals(key, oldIndexKey);
                        }
                        arr[i] = key;
                    }
                    if (sameOldIndexKeys) {
                        return oldKeys;
                    }
                }
                return IndexKeys.ofMany(arr);
            };
        };
    }

    private static <A, B, C> TriKeysExtractor<A, B, C> buildTriKeysExtractor(TriKeyFunction<A, B, C> keyFunction) {
        return (tuple, oldKeys) -> IndexKeys.of(keyFunction.apply(tuple.factA, tuple.factB, tuple.factC, oldKeys));
    }

    private static <A, B, C> TriKeysExtractor<A, B, C> buildTriKeysExtractor(List<TriKeyFunction<A, B, C>> keyFunctionList) {
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionCount) {
            case 1 -> buildTriKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield (tuple, oldKeys) -> {
                    var a = tuple.factA;
                    var b = tuple.factB;
                    var c = tuple.factC;
                    var key1 = keyFunction1.apply(a, b, c, oldKeys);
                    var key2 = keyFunction2.apply(a, b, c, oldKeys);
                    return IndexKeys.of(key1, key2, oldKeys);
                };
            }
            default -> (tuple, oldKeys) -> {
                var a = tuple.factA;
                var b = tuple.factB;
                var c = tuple.factC;
                var arr = new Object[keyFunctionCount];
                if (oldKeys == null) {
                    for (var i = 0; i < keyFunctionCount; i++) {
                        arr[i] = keyFunctionList.get(i).apply(a, b, c, null);
                    }
                } else {
                    var oldIndexKeys = ((ManyIndexKeys) oldKeys).properties();
                    var sameOldIndexKeys = true;
                    for (var i = 0; i < keyFunctionCount; i++) {
                        var oldIndexKey = oldIndexKeys[i];
                        var key = keyFunctionList.get(i).apply(a, b, c, oldIndexKey);
                        if (sameOldIndexKeys) {
                            sameOldIndexKeys = Objects.equals(key, oldIndexKey);
                        }
                        arr[i] = key;
                    }
                    if (sameOldIndexKeys) {
                        return oldKeys;
                    }
                }
                return IndexKeys.ofMany(arr);
            };
        };
    }

    private static <A, B, C, D> QuadKeysExtractor<A, B, C, D> buildQuadKeysExtractor(QuadKeyFunction<A, B, C, D> keyFunction) {
        return (tuple, oldKeys) -> IndexKeys.of(keyFunction.apply(tuple.factA, tuple.factB, tuple.factC, tuple.factD, oldKeys));
    }

    private static <A, B, C, D> QuadKeysExtractor<A, B, C, D>
            buildQuadKeysExtractor(List<QuadKeyFunction<A, B, C, D>> keyFunctionList) {
        var keyFunctionCount = keyFunctionList.size();
        return switch (keyFunctionCount) {
            case 1 -> buildQuadKeysExtractor(keyFunctionList.get(0));
            case 2 -> {
                var keyFunction1 = keyFunctionList.get(0);
                var keyFunction2 = keyFunctionList.get(1);
                yield (tuple, oldKeys) -> {
                    var a = tuple.factA;
                    var b = tuple.factB;
                    var c = tuple.factC;
                    var d = tuple.factD;
                    var key1 = keyFunction1.apply(a, b, c, d, oldKeys);
                    var key2 = keyFunction2.apply(a, b, c, d, oldKeys);
                    return IndexKeys.of(key1, key2, oldKeys);
                };
            }
            default -> (tuple, oldKeys) -> {
                var a = tuple.factA;
                var b = tuple.factB;
                var c = tuple.factC;
                var d = tuple.factD;
                var arr = new Object[keyFunctionCount];
                if (oldKeys == null) {
                    for (var i = 0; i < keyFunctionCount; i++) {
                        arr[i] = keyFunctionList.get(i).apply(a, b, c, d, null);
                    }
                } else {
                    var oldIndexKeys = ((ManyIndexKeys) oldKeys).properties();
                    var sameOldIndexKeys = true;
                    for (var i = 0; i < keyFunctionCount; i++) {
                        var oldIndexKey = oldIndexKeys[i];
                        var key = keyFunctionList.get(i).apply(a, b, c, d, oldIndexKey);
                        if (sameOldIndexKeys) {
                            sameOldIndexKeys = Objects.equals(key, oldIndexKey);
                        }
                        arr[i] = key;
                    }
                    if (sameOldIndexKeys) {
                        return oldKeys;
                    }
                }
                return IndexKeys.ofMany(arr);
            };
        };
    }

}
