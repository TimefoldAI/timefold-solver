package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.JoinerType;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.common.AbstractJoiner;
import ai.timefold.solver.core.impl.score.stream.common.bi.DefaultBiJoiner;
import ai.timefold.solver.core.impl.score.stream.common.penta.DefaultPentaJoiner;
import ai.timefold.solver.core.impl.score.stream.common.quad.DefaultQuadJoiner;
import ai.timefold.solver.core.impl.score.stream.common.tri.DefaultTriJoiner;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

/**
 * {@link Indexer Indexers} form a parent-child hierarchy,
 * each child has exactly one parent.
 * {@link ai.timefold.solver.core.impl.score.stream.bavet.common.index.NoneIndexer} is always at the bottom of the hierarchy,
 * never a parent unless it is the only indexer.
 * Parent indexers delegate to their children,
 * until they reach the ultimate {@link ai.timefold.solver.core.impl.score.stream.bavet.common.index.NoneIndexer}.
 * <p>
 * Example 1: EQUAL+LESS_THAN joiner will become EqualsIndexer -> ComparisonIndexer -> NoneIndexer.
 * <p>
 * Indexers have an id, which is the position of the indexer in the chain.
 * Top-most indexer has id 0, and the id increases as we go down the hierarchy.
 * Each {@link AbstractTuple tuple} is assigned an
 * {@link ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties} instance,
 * which determines its location in the index.
 * {@link ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties} instances are built from
 * {@link AbstractJoiner joiners}
 * using methods such as {@link #buildUniLeftMapping()} and {@link #buildRightMapping()}.
 * Each {@link ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties#toKey(int) index keyFunction} has an
 * id,
 * and this id matches the id of the indexer;
 * each keyFunction in {@link ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties} is associated with a
 * single indexer.
 * <p>
 * Comparison joiners result in a single indexer each,
 * whereas equal joiners will be merged into a single indexer if they are consecutive.
 * In the latter case,
 * a composite keyFunction is created of type {@link Pair}, {@link TriTuple},
 * {@link Quadruple} or {@link ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerKey},
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
public class IndexerFactory<Right_> {

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

    public <A> Function<A, ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties> buildUniLeftMapping() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultBiJoiner<A, Right_>) joiner;
        return switch (joinerCount) {
            case 0 -> a -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = castJoiner.getLeftMapping(0);
                yield a -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                        mapping.apply(a));
            }
            default -> {
                var startIndexInclusive = 0;
                var keyFunctionList = new ArrayList<Function<A, Object>>();
                for (var entry : joinerTypeMap.entrySet()) {
                    var endIndexExclusive = entry.getKey();
                    var keyFunctionLength = endIndexExclusive - startIndexInclusive;
                    // Consecutive EQUAL joiners are merged into a single composite keyFunction.
                    Function<A, Object> keyFunction = switch (keyFunctionLength) {
                        case 1 -> castJoiner.getLeftMapping(startIndexInclusive);
                        case 2 -> {
                            var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                            var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                            yield a -> new Pair<>(mapping1.apply(a), mapping2.apply(a));
                        }
                        case 3 -> {
                            var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                            var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                            var mapping3 = castJoiner.getLeftMapping(startIndexInclusive + 2);
                            yield a -> new Triple<>(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a));
                        }
                        case 4 -> {
                            var mapping1 = castJoiner.getLeftMapping(startIndexInclusive);
                            var mapping2 = castJoiner.getLeftMapping(startIndexInclusive + 1);
                            var mapping3 = castJoiner.getLeftMapping(startIndexInclusive + 2);
                            var mapping4 = castJoiner.getLeftMapping(startIndexInclusive + 3);
                            yield a -> new Quadruple<>(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a),
                                    mapping4.apply(a));
                        }
                        default -> {
                            Function<A, Object>[] mappings = new Function[joinerCount];
                            for (var i = 0; i < joinerCount; i++) {
                                var mapping = castJoiner.getLeftMapping(i);
                                mappings[i] = mapping;
                            }
                            yield a -> {
                                int mappingCount = mappings.length;
                                var result = new Object[mappingCount];
                                for (int i = 0; i < mappingCount; i++) {
                                    result[i] = mappings[i].apply(a);
                                }
                                return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerKey(result);
                            };
                        }
                    };
                    keyFunctionList.add(keyFunction);
                    startIndexInclusive = endIndexExclusive;
                }
                int keyFunctionCount = keyFunctionList.size();
                switch (keyFunctionCount) {
                    case 1 -> {
                        var keyFunction = keyFunctionList.get(0);
                        yield a -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                                keyFunction.apply(a));
                    }
                    case 2 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        yield a -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.TwoIndexProperties<>(
                                keyFunction1.apply(a), keyFunction2.apply(a));
                    }
                    case 3 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        var keyFunction3 = keyFunctionList.get(2);
                        yield a -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ThreeIndexProperties<>(
                                keyFunction1.apply(a), keyFunction2.apply(a),
                                keyFunction3.apply(a));
                    }
                    default -> {
                        yield a -> {
                            Object[] arr = new Object[keyFunctionCount];
                            for (int i = 0; i < keyFunctionCount; i++) {
                                arr[i] = keyFunctionList.get(i).apply(a);
                            }
                            return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ManyIndexProperties(arr);
                        };
                    }
                }
            }
        };
    }

    public <A, B> BiFunction<A, B, ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties>
            buildBiLeftMapping() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultTriJoiner<A, B, Right_>) joiner;
        return switch (joinerCount) {
            case 0 -> (a, b) -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = castJoiner.getLeftMapping(0);
                yield (a, b) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                        mapping.apply(a, b));
            }
            default -> {
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
                                int mappingCount = mappings.length;
                                var result = new Object[mappingCount];
                                for (int i = 0; i < mappingCount; i++) {
                                    result[i] = mappings[i].apply(a, b);
                                }
                                return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerKey(result);
                            };
                        }
                    };
                    keyFunctionList.add(keyFunction);
                    startIndexInclusive = endIndexExclusive;
                }
                int keyFunctionCount = keyFunctionList.size();
                switch (keyFunctionCount) {
                    case 1 -> {
                        var keyFunction = keyFunctionList.get(0);
                        yield (a,
                                b) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                                        keyFunction.apply(a, b));
                    }
                    case 2 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        yield (a, b) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.TwoIndexProperties<>(
                                keyFunction1.apply(a, b), keyFunction2.apply(a, b));
                    }
                    case 3 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        var keyFunction3 = keyFunctionList.get(2);
                        yield (a, b) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ThreeIndexProperties<>(
                                keyFunction1.apply(a, b), keyFunction2.apply(a, b),
                                keyFunction3.apply(a, b));
                    }
                    default -> {
                        yield (a, b) -> {
                            Object[] arr = new Object[keyFunctionCount];
                            for (int i = 0; i < keyFunctionCount; i++) {
                                arr[i] = keyFunctionList.get(i).apply(a, b);
                            }
                            return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ManyIndexProperties(arr);
                        };
                    }
                }
            }
        };
    }

    public <A, B, C> TriFunction<A, B, C, ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties>
            buildTriLeftMapping() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultQuadJoiner<A, B, C, Right_>) joiner;
        return switch (joinerCount) {
            case 0 -> (a, b, c) -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = castJoiner.getLeftMapping(0);
                yield (a, b, c) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                        mapping.apply(a, b, c));
            }
            default -> {
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
                                int mappingCount = mappings.length;
                                var result = new Object[mappingCount];
                                for (int i = 0; i < mappingCount; i++) {
                                    result[i] = mappings[i].apply(a, b, c);
                                }
                                return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerKey(result);
                            };
                        }
                    };
                    keyFunctionList.add(keyFunction);
                    startIndexInclusive = endIndexExclusive;
                }
                int keyFunctionCount = keyFunctionList.size();
                switch (keyFunctionCount) {
                    case 1 -> {
                        var keyFunction = keyFunctionList.get(0);
                        yield (a, b,
                                c) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                                        keyFunction.apply(a, b, c));
                    }
                    case 2 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        yield (a, b,
                                c) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.TwoIndexProperties<>(
                                        keyFunction1.apply(a, b, c), keyFunction2.apply(a, b, c));
                    }
                    case 3 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        var keyFunction3 = keyFunctionList.get(2);
                        yield (a, b,
                                c) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ThreeIndexProperties<>(
                                        keyFunction1.apply(a, b, c), keyFunction2.apply(a, b, c),
                                        keyFunction3.apply(a, b, c));
                    }
                    default -> {
                        yield (a, b, c) -> {
                            Object[] arr = new Object[keyFunctionCount];
                            for (int i = 0; i < keyFunctionCount; i++) {
                                arr[i] = keyFunctionList.get(i).apply(a, b, c);
                            }
                            return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ManyIndexProperties(arr);
                        };
                    }
                }
            }
        };
    }

    public <A, B, C, D> QuadFunction<A, B, C, D, ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties>
            buildQuadLeftMapping() {
        var joinerCount = joiner.getJoinerCount();
        var castJoiner = (DefaultPentaJoiner<A, B, C, D, Right_>) joiner;
        return switch (joinerCount) {
            case 0 -> (a, b, c, d) -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = castJoiner.getLeftMapping(0);
                yield (a, b, c, d) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                        mapping.apply(a, b, c, d));
            }
            default -> {
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
                                int mappingCount = mappings.length;
                                var result = new Object[mappingCount];
                                for (int i = 0; i < mappingCount; i++) {
                                    result[i] = mappings[i].apply(a, b, c, d);
                                }
                                return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerKey(result);
                            };
                        }
                    };
                    keyFunctionList.add(keyFunction);
                    startIndexInclusive = endIndexExclusive;
                }
                int keyFunctionCount = keyFunctionList.size();
                switch (keyFunctionList.size()) {
                    case 1 -> {
                        var keyFunction = keyFunctionList.get(0);
                        yield (a, b, c,
                                d) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                                        keyFunction.apply(a, b, c, d));
                    }
                    case 2 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        yield (a, b, c,
                                d) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.TwoIndexProperties<>(
                                        keyFunction1.apply(a, b, c, d),
                                        keyFunction2.apply(a, b, c, d));
                    }
                    case 3 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        var keyFunction3 = keyFunctionList.get(2);
                        yield (a, b, c,
                                d) -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ThreeIndexProperties<>(
                                        keyFunction1.apply(a, b, c, d),
                                        keyFunction2.apply(a, b, c, d),
                                        keyFunction3.apply(a, b, c, d));
                    }
                    default -> {
                        yield (a, b, c, d) -> {
                            Object[] arr = new Object[keyFunctionCount];
                            for (int i = 0; i < keyFunctionCount; i++) {
                                arr[i] = keyFunctionList.get(i).apply(a, b, c, d);
                            }
                            return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ManyIndexProperties(arr);
                        };
                    }
                }
            }
        };
    }

    public Function<Right_, ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties> buildRightMapping() {
        var joinerCount = joiner.getJoinerCount();
        return switch (joinerCount) {
            case 0 -> a -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = joiner.getRightMapping(0);
                yield a -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                        mapping.apply(a));
            }
            default -> {
                var startIndexInclusive = 0;
                var keyFunctionList = new ArrayList<Function<Right_, Object>>();
                for (var entry : joinerTypeMap.entrySet()) {
                    var endIndexExclusive = entry.getKey();
                    var keyFunctionLength = endIndexExclusive - startIndexInclusive;
                    // Consecutive EQUAL joiners are merged into a single composite keyFunction.
                    Function<Right_, Object> keyFunction = switch (keyFunctionLength) {
                        case 1 -> joiner.getRightMapping(startIndexInclusive);
                        case 2 -> {
                            var mapping1 = joiner.getRightMapping(startIndexInclusive);
                            var mapping2 = joiner.getRightMapping(startIndexInclusive + 1);
                            yield a -> new Pair<>(mapping1.apply(a), mapping2.apply(a));
                        }
                        case 3 -> {
                            var mapping1 = joiner.getRightMapping(startIndexInclusive);
                            var mapping2 = joiner.getRightMapping(startIndexInclusive + 1);
                            var mapping3 = joiner.getRightMapping(startIndexInclusive + 2);
                            yield a -> new Triple<>(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a));
                        }
                        case 4 -> {
                            var mapping1 = joiner.getRightMapping(startIndexInclusive);
                            var mapping2 = joiner.getRightMapping(startIndexInclusive + 1);
                            var mapping3 = joiner.getRightMapping(startIndexInclusive + 2);
                            var mapping4 = joiner.getRightMapping(startIndexInclusive + 3);
                            yield a -> new Quadruple<>(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a),
                                    mapping4.apply(a));
                        }
                        default -> {
                            Function<Right_, Object>[] mappings = new Function[joinerCount];
                            for (var i = 0; i < joinerCount; i++) {
                                var mapping = joiner.getRightMapping(i);
                                mappings[i] = mapping;
                            }
                            yield a -> {
                                int mappingCount = mappings.length;
                                var result = new Object[mappingCount];
                                for (int i = 0; i < mappingCount; i++) {
                                    result[i] = mappings[i].apply(a);
                                }
                                return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerKey(result);
                            };
                        }
                    };
                    keyFunctionList.add(keyFunction);
                    startIndexInclusive = endIndexExclusive;
                }
                int keyFunctionCount = keyFunctionList.size();
                switch (keyFunctionCount) {
                    case 1 -> {
                        var keyFunction = keyFunctionList.get(0);
                        yield a -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.SingleIndexProperties<>(
                                keyFunction.apply(a));
                    }
                    case 2 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        yield a -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.TwoIndexProperties<>(
                                keyFunction1.apply(a), keyFunction2.apply(a));
                    }
                    case 3 -> {
                        var keyFunction1 = keyFunctionList.get(0);
                        var keyFunction2 = keyFunctionList.get(1);
                        var keyFunction3 = keyFunctionList.get(2);
                        yield a -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ThreeIndexProperties<>(
                                keyFunction1.apply(a), keyFunction2.apply(a),
                                keyFunction3.apply(a));
                    }
                    default -> {
                        yield a -> {
                            Object[] arr = new Object[keyFunctionCount];
                            for (int i = 0; i < keyFunctionCount; i++) {
                                arr[i] = keyFunctionList.get(i).apply(a);
                            }
                            return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ManyIndexProperties(arr);
                        };
                    }
                }
            }
        };
    }

    public <T> ai.timefold.solver.core.impl.score.stream.bavet.common.index.Indexer<T> buildIndexer(boolean isLeftBridge) {
        /*
         * Note that if creating indexer for a right bridge node, the joiner type has to be flipped.
         * (<A, B> becomes <B, A>.)
         */
        if (!hasJoiners()) { // NoneJoiner results in NoneIndexer.
            return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.NoneIndexer<>();
        } else if (joiner.getJoinerCount() == 1) { // Single joiner maps directly to EqualsIndexer or ComparisonIndexer.
            var joinerType = joiner.getJoinerType(0);
            if (joinerType == JoinerType.EQUAL) {
                return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.EqualsIndexer<>(NoneIndexer::new);
            } else {
                return new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ComparisonIndexer<>(
                        isLeftBridge ? joinerType : joinerType.flip(), NoneIndexer::new);
            }
        }
        // The following code builds the children first, so it needs to iterate over the joiners in reverse order.
        var descendingJoinerTypeMap = joinerTypeMap.descendingMap();
        Supplier<ai.timefold.solver.core.impl.score.stream.bavet.common.index.Indexer<T>> downstreamIndexerSupplier =
                NoneIndexer::new;
        var indexPropertyId = descendingJoinerTypeMap.size() - 1;
        for (var entry : descendingJoinerTypeMap.entrySet()) {
            var joinerType = entry.getValue();
            var actualDownstreamIndexerSupplier = downstreamIndexerSupplier;
            var effectivelyFinalIndexPropertyId = indexPropertyId;
            if (joinerType == JoinerType.EQUAL) {
                downstreamIndexerSupplier =
                        () -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.EqualsIndexer<>(
                                effectivelyFinalIndexPropertyId, actualDownstreamIndexerSupplier);
            } else {
                var actualJoinerType = isLeftBridge ? joinerType : joinerType.flip();
                downstreamIndexerSupplier =
                        () -> new ai.timefold.solver.core.impl.score.stream.bavet.common.index.ComparisonIndexer<>(
                                actualJoinerType, effectivelyFinalIndexPropertyId,
                                actualDownstreamIndexerSupplier);
            }
            indexPropertyId--;
        }
        return downstreamIndexerSupplier.get();
    }

}
