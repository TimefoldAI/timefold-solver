package ai.timefold.solver.constraint.streams.bavet.common.index;

import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.common.AbstractJoiner;
import ai.timefold.solver.constraint.streams.common.bi.DefaultBiJoiner;
import ai.timefold.solver.constraint.streams.common.penta.DefaultPentaJoiner;
import ai.timefold.solver.constraint.streams.common.quad.DefaultQuadJoiner;
import ai.timefold.solver.constraint.streams.common.tri.DefaultTriJoiner;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;

public final class JoinerUtils {

    private JoinerUtils() {

    }

    public static <A> Function<A, IndexProperties> combineLeftMappings(DefaultBiJoiner<A, ?> joiner) {
        var joinerCount = joiner.getJoinerCount();
        return switch (joinerCount) {
            case 0 -> a -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                Function<A, Object> mapping = joiner.getLeftMapping(0);
                yield a -> new SingleIndexProperties(mapping.apply(a));
            }
            case 2 -> {
                var mapping1 = joiner.getLeftMapping(0);
                var mapping2 = joiner.getLeftMapping(1);
                yield a -> new TwoIndexProperties(mapping1.apply(a), mapping2.apply(a));
            }
            case 3 -> {
                var mapping1 = joiner.getLeftMapping(0);
                var mapping2 = joiner.getLeftMapping(1);
                var mapping3 = joiner.getLeftMapping(2);
                yield a -> new ThreeIndexProperties(mapping1.apply(a), mapping2.apply(a), mapping3.apply(a));
            }
            default -> a -> {
                var mappings = new Object[joinerCount];
                for (var i = 0; i < joinerCount; i++) {
                    mappings[i] = joiner.getLeftMapping(i).apply(a);
                }
                return new ManyIndexProperties(mappings);
            };
        };
    }

    public static <A, B> BiFunction<A, B, IndexProperties> combineLeftMappings(DefaultTriJoiner<A, B, ?> joiner) {
        var joinerCount = joiner.getJoinerCount();
        return switch (joinerCount) {
            case 0 -> (a, b) -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = joiner.getLeftMapping(0);
                yield (a, b) -> new SingleIndexProperties(mapping.apply(a, b));
            }
            case 2 -> {
                var mapping1 = joiner.getLeftMapping(0);
                var mapping2 = joiner.getLeftMapping(1);
                yield (a, b) -> new TwoIndexProperties(mapping1.apply(a, b), mapping2.apply(a, b));
            }
            case 3 -> {
                var mapping1 = joiner.getLeftMapping(0);
                var mapping2 = joiner.getLeftMapping(1);
                var mapping3 = joiner.getLeftMapping(2);
                yield (a, b) -> new ThreeIndexProperties(mapping1.apply(a, b), mapping2.apply(a, b), mapping3.apply(a, b));
            }
            default -> (a, b) -> {
                var mappings = new Object[joinerCount];
                for (var i = 0; i < joinerCount; i++) {
                    mappings[i] = joiner.getLeftMapping(i).apply(a, b);
                }
                return new ManyIndexProperties(mappings);
            };
        };
    }

    public static <A, B, C> TriFunction<A, B, C, IndexProperties> combineLeftMappings(DefaultQuadJoiner<A, B, C, ?> joiner) {
        var joinerCount = joiner.getJoinerCount();
        return switch (joinerCount) {
            case 0 -> (a, b, c) -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = joiner.getLeftMapping(0);
                yield (a, b, c) -> new SingleIndexProperties(mapping.apply(a, b, c));
            }
            case 2 -> {
                var mapping1 = joiner.getLeftMapping(0);
                var mapping2 = joiner.getLeftMapping(1);
                yield (a, b, c) -> new TwoIndexProperties(mapping1.apply(a, b, c), mapping2.apply(a, b, c));
            }
            case 3 -> {
                var mapping1 = joiner.getLeftMapping(0);
                var mapping2 = joiner.getLeftMapping(1);
                var mapping3 = joiner.getLeftMapping(2);
                yield (a, b, c) -> new ThreeIndexProperties(mapping1.apply(a, b, c), mapping2.apply(a, b, c),
                        mapping3.apply(a, b, c));
            }
            default -> (a, b, c) -> {
                var mappings = new Object[joinerCount];
                for (var i = 0; i < joinerCount; i++) {
                    mappings[i] = joiner.getLeftMapping(i).apply(a, b, c);
                }
                return new ManyIndexProperties(mappings);
            };
        };
    }

    public static <A, B, C, D> QuadFunction<A, B, C, D, IndexProperties>
            combineLeftMappings(DefaultPentaJoiner<A, B, C, D, ?> joiner) {
        var joinerCount = joiner.getJoinerCount();
        return switch (joinerCount) {
            case 0 -> (a, b, c, d) -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = joiner.getLeftMapping(0);
                yield (a, b, c, d) -> new SingleIndexProperties(mapping.apply(a, b, c, d));
            }
            case 2 -> {
                var mapping1 = joiner.getLeftMapping(0);
                var mapping2 = joiner.getLeftMapping(1);
                yield (a, b, c, d) -> new TwoIndexProperties(mapping1.apply(a, b, c, d), mapping2.apply(a, b, c, d));
            }
            case 3 -> {
                var mapping1 = joiner.getLeftMapping(0);
                var mapping2 = joiner.getLeftMapping(1);
                var mapping3 = joiner.getLeftMapping(2);
                yield (a, b, c, d) -> new ThreeIndexProperties(mapping1.apply(a, b, c, d), mapping2.apply(a, b, c, d),
                        mapping3.apply(a, b, c, d));
            }
            default -> (a, b, c, d) -> {
                var mappings = new Object[joinerCount];
                for (var i = 0; i < joinerCount; i++) {
                    mappings[i] = joiner.getLeftMapping(i).apply(a, b, c, d);
                }
                return new ManyIndexProperties(mappings);
            };
        };
    }

    public static <Right_> Function<Right_, IndexProperties> combineRightMappings(AbstractJoiner<Right_> joiner) {
        var joinerCount = joiner.getJoinerCount();
        return switch (joinerCount) {
            case 0 -> right -> NoneIndexProperties.INSTANCE;
            case 1 -> {
                var mapping = joiner.getRightMapping(0);
                yield right -> new SingleIndexProperties(mapping.apply(right));
            }
            case 2 -> {
                var mapping1 = joiner.getRightMapping(0);
                var mapping2 = joiner.getRightMapping(1);
                yield right -> new TwoIndexProperties(mapping1.apply(right), mapping2.apply(right));
            }
            case 3 -> {
                var mapping1 = joiner.getRightMapping(0);
                var mapping2 = joiner.getRightMapping(1);
                var mapping3 = joiner.getRightMapping(2);
                yield right -> new ThreeIndexProperties(mapping1.apply(right), mapping2.apply(right), mapping3.apply(right));
            }
            default -> right -> {
                var mappings = new Object[joinerCount];
                for (var i = 0; i < joinerCount; i++) {
                    mappings[i] = joiner.getRightMapping(i).apply(right);
                }
                return new ManyIndexProperties(mappings);
            };
        };
    }
}
