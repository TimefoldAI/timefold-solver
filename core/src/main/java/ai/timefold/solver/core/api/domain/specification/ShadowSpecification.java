package ai.timefold.solver.core.api.domain.specification;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * Describes a shadow variable on an entity.
 *
 * @param <S> the solution type
 */
public sealed interface ShadowSpecification<S> {

    String name();

    Class<?> type();

    Function<?, ?> getter();

    BiConsumer<?, Object> setter();

    record InverseRelation<S>(String name, Class<?> type,
            Function<?, ?> getter, BiConsumer<?, Object> setter,
            String sourceVariableName) implements ShadowSpecification<S> {
    }

    record Index<S>(String name, Class<?> type,
            ToIntFunction<?> intGetter, ObjIntConsumer<?> intSetter,
            Function<?, ?> rawGetter, BiConsumer<?, Object> rawSetter,
            String sourceVariableName) implements ShadowSpecification<S> {

        /**
         * Convenience constructor for programmatic API — wraps int-specific accessors.
         */
        @SuppressWarnings("unchecked")
        public Index(String name, Class<?> type,
                ToIntFunction<?> intGetter, ObjIntConsumer<?> intSetter,
                String sourceVariableName) {
            this(name, type, intGetter, intSetter,
                    (Function<Object, Object>) entity -> ((ToIntFunction<Object>) intGetter).applyAsInt(entity),
                    (BiConsumer<Object, Object>) (entity, value) -> ((ObjIntConsumer<Object>) intSetter)
                            .accept(entity, value != null ? (Integer) value : -1),
                    sourceVariableName);
        }

        @Override
        public Function<?, ?> getter() {
            return rawGetter;
        }

        @Override
        public BiConsumer<?, Object> setter() {
            return rawSetter;
        }
    }

    record PreviousElement<S>(String name, Class<?> type,
            Function<?, ?> getter, BiConsumer<?, Object> setter,
            String sourceVariableName) implements ShadowSpecification<S> {
    }

    record NextElement<S>(String name, Class<?> type,
            Function<?, ?> getter, BiConsumer<?, Object> setter,
            String sourceVariableName) implements ShadowSpecification<S> {
    }

    record Declarative<S>(String name, Class<?> type,
            Function<?, ?> getter, BiConsumer<?, Object> setter,
            Function<?, ?> supplier, List<String> sourcePaths,
            String alignmentKey, Method supplierMethod) implements ShadowSpecification<S> {

        /**
         * Constructor for programmatic API (no supplierMethod).
         */
        public Declarative(String name, Class<?> type,
                Function<?, ?> getter, BiConsumer<?, Object> setter,
                Function<?, ?> supplier, List<String> sourcePaths,
                String alignmentKey) {
            this(name, type, getter, setter, supplier, sourcePaths, alignmentKey, null);
        }
    }

    record CascadingUpdate<S>(String name, Class<?> type,
            Function<?, ?> getter, BiConsumer<?, Object> setter,
            Consumer<?> updateMethod,
            List<String> sourcePaths,
            String targetMethodName) implements ShadowSpecification<S> {

        /**
         * Constructor for programmatic API (no targetMethodName).
         */
        public CascadingUpdate(String name, Class<?> type,
                Function<?, ?> getter, BiConsumer<?, Object> setter,
                Consumer<?> updateMethod,
                List<String> sourcePaths) {
            this(name, type, getter, setter, updateMethod, sourcePaths, null);
        }
    }

    record Inconsistent<S>(String name, Class<?> type,
            Function<?, ?> getter, BiConsumer<?, Object> setter) implements ShadowSpecification<S> {
    }
}
